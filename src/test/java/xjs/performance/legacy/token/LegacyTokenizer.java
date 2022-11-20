package xjs.performance.legacy.token;

import org.jetbrains.annotations.Nullable;
import xjs.exception.SyntaxException;
import xjs.performance.legacy.token.LegacyToken.Type;
import xjs.serialization.util.ImplicitStringUtils;

import java.util.ArrayList;
import java.util.List;

public final class LegacyTokenizer {

    private LegacyTokenizer() {}

    public static LegacyTokenStream stream(final String text) {
        return stream(text, 0, text.length(), 0);
    }

    public static LegacyTokenStream stream(final String text, final int s, final int e, final int o) {
        return new LegacyTokenStream(text, s, e, o, Type.OPEN);
    }

    public static @Nullable LegacyToken single(final String text, int i, int o) {
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
            return new LegacyToken(text, i, i + 1, o, Type.BREAK);
        } else if (Character.isDigit(c)) {
            return number(text, i, o);
        }
        final LegacyToken word = word(text, i, o);
        if (word != null) {
            return word;
        }
        return new LegacySymbolToken(text, i, i + 1, o, c);
    }

    private static LegacyToken doubleQuote(final String text, final int i, final int o) {
        final int str = ImplicitStringUtils.expectQuote(text, i, '"');
        return new LegacyToken(text, i, str + 1, o, Type.DOUBLE);
    }

    private static LegacyToken singleQuote(final String text, final int i, final int o) {
        if (isTripleQuote(text, i)) {
            final int triple = ImplicitStringUtils.expectMulti(text, i + 1);
            return new LegacyToken(text, i, triple + 1, o, Type.TRIPLE);
        }
        final int single = ImplicitStringUtils.expectQuote(text, i, '\'');
        return new LegacyToken(text, i, single + 1, o, Type.SINGLE);
    }

    private static boolean isTripleQuote(final String text, final int i) {
        return i + 2 < text.length() && text.charAt(i + 1) == '\'' && text.charAt(2) == '\'';
    }

    private static LegacyToken comment(final String text, final char c, final int i, final int o) {
        final int comment = ImplicitStringUtils.expectComment(text, i, c);
        if (comment != i + 1) {
            final Type type = c == '/' && text.charAt(i + 1) == '*'
                ? Type.MULTI : Type.LINE;
            return new LegacyToken(text, i, comment, o, type);
        }
        return new LegacySymbolToken(text, i, i + 1, o, c);
    }

    private static @Nullable LegacyToken word(final String text, int i, final int o) {
        final int s = i;
        char c;
        while (i < text.length() && ((c = text.charAt(i)) == '_' || Character.isLetterOrDigit(c))) {
            i++;
        }
        if (s != i) {
            return new LegacyToken(text, s, i, o, Type.WORD);
        }
        return null;
    }

    private static LegacyToken number(final String text, int i, final int o) {
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
                return new LegacyToken(text, s, i, o, Type.WORD);
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

    private static LegacyToken parseNumber(final String text, final int s, final int e, final int o) {
        final String number = text.substring(s, e);
        try {
            return new LegacyNumberToken(text, s, e, o, Double.parseDouble(number));
        } catch (final NumberFormatException ignored) {
            return new LegacyToken(text, s, e, o, Type.WORD);
        }
    }

    public static LegacyContainerToken containerize(final String text) {
        return containerize(text, 0, text.length(), 0);
    }

    public static LegacyContainerToken containerize(final String text, final int s, final int e, final int o) {
        return containerize(stream(text, s, e, o));
    }

    public static LegacyContainerToken containerize(final LegacyTokenStream stream) {
        if (stream instanceof LegacyContainerToken) {
            return (LegacyContainerToken) stream;
        }
        final LegacyContainerToken container = container(
            stream.iterator(), stream.reference, stream.start, stream.offset, Type.OPEN, '\u0000');
        if (container.size() == 1) {
            final LegacyToken firstToken = container.get(0);
            if (firstToken instanceof LegacyContainerToken) {
                return (LegacyContainerToken) firstToken;
            }
        }
        return container;
    }

    private static LegacyContainerToken container(
            final LegacyTokenStream.Itr itr, final String reference,
            final int s, final int o, final Type type, final char closer) {
        final List<LegacyToken> tokens = new ArrayList<>();
        int e = s;
        while (itr.hasNext()) {
            final LegacyToken control = itr.next();
            if (tokenMatchesSymbol(control, closer)) {
                return new LegacyContainerToken(reference, s, control.end, o, type, tokens);
            }
            final LegacyToken next = next(itr, reference, control);
            tokens.add(next);
            e = next.end;
        }
        if (closer != '\u0000') {
            throw SyntaxException.expected(closer, s, o);
        }
        return new LegacyContainerToken(reference, s, e, o, type, tokens);
    }

    private static boolean tokenMatchesSymbol(final LegacyToken token, final char symbol) {
        return token instanceof LegacySymbolToken && ((LegacySymbolToken) token).symbol == symbol;
    }

    private static LegacyToken next(final LegacyTokenStream.Itr itr, final String reference, final LegacyToken control) {
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

    private static char getSymbol(final LegacyToken token) {
        if (token instanceof LegacySymbolToken) {
            return ((LegacySymbolToken) token).symbol;
        }
        return '\u0000';
    }
}
