package xjs.serialization.parser;

import org.jetbrains.annotations.NotNull;
import xjs.core.JsonDecimal;
import xjs.core.JsonInteger;
import xjs.core.JsonValue;
import xjs.exception.SyntaxException;

import java.io.*;

public abstract class AbstractJsonParser {

    protected static final int MIN_BUFFER_SIZE = 10;
    protected static final int DEFAULT_BUFFER_SIZE = 1024;

    protected final Reader reader;
    protected final char[] buffer;
    protected int bufferOffset;
    protected int index;
    protected int fill;
    protected int line;
    protected int lineOffset;
    protected int linesSkipped;
    protected int current;
    protected StringBuilder captureBuffer;
    protected int captureStart;

    /*
     * |                      bufferOffset
     *                        v
     * [a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t]        < input
     *                       [l|m|n|o|p|q|r|s|t|?|?]    < buffer
     *                          ^               ^
     *                       |  index           fill
     */

    protected AbstractJsonParser(final String text) {
        this(new StringReader(text),
            Math.max(MIN_BUFFER_SIZE, Math.min(DEFAULT_BUFFER_SIZE, text.length())));
    }

    protected AbstractJsonParser(final File file) throws IOException {
        this(new FileReader(file));
    }

    protected AbstractJsonParser(final Reader reader) {
        this(reader, DEFAULT_BUFFER_SIZE);
    }

    protected AbstractJsonParser(final Reader reader, final int buffer) {
        this.reader = reader;
        this.buffer = new char[buffer];
        this.line = 1;
        this.captureStart = -1;
    }

    public abstract @NotNull JsonValue parse() throws IOException;

    protected void expect(final char c) throws IOException {
        if (!this.readIf(c)) {
            throw this.expected(c);
        }
    }

    protected String readQuoted(final char quote) throws IOException {
        this.read();
        this.startCapture();
        while (this.current != quote) {
            if (this.current == '\\') {
                this.pauseCapture();
                this.readEscape(quote);
                this.startCapture();
            } else if (this.current < 0x20) {
                throw this.expected("valid string character");
            } else {
                this.read();
            }
        }
        final String string = this.endCapture();
        this.read();
        return string;
    }

    protected void readEscape(final char quote) throws IOException {
        this.read();
        if (this.current == quote) {
            this.captureBuffer.append(quote);
            this.read();
            return;
        }
        switch (this.current) {
            case '"':
            case '/':
            case '\\':
                this.captureBuffer.append((char) this.current);
                break;
            case 'b':
                this.captureBuffer.append('\b');
                break;
            case 'f':
                this.captureBuffer.append('\f');
                break;
            case 'n':
                this.captureBuffer.append('\n');
                break;
            case 'r':
                this.captureBuffer.append('\r');
                break;
            case 't':
                this.captureBuffer.append('\t');
                break;
            case 'u':
                final char[] hexChars = new char[4];
                for(int i = 0; i < 4; i++) {
                    this.read();
                    if (!isHexDigit()) {
                        throw this.expected("hexadecimal digit");
                    }
                    hexChars[i] = (char) this.current;
                }
                this.captureBuffer.append((char) Integer.parseInt(new String(hexChars), 16));
                break;
            default:
                throw this.expected("valid escape sequence");
        }
        this.read();
    }

    protected JsonValue readNumber() throws IOException {
        this.startCapture();
        this.readIf('-');

        final int firstDigit = this.current;
        if (!this.readDigit()) {
            throw this.expected("digit");
        }
        if (firstDigit != '0') {
            this.readAllDigits();
        }
        final boolean decimal = this.readDecimal();
        final boolean exponent = this.readExponent();

        if (decimal || exponent) {
            return new JsonDecimal(Double.parseDouble(this.endCapture()));
        }
        return new JsonInteger(Long.parseLong(this.endCapture()));
    }

    protected boolean readDecimal() throws IOException {
        if (!this.readIf('.')) {
            return false;
        }
        if (!this.readDigit()) {
            throw this.expected("digit");
        }
        this.readAllDigits();
        return true;
    }

