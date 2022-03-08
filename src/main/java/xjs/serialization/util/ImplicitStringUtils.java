package xjs.serialization.util;

import xjs.exception.SyntaxException;

public final class ImplicitStringUtils {

    private ImplicitStringUtils() {}

    public static boolean isBalanced(final String text) {
        if (text.isEmpty()) return true;
        return indexOf(text, 0, '\u0000', false, false) >= 0;
    }

    public static String select(final String text, final int s, final StringContext ctx) {
        return text.substring(s, expect(text, s, ctx));
    }

    public static boolean find(final String text, final StringContext ctx) {
        return indexOf(text, 0, ctx) >= 0;
    }

    public static int indexOf(final String text, final int s, final StringContext ctx) {
        if (ctx == StringContext.KEY) {
            return indexOf(text, s, ':', false, true);
        }
        return indexOf(text, s, ',', true, true);
    }

    public static int indexOf(final String text, int s, final char e, final boolean n, final boolean u) {
        try {
            return expect(text, s, e, n, u);
        } catch (final SyntaxException ignored) {
            return -1;
        }
    }

    public static int expect(final String text, final int s, final StringContext ctx) {
        if (ctx == StringContext.KEY) {
            return expect(text, s, ':', false, true);
        }
        return expect(text, s, ',', true, true);
    }

    public static int expect(final String text, final int s, final char e, final boolean n, final boolean u) {
        int index = search(text, s, e, n, u);
        index = trimWhitespace(text, index);
        index = trimComments(text, index);
        return trimWhitespace(text, index);
    }

