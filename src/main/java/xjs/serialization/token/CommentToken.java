package xjs.serialization.token;

import xjs.comments.CommentStyle;

/**
 * Represents a single comment token: either a line, hash,
 * block comment, or other formatted comment variety.
 */
public class CommentToken extends ParsedToken {
    private final CommentStyle commentStyle;
    private boolean isParsed;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start  The inclusive start index of this token.
     * @param end    The exclusive end index of this token.
     * @param line   The inclusive line number of this token.
     * @param offset The column of the start index.
     * @param type   The style of comment represented by this token.
     * @param text   The parsed or un-parsed text of the token.
     */
    public CommentToken(
            final int start, final int end, final int line, final int offset,
            final CommentStyle type, final String text) {
        super(start, end, line, offset, TokenType.COMMENT, text);
        this.commentStyle = type;
        this.isParsed = false;
    }

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param line     The inclusive line number of this token.
     * @param lastLine The inclusive line number at the end of this token.
     * @param offset   The column of the start index.
     * @param type     The style of comment represented by this token.
     * @param text   The parsed or un-parsed text of the token.
     */
    public CommentToken(
            final int start, final int end, final int line, final int lastLine,
            final int offset, final CommentStyle type, final String text) {
        super(start, end, line, lastLine, offset, TokenType.COMMENT, text);
        this.commentStyle = type;
        this.isParsed = false;
    }

    /**
     * Constructs a new Comment Token with effectively no scope.
     *
     * @param text The text of the comment.
     */
    public CommentToken(final String text) {
        super(TokenType.COMMENT, text);
        this.commentStyle = CommentStyle.LINE;
        this.isParsed = false;
    }

    @Override
    public CommentStyle commentStyle() {
        return this.commentStyle;
    }
}
