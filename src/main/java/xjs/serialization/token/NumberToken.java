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
 *   [ number(123), ]
 * </pre>
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
     * @param reference A reference to the original source of this token.
     * @param start     The inclusive start index of this token.
     * @param end       The exclusive end index of this token.
     * @param offset    The column of the start index.
     * @param number    The number captured by the token.
     */
    public NumberToken(
            final String reference, final int start, final int end, final int offset, final double number) {
        super(reference, start, end, offset, Type.NUMBER);
        this.number = number;
    }
}