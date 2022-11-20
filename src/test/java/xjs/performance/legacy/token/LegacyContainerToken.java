package xjs.performance.legacy.token;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a 
 */
public class LegacyContainerToken extends LegacyTokenStream {

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param reference A reference to the original source of this token.
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param offset   The column of the start index.
     * @param type     The type of token.
     */
    public LegacyContainerToken(
            final String reference, final int start, final int end,
            final int offset, final Type type, final List<LegacyToken> tokens) {
        super(reference, start, end, offset, type, tokens);
    }

    public LegacyToken get(final int i) {
        return this.tokens.get(i);
    }

    public LegacyToken getTokenFromStringIndex(final int i) {
        for (final LegacyToken token : this.tokens) {
            if (i >= token.start && i < token.end) {
                return token;
            }
        }
        throw new IllegalStateException("no token at index: " + i);
    }

    public int size() {
        return this.tokens.size();
    }

    public int indexOf(final char symbol, final boolean exact) {
        final Lookup result = this.lookup(symbol, exact);
        return result != null ? result.index : -1;
    }

    public @Nullable LegacyContainerToken.Lookup lookup(final char symbol, final boolean exact) {
        return this.lookup(symbol, 0, exact);
    }

    public @Nullable LegacyContainerToken.Lookup lookup(final char symbol, final int fromIndex, final boolean exact) {
        for (int i = fromIndex; i < this.tokens.size(); i++) {
            final LegacyToken token = this.tokens.get(i);
            if (token.type == Type.SYMBOL && ((LegacySymbolToken) token).symbol == symbol) {
                final Lookup result = new Lookup(token, i);
                if (exact && (result.followsOtherSymbol() || result.precedesOtherSymbol())) {
                    return this.lookup(symbol, i, true);
                }
                return result;
            }
        }
        return null;
    }

    public int indexOf(final String symbol, final boolean exact) {
        final Lookup result = this.lookup(symbol, exact);
        return result != null ? result.index : -1;
    }

    public @Nullable LegacyContainerToken.Lookup lookup(final String symbol, final boolean exact) {
        return this.lookup(symbol, 0, exact);
    }

    public @Nullable LegacyContainerToken.Lookup lookup(final String symbol, final int fromIndex, final boolean exact) {
        char c = symbol.charAt(0);
        final Lookup firstLookup = this.lookup(c, fromIndex, false);
        if (firstLookup == null) {
            return null;
        }
        if (exact && firstLookup.followsOtherSymbol()) {
            return this.lookup(symbol, fromIndex + 1, true);
        }
        Lookup previousLookup = firstLookup;
        Lookup nextLookup = firstLookup;
        for (int i = 1; i < symbol.length(); i++) {
            c = symbol.charAt(i);
            nextLookup = this.lookup(c, fromIndex + i, false);
            if (nextLookup == null) {
                return null;
            } else if (nextLookup.token.start != previousLookup.token.end
                    || nextLookup.index - previousLookup.index != 1) {
                return this.lookup(symbol, firstLookup.index + 1, exact);
            }
            previousLookup = nextLookup;
        }
        if (exact && nextLookup.precedesOtherSymbol()) {
            return this.lookup(symbol, nextLookup.index + 1, true);
        }
        return firstLookup;
    }

    public LegacyTokenStream slice(final int s, final int e) {
        if (s == 0 && e == this.tokens.size()) {
            return this;
        } else if (s < 0 || e > this.tokens.size()) {
            throw new IllegalArgumentException(
                String.format("slice: (%d, %d) exceeds markers: (0, %d)", s, e, tokens.size()));
        }
        final LegacyToken first = this.tokens.get(s);
        final LegacyToken last = this.tokens.get(e);
        final List<LegacyToken> slice = this.tokens.subList(s, e);
        return new LegacyContainerToken(
            this.reference, first.start, last.end, first.offset, Type.OPEN, slice);
    }

    @Override
    public Itr iterator() {
        return new Itr();
    }

    public class Lookup {
        public final LegacyToken token;
        public final int index;

        protected Lookup(final LegacyToken token, final int index) {
            this.token = token;
            this.index = index;
        }

        public boolean followsOtherSymbol() {
            if (this.index > 0) {
                final LegacyToken previous = tokens.get(this.index - 1);
                return previous.type == Type.SYMBOL && this.token.start == previous.end;
            }
            return false;
        }

        public boolean precedesOtherSymbol() {
            if (this.index < tokens.size() - 1) {
                final LegacyToken following = tokens.get(this.index + 1);
                return following.type == Type.SYMBOL && this.token.end == following.start;
            }
            return false;
        }

        public boolean isFollowedBy(final char symbol) {
            if (tokens.size() > this.index + 1) {
                final LegacyToken following = tokens.get(index + 1);
                return following.type == Type.SYMBOL
                    && this.token.end == following.start
                    && ((LegacySymbolToken) following).symbol == symbol;
            }
            return false;
        }
    }
    
    public class Itr extends LegacyTokenStream.Itr {
        protected LegacyToken current;
        protected int elementIndex;
        
        @Override
        public boolean hasNext() {
            return this.elementIndex < tokens.size();
        }

        @Override
        public LegacyToken next() {
            return this.current = tokens.get(this.elementIndex++);
        }

        @Override
        public void skipTo(final int index) {
            while (this.getTextIndex() < index) {
                this.next();
            }
        }

        protected int getTextIndex() {
            if (this.current != null) {
                return current.start;
            }
            return 0;
        }

        @Override
        public @Nullable LegacyToken peek(final int amount) {
            if (this.elementIndex + amount - 1 < tokens.size()) {
                return null;
            }
            return tokens.get(this.elementIndex + amount - 1);
        }

        @Override
        public @Nullable LegacyToken previous() {
            if (this.elementIndex > 0) {
                return tokens.get(this.elementIndex - 1);
            }
            return null;
        }
    }
}
