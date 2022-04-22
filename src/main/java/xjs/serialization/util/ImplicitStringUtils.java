package xjs.serialization.util;

import xjs.exception.SyntaxException;

/**
 * A collection of utilities for highlighting streams of miscellaneous tokens,
 * written in the so-called "implicit" space. These tokens behave like any other
 * container, comment, quoted string element, number, etc. and follow the same
 * rules when given.
 *
 * <p>This includes:
 *
 * <ul>
 *   <li>Single and double-quoted line strings</li>
 *   <li>Triple-quoted or multiline strings</li>
 *   <li>C-style line and hash comments</li>
 *   <li>Block or multiline comments, and</li>
 *   <li>Containers: <code>[]</code>, <code>{}</code>, and <code>()</code></li>
 * </ul>
 *
 * <p>The syntax described here should be sufficient to support highlighting
 * tokens in a variety of languages. Namely,
 *
 * <ul>
 *   <li>JSON</li>
 *   <li>Java</li>
 *   <li>JavaScript</li>
 *   <li>Python, and</li>
 *   <li>Any other syntax built around similar token streams</li>
 * </ul>
 *
 * <p>Callers should be aware that ImplicitStringUtils is <b>not</b> designed for
 * evaluating and parsing these tokens. Rather, its goal is to <b>isolate</b> the
 * tokens, so that they may be preserved and evaluated at some other time.
 *
 * <p>For example, to generate a substring highlighting the first key in a JSON
 * object:
 *
 * <pre>{@code
 *   final String json = "{ key: value }";
 *   final String key = select(json, 1, StringContext.KEY);
 *   assert "key".equals(key);
 * }</pre>
 *
 * <p>Or, more verbosely, to get the index of a <code>&gt;</code>token in
 * {@link #isBalanced unbalanced} space:
 *
 * <pre>{@code
 *   final String tokens = "(a > b) > (c > d)";
 *   final int index = indexOf(tokens, 0, '>', false, true);
 *   assert 8 == index; // at the very center
 * }</pre>
 *
 * <p>And finally, these utilities may be used to isolate and verify the contents
 * of quoted strings and other similar tokens:
 *
 * <pre>{@code
 *   final String quoted = " 'Hello, world!' ";
 *   final String closer = expectQuote(quoted, 1, '\'');
 *   assert 15 == closer;
 * }</pre>
 */
public final class ImplicitStringUtils {

    private ImplicitStringUtils() {}

    /**
     * Indicates whether the given text is "balanced."
     *
     * <p>A stream of tokens is considered <em>balanced</em> when all of its
     * containers, comments, and quotes are properly closed <b>in the correct
     * order</b>.
     *
     * <p>For example, the following stream of tokens represents a parenthetical
     * container housing a single-quoted string. The container and string are
     * both closed:
     *
     * <pre>{@code
     *   ('hello, world')
     * }</pre>
     *
     * <p>These containers may be nested recursively. As such, the writer must
     * pay careful attention to close the containers <b>in the correct order</b>.
     * The following represents a series of nested containers, the whole of which
     * may be considered <em>balanced</em>:
     *
     * <pre>{@code
     *   (()[{}])
     * }</pre>
     *
     * <p>The exact rules and supported types of balance-able tokens is outlined
     * the header.
     *
     * @param text The text being inspected for balance.
     * @return <code>true</code>, if this text is balanced.
     */
    public static boolean isBalanced(final String text) {
        if (text.isEmpty()) return true;
        return indexOf(text, 0, '\u0000', false, false) >= 0;
    }

    /**
     * Generates a slice of text for the given string context (i.e. either a key
     * or a value).
     *
     * <p>For example, to isolate the value in the following JSON object:
     *
     * <pre>{@code
     *   { "key": "value" }
     * }</pre>
     *
     * <p>Start after the key and select by context:
     *
     * <pre>{@code
     *   final String value = select(json, 9, StringContext.VALUE);
     *   assert "\"value\"".equals(value)
     * }</pre>
     *
     * @param text The stream of tokens being evaluated.
     * @param s    The starting index.
     * @param ctx  The type of implicit space being evaluated (a key or a value)
     * @return The substring of text representing these tokens.
     * @throws SyntaxException If the text is not {@link #isBalanced balanced}.
     */
    public static String select(final String text, final int s, final StringContext ctx) {
        return text.substring(s, expect(text, s, ctx));
    }

