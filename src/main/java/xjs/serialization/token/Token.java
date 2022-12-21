package xjs.serialization.token;

/**
 * Represents a single token: either a character, group of characters,
 * or any other sequence of tokens.
 */
public class Token {

    /**
     * The inclusive start index of this token.
     */
    protected int start;

    /**
     * The exclusive end index of this token.
     */
    protected int end;

    /**
     * The inclusive line number of this token.
     */
    protected int line;

    /**
     * The inclusive line number at the end of this token.
     */
    protected int lastLine;

    /**
     * The column of the start index.
     */
    protected int offset;

    /**
     * The type of token.
     */
    protected Type type;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param line     The inclusive line number of this token.
     * @param offset   The column of the start index.
     * @param type     The type of token.
     */
    public Token(final int start, final int end, final int line, final int offset, final Type type) {
        this.start = start;
        this.end = end;
        this.line = line;
        this.lastLine = line;
        this.offset = offset;
        this.type = type;
    }

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param line     The inclusive line number of this token.
     * @param lastLine The inclusive end line number of this token.
     * @param offset   The column of the start index.
     * @param type     The type of token.
     */
    public Token(
            final int start, final int end, final int line, final int lastLine, final int offset, final Type type) {
        this.start = start;
        this.end = end;
        this.line = line;
        this.lastLine = lastLine;
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

    /**
     * Determines whether this token represents the given symbol.
     *
     * @param symbol The expected symbol.
     * @return true, if this token matches the symbol.
     */
    public boolean isSymbol(final char symbol) {
        return false;
    }

    public int start() {
        return this.start;
    }

    public int end() {
        return this.end;
    }

    public int line() {
        return this.line;
    }

    public int lastLine() {
        return this.lastLine;
    }

    public int offset() {
        return this.offset;
    }

    public Type type() {
        return this.type;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof Token) {
            final Token t = (Token) o;
            return this.start == t.start
                && this.end == t.end
                && this.line == t.line
                && this.offset == t.offset
                && this.type == t.type;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.type + "(start:" + this.start + ",end:" + this.end + ",line:" + this.line + ",offset:" + this.offset + ")";
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
        SINGLE_QUOTE,

        /**
         * A double-quoted string.
         */
        DOUBLE_QUOTE,

        /**
         * A triple-quoted string.
         */
        TRIPLE_QUOTE,

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
         * A single-line, hash-style comment
         */
        HASH_COMMENT,

        /**
         * A single-line, C-style comment
         */
        LINE_COMMENT,

        /**
         * A multi-line, block comment
         */
        BLOCK_COMMENT,

        /**
         * A line-break, either \n or \r\n
         */
        BREAK
    }
}