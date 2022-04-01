package xjs.serialization.util;

/**
 * Describes the context in which an implicit string might be found. This is
 * relevant because, conceptually, an implicit string can be any stream of
 * tokens at all, simply delimited by the other regular JSON tokens.
 * Therefore, its exact behavior is contextual.
 *
 * <p>The only possible cases where this would matter are keys and any other
 * value.
 */
public enum StringContext {

    /**
     * Describes any stream of tokens which can be found when an object key
     * would otherwise be expected.
     *
     * <p>For example, here's how a regular <em>single</em> quoted key might
     * look:
     *
     * <pre>{@code
     *   { 'key': 1234 }
     * }</pre>
     *
     * <p>However, if no quotes are found, the parser will simply scan for
     * the separator--a <code>:</code> token--and treat everything before it
     * as <em>implicitly</em> a string:
     *
     * <pre>{@code
     *   { []{}(): 1234 }
     * }</pre>
     */
    KEY,

    /**
     * Describes any stream of tokens which can be found whenever a regular
     * JSON value would otherwise be expected.
     *
     * <p>For example, here's an array of regular JSON values, including a
     * quoted string, a number, and an empty object:
     *
     * <pre>{@code
     *   [
     *     "string"
     *     1234
     *     {}
     *   ]
     * }</pre>
     *
     * <p>If a regular value is expected, but no regular JSON tokens are
     * found, they will be implicitly treated as strings:
     *
     * <pre>{@code
     *   [
     *     string  // <-- text is unquoted
     *     1234 56 // <-- the space cannot be parsed as a number
     *     {} {}   // <-- the extra, non-delimited object cannot be parsed
     *   ]
     * }</pre>
     *
     * <p>The above example translates to the following in regular JSON:
     *
     * <pre>{@code
     *   [
     *     "string",
     *     "1234 56",
     *     "{} {}"
     *   ]
     * }</pre>
     */
    VALUE
}
