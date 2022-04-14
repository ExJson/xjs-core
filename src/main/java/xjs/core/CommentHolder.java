package xjs.core;

import xjs.serialization.util.CommentUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * Utility layer and container housing comments for any JSON value. This object exposes the position,
 * type, message, and entire formatting of each comment paired with a single JSON value.
 *
 * <p>Callers should <b>not expect</b> comment data to be stored as string values in the future, as
 * this implementation may change.
 */
public class CommentHolder {

    private String headerData;
    private String eolData;
    private String footerData;
    private String valueData;
    private String interiorData;

    /**
     * Indicates whether <b>any</b> comment is present within this container.
     *
     * @return <code>true</code> if any comment is present.
     */
    public boolean hasAny() {
        return this.headerData != null
            && this.eolData != null
            && this.footerData != null
            && this.valueData != null
            && this.interiorData != null;
    }

    /**
     * Indicates whether this container houses a specific type of container.
     *
     * @param type The type of comment being queried against.
     * @return <code>true</code> if this type of comment is present.
     */
    public boolean has(final CommentType type) {
        switch (type) {
            case HEADER: return this.headerData != null;
            case EOL: return this.eolData != null;
            case FOOTER: return this.footerData != null;
            case VALUE: return this.valueData != null;
            default: return this.interiorData != null;
        }
    }

    /**
     * Returns the <b>message</b> present in whichever comment is being queried.
     *
     * <p>For example, a holder containing this exact comment data:
     *
     * <pre>{@code
     *   "// this is a comment"
     * }</pre>
     *
     * <p>Will return this exact string:
     *
     * <pre>{@code
     *   "this is a comment"
     * }</pre>
     *
     * @param type The type of comment being queried.
     * @return The message of this comment, or else <code>""</code>.
     */
    public String get(final CommentType type) {
        return CommentUtils.strip(this.getData(type));
    }

    /**
     * Returns the entire contents of whichever comment is being queried. The return value of this
     * method includes all available formatting, including the comment indicators and any additional
     * lines after the comment message.
     *
     * <p>For example, a holder containing this exact data:
     *
     * <pre>{@code
     *   """
     *   // comment
     *
     *   # comment
     *   """
     * }</pre>
     *
     * <p>Will return the data exactly.
     *
     * @param type The type of comment being queried.
     * @return The entire comment data being stored by this container, or else <code>""</code>.
     */
    public String getData(final CommentType type) {
        switch (type) {
            case HEADER: return this.headerData != null ? this.headerData : "";
            case EOL: return this.eolData != null ? this.eolData : "";
            case FOOTER: return this.footerData != null ? this.footerData : "";
            case VALUE: return this.valueData != null ? this.valueData : "";
            default: return this.interiorData != null ? this.interiorData : "";
        }
    }

    /**
     * Sets the message and style for a given comment position.
     *
     * @param type  The type of comment being set in this container.
     * @param style The style of comment (e.g. hash or line).
     * @param text  The message of the comment being created.
     * @return <code>this</code>, for method chaining.
     */
    public CommentHolder set(final CommentType type, final CommentStyle style, final String text) {
        return this.setData(type, CommentUtils.format(style, text));
    }

    /**
     * Variant of {@link #set(CommentType, CommentStyle, String)} which appends a number of empty
     * lines at the end of the data.
     *
     * @param type  The type of comment being set in this container.
     * @param style The style of comment (e.g. hash or line).
     * @param text  The message of the comment being created.
     * @param lines The number of <em>empty</em> lines at the end of this message.
     * @return <code>this</code>, for method chaining.
     */
    public CommentHolder set(final CommentType type, final CommentStyle style, final String text, final int lines) {
        return this.setData(type, CommentUtils.format(style, text) + createLines(lines));
    }

