package xjs.serialization.parser;

import xjs.core.JsonArray;
import xjs.core.JsonContainer;
import xjs.core.JsonValue;
import xjs.exception.SyntaxException;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.Token.Type;
import xjs.serialization.token.TokenStream;
import xjs.serialization.util.BufferedStack;

import java.util.ArrayList;

public abstract class TokenParser implements ValueParser {

    protected static final TokenStream EMPTY_VALUE =
        new ContainerToken("", 0, 0, 0, 0, 0, Type.OPEN, new ArrayList<>());
    protected static final TokenStream.Itr EMPTY_ITERATOR =
        EMPTY_VALUE.iterator();

    protected final BufferedStack.OfTwo<
        TokenStream.Itr, JsonContainer> stack;
    protected final TokenStream root;
    protected final CharSequence reference;
    protected JsonContainer formatting;
    protected TokenStream.Itr iterator;
    protected Token current;
    protected int linesSkipped;

    protected TokenParser(final TokenStream root) {
        this.stack = BufferedStack.ofTwo();
        this.root = root;
        // Reference can be mutable and may expand lazily
        this.reference = root.reference;
        this.formatting = new JsonArray();
        this.iterator = root.iterator();
        this.current = root;
    }

    protected void next() {
        final TokenStream.Itr itr = this.iterator;
        if (itr == EMPTY_ITERATOR) {
            return;
        }
        if (itr.hasNext()) {
            this.current = this.iterator.next();
        } else {
            this.current = EMPTY_VALUE;
        }
    }

    protected boolean push() {
        if (this.iterator.getParent() == this.current) {
            return false;
        }
        if (this.current instanceof TokenStream) {
            this.stack.push(this.iterator, this.formatting);
            this.iterator = ((TokenStream) this.current).iterator();
            this.formatting = new JsonArray();
            return true;
        }
        return false;
    }

    protected boolean pop() {
        if (this.stack.isEmpty()) {
            this.iterator = EMPTY_ITERATOR;
            return false;
        }
        this.stack.pop();
        this.iterator = this.stack.getFirst();
        this.formatting = this.stack.getSecond();
        return true;
    }

    protected boolean readIf(final char symbol) {
        if (this.current.isSymbol(symbol)) {
            this.next();
            return true;
        }
        return false;
    }

    protected boolean readNl() {
        if (this.current.type() == Type.BREAK) {
            this.next();
            this.flagLineAsSkipped();
            return true;
        }
        return false;
    }

    protected void flagLineAsSkipped() {
        this.linesSkipped++;
    }

    protected boolean isEndOfContainer() {
        return this.current == EMPTY_VALUE;
    }

    protected void readWhitespace() {
        this.readWhitespace(true, true);
    }

    protected void readWhitespace(final boolean resetLinesSkipped) {
        this.readWhitespace(resetLinesSkipped, true);
    }

    protected void readLineWhitespace(final boolean resetLinesSkipped) {
        this.readWhitespace(resetLinesSkipped, false);
    }

    protected void readWhitespace(
            final boolean resetLinesSkipped, final boolean nl) {
        if (resetLinesSkipped) {
            this.linesSkipped = 0;
        }
        final TokenStream.Itr itr = this.iterator;
        if (itr == EMPTY_ITERATOR) {
            return;
        }
        Token t = this.current;
        int peekAmount = 0;
        while (t != EMPTY_VALUE) {
            if (!this.consumeWhitespace(t, nl)) {
                break;
            }
            t = itr.peek(++peekAmount, EMPTY_VALUE);
        }
        this.current = t;
        itr.skip(peekAmount);
    }

    protected boolean consumeWhitespace(final Token t, final boolean nl) {
        if (nl && t.type() == Type.BREAK) {
            this.flagLineAsSkipped();
            return true;
        }
        return false;
    }

    protected int skipToOffset(final int start, final int offset) {
        for (int i = start; i < start + offset; i++) {
            if (!this.isLineWhitespace(this.reference.charAt(i))) {
                return i;
            }
        }
        return start + offset;
    }

    protected boolean isLineWhitespace(final char c) {
        return c == ' ' || c == '\r' || c == '\t';
    }

    protected int skipTo(final char symbol, final boolean nl, final boolean eof) {
        final TokenStream.Itr itr = this.iterator;
        if (!itr.hasNext()) {
            if (eof) return 0;
            throw this.expectedSymbolOrNL(symbol, nl);
        }
        Token t = itr.peek();
        Token lastRecorded = this.current;
        int peekAmount = 1;
        while (t != null) { // newlines would only be inside of containers for values
            if (t.isSymbol(symbol)
                    || (nl && t.type() == Type.BREAK)) {
                itr.skip(peekAmount - 1);
                this.current = lastRecorded;
                return peekAmount - 1;
            } else if (!this.consumeWhitespace(t, false)) {
                lastRecorded = t;
            }
            t = itr.peek(++peekAmount);
        }
        if (eof) {
            if (peekAmount > 1) {
                itr.skip(peekAmount - 1);
                this.current = lastRecorded;
            }
            return peekAmount;
        }
        throw this.expectedSymbolOrNL(symbol, nl);
    }

    protected void setAbove() {
        this.formatting.setLinesAbove(this.takeLinesSkipped());
    }

    protected void setBetween() {
        this.formatting.setLinesBetween(this.takeLinesSkipped());
    }

    protected void setTrailing() {
        this.formatting.setLinesTrailing(this.takeLinesSkipped());
    }

    protected void readAbove() {
        this.readWhitespace(false);
        this.setAbove();
    }

    protected void readBetween(final char kvSeparator) {
        this.readWhitespace();
        this.expect(kvSeparator);
        this.readWhitespace();
        this.setBetween();
    }

    protected void readAfter() {
        this.readLineWhitespace(false);
    }

    protected void readBottom() {
        this.readWhitespace(false);
        this.expectEndOfText();
    }

    protected int takeLinesSkipped() {
        final int skipped = this.linesSkipped;
        this.linesSkipped = 0;
        return skipped;
    }

    protected <T extends JsonValue> T takeFormatting(final T value) {
        value.setDefaultMetadata(this.formatting);
        this.clearFormatting();
        return value;
    }

    protected void clearFormatting() {
        this.formatting.setLinesTrailing(-1)
            .setLinesAbove(-1)
            .setLinesBetween(-1)
            .setComments(null);
    }

    protected void expect(final char expected) {
        if (!this.readIf(expected)) {
            throw this.expected(expected);
        }
    }

    protected void expectEndOfText() {
        if (!this.stack.isEmpty() || this.iterator.hasNext()) {
            throw this.unexpected(this.current.type() + " before end of file");
        }
    }

    protected SyntaxException expectedSymbolOrNL(
            final char symbol, final boolean nl) {
        if (nl) {
            return this.expected("'" + symbol + "' or new line");
        }
        return this.expected(symbol);
    }

    protected SyntaxException expected(final char expected) {
        return SyntaxException.expected(
            expected, this.current.line(), this.current.offset());
    }

    protected SyntaxException expected(final String expected) {
        return SyntaxException.expected(
            expected, this.current.line(), this.current.offset());
    }

    protected SyntaxException unexpected(final char unexpected) {
        return SyntaxException.unexpected(
            unexpected, this.current.line(), this.current.offset());
    }

    protected SyntaxException unexpected(final String unexpected) {
        return SyntaxException.unexpected(
            unexpected, this.current.line(), this.current.offset());
    }
}
