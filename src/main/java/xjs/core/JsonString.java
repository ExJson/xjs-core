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
        return this.value.hashCode();
    }

    @Override
    public JsonString deepCopy(final boolean trackAccess) {
        return (JsonString) new JsonString(this.value, this.type).setDefaultMetadata(this);
    }

    @Override
    public JsonString unformatted() {
        return new JsonString(this.value, this.type);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + this.value.hashCode();
    }

    @Override
    public boolean matches(final JsonValue other) {
        if (other instanceof JsonString) {
            return this.value.equals(((JsonString) other).value);
        }
        return false;
    }

    @Override
    protected boolean matchesMetadata(final JsonValue other) {
        if (other instanceof JsonString) {
            return this.type.equals(((JsonString) other).type)
                && super.matchesMetadata(other);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.value;
    }

}
