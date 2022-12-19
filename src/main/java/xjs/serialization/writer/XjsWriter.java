package xjs.serialization.writer;

import xjs.core.*;
import xjs.serialization.util.ImplicitStringUtils;
import xjs.serialization.util.StringContext;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class XjsWriter extends CommentedElementWriter {

    public XjsWriter(final File file, final boolean format) throws IOException {
        super(file, format);
    }

    public XjsWriter(final Writer writer, final boolean format) {
        super(writer, format);
    }

    public XjsWriter(final File file, final JsonWriterOptions options) throws IOException {
        super(file, options);
    }

    public XjsWriter(final Writer writer, final JsonWriterOptions options) {
        super(writer, options);
    }

    @Override
    protected void write() throws IOException {
        final JsonValue value = this.current();
        if (this.omitRootBraces
                && value.isObject()
                && !value.asObject().isEmpty()) {
            this.writeOpenRoot();
        } else {
            this.writeClosedRoot();
        }
    }

    protected void writeOpenRoot() throws IOException {
        this.level = -1;
        this.writeAbove();
        this.open((char) 0);
        while (this.current != null) {
            this.writeNextMember();
            this.next();
        }
        this.close((char) 0);
        this.writeFooter();
    }

    protected void writeClosedRoot() throws IOException {
        this.writeAbove();
        this.writeValue();
        this.writeAfter();
        this.writeFooter();
    }

    protected void writeValue() throws IOException {
        final JsonValue value = this.current();

        switch (value.getType()) {
            case OBJECT:
                this.writeObject();
                break;
            case ARRAY:
                this.writeArray();
                break;
            case NUMBER:
                this.writeNumber(value.asDouble());
                break;
            case STRING:
                this.writeString(value);
                break;
            default:
                this.tw.write(value.toString());
        }
    }

    @Override
    protected boolean shouldSeparateOpener() {
        final JsonContainer parent = this.parent();
        if (this.format
                && this.level > 0
                && !parent.isEmpty()
                && this.isCondensed()) {
            return parent.isObject()
                || !this.isVoidString(
                    parent.asArray(), 0);
        }
        return false;
    }

    @Override
    protected boolean shouldSeparateCloser() {
        return this.isCondensed()
            && this.level > 0
            && !this.isVoidString(this.previous());
    }

    protected void writeObject() throws IOException {
        this.open('{');
        while (this.current != null) {
            this.writeNextMember();
            this.next();
        }
        this.close('}');
    }

    protected void writeNextMember() throws IOException {
        this.writeAbove();
        this.writeKey();
        this.tw.write(':');
        this.writeBetween();
        this.writeValue();
        this.delimit();
        this.writeAfter();
    }

    protected void writeArray() throws IOException {
        this.open('[');
        while (this.current != null) {
            this.writeNextElement();
            this.next();
        }
        this.close(']');
    }

    protected void writeNextElement() throws IOException {
        this.writeAbove();
        this.writeValue();
        this.delimit();
        this.writeAfter();
    }

    @Override
    protected void delimit() throws IOException {
        if (this.peek != null) {
            if (!this.format) {
                this.tw.write(',');
            } else if (this.allowCondense && this.getLinesAbove(this.peek()) == 0) {
                this.tw.write(',');
                if (!this.isVoidString(this.peek())) {
                    this.tw.write(this.separator);
                }
            }
        } else if (this.parent().isArray()) {
            if (this.isVoidString(
                    this.parent().asArray(), this.parent().size() - 1)) {
                this.tw.write(',');
            }
        }
    }

    protected void writeKey() throws IOException {
        final String key = this.key();
        this.writeString(key, this.getKeyType(key));
    }

    protected void writeString(final JsonValue value) throws IOException {
        this.writeString(value.asString(), this.getStringType(value));
    }

    protected void writeString(
            final String value, final StringType type) throws IOException {
        switch (type) {
            case SINGLE:
                this.writeQuoted(value, '\'');
                break;
            case DOUBLE:
                this.writeQuoted(value, '"');
                break;
            case MULTI:
                this.writeMulti(value);
                break;
            case IMPLICIT:
                this.writeIndented(value);
                break;
            default:
                throw new IllegalStateException("unreachable");
        }
    }

    protected StringType getKeyType(final String key) {
        if (this.format && ImplicitStringUtils.find(key, StringContext.KEY)) {
            return StringType.SINGLE;
        }
        return StringType.IMPLICIT;
    }

    protected StringType getStringType(final JsonValue value) {
        final StringType type = StringType.fromValue(value);
        final String s = value.asString();
        if (type == StringType.NONE) {
            return this.omitQuotes
                ? StringType.select(s)
                : StringType.fast(s);
        }
        return type;
    }

    protected boolean isVoidString(final JsonArray array, final int i) {
        return i >= 0 && i < array.size() && this.isVoidString(array.get(i));
    }

    protected boolean isVoidString(final JsonValue value) {
        if (value instanceof JsonString && value.asString().isEmpty()) {
            return ((JsonString) value).getStringType() == StringType.IMPLICIT;
        }
        return false;
    }
}
