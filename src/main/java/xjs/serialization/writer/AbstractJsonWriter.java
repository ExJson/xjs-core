package xjs.serialization.writer;

import xjs.core.JsonReference;
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
    protected final String eol;
    protected final String indent;
    protected final int emptyLines;

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
        this.outputComments = true;
        this.indent = "  ";
        this.emptyLines = -1;
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
        this.indent = options.getIndent();
        this.emptyLines = options.getEmptyLines();
    }

    public final void write(final JsonValue value) throws IOException {
        this.write(value, 0);
    }

    public abstract void write(final JsonValue value, final int level) throws IOException;

    protected void nl(final int level, final JsonValue value) throws IOException {
        if (this.format) {
            for (int i = 0; i < Math.max(0, value.getLinesAbove()); i++) {
                this.tw.write(this.eol);
            }
            for (int i = 0; i < level; i++) {
                this.tw.write(this.indent);
            }
        }
    }

    protected int linesAbove(final JsonValue value) {
        if (this.format) {
            return Math.max(0, value.getLinesAbove());
        }
        return 0;
    }

    protected void separate(final int level, final JsonValue value) throws IOException {
        if (this.format) {
            final int lines = Math.max(0, value.getLinesBetween());
            for (int i = 0; i < lines; i++) {
                this.tw.write(this.eol);
            }
            if (lines > 0) {
                for (int i = 0; i < level; i++) {
                    this.tw.write(this.indent);
                }
            } else {
                this.tw.write(' ');
            }
        }
    }

    protected int linesBetween(final JsonValue value) {
        if (this.format) {
            return Math.max(0, value.getLinesBetween());
        }
        return 0;
    }

    protected void writeInteger(final long integer) throws IOException {
        this.tw.write(Long.toString(integer));
    }

    protected void writeDecimal(final double decimal) throws IOException {
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
