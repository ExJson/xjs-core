package xjs.serialization.token;

import org.junit.jupiter.api.Test;
import xjs.exception.SyntaxException;
import xjs.serialization.token.Token.Type;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class TokenStreamTest {

    @Test
    public void next_lazilyEvaluatesTokens() {
        final String reference = "1 2 3";
        final TokenStream stream = Tokenizer.stream(reference);
        assertEquals(0, stream.tokens.size());

        final Iterator<Token> iterator = stream.iterator();
        assertEquals(1, stream.tokens.size());
        assertEquals(number(1, 0, 1), iterator.next());

        assertEquals(2, stream.tokens.size());
        assertEquals(number(2, 2, 3), iterator.next());

        assertEquals(3, stream.tokens.size());
        assertEquals(number(3, 4, 5), iterator.next());
    }

    @Test
    public void next_lazilyThrowsSyntaxException() {
        final String reference = "1 2 'hello";
        final TokenStream stream = Tokenizer.stream(reference);
        final Iterator<Token> iterator = stream.iterator();

        assertEquals(number(1, 0, 1), iterator.next());
        assertThrows(SyntaxException.class, iterator::next);
    }

    @Test
    public void peek_doesNotAdvanceIterator() {
        final String reference = "1 2 3 4";
        final TokenStream.Itr iterator =
            Tokenizer.stream(reference).iterator();

        assertEquals(number(1, 0, 1), iterator.next());

        assertEquals(number(2, 2, 3), iterator.peek());
        assertEquals(number(3, 4, 5), iterator.peek(2));
        assertEquals(number(4, 6, 7), iterator.peek(3));

        assertEquals(number(2, 2, 3), iterator.next());
        assertEquals(number(3, 4, 5), iterator.next());
        assertEquals(number(4, 6, 7), iterator.next());
    }

    @Test
    public void peek_toleratesReverseOrder() {
        final String reference = "1 2 3";
        final TokenStream.Itr iterator =
            Tokenizer.stream(reference).iterator();

        assertEquals(number(1, 0, 1), iterator.next());
        assertEquals(number(2, 2, 3), iterator.next());
        assertEquals(number(3, 4, 5), iterator.next());

        assertEquals(number(3, 4, 5), iterator.peek(0));
        assertEquals(number(2, 2, 3), iterator.peek(-1));
        assertEquals(number(1, 0, 1), iterator.peek(-2));
    }

    @Test
    public void skipTo_advancesIterator() {
        final String reference = "1 2 3 4";
        final TokenStream stream = Tokenizer.stream(reference);
        final TokenStream.Itr iterator = stream.iterator();

        assertEquals(number(1, 0, 1), iterator.next());
        iterator.skipTo(2);

        assertEquals(number(3, 4, 5), iterator.next());
        assertEquals(number(4, 6, 7), iterator.next());
    }

    @Test
    public void skip_advancesIterator() {
        final String reference = "1 2 3 4";
        final TokenStream stream = Tokenizer.stream(reference);
        final TokenStream.Itr iterator = stream.iterator();

        assertEquals(number(1, 0, 1), iterator.next());
        iterator.skip(1);

        assertEquals(number(3, 4, 5), iterator.next());
        assertEquals(number(4, 6, 7), iterator.next());
    }

    @Test
    public void skipTo_toleratesReverseOrder() {
        final String reference = "1 2 3";
        final TokenStream stream = Tokenizer.stream(reference);
        final TokenStream.Itr iterator = stream.iterator();

        assertEquals(number(1, 0, 1), iterator.next());
        assertEquals(number(2, 2, 3), iterator.next());
        iterator.skipTo(0);

        assertEquals(number(1, 0, 1), iterator.next());
        assertEquals(number(2, 2, 3), iterator.next());
        assertEquals(number(3, 4, 5), iterator.next());
    }

    @Test
    public void skip_toleratesReverseOrder() {
        final String reference = "1 2 3";
        final TokenStream stream = Tokenizer.stream(reference);
        final TokenStream.Itr iterator = stream.iterator();

        assertEquals(number(1, 0, 1), iterator.next());
        assertEquals(number(2, 2, 3), iterator.next());
        iterator.skip(-2);

        assertEquals(number(1, 0, 1), iterator.next());
        assertEquals(number(2, 2, 3), iterator.next());
        assertEquals(number(3, 4, 5), iterator.next());
    }

    private static Token number(final double number, final int s, final int e) {
        return new NumberToken(s, e, s, number);
    }

    private static Token token(final Type type, final int s, final int e) {
        return new Token(s, e, s, type);
    }
}
