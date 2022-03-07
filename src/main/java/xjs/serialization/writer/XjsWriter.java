package xjs.serialization.writer;

import xjs.core.*;
import xjs.serialization.util.CommentUtils;
import xjs.serialization.util.ImplicitStringUtils;
import xjs.serialization.util.StringContext;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class XjsWriter extends AbstractJsonWriter {

    public XjsWriter(final File file, final boolean format) throws IOException {
        super(file, format);
    }

    public XjsWriter(final Writer writer, final boolean format) {
        super(writer, format);
    }

    public XjsWriter(final Writer writer, final JsonWriterOptions options) {
        super(writer, options);
    }

    @Override
    public void write(final JsonValue value) throws IOException {
        if (value.isObject() && this.shouldWriteOpenRoot(value.asObject())) {
            this.writeOpenRoot(value.asObject());
        } else {
            this.nl(0, true, value);
            this.writeHeader(0, value);
            this.write(value, 0);
            this.writeEolComment(0, value, null);
            this.writeFooterComment(0, value);
        }
    }

    protected boolean shouldWriteOpenRoot(final JsonObject object) {
        return this.omitRootBraces
            && !object.isEmpty()
            && !object.hasComment(CommentType.HEADER)
            && !object.hasComment(CommentType.FOOTER);
    }

    protected void writeOpenRoot(final JsonObject object) throws IOException {
        JsonValue previous = null;
        for (final JsonObject.Member member : object) {
            this.writeNextMember(previous, member, -1);
            previous = member.visit();
        }
        this.writeEolComment(0, previous, null);
        if (!this.isCondensed(object)) {
            this.writeLinesTrailing(object, 0);
        }
        this.writeInteriorComment(0, object, false);
    }

    @Override
    protected void write(final JsonValue value, final int level) throws IOException {
        final boolean condensed = this.isCondensed(value);
        JsonValue previous = null;

        switch (value.getType()) {
            case OBJECT:
                this.open(condensed, '{');
                for (final JsonObject.Member member : value.asObject()) {
                    this.writeNextMember(previous, member, level);
                    previous = member.visit();
                }
                this.writeEolComment(level, previous, null);
                this.close(value.asObject(), condensed, level, '}');
                break;
            case ARRAY:
                final boolean voidStart = this.isVoidString(value.asArray(), 0);
                this.open(condensed && !voidStart, '[');
                for (final JsonValue v : value.asArray().visitAll()) {
                    this.writeNextElement(previous, v, level);
                    previous = v;
                }
                final boolean voidEnd = this.isVoidString(value.asArray(), value.asArray().size() - 1);
                if (voidEnd) {
                    this.tw.write(',');
                }
                this.writeEolComment(level, previous, null);
                if (voidEnd) {
                    this.tw.write(']');
                } else {
                    this.close(value.asArray(), condensed, level, ']');
                }
                break;
            case NUMBER:
                this.writeNumber(value.asDouble());
                break;
            case STRING:
                this.writeString(value, level);
                break;
            default:
                this.tw.write(value.toString());
        }
    }

    protected void writeNextMember(
            final JsonValue previous, final JsonObject.Member member, final int level) throws IOException {
        this.delimit(previous, member.visit());
        this.writeEolComment(level, previous, member.visit());
        this.nl(level + 1, member.visit());
        this.writeHeader(level, member.visit());
        this.writeString(member.getKey(), level);
        this.tw.write(':');
        if (!this.isVoidString(member.visit())) {
            this.separate(level + 2, member.visit());
        }
        this.writeValueComment(level + 2, member.visit());
        this.write(member.visit(), level + 1);
    }

    protected void writeNextElement(
            final JsonValue previous, final JsonValue value, final int level) throws IOException {
        this.delimit(previous, value);
        this.writeEolComment(level, previous, value);
        this.nl(level + 1, value);
        this.writeHeader(level, value);
        this.write(value, level + 1);
    }

    protected void writeString(final String key, final int level) throws IOException {
        this.writeString(key, this.getKeyType(key), level, StringContext.KEY);
    }

    protected void writeString(final JsonValue value, final int level) throws IOException {
        this.writeString(value.asString(), this.getValueType(value), level, StringContext.VALUE);
    }

    protected void writeString(
            final String value, final StringType type, int level, final StringContext ctx) throws IOException {
        switch (type) {
            case SINGLE:
                this.writeQuoted(value, '\'');
                break;
            case DOUBLE:
                this.writeQuoted(value, '"');
                break;
            case MULTI:
                this.writeMulti(value, level + 1);
                break;
            case IMPLICIT:
                this.tw.write(ImplicitStringUtils.escape(value, ctx));
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

    protected StringType getValueType(final JsonValue value) {
        if (this.format) {
            final StringType type = value instanceof JsonString
                ? ((JsonString) value).getStringType() : StringType.NONE;
            return this.checkType(value.asString(), type);
        }
        if (this.compress) {
            final String s = value.asString();
            if (!s.contains("\n") && ImplicitStringUtils.isBalanced(s)) {
                return StringType.IMPLICIT;
            }
        }
        return StringType.SINGLE;
    }

    protected StringType checkType(final String value, final StringType type) {
        if (type == StringType.SINGLE || type == StringType.DOUBLE || type == StringType.MULTI) {
            return type;
        }
        if (type == StringType.NONE) {
            return StringType.fast(value);
        }
        return StringType.select(value);
    }

    protected void writeMulti(final String value, int level) throws IOException {
        this.tw.write("'''");
        for (final String line : value.split("\r?\n")) {
            this.nl(line.isEmpty() ? 0 : level);
            this.tw.write(line);
        }
        this.nl(level);
        this.tw.write("'''");
    }

    protected void delimit(final JsonValue previous, final JsonValue next) throws IOException {
        if (previous == null) {
            return;
        }
        if (!this.format || this.isVoidString(previous) && this.isVoidString(next)) {
            this.tw.write(',');
        } else if (next.getLinesAbove() == 0 && this.allowCondense) {
            this.tw.write(',');
            this.tw.write(this.separator);
        }
    }

    @Override
    protected void close(
            final JsonContainer c, final boolean condensed, final int level, final char closer) throws IOException {
        if (this.format) {
            if (condensed && this.allowCondense) {
                this.tw.write(this.separator);
            } else {
                this.writeLinesTrailing(c, level);
            }
        }
        this.writeInteriorComment(level, c, condensed);
        this.tw.write(closer);
    }

    protected void writeValueComment(final int level, final JsonValue value) throws IOException {
        if (this.outputComments && value.hasComment(CommentType.VALUE)) {
            final String comment = value.getComments().getData(CommentType.VALUE);
            this.writeComment(level, comment);
            if (!comment.endsWith("\n")) {
                // Typically, coerce this value onto the next line
                if (value.getLinesBetween() > 0) {
                    this.nl(level);
                } else {
                    this.tw.write(this.separator);
                }
            }
        }
    }

    protected void writeHeader(final int level, final JsonValue value) throws IOException {
        if (this.outputComments && value.hasComment(CommentType.HEADER)) {
            if (level > 0) {
                this.nl(level);
            }
            this.writeComment(level, value, CommentType.HEADER);
            this.nl(level);
        }
    }

    protected void writeInteriorComment(
            final int level, final JsonContainer c, final boolean condensed) throws IOException {
        if (this.outputComments && c.hasComment(CommentType.INTERIOR)) {
            final String comment = c.getComments().getData(CommentType.INTERIOR);

            if (c.isEmpty() && c.getLinesTrailing() < 1 && !comment.contains("\n")) {
                this.tw.write(this.separator);
                this.writeComment(level + 1, comment, false);
                this.tw.write(this.separator);
                return;
            }
            if (comment.contains("\n") && c.getLinesTrailing() < 1) {
                this.nl(level + 1);
            } else {
                this.tw.write(this.indent); // newline was printed for upper level
            }
            this.writeComment(level + 1, comment, false);
            this.nl(level);
        }
    }

    protected void writeFooterComment(final int level, final JsonValue value) throws IOException {
        if (this.outputComments && value.hasComment(CommentType.FOOTER)) {
            this.nl(level);
            this.writeComment(level, value, CommentType.FOOTER);
        }
    }

    protected void writeComment(final int level, final JsonValue value, final CommentType type) throws IOException {
        this.writeComment(level, value.getComments().getData(type));
    }

    protected void writeComment(final int level, final String comment) throws IOException {
        this.writeComment(level, comment, true);
    }

    protected void writeComment(final int level, final String comment, final boolean indentLast) throws IOException {
        for (int i = 0; i < comment.length(); i++) {
            final char c = comment.charAt(i);
            if (c == '\n') {
                if (i == comment.length() - 1) {
                    if (indentLast) {
                        this.nl(level);
                    } else {
                        this.tw.write(c);
                    }
                } else if (comment.charAt(i + 1) != '\n') {
                    this.nl(level);
                } else {
                    this.tw.write(this.eol);
                }
            } else if (c != '\r') {
                this.tw.write(c);
            }
        }
    }

    protected void writeEolComment(final int level, final JsonValue previous, final JsonValue next) throws IOException {
        if (previous == null) {
            return;
        }
        if (this.outputComments && previous.hasComment(CommentType.EOL)) {
            String comment = previous.getComments().getData(CommentType.EOL);
            if (next != null && next.getLinesAbove() == 0 && this.allowCondense) {
                comment = CommentUtils.rewrite(CommentStyle.BLOCK, comment) + " ";
            } else {
                this.tw.write(this.separator);
            }
            if (comment.contains("\n")) {
                for (final String line : comment.split("\r?\n")) {
                    this.nl(level);
                    this.tw.write(line);
                }
            } else {
                this.tw.write(comment);
            }
        }
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
