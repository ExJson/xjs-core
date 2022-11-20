package xjs.serialization.token;

import org.junit.jupiter.api.Test;
import xjs.exception.SyntaxException;
import xjs.serialization.token.Token.Type;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class TokenStreamTest {

    @Test
    public void iterator_lazilyEvaluatesTokens() {
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
    public void iterator_lazilyThrowsSyntaxException() {
        final String reference = "1 2 'hello";
        final TokenStream stream = Tokenizer.stream(reference);
        final Iterator<Token> iterator = stream.iterator();

        assertEquals(number(1, 0, 1), iterator.next());
        assertThrows(SyntaxException.class, iterator::next);
    }

    private static Token number(final double number, final int s, final int e) {
        return new NumberToken(s, e, s, number);
    }

    private static Token token(final Type type, final int s, final int e) {
        return new Token(s, e, s, type);
    }
}
