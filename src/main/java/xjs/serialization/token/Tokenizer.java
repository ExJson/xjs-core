package xjs.serialization.token;

import org.jetbrains.annotations.Nullable;
import xjs.exception.SyntaxException;
import xjs.serialization.token.Token.Type;
import xjs.serialization.util.ImplicitStringUtils;

import java.util.ArrayList;
import java.util.List;

public final class Tokenizer {

    private Tokenizer() {}

    public static TokenStream stream(final String text) {
        return stream(text, 0, text.length(), 0);
    }

    public static TokenStream stream(final String text, final int s, final int e, final int o) {
        return new TokenStream(text, s, e, o, Type.OPEN);
    }

    public static @Nullable Token single(final String text, int i, int o) {
        if (i >= text.length()) {
            return null;
        }
        char c;
        while ((c = text.charAt(i)) == ' ' || c == '\t' || c == '\r' || c == '\f') {
            i++;
            o++;
            if (i == text.length()) {
                return null;
            }
        }
        if (c == '/' || c == '#') {
            return comment(text, c, i, o);
        } else if (c == '"') {
            return doubleQuote(text, i, o);
        } else if (c == '\'') {
            return singleQuote(text, i, o);
        } else if (c == '\n') {
            return new Token(text, i, i + 1, o, Type.BREAK);
        } else if (Character.isDigit(c)) {
            return number(text, i, o);
        }
        final Token word = word(text, i, o);
        if (word != null) {
            return word;
        }
        return new SymbolToken(text, i, i + 1, o, c);
    }

    private static Token doubleQuote(final String text, final int i, final int o) {
        final int str = ImplicitStringUtils.expectQuote(text, i, '"');
        return new Token(text, i, str + 1, o, Type.DOUBLE);
    }

    private static Token singleQuote(final String text, final int i, final int o) {
        if (isTripleQuote(text, i)) {
            final int triple = ImplicitStringUtils.expectMulti(text, i + 1);
            return new Token(text, i, triple + 1, o, Type.TRIPLE);
        }
        final int single = ImplicitStringUtils.expectQuote(text, i, '\'');
        return new Token(text, i, single + 1, o, Type.SINGLE);
    }

    private static boolean isTripleQuote(final String text, final int i) {
        return i + 2 < text.length() && text.charAt(i + 1) == '\'' && text.charAt(2) == '\'';
    }

    private static Token comment(final String text, final char c, final int i, final int o) {
        final int comment = ImplicitStringUtils.expectComment(text, i, c);
        if (comment != i + 1) {
            final Type type = c == '/' && text.charAt(i + 1) == '*'
                ? Type.MULTI : Type.LINE;
            return new Token(text, i, comment, o, type);
        }
        return new SymbolToken(text, i, i + 1, o, c);
    }

    private static @Nullable Token word(final String text, int i, final int o) {
        final int s = i;
        char c;
        while (i < text.length() && ((c = text.charAt(i)) == '_' || Character.isLetterOrDigit(c))) {
            i++;
        }
        if (s != i) {
            return new Token(text, s, i, o, Type.WORD);
        }
        return null;
    }

    private static Token number(final String text, int i, final int o) {
        final int s = i;
        boolean periodFound = false;
        char c = '\u0000';
        while (i < text.length()) {
            c = text.charAt(i);
            if (c == '.') {
                if (periodFound) {
                    return parseNumber(text, s, i, o);
                }
                periodFound = true;
            } else if (!Character.isDigit(c)) {
                break;
            }
            i++;
        }
        if (c == 'e' || c == 'E') {
            if (i == text.length()) {
                return new Token(text, s, i, o, Type.WORD);
            }
            i++;
            boolean signFound = false;
            while (i < text.length()) {
                c = text.charAt(i);
                if (c == '+' || c == '-') {
                    if (signFound) {
                       return parseNumber(text, s, i - 1, o);
                    }
                    signFound = true;
                } else if (!Character.isDigit(c)) {
                    break;
                }
                i++;
            }
            if (c == '+' || c == '-') {
                return parseNumber(text, s, i - 1, o);
            }
        }
        return parseNumber(text, s, i, o);
    }

    private static Token parseNumber(final String text, final int s, final int e, final int o) {
        final String number = text.substring(s, e);
        try {
            return new NumberToken(text, s, e, o, Double.parseDouble(number));
        } catch (final NumberFormatException ignored) {
            return new Token(text, s, e, o, Type.WORD);
        }
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
            final TokenStream.Itr itr, final String reference,
            final int s, final int o, final Type type, final char closer) {
        final List<Token> tokens = new ArrayList<>();
        int e = s;
        while (itr.hasNext()) {
            final Token control = itr.next();
            if (tokenMatchesSymbol(control, closer)) {
                return new ContainerToken(reference, s, control.end, o, type, tokens);
            }
            final Token next = next(itr, reference, control);
            tokens.add(next);
            e = next.end;
        }
        if (closer != '\u0000') {
            throw SyntaxException.expected(closer, s, o);
        }
        return new ContainerToken(reference, s, e, o, type, tokens);
    }

    private static boolean tokenMatchesSymbol(final Token token, final char symbol) {
        return token instanceof SymbolToken && ((SymbolToken) token).symbol == symbol;
    }

    private static Token next(final TokenStream.Itr itr, final String reference, final Token control) {
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
