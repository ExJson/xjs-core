package xjs.performance.legacy.parser;

import org.jetbrains.annotations.NotNull;
import xjs.core.JsonObject;
import xjs.core.JsonString;
import xjs.core.JsonValue;
import xjs.core.StringType;
import xjs.exception.SyntaxException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

/**
 * A parser providing compatibility with Hjson files.
 */
public class LegacyHjsonParser extends LegacyXjsParser {

    public LegacyHjsonParser(final File file) throws IOException {
        super(file);
    }

    public LegacyHjsonParser(final Reader reader) throws IOException {
        super(reader);
    }

    public LegacyHjsonParser(final String text) {
        super(text);
    }

    @Override
    public @NotNull JsonValue parse() throws IOException {
        if (this.text.isEmpty()) {
            return new JsonObject();
        } else if (this.isOpenRoot()) {
            return this.readOpenObject();
        }
        return this.readClosedRoot();
    }

    protected boolean isOpenRoot() throws IOException {
        this.read();
        this.skipWhitespace();
        final int s = this.index - 1;
        this.reset();
        return this.scanForKey(s);
    }

    protected boolean scanForKey(final int s) {
        final char first = this.text.charAt(s);
        if (first == '[' || first == '{') {
            return false;
        } else if (first == ':') {
            throw this.emptyKey();
        } else if (first == '}' || first == ']' || first == ',') {
            throw this.punctuationInKey(first);
        }
        for (int i = s + 1; i < this.text.length(); i++) {
            final char c = this.text.charAt(i);
            if (c == ':') {
                return true;
            } else if (Character.isWhitespace(c)) {
                return this.expectColon(i);
            }
        }
        return false;
    }

    protected boolean expectColon(final int s) {
        for (int i = s; i < this.text.length(); i++) {
            final char c = this.text.charAt(i);
            if (c == ':') {
                return true;
            } else if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return false;
    }

    @Override
    protected String readKey() throws IOException {
        switch (this.text.charAt(this.index - 1)) {
            case '\'': return this.readQuoted('\'');
            case '"': return this.readQuoted('"');
        }
        return this.readUnquotedKey();
    }

    protected String readUnquotedKey() {
        final int s = this.index - 1;
        while (this.current != -1) {
            if (this.current == ':' || this.isWhitespace()) {
                if (s == this.index - 1) {
                    throw this.emptyKey();
                }
                final String key = this.text.substring(s, this.index - 1);
                this.validatePostKey();
                return key;
            } else if (this.isPunctuationChar((char) this.current)) {
                throw this.punctuationInKey((char) this.current);
            }
            this.read();
        }
        throw this.unexpectedEnd();
    }

    protected void validatePostKey() {
        while (Character.isWhitespace(this.current)) {
            this.read();
            if (this.current == -1) {
                throw this.unexpectedEnd();
            }
        }
        if (this.current != ':') {
            // Hjson does not even allow comments here
            throw this.whitespaceInKey();
        }
    }

    @Override
    protected JsonValue readImplicit() throws IOException {
        switch (this.current) {
            case '"':
                return new JsonString(this.readQuoted('"'), StringType.DOUBLE);
            case '\'':
                if (this.peek(1) == '\'' && this.peek(2) == '\'') {
                    return this.readMulti();
                }
                return new JsonString(this.readQuoted('\''), StringType.SINGLE);
        }
        if (this.isPunctuationChar((char) this.current)) {
            throw this.punctuationInValue((char) this.current);
        }
        final int s = this.index - 1;
        while (this.current != -1 && !this.isEndOfWord()) {
            this.read();
        }
        final String word = this.text.substring(s, this.index - 1).trim();
        final JsonValue literal = this.tryParseValue(word);
        if (literal != null) {
            return literal;
        }
        while (this.current != '\n' && this.current != -1) {
            this.read();
        }
        final String value = this.text.substring(s, this.index - 1).trim();
        return new JsonString(value, StringType.IMPLICIT);
    }

    protected void reset() {
        this.index = this.line = this.linesSkipped = this.lineOffset = 0;
    }

    protected boolean isEndOfWord() {
        final char c = (char) this.current;
        if (c == '/') {
            final int peek = this.peek();
            return peek == '/' || peek == '*';
        }
        return c == '#' || Character.isWhitespace(c) || this.isPunctuationChar(c);
    }

    protected boolean isPunctuationChar(final char c) {
        return c == '{' || c == '}' || c == '[' || c == ']' || c == ',' || c == ':';
    }

    protected SyntaxException emptyKey() {
        return this.expected("key (for an empty key name use quotes)");
    }

    protected SyntaxException whitespaceInKey() {
        return this.unexpected("whitespace in key (use quotes to include)");
    }

    protected SyntaxException punctuationInKey(final char c) {
        return this.unexpected("punctuation ('" + c + "') in key (use quotes to include)");
    }

    protected SyntaxException punctuationInValue(final char c) {
        return this.unexpected("punctuation ('" + c + "') in value (use quotes to include)");
    }

    protected SyntaxException unexpectedEnd() {
        return this.unexpected("end of input");
    }
}
