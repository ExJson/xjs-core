package xjs.performance.legacy.token;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import xjs.exception.SyntaxException;
import xjs.performance.legacy.token.LegacyToken.Type;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LegacyTokenizerTest {

    @Test
    public void single_parsesLineComment() {
        final String reference = "// Hello, world!";
        assertEquals(
            token(reference, Type.LINE),
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void single_parsesHashComment() {
        final String reference = "# Hello, world!";
        assertEquals(
            token(reference, Type.LINE),
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void single_parsesMultiComment() {
        final String reference = "/*\nHello\nworld!\n*/";
        assertEquals(
            token(reference, Type.MULTI),
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void single_parsesDoubleQuote() {
        final String reference = "\"Hello, world!\"";
        assertEquals(
            token(reference, Type.DOUBLE),
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void single_parsesSingleQuote() {
        final String reference = "'Hello, world!'";
        assertEquals(
            token(reference, Type.SINGLE),
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void single_parsesTripleQuote() {
        final String reference = "'''\nHello\nworld!\n'''";
        assertEquals(
            token(reference, Type.TRIPLE),
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void single_parsesInteger() {
        final String reference = "1234";
        assertEquals(
            number(reference, 1234),
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void single_parsesDecimal() {
        final String reference = "1234.5";
        assertEquals(
            number(reference, 1234.5),
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void single_parsesScientificNumber() {
        final String reference = "1234.5E6";
        assertEquals(
            number(reference, 1234.5E6),
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void single_parsesScientificNumber_withExplicitSign() {
        final String reference = "1234.5e+6";
        assertEquals(
            number(reference, 1234.5E+6),
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void single_parsesSignAfterNumber_asTwoTokens() {
        final String reference = "1234e+";
        assertEquals(
            container(
                reference,
                Type.OPEN,
                0,
                6,
                token(reference, Type.WORD, 0, 5),
                symbol(reference, '+', 5, 6)),
            LegacyTokenizer.containerize(reference));
    }

    @Test
    public void single_parsesBreak() {
        final String reference = "\n";
        assertEquals(
            token(reference, Type.BREAK),
            LegacyTokenizer.single(reference, 0, 0));
    }

    @ValueSource(strings = {"+", "-", "<", ">", "=", ":", "{", "}", "[", "]", "(", ")"})
    @ParameterizedTest
    public void single_parsesSymbol(final String reference) {
        assertEquals(
            symbol(reference, reference.charAt(0)),
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void single_skipsWhitespace() {
        final String reference = " \t \t \t 'Hello, world!'";
        assertEquals(
            token(reference, Type.SINGLE, 7, reference.length()),
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void single_readsContainerElements_asSymbols() {
        final String reference = " {hello}";
        assertEquals(
            symbol(reference, '{', 1, 2),
            LegacyTokenizer.single(reference, 0, 0));
    }
    
    @ValueSource(strings = {"'", "\"", "'''"})
    @ParameterizedTest
    public void single_doesNotTolerate_UnclosedQuote(final String quote) {
        final String reference = quote + "hello, world!";
        assertThrows(SyntaxException.class, () ->
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void single_doesNotTolerate_UnclosedMultiLineComment() {
        final String reference = "/*hello, world!";
        assertThrows(SyntaxException.class, () ->
            LegacyTokenizer.single(reference, 0, 0));
    }

    @Test
    public void containerize_readsSingleContainer() {
        final String reference = "{hello,world}";
        assertEquals(
            container(
                reference,
                Type.BRACES,
                0,
                13,
                token(reference, Type.WORD, 1, 6),
                symbol(reference, ',', 6, 7),
                token(reference, Type.WORD, 7, 12)),
            LegacyTokenizer.containerize(reference));
    }

    @Test
    public void containerize_readsNestedContainer() {
        final String reference = "{hello,[world]}";
        assertEquals(
            container(
                reference,
                Type.BRACES,
                0,
                15,
                token(reference, Type.WORD, 1, 6),
                symbol(reference, ',', 6, 7),
                container(
                    reference,
                    Type.BRACKETS,
                    7,
                    14,
                    token(reference, Type.WORD, 8, 13))),
            LegacyTokenizer.containerize(reference));
    }

    @Test
    public void containerize_toleratesTrailingWhitespace() {
        final String reference = "{hello,world} \t";
        assertEquals(
            container(
                reference,
                Type.BRACES,
                0,
                13,
                token(reference, Type.WORD, 1, 6),
                symbol(reference, ',', 6, 7),
                token(reference, Type.WORD, 7, 12)),
            LegacyTokenizer.containerize(reference));
    }

    @Test
    public void containerize_doesNotTolerate_UnclosedContainer() {
        final String reference = "{[}";
        final SyntaxException e =
            assertThrows(SyntaxException.class, () ->
                LegacyTokenizer.containerize(reference));
        assertTrue(e.getMessage().contains("Expected ']'"));
    }

    @Test
    public void stream_returnsLazilyEvaluatedTokens() {
        final LegacyTokenStream stream = LegacyTokenizer.stream("1234");
        stream.iterator().next();
        assertEquals(1, stream.tokens.size());
    }

    private static LegacyToken token(final String reference, final Type type) {
        return token(reference, type, 0, reference.length());
    }

    private static LegacyToken token(final String reference, final Type type, final int s, final int e) {
        return new LegacyToken(reference, s, e, s, type);
    }

    private static LegacyToken number(final String reference, final double number) {
        return new LegacyNumberToken(reference, 0, reference.length(), 0, number);
    }

    private static LegacyToken symbol(final String reference, final char symbol) {
        return symbol(reference, symbol, 0, reference.length());
    }

    private static LegacyToken symbol(final String reference, final char symbol, final int s, final int e) {
        return new LegacySymbolToken(reference, s, e, s, symbol);
    }

    private static LegacyToken container(
            final String reference, final Type type, final int s, final int e, final LegacyToken... tokens) {
        return new LegacyContainerToken(reference, s, e, s, type, List.of(tokens));
    }
}
