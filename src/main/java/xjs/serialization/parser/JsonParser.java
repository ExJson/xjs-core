package xjs.serialization.parser;

import org.jetbrains.annotations.NotNull;
import xjs.core.JsonArray;
import xjs.core.JsonLiteral;
import xjs.core.JsonObject;
import xjs.core.JsonString;
import xjs.core.JsonValue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

public class JsonParser extends AbstractJsonParser {

    public JsonParser(final String text) {
        super(text);
    }

    public JsonParser(final File file) throws IOException {
        super(file);
    }

    public JsonParser(final Reader reader) {
        super(reader);
    }

    public JsonParser(final Reader reader, final int buffer) {
        super(reader, buffer);
    }

    public @NotNull JsonValue parse() throws IOException {
        this.read();
        this.skipWhitespace();
        final int linesAbove = this.linesSkipped;
        final JsonValue result =
            this.readValue().setLinesAbove(linesAbove);
        this.skipWhitespace();
        if (!this.isEndOfText()) {
            throw this.unexpected((char) this.current);
        }
        return result;
    }

    protected JsonValue readValue() throws IOException {
        switch (this.current) {
            case 'n':
                return this.readNull();
            case 't':
                return this.readTrue();
            case 'f':
                return this.readFalse();
            case '"':
                return this.readString();
            case '[':
                return this.readArray();
            case '{':
                return this.readObject();
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return this.readNumber();
            default:
                throw this.expected("value");
        }
    }

    protected JsonValue readNull() throws IOException {
        this.read();
        this.expect('u');
        this.expect('l');
        this.expect('l');
        return JsonLiteral.jsonNull();
    }

    protected JsonValue readTrue() throws IOException {
        this.read();
        this.expect('r');
        this.expect('u');
        this.expect('e');
        return JsonLiteral.jsonTrue();
    }

    protected JsonValue readFalse() throws IOException {
        this.read();
        this.expect('a');
        this.expect('l');
        this.expect('s');
        this.expect('e');
        return JsonLiteral.jsonFalse();
    }

    protected JsonValue readString() throws IOException {
        return new JsonString(this.readQuoted('"'));
    }

    protected JsonArray readArray() throws IOException {
        this.read();
        final JsonArray array = new JsonArray();
        this.skipWhitespace();
        if (this.readIf(']')) {
            return array;
        }
        do {
            this.skipWhitespace(false);
            final int linesAbove = this.linesSkipped;
            array.add(this.readValue().setLinesAbove(linesAbove));
            this.skipWhitespace();
        } while (this.readIf(','));
        if (!this.readIf(']')) {
            throw expected("',' or ']'");
        }
        return (JsonArray) array.setLinesTrailing(this.linesSkipped);
    }

    protected JsonObject readObject() throws IOException {
        this.read();
        final JsonObject object = new JsonObject();
        this.skipWhitespace();
        if (this.readIf('}')) {
            return object;
        }
        do {
            this.skipWhitespace(false);
            final int linesAbove = this.linesSkipped;
            final String key = this.readKey();
            this.skipWhitespace();
            this.expect(':');
            this.skipWhitespace();
            final int linesBetween = this.linesSkipped;
            object.add(key,
                this.readValue()
                    .setLinesAbove(linesAbove)
                    .setLinesBetween(linesBetween));
            this.skipWhitespace();
        } while (this.readIf(','));
        if (!this.readIf('}')) {
            throw this.expected("',' or '}'");
        }
        return (JsonObject) object.setLinesTrailing(this.linesSkipped);
    }

    protected String readKey() throws IOException {
        if (this.current != '"') {
            throw this.expected("key");
        }
        return this.readQuoted('"');
    }
}
