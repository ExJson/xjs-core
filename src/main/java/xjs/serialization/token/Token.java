package xjs.serialization.token;

/**
 * Represents a single token: either a character, group of characters,
 * or any other sequence of tokens.
 */
public class Token {

    /**
     * The inclusive start index of this token.
     */
    public final int start;

    /**
     * The exclusive end index of this token.
     */
    public final int end;

    /**
     * The column of the start index.
     */
    public final int offset;

    /**
     * The type of token.
     */
    public final Type type;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param offset   The column of the start index.
     * @param type     The type of token.
     */
    public Token(final int start, final int end, final int offset, final Type type) {
        this.start = start;
        this.end = end;
        this.offset = offset;
        this.type = type;
    }

    /**
     * Creates a slice of the given {@link CharSequence} representing
     * the region described by this token.
     *
     * @param reference The reference text being sliced.
     * @return The string subsequence of the given reference text.
     */
    public String textOf(final CharSequence reference) {
        return reference.subSequence(this.start, this.end).toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof Token) {
            final Token t = (Token) o;
            return this.start == t.start
                && this.end == t.end
                && this.offset == t.offset
                && this.type == t.type;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.type + "(start:" + this.start + ",end:" + this.end + ",offset:" + this.offset + ")";
    }

    /**
     * The type of token represented by this wrapper.
     */
    public enum Type {

        /**
         * A single word, matching the pattern \\w+
         */
        WORD,

        /**
         * Any non-word stream of characters
         */
        SYMBOL,

        /**
         * Represents any sequence of numeric symbols.
         */
        NUMBER,

        /**
         * A single-quoted string.
         */
        SINGLE,

        /**
         * A double-quoted string.
         */
        DOUBLE,

        /**
         * A triple-quoted string.
         */
        TRIPLE,

        /**
         * An open stream of tokens with no encapsulation.
         */
        OPEN,

        /**
         * Any stream of tokens encapsulated inside of braces.
         */
        BRACES,

        /**
         * Any stream of tokens encapsulated inside of brackets.
         */
        BRACKETS,

        /**
         * Any stream of tokens encapsulated inside of parentheses.
         */
        PARENTHESES,

        /**
         * A single-line comment
         */
        LINE,

        /**
         * A multi-line comment
         */
        MULTI,

        /**
         * A line-break, either \n or \r\n
         */
        BREAK
    }
}