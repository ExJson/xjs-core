package xjs.core;

public class JsonNumber extends JsonValue {

    private final double value;

    public JsonNumber(final double value) {
        this.value = value;
    }

    @Override
    public JsonType getType() {
        return JsonType.NUMBER;
    }

    @Override
    public Number unwrap() {
        return this.value;
    }

    @Override
    public boolean isNumber() {
        return true;
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
    public double intoDouble() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(this.value);
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof JsonNumber) {
            return this.value == ((JsonNumber) other).value;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }
}
