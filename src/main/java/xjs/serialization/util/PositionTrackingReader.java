package xjs.serialization.util;

import xjs.exception.SyntaxException;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * A highly optimized counterpart to {@link Reader} which reduces the amount of
 * read operations to be performed on the underlying input stream. This
 * implementation is <b>not thread-safe</b>. It deliberately <b>deviates from
 * the contract</b> of {@link Reader}. In particular, it does not flush or close
 * the wrapped reader, nor does it ensure that the wrapped reader is open.
 */
public abstract class PositionTrackingReader implements Closeable {

    protected static final int DEFAULT_BUFFER_SIZE = 1024;
    protected static final int MIN_BUFFER_SIZE = 8;

    public int index;
    public int line;
    public int column;
    public int linesSkipped;
    public int current;

    protected StringBuilder capture;
    protected int captureStart;

    protected PositionTrackingReader() {
        this.index = -1;
        this.line = 1;
        this.column = -1;
        this.captureStart = -1;
    }

    public static PositionTrackingReader fromString(final String s) {
        return new DirectStringReader(s);
    }

    public static PositionTrackingReader fromIs(final InputStream is) throws IOException {
        return new DirectInputStreamReader(is, DEFAULT_BUFFER_SIZE, false);
    }

    public static PositionTrackingReader fromIs(
            final InputStream is, final boolean captureFullText) throws IOException {
        return new DirectInputStreamReader(is, DEFAULT_BUFFER_SIZE, captureFullText);
    }

    public static PositionTrackingReader fromIs(
            final InputStream is, final int size, final boolean captureFullText) throws IOException {
        if (size < MIN_BUFFER_SIZE) {
            throw new IllegalArgumentException("buffer size < " + MIN_BUFFER_SIZE);
        }
        return new DirectInputStreamReader(is, size, captureFullText);
    }

    public abstract CharSequence getFullText();

    protected abstract void appendToCapture();

    protected abstract String slice();

    public abstract void read() throws IOException;

    public boolean readIf(final char c) throws IOException {
        if (this.current != c) {
            return false;
        }
        this.read();
        return true;
    }

    public void expect(final char c) throws IOException {
        if (!this.readIf(c)) {
            throw this.expected(c);
        }
    }

    public String readToEnd() throws IOException {
        do {
            this.read();
        } while (!this.isEndOfText());
        return this.getFullText().toString();
    }

    public void startCapture() {
        if (this.capture == null) {
            this.capture = new StringBuilder();
        }
        this.captureStart = this.index;
    }

    public void pauseCapture() {
        this.appendToCapture();
        this.captureStart = -1;
    }

    public String endCapture() {
        final String captured;
        if (this.capture.length() > 0) {
            this.appendToCapture();
            captured = this.capture.toString();
            this.capture.setLength(0);
        } else {
            captured = this.slice();
        }
        this.captureStart = -1;
        return captured;
    }

    public void skipWhitespace() throws IOException {
        this.skipWhitespace(true);
    }

    public void skipWhitespace(final boolean reset) throws IOException {
        if (reset) {
            this.linesSkipped = 0;
        }
        while (this.isWhitespace()) {
            this.read();
        }
    }

    public void skipLineWhitespace() throws IOException {
        while (this.isLineWhitespace()) {
            this.read();
        }
    }

    public void skipToOffset(final int offset) throws IOException {
        for (int i = 0; i < offset; i++) {
            if (!this.isLineWhitespace()) {
                return;
            }
            this.read();
        }
    }

    public void skipToNL() throws IOException {
        while (this.current != '\n' && this.current != -1) {
            this.read();
        }
    }

    public boolean readDigit() throws IOException {
        if (!this.isDigit()) {
            return false;
        }
        this.read();
        return true;
    }

    public void readAllDigits() throws IOException {
        while (this.isDigit()) {
            this.read();
        }
    }

    public double readNumber() throws IOException {
        this.startCapture();
        this.readIf('-');

        final int firstDigit = this.current;
        if (!this.readDigit()) {
            throw this.expected("digit");
        }
        if (firstDigit != '0') {
            this.readAllDigits();
        }
        this.readDecimal();
        this.readExponent();

        return Double.parseDouble(this.endCapture());
    }

    public boolean readDecimal() throws IOException {
        if (!this.readIf('.')) {
            return false;
        }
        if (!this.readDigit()) {
            throw this.expected("digit");
        }
        this.readAllDigits();
        return true;
    }

