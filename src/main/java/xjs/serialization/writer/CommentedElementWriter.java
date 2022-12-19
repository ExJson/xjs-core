package xjs.serialization.writer;

import xjs.core.CommentStyle;
import xjs.core.CommentType;
import xjs.core.JsonValue;
import xjs.serialization.util.CommentUtils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

// todo: simplify via Comment object

public abstract class CommentedElementWriter extends ElementWriter {
    protected final boolean outputComments;

    protected CommentedElementWriter(
            final File file, final boolean format) throws IOException {
        super(file, format);
        this.outputComments = format;
    }

    protected CommentedElementWriter(
            final Writer writer, final boolean format) {
        super(writer, format);
        this.outputComments = format;
    }

    protected CommentedElementWriter(
            final File file, final JsonWriterOptions options) throws IOException {
        super(file, options);
        this.outputComments = options.isOutputComments();
    }

    protected CommentedElementWriter(
            final Writer writer, final JsonWriterOptions options) {
        super(writer, options);
        this.outputComments = options.isOutputComments();
    }

    protected boolean hasComment(final CommentType type) {
        return this.outputComments
            && this.current != null
            && this.current().hasComment(type);
    }

    protected String getComment(final CommentType type) {
        return this.current().getComments().getData(type);
    }

    @Override
    protected void writeAbove() throws IOException {
        if (this.format) {
            this.writeLines(this.getActualLinesAbove());
            if (this.hasComment(CommentType.HEADER)) {
                final String data = this.getComment(CommentType.HEADER);
                this.writeIndented(data, this.level);
                this.nl();
                if (this.level == -1
                        && !data.endsWith("\n")
                        && this.getLinesAbove(this.getFirst(this.current())) <= 0) {
                    this.nl();
                }
            }
        }
    }

    @Override
    protected void writeBetween() throws IOException {
        if (this.format) {
            final int lines =
                this.getActualLinesBetween();
            if (lines > 0) {
                this.writeLines(lines, this.level + 1);
            } else {
                this.tw.write(this.separator);
            }
            if (!this.hasComment(CommentType.VALUE)) {
                return;
            }
            String data =
                this.getComment(CommentType.VALUE);

            if (lines == 0 && !data.endsWith("\n")) {
                data = CommentUtils.rewrite(
                    CommentStyle.BLOCK, data);
            }
            this.writeIndented(data, this.level + 1);

            if (lines <= 0 && !data.endsWith("\n")) {
                this.tw.write(this.separator);
            } else if (!data.endsWith("\n")) {
                // coerce value onto the next line
                this.nl(this.level + 1);
            }
        }
    }

    @Override
    protected void writeAfter() throws IOException {
        if (!this.hasComment(CommentType.EOL)) {
            return;
        }
        final String data =
            this.getComment(CommentType.EOL);
        this.tw.write(this.separator);
        this.writeIndented(data);
    }

    @Override
    protected boolean writeTrailing() throws IOException {
        if (!this.format) {
            return false;
        }

        // todo: simplify

        final boolean hasComment =
            this.parent().hasComment(CommentType.INTERIOR);
        final String data = hasComment
            ? this.parent().getComments().getData(CommentType.INTERIOR)
            : "";

        final boolean empty = this.parent().isEmpty();

        int lines = this.getActualLinesTrailing();
        if (empty) {
            lines = Math.min(1, lines);
        }
        if (data.contains("\n")) {
            lines = Math.max(1, lines);
        }

        final int level = hasComment ? this.level : this.level - 1;
        this.writeLines(lines, level);

        if (!hasComment) {
            return lines > 0;
        }

        final boolean trim = data.endsWith("\n");

        if (lines <= 0) {
            this.tw.write(this.separator);
        }
        this.writeIndented(data, this.level, trim);
        if (empty && lines <= 0) {
            this.tw.write(this.separator);
        }

        if (trim || lines > 0) {
            this.nl(this.level - 1);
        }

        return lines > 0;
    }

    protected void writeFooter() throws IOException {
        final JsonValue source = this.parent != null
             ? this.parent() : this.previous();
        if (source == null || !source.hasComment(CommentType.FOOTER)) {
            return;
        }
        final String data =
            source.getComments().getData(CommentType.FOOTER);
        this.nl(0);
        this.writeIndented(data);
    }

    @Override
    protected boolean requiresSmartSpace(final JsonValue value) {
        if (value == null) return false;
        return value.isContainer() || value.hasComment(CommentType.HEADER);
    }
}
