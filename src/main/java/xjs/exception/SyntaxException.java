package xjs.exception;

public class SyntaxException extends RuntimeException {

    private final int line;
    private final int column;

    public SyntaxException(final String msg, final int line, final int column) {
        super(msg + " at " + line + ":" + column);
        this.line = line;
        this.column = column;
    }

    public static SyntaxException expected(final char expected, final int line, final int column) {
        return new SyntaxException("Expected '" + expected + "'", line, column);
    }

    public static SyntaxException expected(final String expected, final int line, final int column) {
        return new SyntaxException("Expected " + expected, line, column);
    }

    public static SyntaxException unexpected(final char unexpected, final int line, final int column) {
        return new SyntaxException("Unexpected '" + unexpected + "'", line, column);
    }

    public static SyntaxException unexpected(final String unexpected, final int line, final int column) {
        return new SyntaxException("Unexpected " + unexpected, line, column);
    }

    public int getLine() {
        return this.line;
    }

    public int getColumn() {
        return this.column;
    }
}
