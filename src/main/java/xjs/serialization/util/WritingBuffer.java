package xjs.serialization.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;


/**
 * A lightweight writing buffer to reduce the amount of write operations to be performed on the
 * underlying writer. This implementation is not thread-safe. It deliberately deviates from the
 * contract of Writer. In particular, it does not flush or close the wrapped writer nor does it
 * ensure that the wrapped writer is open.
 */
public class WritingBuffer extends Writer {

    private final Writer writer;
    private final char[] buffer;
    private int fill;

    public WritingBuffer(final Writer writer) {
        this(writer, 16);
    }

    public WritingBuffer(final Writer writer, final int bufferSize) {
        this.writer = writer;
        this.buffer = new char[bufferSize];
        this.fill = 0;
    }

    @Override
    public void write(int c) throws IOException {
        if (this.fill > this.buffer.length - 1) {
            this.flush();
        }
        this.buffer[this.fill++] = (char) c;
    }

    @Override
    public void write(
            final char[] cbuf, final int off, final int len) throws IOException {
        if (this.fill > this.buffer.length - len) {
            this.flush();
            if (len > this.buffer.length) {
                this.writer.write(cbuf, off, len);
                return;
            }
        }
        System.arraycopy(cbuf, off, this.buffer, this.fill, len);
        this.fill += len;
    }

    @Override
    public void write(
            final @NotNull String str, final int off, final int len) throws IOException {
        if (this.fill > this.buffer.length - len) {
            this.flush();
            if (len > this.buffer.length) {
                this.writer.write(str, off, len);
                return;
            }
        }
        str.getChars(off, off + len, this.buffer, this.fill);
        this.fill += len;
    }

    /**
     * Flushes the internal buffer but does not flush the wrapped writer.
     */
    @Override
    public void flush() throws IOException {
        this.writer.write(this.buffer, 0, this.fill);
        this.fill = 0;
    }

  /**
   * Does not close or flush the wrapped writer.
   */
    @Override
    public void close() throws IOException {
    }
}