    /**
     * Indicates whether the first stream of tokens in the given text is valid
     * for the expected context.
     *
     * <p>For example, to determine if a given token stream starts with either
     * a key or a value:
     *
     * <pre>{@code
     *   final String json = "key: value";
     *   final boolean isKey = find(json, StringContext.KEY);
     *   assert isKey;
     * }</pre>
     *
     * @param text The stream of tokens being evaluated.
     * @param ctx  The type of tokens expected at the beginning of the text.
     * @return <code>true</code>, if the context is valid.
     */
    public static boolean find(final String text, final StringContext ctx) {
        return indexOf(text, 0, ctx) >= 0;
    }

    /**
     * Locates the end of a token stream for the given string context.
     *
     * <p>For example, to get the index of the first unbalanced <code>:</code>:
     *
     * <pre>{@code
     *   final String json = "{ key: value }";
     *   final int index = indexOf(json, 1, StringContext.KEY);
     *   assert 5 == index;
     * }</pre>
     *
     * <p>Callers should be aware that trailing whitespace and comments will be
     * trimmed from the output. <b>The return value indicates the end of the key
     * up to any trailing tokens</b>.
     *
     * @param text The stream of tokens being evaluated.
     * @param s    The starting index.
     * @param ctx  The type of tokens expected at this index.
     * @return The index at the end of these tokens, <b>or else -1</b>.
     */
    public static int indexOf(final String text, final int s, final StringContext ctx) {
        if (ctx == StringContext.KEY) {
            return indexOf(text, s, ':', false, true);
        }
        return indexOf(text, s, ',', true, true);
    }

    /**
     * Locates the end of a token stream <em>up to</em> the expected character
     * in {@link #isBalanced balanced} space.
     *
     * For example, to get the index of a <code>&gt;</code>token in unbalanced
     * space:
     *
     * <pre>{@code
     *   final String tokens = "(a > b) > (c > d)";
     *   final int index = indexOf(tokens, 0, '>', false, true);
     *   assert 8 == index; // at the very center
     * }</pre>
     *
     * @param text The stream of tokens being evaluated.
     * @param s    The starting index.
     * @param e    The expected character.
     * @param n    Whether to stop at the first unbalanced newline.
     * @param u    Whether to stop when the text becomes unbalanced.
     * @return The first index of this character in unbalanced space, or else -1.
     */
    public static int indexOf(final String text, int s, final char e, final boolean n, final boolean u) {
        try {
            return expect(text, s, e, n, u);
        } catch (final SyntaxException ignored) {
            return -1;
        }
    }

    /**
     * Variant of {@link #indexOf(String, int, StringContext)} which throws a
     * {@link SyntaxException} with details about the error if the syntax of
     * the input invalid.
     *
     * @param text The stream of tokens being evaluated.
     * @param s    The starting index.
     * @param ctx  The Type of tokens expected at this index.
     * @return The index at the end of these tokens.
     * @throws SyntaxException if the syntax of the input is invalid.
     */
    public static int expect(final String text, final int s, final StringContext ctx) {
        if (ctx == StringContext.KEY) {
            return expect(text, s, ':', false, true);
        }
        return expect(text, s, ',', true, true);
    }

    /**
     * Variant of {@link #indexOf(String, int, char, boolean, boolean)} which
     * throws a {@link SyntaxException} with details about the error if the
     * syntax of the input invalid.
     *
     * @param text The stream of tokens being evaluated.
     * @param s    The starting index.
     * @param e    The expected character.
     * @param n    Whether to stop at the first unbalanced newline.
     * @param u    Whether to stop when the text becomes unbalanced.
     * @return The first index of this character in unbalanced space.
     * @throws SyntaxException If the syntax of the input is invalid.
     */
    private static int expect(final String text, final int s, final char e, final boolean n, final boolean u) {
        for (int i = s; i < text.length(); i++) {
            char c = text.charAt(i);
            if (shouldExit(c, e, n, u)) {
                return i;
            }
            // Lookahead to ignore comments and whitespace
            final int next = skipBetweenValues(text, i, n);
            if (next == i) {
                i = getNextIndex(text, i, c);
                continue;
            }
            if (next == text.length()) {
                checkEndOfInput(text, next, e, n);
                return i;
            }
            c = text.charAt(next);
            if (shouldExit(c, e, n, u)) {
                return i;
            }
            i = getNextIndex(text, next, c);
        }
        checkEndOfInput(text, s, e, n);
        return text.length();
    }

    private static boolean shouldExit(final char c, final char e, final boolean n, final boolean u) {
        return c == e || (n && c == '\n') || (u && (c == '}' || c == ']' || c == ')'));
    }

