package xjs.serialization.util;

import xjs.core.CommentStyle;
import xjs.serialization.JsonSerializationContext;

public final class CommentUtils {

    private CommentUtils() {}

    public static String rewrite(final CommentStyle style, final String text) {
        return format(style, strip(text));
    }

    public static String strip(final String data) {
        final String noSingles = data.replaceAll("(?<=^|\n\r?)\\s*(//|#)\\s?", "");
        if (noSingles.contains("/*")) {
            return noSingles.replaceAll("(\\s*\n\r?)?\\s?\\*/", "")
                .replaceAll("(?<=^|\n\r?)\\s*/?\\*\\s?", "");
        }
        return noSingles;
    }

    public static String format(final CommentStyle style, final String text) {
        final String[] lines = text.split("\r?\n");
        if (style.isMultiline()) {
            return formatBlockComment(style, lines);
        }
        final StringBuilder formatted = new StringBuilder();
        final String eol = JsonSerializationContext.getEol();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) formatted.append(eol);

            formatted.append(style.getPrefix());
            formatted.append(' ');
            formatted.append(lines[i]);
        }
        return formatted.toString();
    }

    private static String formatBlockComment(final CommentStyle style, final String... lines) {
        final String eol = JsonSerializationContext.getEol();
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
