package xjs.performance.legacy.writer;

import xjs.comments.CommentType;
import xjs.core.JsonContainer;
import xjs.core.JsonValue;
import xjs.serialization.JsonContext;
import xjs.serialization.writer.JsonWriterOptions;
import xjs.serialization.writer.ValueWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;

/**
 * A basic writer type providing a writer and some formatting options.
 */
public abstract class LegacyAbstractJsonWriter implements ValueWriter {

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
    protected final int defaultSpacing;
    protected final boolean smartSpacing;
    protected final String separator;

    protected LegacyAbstractJsonWriter(final File file, final boolean format) throws IOException {
        this(new FileWriter(file), format);
    }

    protected LegacyAbstractJsonWriter(final Writer writer, final boolean format) {
        this.format = format;
        this.tw = writer;
        this.eol = JsonContext.getEol();
        this.allowCondense = true;
        this.bracesSameLine = true;
        this.nestedSameLine = false;
        this.omitRootBraces = true;
        this.omitQuotes = true;
        this.outputComments = format;
        this.indent = "  ";
        this.minSpacing = 0;
        this.maxSpacing = Integer.MAX_VALUE;
        this.defaultSpacing = format ? 1 : 0;
        this.smartSpacing = false;
        this.separator = format ? " " : "";
    }

    protected LegacyAbstractJsonWriter(final File file, final JsonWriterOptions options) throws IOException {
        this(new FileWriter(file), options);
    }

    protected LegacyAbstractJsonWriter(final Writer writer, final JsonWriterOptions options) {
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
        this.defaultSpacing = options.getDefaultSpacing();
        this.smartSpacing = options.isSmartSpacing();
        this.separator = options.getSeparator();
    }

    /**
     * Appends a {@link JsonValue} of <em>any kind</em> into the writer being
     * wrapped by this object.
     *
     * @param value The value being serialized.
     * @throws IOException If the underlying writer throws an {@link IOException}.
     */
    @Override
    public void write(final JsonValue value) throws IOException {
        this.writeLinesAbove(-1, null, null, false, value);
        this.write(value, 0);
    }

    protected abstract void write(final JsonValue value, final int level) throws IOException;

    protected void open(final JsonContainer c, final boolean condensed, final char opener) throws IOException {
        this.tw.write(opener);
        if (this.format && this.allowCondense) {
            if (this.shouldSeparateOpener(c, condensed)) {
                this.tw.write(this.separator);
            }
        }
    }

    protected boolean shouldSeparateOpener(final JsonContainer c, final boolean condensed) {
        return c.size() > 1 && c.getReference(0).getOnly().getLinesAbove() == 0;
    }

    protected void writeLinesAbove(
            int level, JsonValue parent, JsonValue previous, boolean condensed, JsonValue value) throws IOException {
        if (this.format) {
            final int lines = this.getNumLinesAbove(level, parent, previous, condensed, value);
            if (lines > 0) {
                this.nl(lines, level);
            }
        }
    }

    protected int getNumLinesAbove(int level, JsonValue parent, JsonValue previous, boolean condensed, JsonValue value) {
        final int lines = value.getLinesAbove();
        if (lines < 0) {
            return this.getDefaultLinesAbove(level, parent, previous, value);
        } else if (!(previous == null && level < 1) && !this.allowCondense) {
            return Math.max(1, lines);
        } else if (level >= 0) {
            return this.limitLines(lines, condensed, previous == null);
        }
        return lines;
    }

    private int getDefaultLinesAbove(int level, JsonValue parent, JsonValue previous, JsonValue value) {
        if (level < 0) {
            return 0;
        }
        final int spacing = this.getSpacing(parent, previous, value);
        if (previous == null) {
            if (level > 0) {
                return Math.max(1, spacing - 1);
            }
            return spacing - 1;
        }
        return spacing;
    }

    private int getSpacing(final JsonValue parent, final JsonValue previous, final JsonValue value) {
        if (this.smartSpacing && parent != null && parent.isObject()) {
            if (this.requiresSmartSpace(value) || this.requiresSmartSpace(previous)) {
                return this.defaultSpacing + 1;
            }
        }
        return this.defaultSpacing;
    }

    private boolean requiresSmartSpace(final JsonValue value) {
        return value != null && (value.isContainer() || value.hasComment(CommentType.HEADER));
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
            return Math.max(1, this.defaultSpacing - 1);
        }
        return this.defaultSpacing - 1;
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
