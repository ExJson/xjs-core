package xjs.serialization.token;

/**
 * Represents a single numeric token, either in decimal or
 * scientific notation.
 *
 * <p>For example, the following text:
 *
 * <pre>
 *   123
 * </pre>
 *
 * <p>Counts as the following token:
 *
 * <pre>
 *   [ number(123) ]
 * </pre>
 *
 * <p><b>Implementors must be aware</b> that number tokens
 * may represent any valid double floating-point number in
 * Java format. <b>These tokens may not be legal</b> in
 * all JSON-derivative formats and may have to be manually
 * re-parsed in some cases.
 *
 * <p>If possible, avoid using the standard {@link
 * TokenStream} to generate tokens for such parsers.
 */
public class NumberToken extends Token {

    /**
     * The single number represented by this token. Authors
     * will need to access this number directly to analyze
     * the token.
     */
    public final double number;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start     The inclusive start index of this token.
     * @param end       The exclusive end index of this token.
     * @param offset    The column of the start index.
     * @param number    The number captured by the token.
     */
    public NumberToken(
            final int start, final int end, final int offset, final double number) {
        super(start, end, offset, Type.NUMBER);
        this.number = number;
    }
}