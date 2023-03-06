package xjs.serialization.token;

import org.junit.jupiter.api.Test;
import xjs.exception.SyntaxException;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TokenStreamTest {

    @Test
    public void stringify_printsTokenType_andFullText() {
        final TokenStream stream =
            Tokenizer.stream("word");
        final String expected = """
            [
             WORD('word')
            ]""";
        assertEquals(expected, stream.stringify());
    }

    @Test
    public void stringify_escapesNewlines() {
        final TokenStream stream =
            Tokenizer.stream("'''1\n2'''");
        final String expected = """
            [
             STRING(''''1\\n2'''')
            ]""";
        assertEquals(expected, stream.stringify());
    }

    @Test
    public void stringify_printsAllTokens_inContainer() {
        final TokenStream stream =
            Tokenizer.stream("1 2 3");
        final String expected = """
            [
             NUMBER(1.0)
             NUMBER(2.0)
             NUMBER(3.0)
            ]""";
        assertEquals(expected, stream.stringify());
    }

    @Test
    public void stringify_recurses_intoOtherContainers() {
        final TokenStream stream =
            Tokenizer.containerize("1 [ 2.25 2.5 2.75 ] 3");
        final String expected = """
            [
             NUMBER(1.0)
             BRACKETS([
              NUMBER(2.25)
              NUMBER(2.5)
              NUMBER(2.75)
             ])
             NUMBER(3.0)
            ]""";
        assertEquals(expected, stream.stringify());
    }

    @Test
    public void toString_doesNotReadToEnd() {
        final TokenStream stream =
            Tokenizer.stream("1 2 3");
        // iterator eagerly evaluates first token
        stream.iterator();
        final String expected = """
            [
             NUMBER(1.0)
             <reading...>
            ]""";
        assertEquals(expected, stream.toString());
    }

    @Test
    public void viewTokens_isInternallyMutable() {
        final String reference = "1 2 3";
        final TokenStream stream = Tokenizer.stream(reference);

        final List<Token> tokens = stream.viewTokens();
        assertTrue(tokens.isEmpty());

        stream.iterator().next();
        assertEquals(
            List.of(
                number(1, 0, 1),
                number(2, 2, 3)),
            tokens);
    }

    @Test
    public void viewTokens_isNotExternallyMutable() {
        final String reference = "1 2 3";
        final TokenStream stream = Tokenizer.stream(reference);

        assertThrows(UnsupportedOperationException.class,
            () -> stream.viewTokens().add(number(0, 0, 0)));
    }

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
        return new NumberToken(s, e, 0, s, number);
    }

    private static Token token(final TokenType type, final int s, final int e) {
        return new Token(s, e, 0, s, type);
    }
}
