package xjs.core;

public class JsonNull extends JsonValue {

    private static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {}

    public static JsonNull instance() {
        return INSTANCE;
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
    public String toString() {
        return "null";
    }
}