    private static int skipBetweenValues(final String text, int s, final boolean n) {
        while (s < text.length()) {
            int next = skipWhitespace(text, s, n);
            if (next < text.length()) {
                next = skipComments(text, next, text.charAt(next));
            }
            if (next == s) {
                return s;
            }
            s = next;
        }
        return s;
    }

    private static int skipWhitespace(final String text, int s, final boolean n) {
        char c = text.charAt(s);
        while (true) {
            if ((n && c == '\n') || !Character.isWhitespace(c)) {
                return s;
            }
            if (++s == text.length()) {
                return s;
            }
            c = text.charAt(s);
        }
    }

    private static int skipComments(final String text, final int s, final char c) {
        if (c == '/') return skipSlash(text, s);
        if (c == '#') return skipToNl(text, s);
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
            final int e = text.indexOf("*/", s + 1);
            if (e < 0) throw unclosedComment(text, s);
            return e + 2;
        }
        return s + 1;
    }

    private static int skipToNl(final String text, final int s) {
        final int i = text.indexOf('\n', s);
        return i > 0 ? i : text.length();
    }

    private static int getNextIndex(final String text, final int s, final char c) {
        switch (c) {
            case '{': return search(text, s + 1, '}');
            case '[': return search(text, s + 1, ']');
            case '(': return search(text, s + 1, ')');
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

    private static int search(final String text, final int s, final char e) {
        for (int i = s; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == e) {
                return i;
            }
            i = getNextIndex(text, i, c);
        }
        checkEndOfInput(text, s, e, false);
        return text.length();
    }

    private static void checkEndOfInput(final String text, final int s, final char e, final boolean n) {
        if (e == '}' || e == ']' || e == ')') throw unclosed(text, e, s - 1);
        if (n || e == '\u0000') return;
        throw endOfInput(text, s);
    }

    private static boolean isMulti(final String text, final int s) {
        if (s + 2 < text.length()) {
            return text.charAt(s + 1) == '\'' && text.charAt(s + 2) == '\'';
        }
        return false;
    }

    /**
     * Locates the end of a triple-quoted (multiline) string.
     *
     * @param text The stream of tokens being evaluated.
     * @param s    The starting index.
     * @return The first index after the very last <code>'''</code>.
     * @throws SyntaxException If the syntax at this location is invalid.
     */
    public static int expectMulti(final String text, final int s) {
        int i = s;
        while (i++ < text.length() - 2) {
            if (text.charAt(i) == '\'' && text.charAt(i + 1) == '\'' && text.charAt(i + 2) == '\'') {
                return i + 2;
            }
        }
        throw noMulti(text, i);
    }

    /**
     * Locates the end of a single-quoted (line) string.
     *
     * @param text  The stream of tokens being evaluated.
     * @param s     The starting index.
     * @param quote The type of quote being expected.
     * @return The first index of <code>quote</code>, ignoring escapes.
     * @throws SyntaxException If the syntax at this location is invalid.
     */
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

    public static String escape(final String text, final StringContext ctx) {
        if (ctx == StringContext.KEY) {
            return escape(text, ':', false);
        }
        return escape(text, ',', true);
    }

    /**
     * Generates the escaped output of any {@link #isBalanced partially balanced}
     * stream of tokens.
     *
     * <p>For example, when given the following text:
     *
     * <pre>{@code
     *   There are two types of implicit space:
     *     * keys
     *     * values
     * }</pre>
     *
     * <p>And escaping both <code>:</code> and newline characters:
     *
     * <pre>{@code
     *   final String escaped = escape(text, ':', true);
     * }</pre>
     *
     * <p>The following output is produced:
     *
     * <pre>{@code
     *   There are two types of implicit space\:\
     *     * keys\
     *     * values
     * }</pre>
     *
     * <p>Note that balanced text <em>must</em> be terminated, and thus such
     * characters will <em>not</em> be escaped. For example, the following
     * input will be unchanged in the output:
     *
     * <pre>{@code
     *   (
     *     1,
     *     2,
     *     3
     *   )
     * }</pre>
     *
     * @param text The raw tokens being escaped.
     * @param e    Whichever character may denote the end of the tokens
     * @param toNl Whether to escape newline characters.
     * @return The escaped output of these tokens.
     */
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

    private static SyntaxException unclosedComment(final String text, final int index) {
        final int[] lineColumn = getLineColumn(text, index);
        return SyntaxException.expected("*/", lineColumn[0], lineColumn[1]);
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
