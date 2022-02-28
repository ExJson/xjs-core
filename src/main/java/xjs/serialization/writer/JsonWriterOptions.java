package xjs.serialization.writer;

import xjs.serialization.JsonSerializationContext;

public class JsonWriterOptions {

    private boolean allowCondense = true;
    private boolean bracesSameLine = true;
    private boolean nestedSameLine = false;
    private boolean omitRootBraces = true;
    private boolean outputComments = true;
    private String eol = JsonSerializationContext.getEol();
    private String indent = "  ";
    private String separator = " ";
    private int emptyLines = -1;

    public boolean isAllowCondense() {
        return allowCondense;
    }

    public JsonWriterOptions setAllowCondense(final boolean allowCondense) {
        this.allowCondense = allowCondense;
        return this;
    }

    public boolean isBracesSameLine() {
        return bracesSameLine;
    }

    public JsonWriterOptions setBracesSameLine(final boolean bracesSameLine) {
        this.bracesSameLine = bracesSameLine;
        return this;
    }

    public boolean isNestedSameLine() {
        return nestedSameLine;
    }

    public JsonWriterOptions setNestedSameLine(final boolean nestedSameLine) {
        this.nestedSameLine = nestedSameLine;
        return this;
    }

    public boolean isOmitRootBraces() {
        return omitRootBraces;
    }

    public JsonWriterOptions setOmitRootBraces(final boolean omitRootBraces) {
        this.omitRootBraces = omitRootBraces;
        return this;
    }

    public boolean isOutputComments() {
        return outputComments;
    }

    public JsonWriterOptions setOutputComments(final boolean outputComments) {
        this.outputComments = outputComments;
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
        return indent;
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

    public int getEmptyLines() {
        return emptyLines;
    }

    public JsonWriterOptions setEmptyLines(final int emptyLines) {
        this.emptyLines = emptyLines;
        return this;
    }

}