    public boolean readExponent() throws IOException {
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

    public String readQuoted(final char quote) throws IOException {
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
            this.capture.append(quote);
            this.read();
            return;
        }
        switch (this.current) {
            case '"':
            case '/':
            case '\\':
                this.capture.append((char) this.current);
                break;
            case 'b':
                this.capture.append('\b');
                break;
            case 'f':
                this.capture.append('\f');
                break;
            case 'n':
                this.capture.append('\n');
                break;
            case 'r':
                this.capture.append('\r');
                break;
            case 't':
                this.capture.append('\t');
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
                this.capture.append((char) Integer.parseInt(new String(hexChars), 16));
                break;
            default:
                throw this.expected("valid escape sequence");
        }
        this.read();
    }

    public boolean isLineWhitespace() {
        return this.current == ' ' || this.current == '\t' || this.current == '\r';
    }

    public boolean isWhitespace() {
        return this.current == ' '
            || this.current == '\t'
            || this.current == '\n'
            || this.current == '\r';
    }

    public boolean isDigit() {
        return this.current >= '0' && this.current <= '9';
    }

    public boolean isHexDigit() {
        return this.current >= '0' && this.current <= '9'
            || this.current >= 'a' && this.current <= 'f'
            || this.current >= 'A' && this.current <= 'F';
    }

    public boolean isEndOfText() {
        return this.current == -1;
    }

    public SyntaxException expected(final char expected) {
        return SyntaxException.expected(expected, this.line, this.column);
    }

    public SyntaxException expected(final String expected) {
        return SyntaxException.expected(expected, this.line, this.column);
    }

    public SyntaxException unexpected() {
        return this.unexpected((char) this.current);
    }

    public SyntaxException unexpected(final char unexpected) {
        return SyntaxException.unexpected(unexpected, this.line, this.column);
    }

    public SyntaxException unexpected(final String unexpected) {
        return SyntaxException.unexpected(unexpected, this.line, this.column);
    }

    private static class DirectInputStreamReader extends PositionTrackingReader {
        final InputStreamReader reader;
        final char[] buffer;

        StringBuilder out;
        int bufferIndex;
        int fill;

        DirectInputStreamReader(
                final InputStream is, final int size, final boolean captureFullText) throws IOException {
            this.reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            this.buffer = new char[size];
            this.bufferIndex = 0;
            this.fill = 0;
            if (captureFullText) this.out = new StringBuilder();
            this.read();
        }

        @Override
        public CharSequence getFullText() {
            if (this.out == null) {
                throw new IllegalStateException("output not configured");
            }
            return this.out;
        }

        public void startCapture() {
            if (this.capture == null) {
                this.capture = new StringBuilder();
            }
            this.captureStart = this.bufferIndex - 1;
        }

        @Override
        protected void appendToCapture() {
            final int end = this.current == -1 ? this.bufferIndex : this.bufferIndex - 1;
            this.capture.append(this.buffer, this.captureStart, end - this.captureStart);
        }

        @Override
        protected String slice() {
            final int end = this.current == -1 ? this.bufferIndex : this.bufferIndex - 1;
            return new String(this.buffer, this.captureStart, end - this.captureStart);
        }

        public void read() throws IOException {
            if (this.bufferIndex == this.fill) {
                if (this.captureStart != -1) {
                    this.bufferIndex++; // Force the last index into capture
                    this.appendToCapture();
                    this.captureStart = 0;
                }
                this.fill = this.reader.read(this.buffer, 0, this.buffer.length);
                this.bufferIndex = 0;
                if (this.fill == -1) {
                    this.index++;
                    this.current = -1;
                    return;
                }
                if (this.out != null) {
                    this.out.append(this.buffer, 0, this.fill);
                }
            }
            if (this.current == '\n') {
                this.line++;
                this.linesSkipped++;
                this.column = -1;
            }
            this.index++;
            this.column++;
            this.current = this.buffer[this.bufferIndex++];
        }

        @Override
        public void close() throws IOException {
            this.reader.close();
        }
    }

    private static class DirectStringReader extends PositionTrackingReader {
        final String s;

        DirectStringReader(final String s) {
            this.s = s;
            this.read();
        }

        @Override
        public String getFullText() {
            return this.s;
        }

        @Override
        protected void appendToCapture() {
            this.capture.append(this.s, this.captureStart, this.index);
        }

        @Override
        protected String slice() {
            return this.s.substring(this.captureStart, this.index);
        }

        @Override
        public void read() {
            if (this.index == this.s.length() - 1) {
                this.index = this.s.length();
                this.current = -1;
                return;
            }
            if (this.current == '\n') {
                this.line++;
                this.linesSkipped++;
                this.column = -1;
            }
            this.column++;
            this.current = this.s.charAt(++this.index);
        }

        @Override
        public void close() {}
    }
}