    private static int search(final String text, final int s, final char e, final boolean n, final boolean u) {
        for (int i = s; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == e || (n && c == '\n')) {
                return i;
            }
            if (u && (c == '}' || c == ']' || c == ')')) {
                return i;
            }
            i = getNextIndex(text, i, c);
        }
        if (e == '}' || e == ']' || e == ')') throw unclosed(text, e, s - 1);
        if (n || e == '\u0000') return text.length();
        throw endOfInput(text, s);
    }

    private static int getNextIndex(final String text, final int s, final char c) {
        switch (c) {
            case '{': return search(text, s + 1, '}', false, false);
            case '[': return search(text, s + 1, ']', false, false);
            case '(': return search(text, s + 1, ')', false, false);
            case '}':
            case ']':
            case ')': throw unexpected(text, c, s);
            case '/': return skipSlash(text, s) - 1;
            case '#': return skipToNl(text, s) - 1;
            case '\'': if (isMulti(text, s)) return expectMulti(text, s);
            case '"': return expectQuote(text, s, c);
            case '\\': return skipBackslash(text, s);
        }
        return s;
    }

    private static int skipSlash(final String text, final int s) {
        if (s + 1 >= text.length()) {
            return s + 1;
        }
        final char next = text.charAt(s + 1);
        if (next == '/') {
            final int n = text.indexOf('\n', s + 1);
            return n >= 0 ? n : text.length();
        } else if (next == '*') {
            return text.indexOf("*/", s + 1);
        }
        return s + 1;
    }

    private static int skipToNl(final String text, final int s) {
        final int i = text.indexOf('\n', s);
        return i > 0 ? i : text.length();
    }

    private static boolean isMulti(final String text, final int s) {
        if (s + 2 < text.length()) {
            return text.charAt(s + 1) == '\'' && text.charAt(s + 2) == '\'';
        }
        return false;
    }

    public static int expectMulti(final String text, final int s) {
        int i = s;
        while (i++ < text.length() - 2) {
            if (text.charAt(i) == '\'' && text.charAt(i + 1) == '\'' && text.charAt(i + 2) == '\'') {
                return i + 2;
            }
        }
        throw noMulti(text, i);
    }

    public static int expectQuote(final String text, final int s, final char quote) {
        int i = s;
        char c = '\u0000';
        while (++i < text.length() && (c = text.charAt(i)) != quote) {
            if (c == '\\') {
                i++;
            } else if (c == '\n') {
                throw illegalNl(text, i);
            }
        }
        if (i == s || c != quote) {
            throw unclosed(text, quote, s);
        }
        return i;
    }

    private static int skipBackslash(final String text, final int s) {
        return text.charAt(s + 1) == '\r' ? s + 2 : s + 1;
    }

    private static int trimWhitespace(final String text, int e) {
        while (--e >= 0) { // while e < 0, if char at e - 1, then return e ???
            if (!Character.isWhitespace(text.charAt(e))) {
                return e + 1;
            }
        }
        return e + 1;
    }

    private static int trimComments(final String text, final int e) {
        if (e - 2 >= 0 && text.charAt(e - 1) == '/' && text.charAt(e - 2) == '*') {
            return text.lastIndexOf("/*", e - 1);
        }
        for (int i = e - 1; i > 0; i--) {
            final char c = text.charAt(i);
            if (c == '\n' || c == '}' || c == ']' || c == ')') {
                return e;
            }
            if (c == '#') {
                return i;
            }
            if (c == '/' && text.charAt(i - 1) == '/') {
                return i - 1;
            }
        }
        return e;
    }

    public static String escape(final String text, final StringContext ctx) {
        if (ctx == StringContext.KEY) {
            return escape(text, ':', false);
        }
        return escape(text, ',', true);
    }

    public static String escape(final String text, final char e, final boolean toNl) {
        final StringBuilder sb = new StringBuilder();
        int level = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '{':
                case '[':
                case '(': level++; break;
                case '}':
                case ']':
                case ')': level--; break;
                case '\r': i++; continue;
                case '/':
                    sb.append(text, i, i = skipSlash(text, i));
                    i--;
                    continue;
                case '#':
                    sb.append(text, i, i = text.indexOf('\n', i));
                    i--;
                    continue;
                case '\'':
                case '"': appendValidQuote(sb, text, i, c); break;
            }
            if (level == 0 && (c == e || (toNl && c == '\n'))) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static void appendValidQuote(final StringBuilder sb, final String text, int i, final char quote) {
        sb.append(quote);
        char c;
        while ((c = text.charAt(++i)) != quote) {
            sb.append(c);
            if (c == '\\') {
                sb.append(text.charAt(++i));
            }
        }
        sb.append(quote);
    }

    private static SyntaxException unclosed(final String text, final char c, final int index) {
        final int[] lineColumn = getLineColumn(text, index);
        return new SyntaxException("Unclosed '" + getOpener(c) + "'", lineColumn[0], lineColumn[1]);
    }

    private static SyntaxException noMulti(final String text, final int index) {
        final int[] lineColumn = getLineColumn(text, index);
        return SyntaxException.expected("unclosed triple quote (''')", lineColumn[0], lineColumn[1]);
    }

    private static SyntaxException unexpected(final String text, final char c, final int index) {
        final int[] lineColumn = getLineColumn(text, index);
        return SyntaxException.unexpected(c, lineColumn[0], lineColumn[1]);
    }

    private static SyntaxException illegalNl(final String text, final int index) {
        final int[] lineColumn = getLineColumn(text, index);
        return SyntaxException.unexpected("new line", lineColumn[0], lineColumn[1]);
    }

    private static SyntaxException endOfInput(final String text, final int index) {
        final int[] lineColumn = getLineColumn(text, index);
        return SyntaxException.unexpected("end of input", lineColumn[0], lineColumn[1]);
    }

    private static char getOpener(final char closer) {
        switch (closer) {
            case '}': return '{';
            case ']': return '[';
            case ')': return '(';
        }
        return closer;
    }

    private static int[] getLineColumn(final String text, final int index) {
        int line = 1;
        int column = 1;
        for (int i = 0; i < index; i++) {
            if (text.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new int[] { line, column };
    }
}
