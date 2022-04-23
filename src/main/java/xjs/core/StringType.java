package xjs.core;

import org.jetbrains.annotations.Nullable;
import xjs.serialization.util.ImplicitStringUtils;
import xjs.serialization.util.StringContext;
import xjs.serialization.writer.XjsWriter;

/**
 * Indicates the type of formatting used when creating string values. If the type is unknown
 * or not supported natively by the ecosystem, it will be stored as {@link StringType#NONE}.
 *
 * <p>Note that specifying the style when constructing a {@link JsonString} instance <b>does
 * not</b> guarantee that it will be reprinted in this format, as the {@link XjsWriter writer}
 * will guarantee the validity of {@link #IMPLICIT implicit} strings before printing them.
 */
public enum StringType {

    /**
     * A single-quoted string, e.g.
     * <pre>{@code
     *  'Hello, world!'
     * }</pre>
     */
    SINGLE,

    /**
     * A double-quoted string, e.g.
     * <pre>{@code
     *   "Hello, World!"
     * }</pre>
     */
    DOUBLE,

    /**
     * A triple-quoted, multiline string, e.g.
     * <pre>{@code
     *   '''
     *   Hello, World!
     *   '''
     * }</pre>
     */
    MULTI,

    /**
     * Any text written in raw space, stored <em>implicitly</em> as a string, e.g.
     * <pre>{@code
     *   Hello\, World!
     * }</pre>
     */
    IMPLICIT,

    /**
     * Any other type of string, where the type is either not supported or intentionally
     * not stored. For example, string types in regular JSON or JSON-C may not be preserved,
     * as alternate styles do not exist and thus the user is not implicitly specifying which
     * type to use.
     */
    NONE;

    /**
     * Fast algorithm for selecting a "best-fit" string type for the given text. This algorithm
     * assumes that the input will be regular textual data, including names, descriptions, and
     * any other non-expressive data.
     *
     * @param text The text being analyzed.
     * @return An appropriate string type to use; either single, double, or multi.
     */
    public static StringType fast(final String text) {
        for (int i = 0; i < text.length(); i++) {
            switch (text.charAt(i)) {
                case '\n': return MULTI;
                case '\'': return DOUBLE;
            }
        }
        return SINGLE;
    }

    /**
     * Comprehensive algorithm for selecting a "best-fit" string type for the given text.
     *
     * <p>In addition to regular textual data, this algorithm is valid for code-like
     * expressions and will account for balance.
     *
     * <p>One consequence of using this algorithm is that it will cause numeric data and
     * JSON-literals to be reprinted literally. For example, when given the following input:
     *
     *  <pre>
     *   "1234.56"
     * </pre>
     *
     * <p>This data will be reprinted literally:
     *
     * <pre>
     *   1234.56
     * </pre>
     *
     * @param text <b>Any</b> data represented as text.
     * @return An appropriate string type for reprinting the data.
     */
    public static StringType select(final String text) {
        if (text.isEmpty()) {
            return IMPLICIT;
        } else if (ImplicitStringUtils.isPunctuation(text.charAt(0))) {
            return fast(text);
        } else if (text.length() == ImplicitStringUtils.indexOf(text, 0, StringContext.VALUE)) {
            return IMPLICIT;
        }
        return fast(text);
    }

    /**
     * Determines which string type to use for the given value, or else
     * returns {@link #NONE}.
     *
     * @param value The value being analyzed.
     * @return The most appropriate type for this value.
     */
    public static StringType fromValue(final @Nullable JsonValue value) {
        if (value == null) {
            return NONE;
        } else if (value instanceof JsonString) {
            return ((JsonString) value).getStringType();
        }
        return value.isPrimitive() ? IMPLICIT : NONE;
    }
}
