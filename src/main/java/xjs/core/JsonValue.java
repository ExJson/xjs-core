package xjs.core;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;
import xjs.serialization.writer.JsonWriter;
import xjs.serialization.writer.XjsWriter;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;

public abstract class JsonValue implements Serializable {

    protected int linesAbove;
    protected int linesBetween;
    protected int flags;
    protected CommentHolder comments;

    protected JsonValue() {
        this.linesAbove = -1;
        this.linesBetween = -1;
        this.flags = JsonFlags.NULL;
        this.comments = null;
    }

    public static JsonNumber valueOf(final long value) {
        return new JsonNumber(value);
    }

    public static JsonNumber valueOf(final double value) {
        return new JsonNumber(value);
    }

    public static JsonLiteral valueOf(final boolean value) {
        return value ? JsonLiteral.jsonTrue() : JsonLiteral.jsonFalse();
    }

    public static JsonValue valueOf(final @Nullable String value) {
        return value != null ? JsonString.auto(value) : JsonLiteral.jsonNull();
    }

    public static JsonValue nonnull(final @Nullable JsonValue value) {
        if (value == null) {
            return JsonLiteral.jsonNull();
        }
        return value;
    }

    public int getLinesAbove() {
        return this.linesAbove;
    }

    public JsonValue setLinesAbove(final int linesAbove) {
        this.linesAbove = linesAbove;
        return this;
    }

    public int getLinesBetween() {
        return this.linesBetween;
    }

    public JsonValue setLinesBetween(final int linesBetween) {
        this.linesBetween = linesBetween;
        return this;
    }

    @MagicConstant(flagsFromClass = JsonFlags.class)
    public int getFlags() {
        return this.flags;
    }

    public JsonValue setFlags(
            final @MagicConstant(flagsFromClass = JsonFlags.class) int flags) {
        this.flags = flags;
        return this;
    }

    public boolean hasFlag(
            final @MagicConstant(flagsFromClass = JsonFlags.class) int flag) {
        return (this.flags & flag) == flag;
    }

    public JsonValue addFlag(
            final @MagicConstant(flagsFromClass = JsonFlags.class) int flag) {
        this.flags &= ~JsonFlags.NULL;
        this.flags |= flag;
        return this;
    }

    public JsonValue removeFlag(
            final @MagicConstant(flagsFromClass = JsonFlags.class) int flag) {
        this.flags &= ~flag;
        return this;
    }

    public CommentHolder getComments() {
        if (this.comments == null) {
            return this.comments = new CommentHolder();
        }
        return this.comments;
    }

    public JsonValue setComments(final CommentHolder comments) {
        this.comments = comments;
        return this;
    }

    public boolean hasComments() {
        return this.comments != null && this.comments.hasAny();
    }

    public boolean hasComment(final CommentType type) {
        return this.comments != null && this.comments.has(type);
    }

    public JsonValue setComment(final String text) {
        return this.setComment(CommentType.HEADER, CommentStyle.LINE, text);
    }

    public JsonValue setComment(final CommentType type, final CommentStyle style, final String text) {
        this.getComments().set(type, style, text);
        return this;
    }

    public JsonValue setComment(final CommentType type, final CommentStyle style, final String text, final int lines) {
        this.getComments().set(type, style, text, lines);
        return this;
    }

    public String getComment(final CommentType type) {
        return this.getComments().get(type);
    }

    public JsonValue setDefaultMetadata(final JsonValue metadata) {
        if (this.linesAbove < 0) this.linesAbove = metadata.linesAbove;
        if (this.linesBetween < 0) this.linesBetween = metadata.linesBetween;
        if (this.hasFlag(JsonFlags.NULL)) this.flags = metadata.flags;
        if (this.comments == null) this.comments = metadata.comments;
        return this;
    }

    public abstract JsonType getType();

    public boolean isPrimitive() {
        return false;
    }

    public boolean isNumber() {
        return false;
    }

    public boolean isInteger() {
        return false;
    }

    public boolean isDecimal() {
        return false;
    }

    public boolean isBoolean() {
        return false;
    }

    public boolean isTrue() {
        return false;
    }

    public boolean isFalse() {
        return false;
    }

    public boolean isString() {
        return false;
    }

    public boolean isContainer() {
        return false;
    }

    public boolean isObject() {
        return false;
    }

    public boolean isArray() {
        return false;
    }

    public boolean isNull() {
        return false;
    }

    public boolean isReference() {
        return false;
    }

    public Number asNumber() {
        throw new UnsupportedOperationException();
    }

    public long asLong() {
        throw new UnsupportedOperationException();
    }

    public int asInt() {
        throw new UnsupportedOperationException();
    }

    public double asDouble() {
        throw new UnsupportedOperationException();
    }

    public float asFloat() {
        throw new UnsupportedOperationException();
    }

    public boolean asBoolean() {
        throw new UnsupportedOperationException();
    }

    public String asString() {
        throw new UnsupportedOperationException();
    }

    public JsonContainer asContainer() {
        throw new UnsupportedOperationException();
    }

    public JsonObject asObject() {
        throw new UnsupportedOperationException();
    }

    public JsonArray asArray() {
        throw new UnsupportedOperationException();
    }

    public abstract Number intoNumber();

    public abstract long intoLong();

    public abstract int intoInt();

    public abstract double intoDouble();

    public abstract float intoFloat();

    public abstract boolean intoBoolean();

    public abstract String intoString();

    public abstract JsonContainer intoContainer();

    public abstract JsonObject intoObject();

    public abstract JsonArray intoArray();

    public JsonValue shallowCopy() {
        return this.deepCopy(false);
    }

    public JsonValue deepCopy() {
        return this.deepCopy(false);
    }

    public abstract JsonValue deepCopy(final boolean trackAccess);

    public abstract JsonValue unformatted();

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + this.linesAbove;
        result = 31 * result + this.linesBetween;
        result = 31 * result + this.flags;

        if (this.comments != null) {
            result = 31 * result + this.comments.hashCode();
        }
        return result;
    }

    protected boolean metadataEquals(final JsonValue other) {
        return this.linesAbove == other.linesAbove
            && this.linesBetween == other.linesBetween
            && this.flags == other.flags
            && Objects.equals(this.comments, other.comments);
    }

    @Override
    public String toString() {
        return this.toString(JsonFormat.JSON);
    }

    public String toString(final JsonFormat format) {
        final StringWriter sw = new StringWriter();
        try {
            switch (format) {
                case JSON:
                    new JsonWriter(sw, false).write(this);
                    break;
                case JSON_FORMATTED:
                    new JsonWriter(sw, true).write(this);
                    break;
                case XJS:
                    new XjsWriter(sw, false).write(this);
                    break;
                case XJS_FORMATTED:
                    new XjsWriter(sw, true).write(this);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Encoding error", e);
        }
        return sw.toString();
    }
}
