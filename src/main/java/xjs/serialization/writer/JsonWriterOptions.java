package xjs.serialization.writer;

import xjs.serialization.JsonSerializationContext;

public class JsonWriterOptions {

    private boolean allowCondense = true;
    private boolean bracesSameLine = true;
    private boolean nestedSameLine = false;
    private boolean omitRootBraces = true;
    private boolean outputComments = true;
    private boolean compressed = true;
    private String eol = JsonSerializationContext.getEol();
    private String indent = "  ";
    private String separator = " ";
    private int minSpacing = 0;
    private int maxSpacing = Integer.MAX_VALUE;
    private int lineSpacing = 1;

    public JsonWriterOptions() {}

    public JsonWriterOptions(final JsonWriterOptions source) {
        this.allowCondense = source.allowCondense;
        this.bracesSameLine = source.bracesSameLine;
        this.nestedSameLine = source.nestedSameLine;
        this.omitRootBraces = source.omitRootBraces;
        this.outputComments = source.outputComments;
        this.compressed = source.compressed;
        this.eol = source.eol;
        this.indent = source.indent;
        this.separator = source.separator;
        this.minSpacing = source.minSpacing;
        this.maxSpacing = source.maxSpacing;
        this.lineSpacing = source.lineSpacing;
    }

    public boolean isAllowCondense() {
        return this.allowCondense;
    }

    public JsonWriterOptions setAllowCondense(final boolean allowCondense) {
        this.allowCondense = allowCondense;
        return this;
    }

    public boolean isBracesSameLine() {
        return this.bracesSameLine;
    }

    public JsonWriterOptions setBracesSameLine(final boolean bracesSameLine) {
        this.bracesSameLine = bracesSameLine;
        return this;
    }

    public boolean isNestedSameLine() {
        return this.nestedSameLine;
    }

    public JsonWriterOptions setNestedSameLine(final boolean nestedSameLine) {
        this.nestedSameLine = nestedSameLine;
        return this;
    }

    public boolean isOmitRootBraces() {
        return this.omitRootBraces;
    }

    public JsonWriterOptions setOmitRootBraces(final boolean omitRootBraces) {
        this.omitRootBraces = omitRootBraces;
        return this;
    }

    public boolean isOutputComments() {
        return this.outputComments;
    }

    public JsonWriterOptions setOutputComments(final boolean outputComments) {
        this.outputComments = outputComments;
        return this;
    }

    public boolean isCompressed() {
        return this.compressed;
    }

    public JsonWriterOptions setCompressed(final boolean compressed) {
        this.compressed = compressed;
        return this;
    }

    public String getEol() {
        return eol;
    }

    public JsonWriterOptions setEol(String eol) {
        this.eol = eol;
        return this;
    }

    public String getIndent() {
        return this.indent;
    }

    public JsonWriterOptions setIndent(final String indent) {
        this.indent = indent;
        return this;
    }

    public JsonWriterOptions setTabSize(final int num) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < num; i++) {
            sb.append(' ');
        }
        this.indent = sb.toString();
        return this;
    }

    public String getSeparator() {
        return this.separator;
    }

    public JsonWriterOptions setSeparator(final String separator) {
        this.separator = separator;
        return this;
    }

    public int getMinSpacing() {
        return this.minSpacing;
    }

    public JsonWriterOptions setMinSpacing(final int minSpacing) {
        this.minSpacing = minSpacing;
        return this;
    }

    public int getMaxSpacing() {
        return this.maxSpacing;
    }

    public JsonWriterOptions setMaxSpacing(final int maxSpacing) {
        this.maxSpacing = maxSpacing;
        return this;
    }

    public int getLineSpacing() {
        return this.lineSpacing;
    }

    public JsonWriterOptions setLineSpacing(final int lineSpacing) {
        this.lineSpacing = lineSpacing;
        return this;
    }
}
