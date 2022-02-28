package xjs.core;

public class JsonDecimal extends JsonValue {

    private final double value;

    public JsonDecimal(final double value) {
        this.value = value;
    }

    @Override
    public JsonType getType() {
        return JsonType.DECIMAL;
    }

    @Override
    public boolean isDecimal() {
        return true;
    }

    @Override
    public Number asNumber() {
        return this.value;
    }

    @Override
    public long asLong() {
        return (long) this.value;
    }

    @Override
    public int asInt() {
        return (int) this.value;
    }

    @Override
    public double asDouble() {
        return this.value;
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
        return (long) this.value;
    }

    @Override
    public int intoInt() {
        return (int) this.value;
    }

    @Override
    public double intoDouble() {
        return this.value;
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
    public JsonDecimal deepCopy(final boolean trackAccess) {
        return new JsonDecimal(this.value);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Double.hashCode(this.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof JsonDecimal) {
            return this.value == ((JsonDecimal) o).value
                && this.metadataEquals((JsonDecimal) o);
        }
        if (o instanceof JsonInteger) {
            return this.value == ((JsonInteger) o).asDouble()
                && this.metadataEquals((JsonInteger) o);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }
}
