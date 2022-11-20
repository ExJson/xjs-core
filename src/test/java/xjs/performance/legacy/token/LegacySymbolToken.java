package xjs.performance.legacy.token;

/**
 * Represents a single symbol or special character. The type of
 * token captured by this object can be represented by the
 * following pattern:
 *
 * <pre>
 *   [^\w\s]
 * </pre>
 *
 * <p>Note that this token does not necessarily follow or
 * precede any whitespace.
 *
 * <p>For example, the following text:
 *
 * <pre>
 *   hello123?
 * </pre>
 *
 * <p>Counts as <em>two</em> tokens:
 *
 * <pre>
 *   [ word('hello123'), symbol('?')]
 * </pre>
 *
 * <p>Likewise, groups of symbols are considered multiple symbol
 * tokens. Authors will need to account for multi-character
 * symbols manually.
 *
 * <p>This text:
 *
 * <pre>
 *   ?:
 * </pre>
 *
 * <p><em>Also</em> counts as two tokens:
 *
 * <pre>
 *   [ symbol('?'), symbol(':') ]
 * </pre>
 */
public class LegacySymbolToken extends LegacyToken {

    /**
     * The single symbol represented by this token. Authors
     * will need to access this character directly to analyze
     * the token.
     */
    public final char symbol;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param reference A reference to the original source of this token.
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param offset   The column of the start index.
     * @param symbol   The raw symbol represented by this token.
     */
    public LegacySymbolToken(
            final String reference, final int start, final int end, final int offset, final char symbol) {
        super(reference, start, end, offset, Type.SYMBOL);
        this.symbol = symbol;
    }
}