package xjs.serialization.token;

import org.jetbrains.annotations.Nullable;
import xjs.exception.SyntaxException;
import xjs.serialization.token.Token.Type;
import xjs.serialization.util.PositionTrackingReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class Tokenizer {

    private Tokenizer() {}

    public static TokenStream stream(final String text) {
        return stream(text, 0, text.length(), 0);
    }

    public static TokenStream stream(final String text, final int s, final int e, final int o) {
        return new TokenStream(PositionTrackingReader.fromString(text), s, e, o, Type.OPEN);
    }

    public static TokenStream stream(final InputStream is) throws IOException {
        return new TokenStream(PositionTrackingReader.fromIs(is, true), 0, 0, 0, Type.OPEN);
    }

    public static @Nullable Token single(final PositionTrackingReader reader) throws IOException {
        reader.skipLineWhitespace();
        if (reader.isEndOfText()) {
            return null;
        }
        final char c = (char) reader.current;
        final int s = reader.index;
        final int o = reader.column;
        if (c == '/' || c == '#') {
            return comment(reader, c, s, o);
        } else if (c == '\'' || c == '\"') {
            return quote(reader, s, o, c);
        } else if (c == '\n') {
            reader.read();
            return new Token(s, s + 1, o, Type.BREAK);
        } else if (c == '.') {
            return dot(reader, s, o);
        } else if (Character.isDigit(c)) {
            return number(reader, s, o);
        }
        return word(reader, s, o);
    }

    private static Token quote(
            final PositionTrackingReader reader, final int i, final int o, final char quote) throws IOException {
        final String parsed = reader.readQuoted(quote);
        if (parsed.isEmpty() && quote == '\'' && reader.readIf('\'')) {
            return triple(reader, i, o);
        }
        final Type type = quote == '\'' ? Type.SINGLE : Type.DOUBLE;
        return new StringToken(i, reader.index, o, type, parsed);
    }

    private static Token triple(final PositionTrackingReader reader, final int i, final int o) throws IOException {
        int quotes = 0;
        while (!reader.isEndOfText()) {
            final char c = (char) reader.current;
            if (c == '\\') {
                quotes = 0;
                reader.read();
                continue;
            }
            quotes = c == '\'' ? quotes + 1 : 0;
            reader.read();
            if (quotes == 3) {
                return new Token(i, reader.index, o, Type.TRIPLE);
            }
        }
        throw reader.expected("end of multiline string (\"'''\")");
    }

    private static Token comment(
            final PositionTrackingReader reader, char c, final int i, final int o) throws IOException {
        reader.read();
        if (c == '#' || reader.readIf('/')) {
            reader.skipToNL();
            return new Token(i, reader.index, o, Type.LINE);
        } else if (!reader.readIf('*')) {
            return new SymbolToken(i, i + 1, o, c);
        }
        boolean asterisk = false;
        while (!reader.isEndOfText()) {
            c = (char) reader.current;
            reader.read();
            if (asterisk && c == '/') {
                return new Token(i, reader.index, o, Type.MULTI);
            }
            asterisk = c == '*';
        }
        throw reader.expected("end of multiline comment ('*/')");
    }

    private static Token word(
            final PositionTrackingReader reader, final int i, final int o) throws IOException {
        do {
            final char c = (char) reader.current;
            if (c == '_' || Character.isLetterOrDigit(c)) {
                reader.read();
            } else if (reader.index - i == 0) {
                reader.read();
                return new SymbolToken(i, reader.index, o, c);
            } else {
                break;
            }
        } while (!reader.isEndOfText());
        return new Token(i, reader.index, o, Type.WORD);
    }

    private static Token dot(
            final PositionTrackingReader reader, final int i, final int o) throws IOException {
        reader.read();
        if (reader.isDigit()) {
            return number(reader, i, o);
        }
        return new SymbolToken(i, i + 1, o, '.');
    }

    private static Token number(
            final PositionTrackingReader reader, int i, final int o) throws IOException {
        reader.startCapture();
        if (reader.current != '0') { // ???
            reader.readAllDigits();
        }
        if (reader.readIf('.')) {
            if (!reader.isDigit()) {
                return parseNumber(reader, i, o);
            }
            reader.readAllDigits();
        }
        if (reader.readIf('e') || reader.readIf('E')) {
            if (!reader.readIf('+')) {
                reader.readIf('-'); // if no other numbers, result is ignored
            }
            if (!reader.readDigit()) {
                return new Token(i, reader.index, o, Type.WORD);
            }
            reader.readAllDigits();
        }
        return parseNumber(reader, i, o);
    }

    private static NumberToken parseNumber(
            final PositionTrackingReader reader, final int i, final int o) {
        final double number = Double.parseDouble(reader.endCapture());
        return new NumberToken(i, reader.index, o, number);
    }

    public static ContainerToken containerize(final InputStream is) throws IOException {
        return containerize(stream(is));
    }

    public static ContainerToken containerize(final String text) {
        return containerize(text, 0, text.length(), 0);
    }

    public static ContainerToken containerize(final String text, final int s, final int e, final int o) {
        return containerize(stream(text, s, e, o));
    }

    public static ContainerToken containerize(final TokenStream stream) {
        if (stream instanceof ContainerToken) {
            return (ContainerToken) stream;
        }
        final ContainerToken container = container(
            stream.iterator(), stream.reference, stream.start, stream.offset, Type.OPEN, '\u0000');
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
            final int s, final int o, final Type type, final char closer) {
        final List<Token> tokens = new ArrayList<>();
        int e = s;
        while (itr.hasNext()) {
            final Token control = itr.next();
            if (tokenMatchesSymbol(control, closer)) {
                return new ContainerToken(reference.toString(), s, control.end, o, type, tokens);
            }
            final Token next = next(itr, reference, control);
            tokens.add(next);
            e = next.end;
        }
        if (closer != '\u0000') {
            throw SyntaxException.expected(closer, s, o);
        }
        return new ContainerToken(reference.toString(), s, e, o, type, tokens);
    }

    private static boolean tokenMatchesSymbol(final Token token, final char symbol) {
        return token instanceof SymbolToken && ((SymbolToken) token).symbol == symbol;
    }

    private static Token next(final TokenStream.Itr itr, final CharSequence reference, final Token control) {
        switch (getSymbol(control)) {
            case '{':
                return container(itr, reference, control.start, control.offset, Type.BRACES, '}');
            case '[':
                return container(itr, reference, control.start, control.offset, Type.BRACKETS, ']');
            case '(':
                return container(itr, reference, control.start, control.offset, Type.PARENTHESES, ')');
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
