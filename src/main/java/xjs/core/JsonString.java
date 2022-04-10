package xjs.core;

public class JsonString extends JsonValue {

    private final String value;
    private final StringType type;

    public JsonString(final String value) {
        this(value, StringType.NONE);
    }

    public JsonString(final String value, final StringType type) {
        this.value = value;
        this.type = type;
    }

    public static JsonString auto(final String value) {
        return new JsonString(value, StringType.fast(value));
    }

    public StringType getStringType() {
        return this.type;
    }

    @Override
    public JsonType getType() {
        return JsonType.STRING;
    }

    @Override
    public String unwrap() {
        return this.value;
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
    public double intoDouble() {
        return this.value.length();
    }

    @Override
    public int hashCode() {
        return this.value.hashCode() + 31 * this.type.hashCode();
    }

    @Override
    public boolean matches(final JsonValue other) {
        if (other instanceof JsonString) {
            return this.value.equals(((JsonString) other).value);
        }
        return false;
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof JsonString) {
            final JsonString s = (JsonString) other;
            return this.value.equals(s.value) && this.type.equals(s.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.value;
    }

}
