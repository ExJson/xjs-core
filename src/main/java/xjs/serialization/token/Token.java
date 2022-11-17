package xjs.serialization.token;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a single token: either a character, group of characters,
 * or any other sequence of tokens.
 */
public class Token {
    public final int start;
    public final int end;
    public final int offset;
    public final Type type;
    protected final String reference;
    protected volatile @Nullable String text;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param reference A reference to the original source of this token.
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param offset   The column of the start index.
     * @param type     The type of token.
     */
    public Token(final String reference, final int start, final int end, final int offset, final Type type) {
        this.reference = reference;
        this.start = start;
        this.end = end;
        this.offset = offset;
        this.type = type;
    }

    public String getText() {
        if (this.text == null) {
            return this.text = this.reference.substring(this.start, this.end);
        }
        return text;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof Token) {
            final Token t = (Token) o;
            return this.start == t.start
                && this.end == t.end
                && this.offset == t.offset
                && this.type == t.type
                && this.reference.equals(t.reference);
        }
        return false;
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