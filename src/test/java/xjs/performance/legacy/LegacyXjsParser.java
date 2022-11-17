package xjs.performance.legacy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xjs.core.*;
import xjs.serialization.parser.AbstractJsonParser;
import xjs.serialization.util.ImplicitStringUtils;
import xjs.serialization.util.StringContext;

import java.io.*;
import java.util.Arrays;

public class LegacyXjsParser extends AbstractJsonParser {

    protected final String text;
    protected final StringBuilder commentBuffer;
    protected int valueOffset;

    public LegacyXjsParser(final File file) throws IOException {
        this(new FileReader(file));
    }

    public LegacyXjsParser(final Reader reader) throws IOException {
        this(readToEnd(reader));
    }

    public LegacyXjsParser(final String text) {
        super(new StringReader(text), text.length());
        this.text = text;
        this.commentBuffer = new StringBuilder();
        this.valueOffset = -1;
        this.resetBuffer();
    }

    protected static String readToEnd(final Reader reader) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final char[] buf = new char[8 * 1024];
        int n;

        while ((n = reader.read(buf, 0, buf.length)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    protected void resetBuffer() {
        for (int i = 0; i < this.text.length(); i++) {
            this.buffer[i] = this.text.charAt(i);
        }
    }

    public @NotNull JsonValue parse() throws IOException {
        if (ImplicitStringUtils.find(this.text, StringContext.KEY)) {
            return this.readOpenObject();
        }
        return this.readClosedRoot();
    }

    protected JsonValue readClosedRoot() throws IOException {
        this.read();
        this.skipWhitespace();
        final String header = this.takeComment(true);
        final int linesAbove = this.linesSkipped;
        final JsonValue result =
            this.readValue().setLinesAbove(linesAbove);
        this.skipWhitespace();
        this.prependLinesSkippedToComment();
        final String footer = this.takeComment(false);
        if (!this.isEndOfText()) {
            throw this.unexpected((char) this.current);
        }
        this.appendComment(result, CommentType.HEADER, header);
        this.appendComment(result, CommentType.FOOTER, footer);
        return result;
    }

    protected JsonValue readValue() throws IOException {
        switch (this.current) {
            case '[':
                return this.readArray();
            case '{':
                return this.readClosedObject();
            default:
                return readImplicit();
        }
    }

    protected JsonArray readArray() throws IOException {
        final JsonArray array = new JsonArray();
        this.read();
        this.skipWhitespace();
        do {
            this.skipWhitespace(false);
            if (this.readIf(']')) {
                return (JsonArray) this.closeContainer(array);
            }
        } while (this.readNextElement(array));
        if (!this.readIf(']')) {
            throw expected("',', ']', or newline");
        }
        return (JsonArray) this.closeContainer(array);
    }

    protected boolean readNextElement(final JsonArray array) throws IOException {
        final String header = this.takeComment(true);
        final int linesAbove = this.linesSkipped;
        this.valueOffset = this.lineOffset;

        final JsonValue value = this.readValue();
        this.appendComment(value, CommentType.HEADER, header);
        value.setLinesAbove(linesAbove);

        array.add(value);

        final boolean delimiter = this.readDelimiter();
        this.appendComment(value, CommentType.EOL, this.takeComment(true));
        return delimiter;
    }

    protected JsonObject readOpenObject() throws IOException {
        final JsonObject object = new JsonObject();
        this.read();
        this.skipWhitespace();
        this.splitOpenHeader(object);
        do {
            this.skipWhitespace(false);
            if (this.isEndOfText()) {
                break;
            }
        } while (this.readNextMember(object));
        this.appendComment(object, CommentType.FOOTER, false);
        object.setLinesTrailing(this.linesSkipped);
        if (!this.isEndOfText()) {
            throw this.unexpected("'" + (char) this.current + "' before end of file");
        }
        return object;
    }

    protected void splitOpenHeader(final JsonObject root) {
        if (this.commentBuffer.length() > 0) {
            final String header = this.commentBuffer.toString();
            final int end = this.getLastGap(header);
            if (end > 0) {
                root.getComments().setData(CommentType.HEADER, header.substring(0, end));
                root.setLinesAbove(this.linesSkipped);
                this.commentBuffer.delete(0, header.indexOf('\n', end + 1) + 1);
                this.linesSkipped = 0;
            }
        }
    }

    private int getLastGap(final String s) {
        for (int i = s.length() - 1; i > 0; i--) {
            if (s.charAt(i) != '\n') {
                continue;
            }
            while (i > 1) {
                final char next = s.charAt(--i);
                if (next == '\n') {
                    return i;
                } else if (next != ' ' && next != '\t' && next != '\r') {
                    break;
                }
            }
        }
        return -1;
    }

    protected JsonObject readClosedObject() throws IOException {
        final JsonObject object = new JsonObject();
        this.read();
        this.skipWhitespace();

        do {
            this.skipWhitespace(false);
            if (this.readIf('}')) {
                return (JsonObject) this.closeContainer(object);
            }
        } while (this.readNextMember(object));
        if (!this.readIf('}')) {
            throw this.expected("',', '}', or newline");
        }
        return (JsonObject) this.closeContainer(object);
    }

    protected boolean readNextMember(final JsonObject object) throws IOException {
        final String header = this.takeComment(true);
        final int linesAbove = this.linesSkipped;
        this.valueOffset = this.lineOffset;

        final String key = this.readKey();
        this.separateKeyValue();

        final String valueComment = this.takeComment(false);
        final int linesBetween = this.linesSkipped;

        final JsonValue value = this.readValue();
        this.appendComment(value, CommentType.HEADER, header);
        this.appendComment(value, CommentType.VALUE, valueComment);
        value.setLinesAbove(linesAbove);
        value.setLinesBetween(linesBetween);

        object.add(key, value);

        final boolean delimiter = this.readDelimiter();
        this.appendComment(value, CommentType.EOL, true);
        return delimiter;
    }

    protected String readKey() throws IOException {
        final int start = this.index - 1;
        final int end = ImplicitStringUtils.expect(this.text, start, StringContext.KEY);
        final JsonValue parsed = this.tryReadQuoted(start, end);
        if (parsed != null) {
            // Todo: introduce JsonKeys to preserve formatting
            return parsed.asString();
        }
        return this.buildImplicitText(start, end);
    }

    protected void separateKeyValue() throws IOException {
        this.skipWhitespace();
        this.expect(':');
        this.skipWhitespace();
    }

    protected JsonContainer closeContainer(final JsonContainer container) throws IOException {
        return this.closeContainer(container, true);
    }

    protected JsonContainer closeContainer(final JsonContainer container, final boolean trim) throws IOException {
        this.appendComment(container, CommentType.INTERIOR, trim);
        container.setLinesTrailing(this.linesSkipped);
        this.skipLineWhitespace();
        this.appendComment(container, CommentType.EOL, true);
        return container;
    }

    protected JsonValue readImplicit() throws IOException {
        final int start = this.index - 1;
        final int end = ImplicitStringUtils.expect(this.text, start, StringContext.VALUE);

        JsonValue parsed = this.tryReadQuoted(start, end);
        if (parsed != null) {
            return parsed;
        }
        final String value = this.buildImplicitText(start, end);
        parsed = this.tryParseValue(value);
        if (parsed != null) {
            return parsed;
        }
        return new JsonString(value, StringType.IMPLICIT);
    }

    protected String buildImplicitText(final int start, final int end) {
        final StringBuilder value = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (this.current == '\n') {
                value.append('\n');
                this.read();
                for (int j = 0; j < this.valueOffset; j++) {
                    if (this.isLineWhitespace()) {
                        i++;
                        this.read();
                    }
                }
            } else if (this.current == '\\' && this.index < this.text.length())  {
                this.read();
                i++;
                if (!this.shouldUnescape()) {
                    value.append('\\');
                }
                value.append((char) this.current);
                this.read();
            } else {
                value.append((char) this.current);
                this.read();
            }
        }
        return value.toString();
    }

    protected @Nullable JsonValue tryReadQuoted(final int start, final int end) throws IOException{
        if (this.current == '\'') {
            if (this.peek(1) == '\'' && this.peek(2) == '\'') {
                if (end - 1 == ImplicitStringUtils.expectMulti(this.text, start + 2)) {
                    return this.readMulti();
                }
            } else if (end - 1 == ImplicitStringUtils.expectQuote(this.text, start, '\'')) {
                return new JsonString(this.readQuoted('\''), StringType.SINGLE);
            }
        } else if (this.current == '"') {
            if (end - 1 == ImplicitStringUtils.expectQuote(this.text, start, '"')) {
                return new JsonString(this.readQuoted('"'), StringType.DOUBLE);
            }
        }
        return null;
    }

    protected JsonValue readMulti() throws IOException {
        this.expect('\'');
        this.expect('\'');
        this.expect('\'');

        final StringBuilder sb = new StringBuilder();
        final int offset = this.lineOffset - 3;

        this.skipLineWhitespace();
        if (this.current == '\n') {
            this.read();
            this.skipToOffset(offset);
        }

        int triple = 0;
        while (true) {
            if (this.current < 0) {
                throw this.expected("end of multiline string (''')");
            } else if (this.current == '\'') {
                triple++;
                this.read();
                if (triple == 3) {
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    return new JsonString(sb.toString(), StringType.MULTI);
                } else {
                    continue;
                }
            } else {
                while (triple > 0) {
                    sb.append('\'');
                    triple--;
                }
            }
            if (this.current == '\n') {
                sb.append('\n');
                this.read();
                this.skipToOffset(offset);
            } else {
                if (this.current != '\r') {
                    sb.append((char) this.current);
                }
                this.read();
            }
        }
    }

    protected @Nullable JsonValue tryParseValue(final String value) {
        switch (value) {
            case "true": return JsonLiteral.jsonTrue();
            case "false": return JsonLiteral.jsonFalse();
            case "null": return JsonLiteral.jsonNull();
        }
        try {
            return new JsonNumber(Double.parseDouble(value));
        } catch (final NumberFormatException ignored) {}
        return null;
    }

    @Override
    protected void skipWhitespace(boolean reset) throws IOException {
        if (reset) {
            this.linesSkipped = 0;
            this.commentBuffer.setLength(0);
        }
        while (this.isWhitespace()) {
            this.read();
        }
        final int linesAbove = this.linesSkipped;
        this.readAllComments();
        this.appendLinesSkippedToComment();
        this.linesSkipped = linesAbove;
    }

    protected void readAllComments() throws IOException {
        int last = -1;

        while (last != this.index) {
            this.linesSkipped = 0;
            while (this.isWhitespace()) {
                this.read();
            }
            last = this.index;
            this.readSingleComment();
        }
    }

    protected void readSingleComment() throws IOException {
        if (this.current == '/') {
            final int peek = this.peek();
            if (peek == '/') {
                this.readLineComment();
            } else if (peek == '*') {
                this.readInlineComment();
            }
        } else if (this.current == '#') {
            this.readLineComment();
        }
    }

    protected void readLineComment() {
        this.appendLinesSkippedToComment();
        int lastLetter = this.index;
        int i = this.index;
        while (++i < this.text.length() + 1) {
            final int peek = this.peek(i - this.index);
            if (peek == '\n') {
                break;
            } else if (peek != ' ' && peek != '\t' && peek != '\r') {
                lastLetter = i;
            }
        }
        this.commentBuffer.append(this.text, this.index - 1, lastLetter);
        this.skipTo(lastLetter + 1);
    }

    protected void readInlineComment() throws IOException {
        this.appendLinesSkippedToComment();
        final int commentOffset = this.lineOffset;
        this.expect('/');
        this.expect('*');

        this.commentBuffer.append("/*");
        while (this.current >= 0) {
            if (this.current != '\r') {
                this.commentBuffer.append((char) current);
            }
            if (this.current == '\n') {
                this.read();
                this.skipToOffset(commentOffset);
            } else if (this.current == '*' && this.peek() == '/') {
                this.read();
                this.read();
                break;
            } else {
                this.read();
            }
        }
        this.commentBuffer.append('/');
    }

    protected void appendLinesSkippedToComment() {
        for (int i = 0; i < this.linesSkipped; i++) {
            this.commentBuffer.append('\n');
        }
    }

    protected void prependLinesSkippedToComment() {
        if (this.linesSkipped > 1) {
            final char[] lines = new char[this.linesSkipped - 1];
            Arrays.fill(lines, '\n');
            this.commentBuffer.insert(0, lines);
        }
    }

    protected boolean readDelimiter() throws IOException {
        this.skipLineWhitespace();
        if (this.readIf(',')) {
            this.skipLineWhitespace();
            this.readIf('\n');
            return true;
        } else {
            return this.readIf('\n');
        }
    }

    protected void skipLineWhitespace() throws IOException {
        this.linesSkipped = 0;
        while (this.isLineWhitespace()) {
            this.read();
        }
        this.readSingleComment();
        while (this.isLineWhitespace()) {
            this.read();
        }
    }

    protected void appendComment(final JsonValue value, final CommentType type, final String data) {
        if (!data.isEmpty()) {
            value.getComments().setData(type, data);
        }
    }

    protected void appendComment(final JsonValue value, final CommentType type, final boolean trim) {
        if (this.commentBuffer.length() > 0) {
            value.getComments().setData(type, this.takeComment(trim));
        }
    }

    protected String takeComment(final boolean trim) {
        if (this.commentBuffer.length() == 0) {
            return "";
        }
        if (trim && this.commentBuffer.charAt(this.commentBuffer.length() - 1) == '\n') {
            this.commentBuffer.setLength(this.commentBuffer.length() - 1);
        }
        final String comment = this.commentBuffer.toString();
        this.commentBuffer.setLength(0);
        return comment;
    }

    protected boolean shouldUnescape() {
        return this.current == '\n'
            || this.current == '}'
            || this.current == ']'
            || this.current == ')'
            || this.current == ':'
            || this.current == ',';
    }

    protected int peek() {
        return this.peek(1);
    }

    protected int peek(final int offset) {
        if (this.index + offset <= this.text.length()) {
            return this.buffer[this.index + offset - 1];
        }
        return -1;
    }

    protected void skipTo(final int idx) {
        if (idx >= this.text.length()) {
            this.skipToEnd();
            return;
        }
        for (int i = this.index; i < idx - 1; i++) {
            if (this.buffer[i] == '\n') {
                this.line++;
                this.linesSkipped++;
                this.lineOffset = 0;
            } else {
                this.lineOffset++;
            }
        }
        this.index = idx;
        this.current = this.buffer[idx - 1];
    }

    protected void skipToEnd() {
        for (int i = this.index; i < this.text.length(); i++) {
            if (this.buffer[i] == '\n') {
                this.line++;
                this.linesSkipped++;
                this.lineOffset = 0;
            } else {
                this.lineOffset++;
            }
        }
        this.index = this.text.length();
        this.current = -1;
    }

    @Override
    protected void read() {
        if (this.current == '\n') {
            this.line++;
            this.linesSkipped++;
            this.lineOffset = 0;
        } else {
            this.lineOffset++;
        }
        final int idx = this.index++;
        if (idx < this.buffer.length) {
            this.current = this.buffer[idx];
        } else {
            this.current = -1;
        }
    }

    @Override
    protected int getColumn() {
        return this.lineOffset;
    }
}
