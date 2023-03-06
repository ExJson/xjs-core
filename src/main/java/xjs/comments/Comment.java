package xjs.comments;

import xjs.serialization.token.CommentToken;

public class Comment {
    public final CommentStyle style;
    public final String text;

    public Comment(final CommentToken token) {
        this(token.commentStyle(), token.parsed());
    }

    public Comment(final CommentStyle style, final String text) {
        this.style = style;
        this.text = text;
    }

    @Override
    public int hashCode() {
        return this.style.hashCode() + 31 * text.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof Comment) {
            final Comment c = (Comment) o;
            return this.style == c.style
                && this.text.equals(c.text);
        }
        return false;
    }
}
