package xjs.core;

public class JsonString extends JsonValue {

    private final String value;
    private final Type type;

    public JsonString(final String value) {
        this(value, Type.NONE);
    }

    public JsonString(final String value, final Type type) {
        this.value = value;
        this.type = type;
    }

    public Type getStringType() {
        return this.type;
    }

    @Override
    public JsonType getType() {
        return JsonType.STRING;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public boolean isString() {
        return true;
    }

    @Override
    public String asString() {
        return this.value;
    }

    @Override
    public Number intoNumber() {
        return this.value.length();
    }

    @Override
    public long intoLong() {
        return this.value.length();
    }

    @Override
    public int intoInt() {
        return this.value.length();
    }

    @Override
    public double intoDouble() {
        return this.value.length();
    }

    @Override
    public float intoFloat() {
        return this.value.length();
    }

    @Override
    public boolean intoBoolean() {
        return this.value.length() > 0;
    }

    @Override
    public String intoString() {
        return this.value;
    }

    @Override
    public JsonContainer intoContainer() {
        return this.intoArray();
    }

    @Override
    public JsonObject intoObject() {
        return new JsonObject().add("value", this);
    }

    @Override
    public JsonArray intoArray() {
        return new JsonArray().add(this);
    }

    @Override
    public JsonString deepCopy(final boolean trackAccess) {
        return new JsonString(this.value, this.type);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + this.value.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof JsonString) {
            return this.value.equals(((JsonString) o).value)
                && this.metadataEquals((JsonString) o);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public enum Type {
        SINGLE,
        DOUBLE,
        MULTI,
        IMPLICIT,
        NONE
    }
}
