package xjs.core;

public class JsonNull extends JsonValue {

    public JsonNull() {}

    public static JsonNull instance() {
        return new JsonNull();
    }

    @Override
    public JsonType getType() {
        return JsonType.NULL;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public Number intoNumber() {
        return 0;
    }

    @Override
    public long intoLong() {
        return 0L;
    }

    @Override
    public int intoInt() {
        return 0;
    }

    @Override
    public double intoDouble() {
        return 0.0;
    }

    @Override
    public float intoFloat() {
        return 0.0F;
    }

    @Override
    public boolean intoBoolean() {
        return false;
    }

    @Override
    public String intoString() {
        return "null";
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
    public JsonNull deepCopy(boolean trackAccess) {
        return new JsonNull();
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public int hashCode() {
        return 17 * super.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof JsonNull) {
            return super.metadataEquals((JsonNull) o);
        }
        return false;
    }
}
