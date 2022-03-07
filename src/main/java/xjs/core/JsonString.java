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

}
