package xjs.serialization.token;

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
public class SymbolToken extends Token {

    /**
     * The single symbol represented by this token. Authors
     * will need to access this character directly to analyze
     * the token.
     */
    public final char symbol;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param line   The inclusive line number of this token.
     * @param offset   The column of the start index.
     * @param symbol   The raw symbol represented by this token.
     */
    public SymbolToken(final int start, final int end, final int line, final int offset, final char symbol) {
        this(start, end, line, offset, Type.SYMBOL, symbol);
    }

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param line   The inclusive line number of this token.
     * @param offset   The column of the start index.
     * @param type     The type of token.
     * @param symbol   The raw symbol represented by this token.
     */
    public SymbolToken(
            final int start, final int end, final int line, final int offset, final Type type, final char symbol) {
        super(start, end, line, offset, type);
        this.symbol = symbol;
    }

    @Override
    public boolean isSymbol(final char symbol) {
        return this.symbol == symbol;
    }
}