package xjs.serialization.writer;

import xjs.core.JsonContainer;
import xjs.core.JsonValue;
import xjs.serialization.JsonSerializationContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;

/**
 * The basic writer type to be used for all sorts of JSON formats.
 */
public abstract class AbstractJsonWriter implements AutoCloseable {

    protected final boolean format;
    protected final Writer tw;
    protected final boolean allowCondense;
    protected final boolean bracesSameLine;
    protected final boolean nestedSameLine;
    protected final boolean omitRootBraces;
    protected final boolean outputComments;
    protected final boolean omitQuotes;
    protected final String eol;
    protected final String indent;
    protected final int minSpacing;
    protected final int maxSpacing;
    protected final int lineSpacing;
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
        this.omitQuotes = true;
        this.outputComments = format;
        this.indent = "  ";
        this.minSpacing = 0;
        this.maxSpacing = Integer.MAX_VALUE;
        this.lineSpacing = format ? 1 : 0;
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
        this.omitQuotes = options.isOmitQuotes();
        this.indent = options.getIndent();
        this.minSpacing = options.getMinSpacing();
        this.maxSpacing = options.getMaxSpacing();
        this.lineSpacing = options.getLineSpacing();
        this.separator = options.getSeparator();
    }

    /**
     * Appends a {@link JsonValue} of <em>any kind</em> into the writer being
     * wrapped by this object.
     *
     * @param value The value being serialized.
     * @throws IOException If the underlying writer throws an {@link IOException}.
     */
    public void write(final JsonValue value) throws IOException {
        this.writeLinesAbove(-1, true, false, value);
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

    protected void writeLinesAbove(
            final int level, final boolean top, final boolean condensed, final JsonValue value) throws IOException {
        if (this.format) {
            final int lines =
                this.getNumLinesAbove(value.getLinesAbove(), level, condensed, top);
            if (lines > 0) {
                this.nl(lines, level);
            }
        }
    }

    protected int getNumLinesAbove(final int lines, final int level, final boolean condensed, final boolean top) {
        if (lines < 0) {
            return this.getDefaultLinesAbove(level, top);
        } else if (!(top && level < 1) && !this.allowCondense) {
            return Math.max(1, lines);
        } else if (level >= 0) {
            return this.limitLines(lines, condensed, top);
        }
        return lines;
    }

    private int getDefaultLinesAbove(final int level, final boolean top) {
        if (level < 0) {
            return 0;
        } else if (top) {
            if (level > 0) {
                return Math.max(1, this.lineSpacing - 1);
            }
            return this.lineSpacing - 1;
        }
        return this.lineSpacing;
    }

    protected void nl(final int level) throws IOException {
        if (this.format) {
            this.tw.write(this.eol);
            for (int i = 0; i < level; i++) {
                this.tw.write(this.indent);
            }
        }
    }

    protected void nl(final int lines, final int level) throws IOException {
        for (int i = 0; i < lines; i++) {
            this.tw.write(this.eol);
        }
        for (int i = 0; i < level; i++) {
            this.tw.write(this.indent);
        }
    }

    protected boolean isCondensed(final JsonValue value) {
        if (this.allowCondense && value.isContainer()) {
            final JsonContainer c = value.asContainer();
            if (!c.isEmpty()) {
                if (c.getReference(0).getOnly().getLinesAbove() != 0) {
                    return false;
                } // Intentionally shallow check for formatting purposes
                return c.size() == 1 || c.getReference(c.size() - 1).getOnly().getLinesAbove() == 0;
            }
        }
        return false;
    }

    protected void separate(int level, final JsonValue value) throws IOException {
        if (this.format) {
            int lines = Math.max(0, value.getLinesBetween());
            if (lines == 0 && value.isContainer() && !this.bracesSameLine) {
               lines = 1;
               level -= 1;
            }
            // Ignore min lines between keys and values.
            lines = Math.min(lines, this.maxSpacing);
            if (lines > 0) {
                this.nl(lines, level);
            } else {
                this.tw.write(this.separator);
            }
        }
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
            final int lines = this.getLinesTrailing(c.getLinesTrailing(), c, level);
            if (lines > 0) {
                this.nl(lines, level);
            }
        }
    }

    protected int getLinesTrailing(final int lines, final JsonContainer c, final int level) {
        if (lines < 0 && c.size() > 0) {
            return this.getDefaultLinesTrailing(level);
        }
        return this.limitLines(lines, this.isCondensed(c), true);
    }

    private int getDefaultLinesTrailing(final int level) {
        if (level >= 0) {
            return Math.max(1, this.lineSpacing - 1);
        }
        return this.lineSpacing - 1;
    }

    protected int limitLines(final int lines, final boolean condensed, final boolean topOrBottom) {
        if (condensed) { // Condensed arrays are allowed to have 0 lines between values.
            return Math.min(lines, this.maxSpacing);
        } else if (topOrBottom) { // The top and bottom of each container should be slightly smaller.
            return Math.max(Math.min(lines, this.maxSpacing - 1), this.minSpacing - 1);
        }
        return Math.max(Math.min(lines, this.maxSpacing), this.minSpacing);
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
