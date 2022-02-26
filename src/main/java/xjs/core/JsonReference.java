package xjs.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

public class JsonReference extends JsonValue {

    protected JsonValue referent;
    protected boolean accessed;
    protected int emptyLines;
    protected boolean forceNewLine;
    protected int flags;

    public JsonReference(final @Nullable JsonValue referent) {
        this.referent = JsonValue.nonnull(referent);
    }

    public static JsonValue unwrap(@Nullable JsonValue referent) {
        while (referent instanceof JsonReference) {
            referent = ((JsonReference) referent).get();
        }
        return JsonValue.nonnull(referent);
    }

    public @NotNull JsonValue get() {
        this.accessed = true;
        return this.referent;
    }

    public JsonReference set(final @Nullable JsonValue referent) {
        this.referent = JsonValue.nonnull(referent);
        this.accessed = true;
        return this;
    }

    public JsonReference update(final UnaryOperator<JsonValue> updater) {
        return this.set(updater.apply(this.referent));
    }

    public JsonValue visit() {
        return this.referent;
    }

    public JsonReference mutate(final @Nullable JsonValue referent) {
        this.referent = JsonValue.nonnull(referent);
        return this;
    }

    public JsonReference apply(final UnaryOperator<JsonValue> updater) {
        return this.mutate(updater.apply(this.referent));
    }

    public boolean isAccessed() {
        return this.accessed;
    }

    public JsonReference setAccessed(final boolean accessed) {
        this.accessed = accessed;
        return this;
    }

    public int getEmptyLines() {
        return this.emptyLines;
    }

    public JsonReference setEmptyLines(final int emptyLines) {
        this.emptyLines = emptyLines;
        return this;
    }

    public boolean isForceNewLine() {
        return this.forceNewLine;
    }

    public JsonReference setForceNewLine(final boolean forceNewLine) {
        this.forceNewLine = forceNewLine;
        return this;
    }

    public int getFlags() {
        return this.flags;
    }

    public JsonReference setFlags(final int flags) {
        this.flags = flags;
        return this;
    }

    public boolean hasFlag(final int flag) {
        return (this.flags & flag) == flag;
    }

    public JsonReference addFlag(final int flag) {
        this.flags |= flag;
        return this;
    }

    public JsonReference removeFlag(final int flag) {
        this.flags &= ~flag;
        return this;
    }

    @Override
    public JsonType getType() {
        return this.get().getType();
    }

    @Override
    public boolean isPrimitive() {
        return this.get().isPrimitive();
    }

    @Override
    public boolean isNumber() {
        return this.get().isNumber();
    }

    @Override
    public boolean isInteger() {
        return this.get().isInteger();
    }

    @Override
    public boolean isDecimal() {
        return this.get().isDecimal();
    }

    @Override
    public boolean isBoolean() {
        return this.get().isBoolean();
    }

    @Override
    public boolean isTrue() {
        return this.get().isTrue();
    }

    @Override
    public boolean isFalse() {
        return this.get().isFalse();
    }

    @Override
    public boolean isString() {
        return this.get().isString();
    }

    @Override
    public boolean isContainer() {
        return this.get().isContainer();
    }

    @Override
    public boolean isObject() {
        return this.get().isObject();
    }

    @Override
    public boolean isArray() {
        return this.get().isArray();
    }

    @Override
    public boolean isNull() {
        return this.get().isNull();
    }

    @Override
    public boolean isReference() {
        return this.get().isReference();
    }

    @Override
    public Number asNumber() {
        return this.get().asNumber();
    }

    @Override
    public long asLong() {
        return this.get().asLong();
    }

    @Override
    public int asInt() {
        return this.get().asInt();
    }

    @Override
    public double asDouble() {
        return this.get().asDouble();
    }

    @Override
    public float asFloat() {
        return this.get().asFloat();
    }

    @Override
    public boolean asBoolean() {
        return this.get().asBoolean();
    }

    @Override
    public String asString() {
        return this.get().asString();
    }

    @Override
    public JsonContainer asContainer() {
        return this.get().asContainer();
    }

    @Override
    public JsonObject asObject() {
        return this.get().asObject();
    }

    @Override
    public JsonArray asArray() {
        return this.get().asArray();
    }

    @Override
    public Number intoNumber() {
        return this.get().intoNumber();
    }

    @Override
    public long intoLong() {
        return this.get().intoLong();
    }

    @Override
    public int intoInt() {
        return this.get().intoInt();
    }

    @Override
    public double intoDouble() {
        return this.get().intoDouble();
    }

    @Override
    public float intoFloat() {
        return this.get().intoFloat();
    }

    @Override
    public boolean intoBoolean() {
        return this.get().intoBoolean();
    }

    @Override
    public String intoString() {
        return this.get().intoString();
    }

    @Override
    public JsonContainer intoContainer() {
        return this.get().intoContainer();
    }

    @Override
    public JsonObject intoObject() {
        return this.get().intoObject();
    }

    @Override
    public JsonArray intoArray() {
        return this.get().intoArray();
    }

    public JsonReference clone(final boolean trackAccess) {
        final JsonReference clone = new JsonReference(this.referent).setEmptyLines(this.emptyLines);
        return trackAccess ? clone.setAccessed(this.accessed) : clone;
    }

    @Override
    public int hashCode() {
        return this.referent.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof JsonReference) {
            final JsonReference other = (JsonReference) o;
            return this.referent.equals(other.referent)
                && this.emptyLines == other.emptyLines;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.referent.toString();
    }
}
