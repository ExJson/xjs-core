package xjs.serialization.token;

import org.jetbrains.annotations.Nullable;
import xjs.exception.SyntaxException;
import xjs.serialization.token.Token.Type;
import xjs.serialization.util.PositionTrackingReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A handful of utilities for streaming and containerizing tokens.
 *
 * <p>In the future, this API may be exposed to extenders. In the
 * meantime, users will have to provide fully-custom tokenizers
 * to support some exotic formats.
 *
 * Todo: expand line counting semantics to string and comment tokens.
 */
public final class Tokenizer {

    private Tokenizer() {}

    /**
     * Generates a lazily-evaluated {@link TokenStream stream of
     * tokens} from the input text.
     *
     * @param text The full reference and source of tokens.
     * @return A new {@link TokenStream}.
     */
    public static TokenStream stream(final String text) {
        return new TokenStream(PositionTrackingReader.fromString(text), 0, text.length(), 0, 0, 0, Type.OPEN);
    }

    /**
     * Generates a lazily-evaluated {@link TokenStream stream of tokens}
     * wrapping an {@link InputStream}.
     *
     * @param is The source of tokens being parsed.
     * @return A new {@link TokenStream}.
     * @throws IOException If the initial read operation throws an exception.
     */
    public static TokenStream stream(final InputStream is) throws IOException {
        return new TokenStream(PositionTrackingReader.fromIs(is, true), 0, 0, 0, 0, 0, Type.OPEN);
    }

    /**
     * Reads a single, non-container token from the given reader.
     *
     * <p><b>Note:</b> there is a <b>known bug</b> with this method.
     * Numbers with incomplete exponents will <em>not</em> be returned
     * as multiple symbols and will instead be returned as a single
     * word. This violates the contract that symbol characters--including
     * <code>-</code> and <code>+</code>--will always be represented as
     * {@link SymbolToken symbol tokens}. An eventual fix is expected,
     * but the exact solution has not yet been determined.
     *
     * @param reader A reader tracking characters and positional data.
     * @return The next possible token, or else <code>null</code>.
     * @throws IOException If the given reader throws an exception.
     */
    public static @Nullable Token single(final PositionTrackingReader reader) throws IOException {
        reader.skipLineWhitespace();
        if (reader.isEndOfText()) {
            return null;
        }
        final char c = (char) reader.current;
        final int s = reader.index;
        final int l = reader.line;
        final int o = reader.column;
        if (c == '/' || c == '#') {
            return comment(reader, c, s, l, o);
        } else if (c == '\'' || c == '"') {
            return quote(reader, s, l, o, c);
        } else if (c == '\n') {
            reader.read();
            return new SymbolToken(s, s + 1, l, o, Type.BREAK, '\n');
        } else if (c == '.') {
            return dot(reader, s, l, o);
        } else if (Character.isDigit(c)) {
            return number(reader, s, l, o);
        }
        return word(reader, s, l, o);
    }

    private static Token quote(
            final PositionTrackingReader reader, final int i, final int l, final int o, final char quote) throws IOException {
        final String parsed = reader.readQuoted(quote);
        if (parsed.isEmpty() && quote == '\'' && reader.readIf('\'')) {
            final String multi = reader.readMulti(false);
            return new StringToken(i, reader.index, l, o, Type.TRIPLE_QUOTE, multi);
        }
        final Type type = quote == '\'' ? Type.SINGLE_QUOTE : Type.DOUBLE_QUOTE;
        return new StringToken(i, reader.index, l, o, type, parsed);
    }

    private static Token comment(
            final PositionTrackingReader reader, char c, final int i, final int l, final int o) throws IOException {
        reader.read();
        if (c == '#' || reader.readIf('/')) {
            final int lastChar = reader.skipToNL();
            final Type type = c == '#'
                ? Type.HASH_COMMENT : Type.LINE_COMMENT;
            return new Token(i, lastChar + 1, l, o, type);
        } else if (!reader.readIf('*')) {
            return new SymbolToken(i, i + 1, l, o, c);
        }
        boolean asterisk = false;
        while (!reader.isEndOfText()) {
            c = (char) reader.current;
            reader.read();
            if (asterisk && c == '/') {
                return new Token(i, reader.index, l, o, Type.BLOCK_COMMENT);
            }
            asterisk = c == '*';
        }
        throw reader.expected("end of multiline comment ('*/')");
    }

