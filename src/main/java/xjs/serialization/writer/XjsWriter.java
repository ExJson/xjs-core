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
    public void write(final JsonReference reference) throws IOException {
        final JsonValue value = reference.visit();
        if (value.isObject() && this.omitRootBraces && !value.asObject().isEmpty()) {
            this.writeOpenRoot(reference);
        } else {
            this.writeLinesAbove(-1, true, false, reference);
            this.writeHeader(0, reference);
            this.write(reference, 0);
            this.writeEolComment(0, reference, null);
            this.writeFooterComment(reference);
        }
    }

    protected void writeOpenRoot(final JsonReference root) throws IOException {
        final boolean condensed = this.isCondensed(root);
        JsonReference previous = null;
        this.writeOpenHeader(root);
        for (final JsonObject.Member member : root.visit().asObject()) {
            this.writeNextMember(previous, member, condensed, -1);
            previous = member.getReference();
        }
        this.writeEolComment(0, previous, null);
        if (!condensed) {
            this.writeLinesTrailing(root, -1);
        }
        this.writeOpenFooter(root);
    }

    protected void writeOpenHeader(final JsonReference root) throws IOException {
        if (this.outputComments && root.hasComment(CommentType.HEADER)) {
            this.writeLinesAbove(-1, true, false, root);
            this.writeComment(-1, root, CommentType.HEADER);
            this.nl(-1);
            this.nl(-1);
        }
    }

    protected void writeOpenFooter(final JsonReference root) throws IOException {
        if (this.outputComments) {
            if (root.hasComment(CommentType.INTERIOR)) {
                if (root.visit().asContainer().getLinesTrailing() < 0) {
                    this.nl(-1);
                }
                this.writeComment(-1, root, CommentType.INTERIOR);
                if (root.hasComment(CommentType.FOOTER)) {
                    this.nl(-1);
                    this.nl(-1);
                    this.writeComment(-1, root, CommentType.FOOTER);
                }
            } else if (root.hasComment(CommentType.FOOTER)) {
                if (root.visit().asContainer().getLinesTrailing() < 0) {
                    this.nl(-1);
                    this.nl(-1);
                }
                this.writeComment(-1, root, CommentType.FOOTER);
            }
        }
    }

    @Override
    protected void write(final JsonReference reference, final int level) throws IOException {
        final JsonValue value = reference.visit();
        final boolean condensed = this.isCondensed(reference);
        JsonReference previous = null;

        switch (value.getType()) {
            case OBJECT:
                this.open(condensed, '{');
                for (final JsonObject.Member member : value.asObject()) {
                    this.writeNextMember(previous, member, condensed, level);
                    previous = member.getReference();
                }
                this.writeEolComment(level, previous, null);
                this.close(reference, condensed, level, '}');
                break;
            case ARRAY:
                final boolean voidStart = this.isVoidString(value.asArray(), 0);
                this.open(condensed && !voidStart, '[');
                for (final JsonReference r : value.asArray().references()) {
                    this.writeNextElement(previous, r, condensed, level);
                    previous = r;
                }
                final boolean voidEnd = this.isVoidString(value.asArray(), value.asArray().size() - 1);
                if (voidEnd) {
                    this.tw.write(',');
                }
                this.writeEolComment(level, previous, null);
                if (voidEnd) {
                    this.tw.write(']');
                } else {
                    this.close(reference, condensed, level, ']');
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
            final JsonReference previous, final JsonObject.Member member, final boolean condensed, final int level) throws IOException {
        this.delimit(previous, member.getReference());
        this.writeEolComment(level, previous, member.getReference());
        this.writeLinesAbove(level + 1, previous == null, condensed, member.getReference());
        this.writeHeader(level + 1, member.getReference());
        this.writeString(member.getKey(), level);
        this.tw.write(':');
        if (!this.isVoidString(member.visit())) {
            this.separate(level + 2, member.getReference());
        }
        this.writeValueComment(level + 2, member.getReference());
        this.write(member.getReference(), level + 1);
    }

    protected void writeNextElement(
            final JsonReference previous, final JsonReference value, final boolean condensed, final int level) throws IOException {
        this.delimit(previous, value);
        this.writeEolComment(level, previous, value);
        this.writeLinesAbove(level + 1, previous == null, condensed, value);
        this.writeHeader(level + 1, value);
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
            final StringType type = value.filter(JsonFilter.STRING_TYPE).orElse(StringType.NONE);
            return this.checkType(value.asString(), type);
        }
        if (this.omitQuotes) {
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

    protected void delimit(final JsonReference previous, final JsonReference next) throws IOException {
        if (previous == null) {
            return;
        }
        if (!this.format || this.isVoidString(previous.visit()) && this.isVoidString(next.visit())) {
            this.tw.write(',');
        } else if (next.getLinesAbove() == 0 && this.allowCondense) {
            this.tw.write(',');
            this.tw.write(this.separator);
        }
    }

    @Override
    protected void close(
            final JsonReference c, final boolean condensed, final int level, final char closer) throws IOException {
        if (this.format) {
            if (condensed && this.allowCondense) {
                this.tw.write(this.separator);
            } else {
                this.writeLinesTrailing(c, level);
            }
        }
        this.writeInteriorComment(level, c);
        this.tw.write(closer);
    }

    protected void writeValueComment(final int level, final JsonReference reference) throws IOException {
        if (this.outputComments && reference.hasComment(CommentType.VALUE)) {
            final String comment = reference.getComments().getData(CommentType.VALUE);
            this.writeComment(level, comment);
            if (!comment.endsWith("\n")) {
                // Typically, coerce this value onto the next line
                if (reference.getLinesBetween() > 0) {
                    this.nl(level);
                } else {
                    this.tw.write(this.separator);
                }
            }
        }
    }

    protected void writeHeader(final int level, final JsonReference reference) throws IOException {
        if (this.outputComments && reference.hasComment(CommentType.HEADER)) {
            this.writeComment(level, reference, CommentType.HEADER);
            this.nl(level);
        }
    }

    protected void writeInteriorComment(final int level, final JsonReference reference) throws IOException {
        if (this.outputComments && reference.hasComment(CommentType.INTERIOR)) {
            final String comment = reference.getComments().getData(CommentType.INTERIOR);
            final JsonContainer c = reference.visit().asContainer();

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

    protected void writeFooterComment(final JsonReference reference) throws IOException {
        if (this.outputComments && reference.hasComment(CommentType.FOOTER)) {
            this.nl(0);
            this.writeComment(0, reference, CommentType.FOOTER);
        }
    }

    protected void writeComment(final int level, final JsonReference reference, final CommentType type) throws IOException {
        this.writeComment(level, reference.getComments().getData(type));
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

    protected void writeEolComment(final int level, final JsonReference previous, final JsonReference next) throws IOException {
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
