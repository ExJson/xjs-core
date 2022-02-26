package xjs.core;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public abstract class JsonValue implements Serializable {

    protected JsonValue() {}

    public static JsonInteger valueOf(final long value) {
        return new JsonInteger(value);
    }

    public static JsonDecimal valueOf(final double value) {
        return new JsonDecimal(value);
    }

    public static JsonBoolean valueOf(final boolean value) {
        return JsonBoolean.get(value);
    }

    public static JsonValue valueOf(final @Nullable String value) {
        return value != null ? new JsonString(value) : JsonNull.instance();
    }

    public static JsonValue nonnull(final @Nullable JsonValue value) {
        if (value == null) {
            return JsonNull.instance();
        }
        return value;
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

}
