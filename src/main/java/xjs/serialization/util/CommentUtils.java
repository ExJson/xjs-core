package xjs.serialization.util;

import xjs.core.CommentStyle;
import xjs.serialization.JsonContext;

/**
 * A collection of utilities for generating formatted comments and parsing their contents
 * as regular text. These utilities can be used by foreign callers to generate custom
 * formatted comments or manipulate existing comments paired with any regular JSON value.
 *
 * <p>For example, to format a string message as a regular {@link CommentStyle#LINE line}
 * comment:
 *
 * <pre>{@code
 *   final String message = "Hello, world!";
 *   final String comment = format(CommentStyle.LINE, message);
 *   assert "// Hello, world!".equals(comment);
 * }</pre>
 *
 * <p>Or, to strip the message contents out of this comment:
 *
 * <pre>{@code
 *   final String comment = "// Hello, world!";
 *   final String message = strip(comment);
 *   assert "Hello, world!".equals(message);
 * }</pre>
 *
 * <p>And finally, to reformat a comment into a different style:
 *
 * <pre>{@code
 *   final String line = "// Hello, world!";
 *   final String hash = rewrite(CommentStyle.HASH, line);
 *   assert "# Hello, world!".equals(hash);
 * }</pre>
 */
public final class CommentUtils {

    private CommentUtils() {}

    /**
     * Reformats the given comment data (or message) into some other {@link CommentStyle}.
     *
     * @param style The style of the output comment data.
     * @param text  The data or message being formatted.
     * @return An equivalent comment in the new format.
     */
    public static String rewrite(final CommentStyle style, final String text) {
        return format(style, strip(text));
    }

    /**
     * Strips the message out of the given comment data.
     *
     * <p>For example, reads <code>Hello, world!</code> from <code>// Hello, world!</code>.
     *
     * @param data The formatted comment data, including all comment-related symbols.
     * @return The message written inside this data.
     */
    public static String strip(final String data) {
        final String noSingles = data.replaceAll("(?<=^|\n\r?)\\s*(//|#)\\s?", "");
        if (noSingles.contains("/*")) {
            return noSingles.replaceAll("(\\s*\n\r?)?\\s?\\*/", "")
                .replaceAll("(?<=^|\n\r?)\\s*/?\\*\\s?", "");
        }
        return noSingles;
    }

    /**
     * Generates a formatted comment form the given message.
     *
     * <p>For example, writes <code>// Hello, world!</code> from <code>Hello, world!</code>.
     *
     * @param style The <em>style</em> of comment being written, e.g. hash, line, block, etc.
     * @param text  The message being written inside this comment.
     * @return A formatted comment containing the given message.
     */
    public static String format(final CommentStyle style, final String text) {
        final String[] lines = text.split("\r?\n");
        if (style.isMultiline()) {
            return formatBlockComment(style, lines);
        }
        final StringBuilder formatted = new StringBuilder();
        final String eol = JsonContext.getEol();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) formatted.append(eol);

            formatted.append(style.getPrefix());
            formatted.append(' ');
            formatted.append(lines[i]);
        }
        return formatted.toString();
    }

    private static String formatBlockComment(final CommentStyle style, final String... lines) {
        final String eol = JsonContext.getEol();
        final StringBuilder formatted = new StringBuilder();
        if (lines.length == 1) {
            formatted.append(style.getPrefix());
            formatted.append(' ');
            formatted.append(lines[0]);
            formatted.append(" */");
            return formatted.toString();
        }
        formatted.append(style.getPrefix());
        formatted.append(eol);
        for (final String line : lines) {
            formatted.append(" * ");
            formatted.append(line);
            formatted.append(eol);
        }
        formatted.append(" */");
        return formatted.toString();
    }
}
