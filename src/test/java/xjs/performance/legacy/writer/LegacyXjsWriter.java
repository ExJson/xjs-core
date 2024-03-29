package xjs.performance.legacy.writer;

import xjs.comments.CommentData;
import xjs.comments.CommentType;
import xjs.core.JsonArray;
import xjs.core.JsonContainer;
import xjs.core.JsonObject;
import xjs.core.JsonString;
import xjs.core.JsonValue;
import xjs.core.StringType;
import xjs.serialization.util.ImplicitStringUtils;
import xjs.serialization.util.StringContext;
import xjs.serialization.writer.JsonWriterOptions;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class LegacyXjsWriter extends LegacyAbstractJsonWriter {

    public LegacyXjsWriter(final File file, final boolean format) throws IOException {
        super(file, format);
    }

    public LegacyXjsWriter(final Writer writer, final boolean format) {
        super(writer, format);
    }

    public LegacyXjsWriter(final File file, final JsonWriterOptions options) throws IOException {
        super(file, options);
    }

    public LegacyXjsWriter(final Writer writer, final JsonWriterOptions options) {
        super(writer, options);
    }

    @Override
    public void write(final JsonValue value) throws IOException {
        if (value.isObject() && this.omitRootBraces && !value.asObject().isEmpty()) {
            this.writeOpenRoot(value.asObject());
        } else {
            this.writeLinesAbove(-1, null, null, false, value);
            this.writeHeader(0, value);
            this.write(value, 0);
            this.writeEolComment(0, value, null);
            this.writeFooterComment(value);
        }
    }

    protected void writeOpenRoot(final JsonObject object) throws IOException {
        final boolean condensed = this.isOpenRootCondensed(object);
        JsonValue previous = null;
        this.writeOpenHeader(object);
        for (final JsonObject.Member member : object) {
            this.writeNextMember(object, previous, member, condensed, -1);
            previous = member.getOnly();
        }
        this.writeEolComment(0, previous, null);
        if (!condensed) {
            this.writeLinesTrailing(object, -1);
        }
        this.writeOpenFooter(object);
    }


    protected boolean isOpenRootCondensed(final JsonValue value) {
        if (this.allowCondense && value.isContainer()) {
            final JsonContainer c = value.asContainer();
            if (c.size() < 2) {
                return false;
            }
            // Intentionally shallow check for formatting purposes
            if (c.getReference(0).getOnly().getLinesAbove() != 0) {
                return false;
            }
            return c.getReference(c.size() - 1).getOnly().getLinesAbove() == 0;
        }
        return false;
    }

    protected void writeOpenHeader(final JsonObject root) throws IOException {
        if (this.outputComments && root.hasComment(xjs.comments.CommentType.HEADER)) {
            this.writeLinesAbove(-1, null, null, false, root);
            this.writeComment(-1, root, xjs.comments.CommentType.HEADER);
            this.nl(-1);
            if (!root.isEmpty() && root.getReference(0).getOnly().getLinesAbove() <= 0) {
                this.nl(-1);
            }
        }
    }

    protected void writeOpenFooter(final JsonObject root) throws IOException {
        if (this.outputComments) {
            if (root.hasComment(xjs.comments.CommentType.INTERIOR)) {
                if (root.getLinesTrailing() < 0) {
                    this.nl(-1);
                }
                this.writeComment(-1, root, xjs.comments.CommentType.INTERIOR);
                if (root.hasComment(xjs.comments.CommentType.FOOTER)) {
                    this.nl(-1);
                    this.nl(-1);
                    this.writeComment(-1, root, xjs.comments.CommentType.FOOTER);
                }
            } else if (root.hasComment(xjs.comments.CommentType.FOOTER)) {
                if (root.getLinesTrailing() < 0) {
                    this.nl(-1);
                    this.nl(-1);
                }
                this.writeComment(-1, root, xjs.comments.CommentType.FOOTER);
            }
        }
    }

    @Override
    protected void write(final JsonValue value, final int level) throws IOException {
        final boolean condensed = this.isCondensed(value);
        JsonValue previous = null;

        switch (value.getType()) {
            case OBJECT:
                this.open(value.asObject(), condensed, '{');
                for (final JsonObject.Member member : value.asObject()) {
                    this.writeNextMember(value, previous, member, condensed, level);
                    previous = member.getOnly();
                }
                this.writeEolComment(level, previous, null);
                this.close(value.asObject(), condensed, level, '}');
                break;
            case ARRAY:
                this.open(value.asArray(), condensed, '[');
                for (final JsonValue v : value.asArray().visitAll()) {
                    this.writeNextElement(value, previous, v, condensed, level);
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

    @Override
    protected boolean shouldSeparateOpener(final JsonContainer c, final boolean condensed) {
        if (c.isEmpty()) {
            return false;
        } else if (c.isObject()) {
            return condensed;
        }
        return condensed && !this.isVoidString(c.asArray(), 0);
    }

    protected void writeNextMember(
            JsonValue parent, JsonValue previous, JsonObject.Member member, boolean condensed, int level) throws IOException {
        this.delimit(previous, member.getOnly());
        this.writeEolComment(level, previous, member.getOnly());
        this.writeLinesAbove(level + 1, parent, previous, condensed, member.getOnly());
        this.writeHeader(level + 1, member.getOnly());
        this.writeString(member.getKey(), level);
        this.tw.write(':');
        if (!this.isVoidString(member.getOnly())) {
            this.separate(level + 2, member.getOnly());
        }
        this.writeValueComment(level + 2, member.getOnly());
        if (StringType.fromValue(member.getOnly()) == StringType.MULTI) {
            this.write(member.getOnly(), level + 2);
        } else {
            this.write(member.getOnly(), level + 1);
        }
    }

    protected void writeNextElement(
            JsonValue parent, JsonValue previous, JsonValue value, boolean condensed, int level) throws IOException {
        this.delimit(previous, value);
        this.writeEolComment(level, previous, value);
        this.writeLinesAbove(level + 1, parent, previous, condensed, value);
        this.writeHeader(level + 1, value);
        this.write(value, level + 1);
    }

    protected void writeString(final String key, final int level) throws IOException {
        this.writeString(key, this.getKeyType(key), level, StringContext.KEY);
    }

    protected void writeString(final JsonValue value, final int level) throws IOException {
        this.writeString(value.asString(), this.getStringType(value), level, StringContext.VALUE);
    }

    protected void writeString(
            final String value, final StringType type, int level, final StringContext ctx) throws IOException {
        if (type == null) {
            // optimization to avoid unnecessary escapes for verified strings.
            this.tw.write(ImplicitStringUtils.escape(value, ctx));
            return;
        }
        switch (type) {
            case SINGLE:
                this.writeQuoted(value, '\'');
                break;
            case DOUBLE:
                this.writeQuoted(value, '"');
                break;
            case MULTI:
                this.writeMulti(value, level);
                break;
            case IMPLICIT:
                this.tw.write(value);
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
        if (type == StringType.MULTI) {
            return type;
        } else if (type == StringType.SINGLE || type == StringType.DOUBLE) {
            return this.omitQuotes ? StringType.select(s) : type;
        } else if (type == StringType.NONE) {
            return this.omitQuotes ? StringType.select(s) : StringType.fast(s);
        }
        return ImplicitStringUtils.isBalanced(s) ? null : StringType.fast(s);
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
            if (c.getLinesTrailing() < 1 && condensed && this.allowCondense) {
                this.tw.write(this.separator);
            } else {
                this.writeLinesTrailing(c, level);
            }
        }
        this.writeInteriorComment(level, c);
        this.tw.write(closer);
    }

    protected void writeValueComment(final int level, final JsonValue value) throws IOException {
        if (this.outputComments && value.hasComment(xjs.comments.CommentType.VALUE)) {
            final CommentData data = value.getComments().getData(xjs.comments.CommentType.VALUE);
            this.writeComment(level, data);
            if (!data.endsWithNewline()) {
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
        if (this.outputComments && value.hasComment(xjs.comments.CommentType.HEADER)) {
            this.writeComment(level, value, xjs.comments.CommentType.HEADER);
            this.nl(level);
        }
    }

    protected void writeInteriorComment(final int level, final JsonContainer c) throws IOException {
        if (this.outputComments && c.hasComment(xjs.comments.CommentType.INTERIOR)) {
            final CommentData data = c.getComments().getData(xjs.comments.CommentType.INTERIOR);

            if (c.isEmpty() && c.getLinesTrailing() < 1 && data.getLines() == 1) {
                this.tw.write(this.separator);
                this.writeComment(level + 1, data, false);
                this.tw.write(this.separator);
                return;
            }
            if (data.getLines() > 0 && c.getLinesTrailing() < 1) {
                this.nl(level + 1);
            } else {
                this.tw.write(this.indent); // newline was printed for upper level
            }
            this.writeComment(level + 1, data, false);
            this.nl(level);
        }
    }

    protected void writeFooterComment(final JsonValue value) throws IOException {
        if (this.outputComments && value.hasComment(xjs.comments.CommentType.FOOTER)) {
            this.nl(0);
            this.writeComment(0, value, xjs.comments.CommentType.FOOTER);
        }
    }

    protected void writeComment(final int level, final JsonValue value, final xjs.comments.CommentType type) throws IOException {
        this.writeComment(level, value.getComments().getData(type));
    }

    protected void writeComment(final int level, final CommentData comment) throws IOException {
        this.writeComment(level, comment, true);
    }

    protected void writeComment(final int level, final CommentData data, final boolean indentLast) throws IOException {
        data.writeTo(this.tw, this.separator, level, this.eol);
        if (indentLast && data.endsWithNewline()) {
            for (int i = 0; i < level; i++) {
                this.tw.write(this.indent);
            }
        }
    }

    protected void writeEolComment(final int level, final JsonValue previous, final JsonValue next) throws IOException {
        if (previous == null) {
            return;
        }
        if (this.outputComments && previous.hasComment(xjs.comments.CommentType.EOL)) {
            CommentData data = previous.getComments().getData(CommentType.EOL);
            if (next != null && next.getLinesAbove() == 0 && this.allowCondense) {
                // NOT UPDATED -- LEGACY CODE -- TO BE REMOVED
                // data = CommentUtils.rewrite(CommentStyle.BLOCK, data) + " ";
            } else {
                this.tw.write(this.separator);
            }
            if (data.getLines() > 1) {
                this.nl(level);
            }
            data.writeTo(this.tw, this.separator, level, this.eol);
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