    /**
     * Appends an additional comment <em>after</em> the existing comment at this position.
     *
     * <p>For example, if this container is already housing the following comment:
     *
     * <pre>{@code
     *   # line 1
     * }</pre>
     *
     * <p>If the following comment is appended:
     *
     * <pre>{@code
     *   CommentStyle.HASH, "line 2"
     * }</pre>
     *
     * <p>The data will be updated as follows:
     *
     * <pre>{@code
     *   # line 1
     *   # line 2
     * }</pre>
     *
     * @param type  The type of comment being set in this container.
     * @param style The style of comment (e.g. hash or line).
     * @param text  The message of the comment being appended.
     * @return <code>this</code>, for method chaining.
     */
    public CommentHolder append(final CommentType type, final CommentStyle style, final String text) {
        final String data = this.getData(type);
        if (data.isEmpty()) {
            return this.setData(type, CommentUtils.format(style, text));
        }
        return this.setData(type, data + "\n" + CommentUtils.format(style, text));
    }

    /**
     * Prepends an additional comment <em>before</em> the existing comment at this position.
     *
     * @param type  The type of comment being set in this container.
     * @param style The style of comment (e.g. hash or line).
     * @param text  The message of the comment being prepended.
     * @return <code>this</code>, for method chaining.
     */
    public CommentHolder prepend(final CommentType type, final CommentStyle style, final String text) {
        final String data = this.getData(type);
        if (data.isEmpty()) {
            return this.setData(type, CommentUtils.format(style, text));
        }
        return this.setData(type, CommentUtils.format(style, text) + "\n" + data);
    }

    /**
     * Sets the raw comment data for the given position. Note that the comment must be
     * syntactically valid or else it will produce a <b>silent error</b> when serialized.
     *
     * @param type The type of comment being set in this container.
     * @param data The raw, <b>already formatted</b> data being placed on the corresponding value.
     * @return <code>this</code>, for method chaining.
     */
    public CommentHolder setData(final CommentType type, final String data) {
        switch (type) {
            case HEADER: this.headerData = data; break;
            case EOL: this.eolData = data; break;
            case FOOTER: this.footerData = data; break;
            case VALUE: this.valueData = data; break;
            default: this.interiorData = data;
        }
        return this;
    }

    /**
     * Adds a number of additional, <em>empty</em> lines after the comment in its current state.
     *
     * @param type  The type comment being appended to.
     * @param lines The number of <em>empty</em> lines after the existing content.
     * @return <code>this</code>, for method chaining.
     */
    public CommentHolder setLinesAfter(final CommentType type, final int lines) {
        switch (type) {
            case HEADER: this.headerData = replaceLines(this.headerData, lines); break;
            case EOL: this.eolData = replaceLines(this.eolData, lines); break;
            case FOOTER: this.footerData = replaceLines(this.footerData, lines); break;
            case VALUE: this.valueData = replaceLines(this.valueData, lines); break;
            default: this.interiorData = replaceLines(this.interiorData, lines);
        }
        return this;
    }

    private static String replaceLines(final String data, final int lines) {
        if (data == null) {
            return lines > 0 ? createLines(lines) : null;
        }
        return data.replaceFirst("[\\s\\n]*$", createLines(lines));
    }

    private static String createLines(final int lines) {
        if (lines == 0) {
            return "";
        }
        final char[] newlines = new char[lines];
        Arrays.fill(newlines, '\n');
        return new String(newlines);
    }

    /**
     * Generates a clone of this object, housing the same comments.
     *
     * @return A clone of this object.
     */
    public CommentHolder copy() {
        final CommentHolder copy = new CommentHolder();
        copy.headerData = this.headerData;
        copy.eolData = this.eolData;
        copy.footerData = this.footerData;
        copy.valueData = this.valueData;
        copy.interiorData = this.interiorData;
        return copy;
    }

    @Override
    public int hashCode() {
        int result = 0;
        if (this.headerData != null) result = 31 * result + this.headerData.hashCode();
        if (this.eolData != null) result = 31 * result + this.eolData.hashCode();
        if (this.footerData != null) result = 31 * result + this.footerData.hashCode();
        if (this.valueData != null) result = 31 * result + this.valueData.hashCode();
        if (this.interiorData != null) result = 31 * result + this.interiorData.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof CommentHolder) {
            final CommentHolder other = (CommentHolder) o;
            return Objects.equals(this.headerData, other.headerData)
                && Objects.equals(this.eolData, other.eolData)
                && Objects.equals(this.footerData, other.footerData)
                && Objects.equals(this.valueData, other.valueData)
                && Objects.equals(this.interiorData, other.interiorData);
        }
        return false;
    }
}
