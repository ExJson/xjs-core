package xjs.performance.legacy.token;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
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
public class LegacyTokenStream extends LegacyToken implements Iterable<LegacyToken> {
    protected final List<LegacyToken> tokens;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param reference A reference to the original source of this token.
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param offset   The column of the start index.
     * @param type     The type of token.
     * @param tokens   A list of any known tokens, in order.
     */
    protected LegacyTokenStream(final String reference, final int start, final int end,
                                final int offset, final Type type, final List<LegacyToken> tokens) {
        super(reference, start, end, offset, type);
        this.tokens = new ArrayList<>(tokens);
    }

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param reference A reference to the original source of this token.
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param offset   The column of the start index.
     * @param type     The type of token.
     */
    public LegacyTokenStream(final String reference, final int start, final int end,
                             final int offset, final Type type) {
        this(reference, start, end, offset, type, Collections.emptyList());
    }

    public String stringify() {
        return this.stringify(1);
    }

    protected String stringify(final int level) {
        final Itr itr = this.iterator();
        final StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        if (itr.hasNext()) {
            stringifySingle(sb, itr.next(), level);
        }
        while (itr.hasNext()) {
            sb.append('\n');
            stringifySingle(sb, itr.next(), level);
        }
        sb.append('\n');
        for (int i = 0; i < level - 1; i++) {
            sb.append(' ');
        }
        return sb.append("]").toString();
    }

    private static void stringifySingle(final StringBuilder sb, final LegacyToken token, final int level) {
        for (int i = 0; i < level; i++) {
            sb.append(' ');
        }
        sb.append(token.type).append('(');
        if (token.type == Type.NUMBER) {
            sb.append(((LegacyNumberToken) token).number);
        } else if (token instanceof LegacyTokenStream) {
            sb.append(((LegacyTokenStream) token).stringify(level + 1));
        } else {
            final String text = token.getText()
                .replace("\n", "\\n").replace("\t", "\\t");
            sb.append('\'').append(text).append('\'');
        }
        sb.append(')');
    }

    @Override
    public Itr iterator() {
        return new Itr();
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof LegacyTokenStream) {
            return super.equals(o) && this.tokens.equals(((LegacyTokenStream) o).tokens);
        }
        return false;
    }

    public class Itr implements Iterator<LegacyToken> {
        protected int textIndex;
        protected int elementIndex;
        protected int lineIndex;
        protected int offset;
        protected LegacyToken next;

        protected Itr() {
            this.search();
        }

        @Override
        public boolean hasNext() {
            return this.next != null;
        }

        @Override
        public LegacyToken next() {
            final LegacyToken current = this.next;
            this.search();
            return current;
        }

        protected void search() {
            this.next = this.peek();
            this.elementIndex++;
        }

        public void skipTo(final int index) {
            this.textIndex = index;
        }

        public @Nullable LegacyToken peek() {
            return this.peek(1);
        }

        public @Nullable LegacyToken peek(final int amount) {
            LegacyToken next = null;
            final int peekIndex = this.elementIndex + amount - 1;
            if (peekIndex < tokens.size()) {
                final LegacyToken peek = tokens.get(peekIndex);
                this.textIndex = peek.end;
                return peek;
            }
            for (int i = 0; i < amount; i++) {
                next = LegacyTokenizer.single(reference, this.textIndex, this.offset);
                if (next == null) {
                    return null;
                }
                if (next.type == Type.BREAK) {
                    this.lineIndex = next.end;
                    this.offset = 0;
                } else {
                    this.offset = next.end - this.lineIndex;
                }
                tokens.add(next);
                this.textIndex = next.end;
            }
            return next;
        }

        public @Nullable LegacyToken previous() {
            if (this.elementIndex > 0) {
                return tokens.get(this.elementIndex - 1);
            }
            return null;
        }
    }
}
