package xjs.serialization.token;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xjs.serialization.util.PositionTrackingReader;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents any sequence of other tokens.
 *
 * For example, the following <em>token</em> is a single
 * sequence of tokens and is thus eligible to be represented
 * by this object:
 *
 * <pre>
 *    { k : v }
 * </pre>
 */
public class TokenStream extends Token implements Iterable<Token>, Closeable {
    protected final List<Token> tokens;
    protected volatile @Nullable PositionTrackingReader reader;
    public final CharSequence reference;
    protected int lastLine;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param reference A reference to the original source of this token.
     * @param start     The inclusive start index of this token.
     * @param end       The exclusive end index of this token.
     * @param line      The inclusive line number of this token.
     * @param lastLine  The inclusive end line number of this token.
     * @param offset    The column of the start index.
     * @param type      The type of token.
     * @param tokens    A list of any known tokens, in order.
     */
    protected TokenStream(final CharSequence reference, final int start, final int end,
                          final int line, final int lastLine, final int offset,
                          final Type type, final List<Token> tokens) {
        super(start, end, line, offset, type);
        this.reference = reference;
        this.tokens = new ArrayList<>(tokens);
        this.lastLine = lastLine;
    }

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param reader   A reader for extracting tokens OTF.
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param line     The inclusive line number of this token.
     * @param lastLine The inclusive end line number of this token.
     * @param offset   The column of the start index.
     * @param type     The type of token.
     */
    public TokenStream(final @NotNull PositionTrackingReader reader, final int start, final int end,
                       final int line, final int lastLine, final int offset, final Type type) {
        super(start, end, line, offset, type);
        this.tokens = new ArrayList<>();
        this.reader = reader;
        this.reference = reader.getFullText();
        this.lastLine = lastLine;
    }

    public String stringify() {
        return this.stringify(1, true);
    }

    protected String stringify(final int level, final boolean readToEnd) {
        if (readToEnd) {
            this.readToEnd();
        }
        final StringBuilder sb = new StringBuilder("[");
        final List<Token> copy = new ArrayList<>(this.tokens);
        for (final Token token : copy) {
            this.stringifySingle(sb, token, level, readToEnd);
        }
        if (this.reader != null || this.tokens.size() != copy.size()) {
            this.writeNewLine(sb, level);
            sb.append("<reading...>");
        }
        this.writeNewLine(sb, level - 1);
        return sb.append("]").toString();
    }

    protected void readToEnd() {
        final PositionTrackingReader reader = this.reader;
        if (reader != null) {
            synchronized (this) {
                this.forEach(token -> {});
            }
        }
    }

    private void stringifySingle(
            final StringBuilder sb, final Token token, final int level, final boolean readToEnd) {
        this.writeNewLine(sb, level);
        sb.append(token.type).append('(');
        if (token.type == Type.NUMBER) {
            sb.append(((NumberToken) token).number);
        } else if (token instanceof TokenStream) {
            sb.append(((TokenStream) token).stringify(level + 1, readToEnd));
        } else {
            final String text = token.textOf(this.reference)
                .replace("\n", "\\n").replace("\t", "\\t");
            sb.append('\'').append(text).append('\'');
        }
        sb.append(')');
    }

    private void writeNewLine(final StringBuilder sb, final int level) {
        sb.append('\n');
        for (int i = 0; i < level; i++) {
            sb.append(' ');
        }
    }

    @Override
    public Itr iterator() {
        return new Itr();
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof TokenStream) {
            return super.equals(o)
                && this.lastLine == ((TokenStream) o).lastLine
                && this.tokens.equals(((TokenStream) o).tokens);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.stringify(1, false);
    }

    @Override
    public void close() throws IOException {
        final PositionTrackingReader reader;
        synchronized (this) {
            reader = this.reader;
        }
        if (reader != null) {
            reader.close();
        }
    }

    public class Itr implements Iterator<Token> {
        protected final PositionTrackingReader reader;
        protected Token next;
        protected int elementIndex;

        protected Itr() {
            this.reader = TokenStream.this.reader;
            this.elementIndex = -1;
            this.read();
        }

        @Override
        public boolean hasNext() {
            return this.next != null;
        }

        @Override
        public Token next() {
            final Token current = this.next;
            this.read();
            return current;
        }

        public Token peekOrParent() {
            return this.next != null ? this.next : TokenStream.this;
        }

        protected void read() {
            this.elementIndex++;
            final Token next = this.peek(1);
            this.next = next;
            if (next != null) {
                this.tryClose();
                TokenStream.this.end = next.end;
                TokenStream.this.lastLine = next.line;
            }
        }

        protected void tryClose() {
            if (this.reader != null) {
                try {
                    this.reader.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                TokenStream.this.reader = null;
            }
        }

        public void skipTo(final int index) {
            final int amount = index - this.elementIndex;
            this.next = this.peek(amount + 1);
            this.elementIndex = index;
            this.tryClose();
        }

        public void skip(final int amount) {
            this.next = this.peek(amount + 1);
            this.elementIndex = this.elementIndex + amount;
            this.tryClose();
        }

        public String getText() {
            final Token t = this.peekOrParent();
            return this.getText(t.start, t.end);
        }

        public String getText(final int s, final int e) {
            return this.getReference().subSequence(s, e).toString();
        }

        public CharSequence getReference() {
            return reference;
        }

        public TokenStream getParent() {
            return TokenStream.this;
        }

        public @Nullable Token peek() {
            return this.next;
        }

        public Token peek(final int amount, final Token defaultValue) {
            final Token peek = this.peek(amount);
            return peek != null ? peek : defaultValue;
        }

        public @Nullable Token peek(final int amount) {
            Token next = null;
            final int peekIndex = this.elementIndex + amount - 1;
            if (peekIndex >= 0 && peekIndex < tokens.size()) {
                return tokens.get(peekIndex);
            }
            if (this.reader == null) {
                return null;
            }
            while (tokens.size() < this.elementIndex + amount) {
                try {
                    next = Tokenizer.single(this.reader);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
                if (next == null) {
                    return null;
                }
                tokens.add(next);
            }
            return next;
        }
    }
}
