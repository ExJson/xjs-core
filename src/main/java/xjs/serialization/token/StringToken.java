package xjs.serialization.token;

/**
 * Represents a single string token, either a single-quoted
 * string or a double-quoted string.
 *
 * <p>For example, the following text:
 *
 * <pre>
 *   "123"
 * </pre>
 *
 * <p>Counts as the following token:
 *
 * <pre>
 *   [ double('123') ]
 * </pre>
 */
public class StringToken extends Token {

    /**
     * The text represented by this token. Authors will need
     * to access this field directly to analyze the token.
     */
    public final String parsed;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start  The inclusive start index of this token.
     * @param end    The exclusive end index of this token.
     * @param offset The column of the start index.
     * @param type   The type of string represented by the token.
     * @param parsed The un-escaped, parsed text.
     */
    public StringToken(final int start, final int end, final int offset, final Type type, final String parsed) {
        super(start, end, offset, type);
        this.parsed = parsed;
    }
}
