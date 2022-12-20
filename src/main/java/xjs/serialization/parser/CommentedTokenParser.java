package xjs.serialization.parser;

import xjs.core.CommentType;
import xjs.core.JsonObject;
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
        switch (t.type()) {
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

    @Override
    protected void setAbove() {
        this.setComment(CommentType.HEADER);
        super.setAbove();
    }

    @Override
    protected void setBetween() {
        this.setComment(CommentType.VALUE);
        super.setBetween();
    }

    @Override
    protected void setTrailing() {
        this.setComment(CommentType.INTERIOR);
        super.setTrailing();
    }

    protected void readAboveOpenRoot(final JsonObject root) {
        this.readWhitespace();
        this.splitOpenHeader(root);
    }

    @Override
    protected void readAfter() {
        this.readLineWhitespace(false);
        this.setComment(CommentType.EOL);
    }

    @Override
    protected void readBottom() {
        this.readWhitespace(false);

        this.prependLinesSkippedToComment();
        this.setComment(CommentType.FOOTER);
        this.expectEndOfText();
    }

    protected void appendComment(final Token t) {
        this.commentBuffer.append(
            this.reference, t.start(), t.end());
    }

    protected void appendMultilineComment(final Token t) {
        int lineStart = t.start();
        int lastChar = t.start();
        for (int i = t.start(); i < t.end(); i++) {
            final char c = this.reference.charAt(i);
            if (c == '\n') {
                this.commentBuffer.append(
                    this.reference, lineStart, lastChar + 1);
                this.commentBuffer.append('\n');
                i = this.skipToOffset(i + 1, t.offset());
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

    protected void setComment(final CommentType type) {
        final String comment = this.takeComment(type);
        if (!comment.isEmpty()) {
            this.formatting.getComments().setData(type, comment);
        }
    }

    protected String takeComment(final CommentType type) {
        if (this.commentBuffer.length() == 0) {
            return "";
        }
        // line comments _must_ have newlines,
        // so they will be added later.
        if (this.commentHasNl()) {
            if (this.shouldTakeNl(type)) {
                this.trimComment();
                this.linesSkipped++;
            } else if (this.shouldTrimNl(type)) {
                this.trimComment();
            }
        }
        final String comment = this.commentBuffer.toString();
        this.commentBuffer.setLength(0);
        return comment;
    }

    // header comments always include a newline
    protected final boolean shouldTrimNl(final CommentType type) {
        return type == CommentType.HEADER;
    }

    // eol comments go to eol, but do not include the newline
    protected final boolean shouldTakeNl(final CommentType type) {
        return type == CommentType.EOL;
    }

    protected final boolean commentHasNl() {
        return this.commentBuffer.charAt(this.commentBuffer.length() - 1) == '\n';
    }

    protected final void trimComment() {
        this.commentBuffer.setLength(this.commentBuffer.length() - 1);
    }

    protected void splitOpenHeader(final JsonObject root) {
        if (this.commentBuffer.length() > 0) {
            final String header = this.commentBuffer.toString();
            final int end = this.getLastGap(header);
            if (end > 0) {
                root.getComments().setData(CommentType.HEADER, header.substring(0, end));
                root.setLinesAbove(this.linesSkipped);
                this.commentBuffer.delete(0, header.indexOf('\n', end + 1) + 1);
            }
        }
    }

    private int getLastGap(final String s) {
        for (int i = s.length() - 1; i > 0; i--) {
            if (s.charAt(i) != '\n') {
                continue;
            }
            while (i > 1) {
                final char next = s.charAt(--i);
                if (next == '\n') {
                    return i;
                } else if (next != ' ' && next != '\t' && next != '\r') {
                    break;
                }
            }
        }
        return -1;
    }
}
