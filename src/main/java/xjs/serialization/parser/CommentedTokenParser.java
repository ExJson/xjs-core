package xjs.serialization.parser;

import xjs.core.CommentType;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenStream;

import java.util.Arrays;

public abstract class CommentedTokenParser extends TokenParser {
    final StringBuilder commentBuffer;

    protected CommentedTokenParser(final TokenStream root) {
        super(root);
        this.commentBuffer = new StringBuilder();
    }

    @Override
    protected boolean consumeWhitespace(
            final Token t, final boolean nl) {
        switch (t.type) {
            case HASH_COMMENT:
            case LINE_COMMENT:
                this.appendComment(t);
                return true;
            case BLOCK_COMMENT:
                this.appendMultilineComment(t);
                return true;
            case BREAK:
                if (nl) {
                    this.flagLineAsSkipped();
                    return true;
                }
        }
        return false;
    }

    @Override
    protected void flagLineAsSkipped() {
        if (this.commentBuffer.length() > 0) {
            this.commentBuffer.append('\n');
        } else {
            this.linesSkipped++;
        }
    }

    protected void appendComment(final Token t) {
        this.commentBuffer.append(
            this.reference, t.start, t.end);
    }

    protected void appendMultilineComment(final Token t) {
        int lineStart = t.start;
        int lastChar = t.start;
        for (int i = t.start; i < t.end; i++) {
            final char c = this.reference.charAt(i);
            if (c == '\n') {
                this.commentBuffer.append(
                    this.reference, lineStart, lastChar + 1);
                this.commentBuffer.append('\n');
                i = this.skipToOffset(i + 1, t.offset);
                lineStart = i;
            } else if (!Character.isWhitespace(c)) {
                lastChar = i;
            }
        }
        this.commentBuffer.append(
            this.reference, lineStart, lastChar + 1);
    }

    protected void prependLinesSkippedToComment() {
        if (this.linesSkipped > 1) {
            final char[] lines = new char[this.linesSkipped - 1];
            Arrays.fill(lines, '\n');
            this.commentBuffer.insert(0, lines);
            this.linesSkipped = 0;
        }
    }

    protected void setComment(final CommentType type, final boolean trim) {
        final String comment = this.takeComment(trim);
        if (!comment.isEmpty()) {
            this.formatting.getComments().setData(type, comment);
        }
    }

    protected String takeComment(final boolean trim) {
        if (this.commentBuffer.length() == 0) {
            return "";
        }
        // line comments _must_ have newlines,
        // so they will be added later.
        if (trim && this.commentBuffer.charAt(this.commentBuffer.length() - 1) == '\n') {
            this.commentBuffer.setLength(this.commentBuffer.length() - 1);
        }
        final String comment = this.commentBuffer.toString();
        this.commentBuffer.setLength(0);
        return comment;
    }
}