    private static Token word(
            final PositionTrackingReader reader, final int i, final int l, final int o) throws IOException {
        do {
            final char c = (char) reader.current;
            if (c == '_' || Character.isLetterOrDigit(c)) {
                reader.read();
            } else if (reader.index - i == 0) {
                reader.read();
                return new SymbolToken(i, reader.index, l, o, c);
            } else {
                break;
            }
        } while (!reader.isEndOfText());
        return new Token(i, reader.index, l, o, Type.WORD);
    }

    private static Token dot(
            final PositionTrackingReader reader, final int i, final int l, final int o) throws IOException {
        reader.read();
        if (reader.isDigit()) {
            return number(reader, i, l, o);
        }
        return new SymbolToken(i, i + 1, l, o, '.');
    }

    private static Token number(
            final PositionTrackingReader reader, int i, final int l, final int o) throws IOException {
        reader.startCapture();
        if (reader.current != '0') { // ???
            reader.readAllDigits();
        }
        if (reader.readIf('.')) {
            if (!reader.isDigit()) {
                return parseNumber(reader, i, l, o);
            }
            reader.readAllDigits();
        }
        if (reader.readIf('e') || reader.readIf('E')) {
            if (!reader.readIf('+')) {
                reader.readIf('-'); // if no other numbers, result is ignored
            }
            if (!reader.readDigit()) {
                return new Token(i, reader.index, l, o, Type.WORD);
            }
            reader.readAllDigits();
        }
        return parseNumber(reader, i, l, o);
    }

    private static NumberToken parseNumber(
            final PositionTrackingReader reader, final int i, final int l, final int o) {
        final double number = Double.parseDouble(reader.endCapture());
        return new NumberToken(i, reader.index, l, o, number);
    }

    /**
     * Generates a recursive {@link ContainerToken} data structure from
     * the given source.
     *
     * @param is The source of characters being decoded.
     * @return A recursive {@link Token} data structure.
     * @throws IOException If the reader throws an exception at any point.
     */
    public static ContainerToken containerize(final InputStream is) throws IOException {
        return containerize(stream(is));
    }

    /**
     * Generates a recursive {@link ContainerToken} data structure from
     * the given full text.
     *
     * @param text The full reference and source of tokens.
     * @return A recursive {@link Token} data structure.
     */
    public static ContainerToken containerize(final String text) {
        return containerize(stream(text));
    }

    /**
     * Generates a recursive {@link ContainerToken} data structure from
     * an existing {@link TokenStream stream of tokens}.
     *
     * @param stream An existing stream of tokens.
     * @return A recursive {@link Token} data structure.
     */
    public static ContainerToken containerize(final TokenStream stream) {
        if (stream instanceof ContainerToken) {
            return (ContainerToken) stream;
        }
        final ContainerToken container = container(
            stream.iterator(), stream.reference, stream.start, stream.line, stream.offset, Type.OPEN, '\u0000');
        if (container.size() == 1) {
            final Token firstToken = container.get(0);
            if (firstToken instanceof ContainerToken) {
                return (ContainerToken) firstToken;
            }
        }
        return container;
    }

    private static ContainerToken container(
            final TokenStream.Itr itr, final CharSequence reference,
            final int s, final int l, final int o, final Type type, final char closer) {
        final List<Token> tokens = new ArrayList<>();
        int e = s;
        int el = l;
        while (itr.hasNext()) {
            final Token control = itr.next();
            if (tokenMatchesSymbol(control, closer)) {
                return new ContainerToken(reference.toString(), s, control.end, l, el, o, type, tokens);
            }
            final Token next = next(itr, reference, control);
            tokens.add(next);
            e = next.end;
            el = next.line;
        }
        if (closer != '\u0000') {
            throw SyntaxException.expected(closer, s, o);
        }
        return new ContainerToken(reference.toString(), s, e, l, el, o, type, tokens);
    }

    private static boolean tokenMatchesSymbol(final Token token, final char symbol) {
        return token instanceof SymbolToken && ((SymbolToken) token).symbol == symbol;
    }

    private static Token next(final TokenStream.Itr itr, final CharSequence reference, final Token control) {
        switch (getSymbol(control)) {
            case '{':
                return container(itr, reference, control.start, control.line, control.offset, Type.BRACES, '}');
            case '[':
                return container(itr, reference, control.start, control.line, control.offset, Type.BRACKETS, ']');
            case '(':
                return container(itr, reference, control.start, control.line, control.offset, Type.PARENTHESES, ')');
        }
        return control;
    }

    private static char getSymbol(final Token token) {
        if (token instanceof SymbolToken) {
            return ((SymbolToken) token).symbol;
        }
        return '\u0000';
    }
}
