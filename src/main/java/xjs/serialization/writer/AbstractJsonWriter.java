package xjs.serialization.writer;

import xjs.core.JsonContainer;
import xjs.core.JsonValue;
import xjs.serialization.JsonSerializationContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;

public abstract class AbstractJsonWriter implements AutoCloseable {

    protected final boolean format;
    protected final Writer tw;
    protected final boolean allowCondense;
    protected final boolean bracesSameLine;
    protected final boolean nestedSameLine;
    protected final boolean omitRootBraces;
    protected final boolean outputComments;
    protected final boolean compress;
    protected final String eol;
    protected final String indent;
    protected final int maxLines;
    protected String separator;

    protected AbstractJsonWriter(final File file, final boolean format) throws IOException {
        this(new FileWriter(file), format);
    }

    protected AbstractJsonWriter(final Writer writer, final boolean format) {
        this.format = format;
        this.tw = writer;
        this.eol = JsonSerializationContext.getEol();
        this.allowCondense = true;
        this.bracesSameLine = true;
        this.nestedSameLine = false;
        this.omitRootBraces = true;
        this.compress = true;
        this.outputComments = format;
        this.indent = "  ";
        this.maxLines = Integer.MAX_VALUE;
        this.separator = format ? " " : "";
    }

    protected AbstractJsonWriter(final Writer writer, final JsonWriterOptions options) {
        this.format = true;
        this.tw = writer;
        this.eol = options.getEol();
        this.allowCondense = options.isAllowCondense();
        this.bracesSameLine = options.isBracesSameLine();
        this.nestedSameLine = options.isNestedSameLine();
        this.omitRootBraces = options.isOmitRootBraces();
        this.outputComments = options.isOutputComments();
        this.compress = options.isCompressed();
        this.indent = options.getIndent();
        this.maxLines = options.getMaxLines();
        this.separator = options.getSeparator();
    }

    public void write(final JsonValue value) throws IOException {
        this.nl(0, true, value);
        this.write(value, 0);
    }

    protected abstract void write(final JsonValue value, final int level) throws IOException;

    protected void open(final boolean condensed, final char opener) throws IOException {
        this.tw.write(opener);
        if (this.format) {
            if (condensed && this.allowCondense) {
                this.tw.write(this.separator);
            }
        }
    }

    protected void nl(final int level, final JsonValue value) throws IOException {
        this.nl(level, false, value);
    }

    protected void nl(final int level, final boolean top, final JsonValue value) throws IOException {
        if (this.format) {
            int lines = value.getLinesAbove();
            if (lines < 0) {
                lines = top ? 0 : 1;
            }
            if (!top && !this.allowCondense) {
                lines = Math.max(1, lines);
            }
            lines = Math.min(lines, this.maxLines);
            if (lines > 0) {
                for (int i = 0; i < lines; i++) {
                    this.tw.write(this.eol);
                }
                for (int i = 0; i < level; i++) {
                    this.tw.write(this.indent);
                }
            }
        }
    }

    protected void nl(final int level) throws IOException {
        if (this.format) {
            this.tw.write(this.eol);
            for (int i = 0; i < level; i++) {
                this.tw.write(this.indent);
            }
        }
    }

    protected boolean isCondensed(final JsonValue value) {
        if (this.allowCondense && value.isContainer()) {
            final JsonContainer c = value.asContainer();
            if (!c.isEmpty()) {
                // Intentionally shallow check for formatting purposes
                return c.getReference(0).visit().getLinesAbove() == 0;
            }
        }
        return false;
    }

    protected int linesAbove(final JsonValue value) {
        if (this.format) {
            return Math.max(0, value.getLinesAbove());
        }
        return 0;
    }

    protected void separate(int level, final JsonValue value) throws IOException {
        if (this.format) {
            int lines = Math.max(0, value.getLinesBetween());
            if (lines == 0 && value.isContainer() && !this.bracesSameLine) {
               lines = 1;
               level -= 1;
            }
            lines = Math.min(lines, this.maxLines);
            for (int i = 0; i < lines; i++) {
                this.tw.write(this.eol);
            }
            if (lines > 0) {
                for (int i = 0; i < level; i++) {
                    this.tw.write(this.indent);
                }
            } else {
                this.tw.write(this.separator);
            }
        }
    }

    protected int linesBetween(final JsonValue value) {
        if (this.format) {
            return Math.max(0, value.getLinesBetween());
        }
        return 0;
    }

    protected void delimit(final boolean following, final int nextAbove) throws IOException {
        if (following) {
            this.tw.write(',');
            if ((nextAbove == 0) && this.allowCondense) {
                this.tw.write(this.separator);
            }
        }
    }

    protected void close(
            final JsonContainer c, final boolean condensed, final int level, final char closer) throws IOException {
        if (this.format) {
            if (condensed && this.allowCondense) {
                this.tw.write(this.separator);
            } else {
                this.writeLinesTrailing(c, level);
            }
        }
        this.tw.write(closer);
    }

    protected void writeLinesTrailing(final JsonContainer c, final int level) throws IOException {
        if (this.format) {
            final int lines = Math.min(c.getLinesTrailing(), this.maxLines);
            if (lines > 0 || (lines < 0 && c.size() > 0)) {
                for (int i = 0; i < Math.max(0, c.getLinesTrailing()) - 1; i++) {
                    this.tw.write(this.eol);
                }
                this.nl(level);
            }
        }
    }

    protected void writeNumber(final double decimal) throws IOException {
        final long integer = (long) decimal;
        if (integer == decimal) {
            this.tw.write(Long.toString(integer));
            return;
        }
        String res = BigDecimal.valueOf(decimal).toEngineeringString();
        if (res.endsWith(".0")) {
            res = res.substring(0, res.length() - 2);
        } else if (res.contains("E")) {
            res = Double.toString(decimal).replace("E", "e");
        }
        this.tw.write(res);
    }

    protected void writeBoolean(final boolean b) throws IOException {
        this.tw.write(b ? "true" : "false");
    }

    protected void writeQuoted(final String text, final char quote) throws IOException {
        this.tw.write(quote);
        this.tw.write(escapeQuoted(text, quote));
        this.tw.write(quote);
    }

    protected static String escapeQuoted(final String text, char quote) {
        if (text == null) return null;

        for (int i = 0; i < text.length(); i++) {
            if (getEscapedChar(text.charAt(i), quote) != null) {
                final StringBuilder sb = new StringBuilder();
                if (i > 0) sb.append(text, 0, i);
                return doEscapeString(sb, text, i, quote);
            }
        }
        return text;
    }

    protected static String doEscapeString(
            final StringBuilder sb, final String text, final int cur, final char quote) {
        int start = cur;
        for (int i = cur; i < text.length(); i++) {
            final String escaped = getEscapedChar(text.charAt(i), quote);
            if (escaped != null) {
                sb.append(text, start, i);
                sb.append(escaped);
                start = i + 1;
            }
        }
        sb.append(text, start, text.length());
        return sb.toString();
    }

    protected static String getEscapedChar(final char c, final char quote) {
        if (c == quote) {
            return "\\" + quote;
        }
        switch (c) {
            case '\t': return "\\t";
            case '\n': return "\\n";
            case '\r': return "\\r";
            case '\f': return "\\f";
            case '\b': return "\\b";
            case '\\': return "\\\\";
            default: return null;
        }
    }

    @Override
    public void close() throws Exception {
        this.tw.close();
    }
}
