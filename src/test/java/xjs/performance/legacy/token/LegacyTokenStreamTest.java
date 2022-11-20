package xjs.performance.legacy.token;

import org.junit.jupiter.api.Test;

import java.util.Iterator;

import xjs.exception.SyntaxException;
import xjs.performance.legacy.token.LegacyToken.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class LegacyTokenStreamTest {

    @Test
    public void iterator_lazilyEvaluatesTokens() {
        final String reference = "1 2 3";
        final LegacyTokenStream stream = LegacyTokenizer.stream(reference);
        assertEquals(0, stream.tokens.size());

        final Iterator<LegacyToken> iterator = stream.iterator();
        assertEquals(1, stream.tokens.size());
        assertEquals(number(reference, 1, 0, 1), iterator.next());

        assertEquals(2, stream.tokens.size());
        assertEquals(number(reference, 2, 2, 3), iterator.next());

        assertEquals(3, stream.tokens.size());
        assertEquals(number(reference, 3, 4, 5), iterator.next());
    }

    @Test
    public void iterator_lazilyThrowsSyntaxException() {
        final String reference = "1 2 'hello";
        final LegacyTokenStream stream = LegacyTokenizer.stream(reference);
        final Iterator<LegacyToken> iterator = stream.iterator();

        assertEquals(number(reference, 1, 0, 1), iterator.next());
        assertThrows(SyntaxException.class, iterator::next);
    }

    private static LegacyToken number(final String reference, final double number, final int s, final int e) {
        return new LegacyNumberToken(reference, s, e, s, number);
    }

    private static LegacyToken token(final String reference, final Type type, final int s, final int e) {
        return new LegacyToken(reference, s, e, s, type);
    }
}
