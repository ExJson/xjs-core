package xjs.serialization.parser;

import org.jetbrains.annotations.NotNull;
import xjs.core.CommentType;
import xjs.core.Json;
import xjs.core.JsonArray;
import xjs.core.JsonContainer;
import xjs.core.JsonLiteral;
import xjs.core.JsonObject;
import xjs.core.JsonString;
import xjs.core.JsonValue;
import xjs.core.StringType;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.NumberToken;
import xjs.serialization.token.StringToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.Token.Type;
import xjs.serialization.token.Tokenizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class XjsParser extends CommentedTokenParser {

    public XjsParser(final File file) throws IOException {
        this(Tokenizer.containerize(new FileInputStream(file)));
    }

    public XjsParser(final String text) {
        this(Tokenizer.containerize(text));
    }

    public XjsParser(final ContainerToken root) {
        super(root);
    }

    @Override
    public @NotNull JsonValue parse() {
        final ContainerToken rootContainer =
            (ContainerToken) this.root;
        if (rootContainer.lookup(':', false) != null) {
            return this.readOpenRoot();
        }
        return this.readClosedRoot();
    }

    protected JsonObject readOpenRoot() {
        final JsonObject object = new JsonObject();
        this.read();
        this.skipWhitespace();
        this.splitOpenHeader(object);
        do {
            this.skipWhitespace(false);
            if (this.isEndOfContainer()) {
                break;
            }
        } while (this.readNextMember(object));
        this.setComment(CommentType.FOOTER);
        this.setLinesTrailing();
        this.expectEndOfText();
        return this.takeFormatting(object);
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

    protected JsonValue readClosedRoot() {
        if (this.current.type() == Type.OPEN) {
            this.read();
        }
        this.skipWhitespace();
        this.setComment(CommentType.HEADER);
        this.setLinesAbove();

        final JsonValue result = this.readValue();

        this.skipWhitespace();

        this.prependLinesSkippedToComment();
        this.setComment(CommentType.FOOTER);
        this.expectEndOfText();
        return this.takeFormatting(result);
    }

    protected JsonValue readValue() {
        return this.readValue(this.current.offset());
    }

    protected JsonValue readValue(final int offset) {
        if (this.current.isSymbol(',')) {
            return new JsonString("", StringType.IMPLICIT);
        }
        switch (this.current.type()) {
            case BRACKETS:
                return this.readArray();
            case BRACES:
                return this.readObject();
            default:
                final JsonValue value = this.readImplicit(offset);
                this.read();
                return value;
        }
    }

    protected JsonObject readObject() {
        final JsonObject object = new JsonObject();
        this.push();
        if (!this.iterator.hasNext()) {
            return this.closeContainer(object);
        }
        this.read();
        this.skipWhitespace();
        do { // todo: can there be unexpected symbols?
            this.skipWhitespace(false);
            if (this.isEndOfContainer()) {
                return this.closeContainer(object);
            }
        } while (this.readNextMember(object));
        return this.closeContainer(object);
    }

    protected boolean readNextMember(final JsonObject object) {
        this.setComment(CommentType.HEADER);
        this.setLinesAbove();

        final int offset = this.current.offset();
        final String key = this.readKey();

        this.skipWhitespace();
        this.expect(':');
        this.skipWhitespace();

        this.setComment(CommentType.VALUE);
        this.setLinesBetween();

        final JsonValue value = this.readValue(offset);

        object.add(key, value);

        final boolean delimiter = this.readDelimiter();
        this.takeFormatting(value);
        return delimiter;
    }

    protected String readKey() {
        if (this.current.isSymbol(':')) {
            return "";
        }

        final int start = this.current.start();
        final int offset = this.current.offset();
        final int lineBefore = this.current.line();

        final int skipped =
            this.skipTo(':', false, false);
        final Token previous = this.current;
        this.read();

        if (skipped == 0 && previous instanceof StringToken) {
            return ((StringToken) previous).parsed;
        }
        final int end = previous.end();
        return this.getText(lineBefore, start, offset, end);
    }

    protected JsonArray readArray() {
        final JsonArray array = new JsonArray();
        this.push();
        if (!this.iterator.hasNext()) {
            return this.closeContainer(array);
        }
        this.read();
        this.skipWhitespace();
        do { // todo: can there be unexpected symbols?
            this.skipWhitespace(false);
            if (this.isEndOfContainer()) {
                return this.closeContainer(array);
            }
        } while (this.readNextElement(array));
        return this.closeContainer(array);
    }

    protected boolean readNextElement(final JsonArray array) {
        this.setComment(CommentType.HEADER);
        this.setLinesAbove();

        final JsonValue value = this.readValue();

        array.add(value);

        final boolean delimiter = this.readDelimiter();
        this.takeFormatting(value);
        return delimiter;
    }

    protected boolean readDelimiter() {
        this.readComments(false);
        if (this.readIf(',')) {
            this.readComments(false);
            this.readNl();
            this.setComment(CommentType.EOL);
            return true;
        } else if (this.readNl()) {
            this.setComment(CommentType.EOL);
            this.skipWhitespace(false);
            this.readIf(',');
            return true;
        }
        return false;
    }

    protected <T extends JsonContainer> T closeContainer(
            final T container) {
        this.setComment(CommentType.INTERIOR);
        this.setLinesTrailing();
        this.takeFormatting(container);
        this.pop();
        this.read();
        this.readComments(false);
        this.setComment(CommentType.EOL);
        return container;
    }

    protected JsonValue readImplicit(final int offset) {
        final int start = this.current.start();
        final int lineBefore = this.current.line();
        final int numTokens = this.skipTo(',', true, true);
        if (numTokens == 0) {
            switch (this.current.type()) {
                case NUMBER: return Json.value(
                    ((NumberToken) this.current).number);
                case SINGLE_QUOTE: return new JsonString(
                    ((StringToken) this.current).parsed, StringType.SINGLE);
                case DOUBLE_QUOTE: return new JsonString(
                    ((StringToken) this.current).parsed, StringType.DOUBLE);
                case TRIPLE_QUOTE: return new JsonString(
                    ((StringToken) this.current).parsed, StringType.MULTI);
            }
        }
        final int end = this.current.end();
        final String text = this.getText(lineBefore, start, offset, end);
        switch (text) {
            case "true": return JsonLiteral.jsonTrue();
            case "false": return JsonLiteral.jsonFalse();
            case "null": return JsonLiteral.jsonNull();
            default: return new JsonString(text, StringType.IMPLICIT);
        }
    }

    protected String getText(
            final int lineBefore, final int start, final int offset, final int end) {
        // optimization to avoid redundant text analysis.
        if (lineBefore < this.current.lastLine()) {
            return this.buildImplicitText(start, offset, end);
        }
        return this.iterator.getText(start, end);
    }

    protected String buildImplicitText(
            final int start, final int offset, final int end) {
        final StringBuilder sb = new StringBuilder();
        final CharSequence reference = this.iterator.getReference();
        int marker = start;
        for (int i = start; i < end; i++) {
            char c = reference.charAt(i);
            if (c == '\n') {
                sb.append(reference, marker, i + 1);
                i = this.skipToOffset(i + 1, offset);
                marker = i;
            } else if (c == '\\' && i < end - 1) {
                sb.append(reference, marker, i);
                c = reference.charAt(++i);
                if (!this.shouldUnescape(c)) {
                    sb.append('\\');
                }
                sb.append(c);
                marker = i;
            }
        }
        sb.append(reference, marker, end);
        return sb.toString();
    }

    protected boolean shouldUnescape(final char c) {
        return c == '\n'
            || c == '}'
            || c == ']'
            || c == ')'
            || c == ':'
            || c == ',';
    }

    @Override
    public void close() {}
}
