package xjs.serialization.parser;

import org.jetbrains.annotations.NotNull;
import xjs.core.*;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.NumberToken;
import xjs.serialization.token.StringToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.Token.Type;
import xjs.serialization.token.Tokenizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Parses a stream of {@link Token tokens} in XJS into a {@link
 * JsonObject JSON object}.
 */
public class XjsParser extends CommentedTokenParser {

    /**
     * Constructs the parser when given a file in XJS format.
     *
     * @param file The file containing XJS data.
     * @throws IOException If an error occurs when reading the file.
     */
    public XjsParser(final File file) throws IOException {
        this(Tokenizer.containerize(new FileInputStream(file)));
    }

    /**
     * Constructs the parser from raw text data in XJS format.
     *
     * @param text The JSON text in XJS format.
     */
    public XjsParser(final String text) {
        this(Tokenizer.containerize(text));
    }

    /**
     * Constructs the parser from a know set of tokens in XJS format.
     *
     * @param root The root token container.
     */
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
        this.readAboveOpenRoot(object);
        while (true) {
            this.readWhitespace(false);
            if (this.isEndOfContainer()) {
                break;
            }
            this.readNextMember(object);
        }
        this.readBottom();
        return this.takeFormatting(object);
    }

    protected JsonValue readClosedRoot() {
        if (this.current.type() == Type.OPEN) {
            this.read();
        }
        this.readAbove();
        final JsonValue result = this.readValue();

        this.readAfter();
        this.readBottom();
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
        if (!this.open()) {
            return this.close(object);
        }
        while (true) {
            this.readWhitespace(false);
            if (this.isEndOfContainer()) {
                return this.close(object);
            }
            this.readNextMember(object);
        }
    }

    protected void readNextMember(final JsonObject object) {
        this.setAbove();

        final int offset = this.current.offset();
        final String key = this.readKey();

        this.readBetween(':');

        final JsonValue value = this.readValue(offset);
        object.add(key, value);

        this.readDelimiter();
        this.takeFormatting(value);
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
        if (!this.open()) {
            return this.close(array);
        }
        while (true) {
            this.readWhitespace(false);
            if (this.isEndOfContainer()) {
                return this.close(array);
            }
            this.readNextElement(array);
        }
    }

    protected void readNextElement(final JsonArray array) {
        this.setAbove();

        final JsonValue value = this.readValue();
        array.add(value);

        this.readDelimiter();
        this.takeFormatting(value);
    }

    protected void readDelimiter() {
        this.readLineWhitespace();
        if (this.readIf(',')) {
            this.readLineWhitespace();
            this.readNl();
            this.setComment(CommentType.EOL);
        } else if (this.readNl()) {
            this.setComment(CommentType.EOL);
            this.readWhitespace(false);
            this.readIf(',');
        }
    }

    protected boolean open() {
        this.push();
        if (this.isEndOfContainer()) {
            return false;
        }
        this.read();
        this.readWhitespace();
        return true;
    }

    protected <T extends JsonContainer> T close(
            final T container) {
        this.setTrailing();
        this.takeFormatting(container);
        this.pop();
        this.read();
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
                i = this.getActualOffset(i + 1, offset);
                marker = i;
            } else if (c == '\\' && i < end - 1) {
                sb.append(reference, marker, i);
                c = reference.charAt(++i);
                if (c != '\r' && c != '\n') {
                    sb.append('\\');
                }
                sb.append(c);
                marker = i;
            }
        }
        sb.append(reference, marker, end);
        return sb.toString();
    }

    @Override
    public void close() {}
}
