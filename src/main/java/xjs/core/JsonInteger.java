package xjs.core;

public class JsonInteger extends JsonValue {

    private final long value;

    public JsonInteger(final long value) {
        this.value = value;
    }

    @Override
    public JsonType getType() {
        return JsonType.INTEGER;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public boolean isInteger() {
        return true;
    }

    @Override
    public Number asNumber() {
        return this.value;
    }

    @Override
    public long asLong() {
        return this.value;
    }

    @Override
    public int asInt() {
        return (int) this.value;
    }

    @Override
    public double asDouble() {
        return (double) this.value;
    }

    @Override
    public float asFloat() {
        return (float) this.value;
    }

    @Override
    public Number intoNumber() {
        return this.value;
    }

    @Override
    public long intoLong() {
        return this.value;
    }

    @Override
    public int intoInt() {
        return (int) this.value;
    }

    @Override
    public double intoDouble() {
        return (double) this.value;
    }

    @Override
    public float intoFloat() {
        return (float) this.value;
    }

    @Override
    public boolean intoBoolean() {
        return this.value != 0;
    }

    @Override
    public String intoString() {
        return String.valueOf(this.value);
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
    public JsonInteger deepCopy(final boolean trackAccess) {
        return (JsonInteger) new JsonInteger(this.value).setDefaultMetadata(this);
    }

    @Override
    public JsonInteger unformatted() {
        return new JsonInteger(this.value);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Long.hashCode(this.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof JsonInteger) {
            return this.value == ((JsonInteger) o).value
                && this.metadataEquals((JsonInteger) o);
        }
        if (o instanceof JsonDecimal) {
            return this.value == ((JsonDecimal) o).asLong()
                && this.metadataEquals((JsonDecimal) o);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }
}