    protected boolean readExponent() throws IOException {
        if (!this.readIf('e') && !this.readIf('E')) {
            return false;
        }
        if (!this.readIf('+')) {
            this.readIf('-');
        }
        if (!this.readDigit()) {
            throw this.expected("digit");
        }
        this.readAllDigits();
        return true;
    }

    protected boolean readIf(final char c) throws IOException {
        if (this.current != c) {
            return false;
        }
        this.read();
        return true;
    }

    protected void readAllDigits() throws IOException {
        while (this.isDigit()) {
            this.read();
        }
    }

    protected boolean readDigit() throws IOException {
        if (!this.isDigit()) {
            return false;
        }
        this.read();
        return true;
    }

    protected void skipWhitespace() throws IOException {
        this.skipWhitespace(true);
    }

    protected void skipWhitespace(final boolean reset) throws IOException {
        if (reset) {
            this.linesSkipped = 0;
        }
        while (this.isWhitespace()) {
            this.read();
        }
    }

    protected void skipToOffset(final int offset) throws IOException {
        for (int i = 0; i < offset; i++) {
            if (!this.isLineWhitespace()) {
                return;
            }
            this.read();
        }
    }

    protected void read() throws IOException {
        if (this.index == this.fill) {
            if (this.captureStart != -1) {
                this.captureBuffer.append(this.buffer, this.captureStart, this.fill - this.captureStart);
                this.captureStart = 0;
            }
            this.bufferOffset += this.fill;
            this.fill = this.reader.read(this.buffer, 0, this.buffer.length);
            this.index = 0;
            if (this.fill == -1) {
                this.current = -1;
                return;
            }
        }
        if (this.current == '\n') {
            this.line++;
            this.linesSkipped++;
            this.lineOffset = this.bufferOffset + this.index;
        }
        this.current = this.buffer[this.index++];
    }

    protected void startCapture() {
        if (this.captureBuffer == null) {
            this.captureBuffer = new StringBuilder();
        }
        this.captureStart = this.index - 1;
    }

    protected void pauseCapture() {
        final int end = this.current == -1 ? this.index : this.index - 1;
        this.captureBuffer.append(this.buffer, this.captureStart, end - this.captureStart);
        this.captureStart = -1;
    }

    protected String endCapture() {
        final int end = this.current == -1 ? this.index : this.index - 1;
        final String captured;
        if (this.captureBuffer.length() > 0) {
            this.captureBuffer.append(this.buffer, this.captureStart, end - this.captureStart);
            captured = this.captureBuffer.toString();
            this.captureBuffer.setLength(0);
        } else {
            captured = new String(this.buffer, this.captureStart, end - this.captureStart);
        }
        this.captureStart = -1;
        return captured;
    }

    protected boolean isLineWhitespace() {
        return this.current == ' ' || this.current == '\t';
    }

    protected boolean isWhitespace() {
        return this.current == ' '
            || this.current == '\t'
            || this.current == '\n'
            || this.current == '\r';
    }

    protected boolean isDigit() {
        return this.current >= '0' && this.current <= '9';
    }

    protected boolean isHexDigit() {
        return this.current >= '0' && this.current <= '9'
            || this.current >= 'a' && this.current <= 'f'
            || this.current >= 'A' && this.current <= 'F';
    }

    protected boolean isEndOfText() {
        return this.current == -1;
    }

    protected SyntaxException expected(final char expected) {
        return SyntaxException.expected(expected, this.line, this.getColumn());
    }

    protected SyntaxException expected(final String expected) {
        return SyntaxException.expected(expected, this.line, this.getColumn());
    }

    protected SyntaxException unexpected(final char unexpected) {
        return SyntaxException.unexpected(unexpected, this.line, this.getColumn());
    }

    protected SyntaxException unexpected(final String unexpected) {
        return SyntaxException.unexpected(unexpected, this.line, this.getColumn());
    }

    protected int getColumn() {
        return this.bufferOffset + this.index - this.lineOffset - 1;
    }
}
