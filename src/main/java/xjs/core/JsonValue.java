package xjs.core;

import org.jetbrains.annotations.ApiStatus;
import xjs.serialization.JsonSerializationContext;
import xjs.serialization.writer.JsonWriter;
import xjs.serialization.writer.JsonWriterOptions;
import xjs.serialization.writer.XjsWriter;

import java.io.*;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a JSON value. This may be a primitive value, such as a
 *
 * <ul>
 *   <li>{@link JsonNumber JSON number}, or a</li>
 *   <li>{@link JsonString JSON string}</li>
 * </ul>
 *
 * <p>or one of the {@link JsonLiteral literals}:
 *
 * <ul>
 *   <li><code>true</code></li>
 *   <li><code>false</code>, or</li>
 *   <li><code>null</code></li>
 * </ul>
 *
 * <p>or even a type of {@link JsonContainer container}, such as a
 *
 * <ul>
 *   <li>{@link JsonArray JSON array}, or a</li>
 *   <li>{@link JsonObject JSON object}</li>
 * </ul>
 *
 * <p>Callers should be aware that this type hierarchy is intentionally
 * open to extenders and foreign implementors. This is primarily intended
 * to facilitate JEL expressions. For this reason, callers should avoid
 * using <code>instanceof</code> checks and instead prefer a method such
 * as:
 *
 * <ul>
 *   <li>{@link #isArray}</li>
 *   <li>{@link #isNumber}</li>
 *   <li>etc</li>
 * </ul>
 *
 * <p>Likewise, to <em>cast</em> this value into some other type, callers
 * may use an <code>as</code> method, such as:
 *
 * <ul>
 *   <li>{@link #asArray}</li>
 *   <li>{@link #asDouble}</li>
 *   <li>etc</li>
 * </ul>
 *
 * <p>Finally, callers may optionally <em>convert</em> values between types
 * using the <code>into</code> pattern:
 *
 * <ul>
 *   <li>{@link #intoArray}</li>
 *   <li>{@link #intoDouble}</li>
 *   <li>etc</li>
 * </ul>
 *
 * <p>To get started using the provided type hierarchy for this ecosystem,
 * callers should defer to the factory methods in {@link Json} when possible.
 */
public abstract class JsonValue implements Serializable {

    /**
     * Gets the type of being represented by this wrapper.
     *
     * @return A {@link JsonType}, e.g. a string, number, etc.
     */
    public abstract JsonType getType();

    /**
     * "Unwraps" this value by returning e.g. a raw {@link Number},
     * {@link Map}, etc.
     *
     * @return The "Java" counterpart to this wrapper.
     */
    public abstract Object unwrap();

    /**
     * Indicates whether this value represents some simple type, such as a
     *
     * <ul>
     *   <li>String</li>
     *   <li>Number</li>
     *   <li>Boolean</li>
     *   <li>null</li>
     * </ul>
     *
     * @return Whether this value represents one of the above types.
     */
    public boolean isPrimitive() {
        return true;
    }

    /**
     * Indicates whether this value represents a number.
     *
     * @return <code>true</code>, if this value is a number.
     */
    public boolean isNumber() {
        return false;
    }

    /**
     * Indicates whether this value represents a boolean value.
     *
     * @return <code>true</code>, if this value is a boolean value.
     */
    public boolean isBoolean() {
        return false;
    }

    /**
     * Indicates whether this value represents the literal <code>true</code>.
     *
     * @return <code>true</code>, if this value is <code>true</code>.
     */
    public boolean isTrue() {
        return false;
    }

    /**
     * Indicates whether this value represents the literal <code>false</code>.
     *
     * @return <code>true</code>, if this value is <code>false</code>.
     */
    public boolean isFalse() {
        return false;
    }

    /**
     * Indicates whether this value represents a string value.
     *
     * @return <code>true</code>, if this value is a string value.
     */
    public boolean isString() {
        return false;
    }

    /**
     * Indicates whether this value is additionally a container of many values,
     * such as an {@link JsonArray array} or {@link JsonObject object}.
     *
     * @return <code>true</code>, if this value is a container.
     */
    public boolean isContainer() {
        return false;
    }

    /**
     * Indicates whether this value <em>represents</em> a {@link JsonObject}.
     *
     * @return <code>true</code>, if this value can be treated as an object.
     */
    public boolean isObject() {
        return false;
    }

    /**
     * Indicates whether this value <em>represents</em> a {@link JsonArray}.
     *
     * @return <code>true</code>, if this value can be treated as an array.
     */
    public boolean isArray() {
        return false;
    }

    /**
     * Indicates whether this value represents the literal <code>null</code>.
     *
     * @return <code>true</code>, if this value is <code>null</code>.
     */
    public boolean isNull() {
        return false;
    }

    /**
     * Returns the long value being represented by this wrapper.
     *
     * @return The number being wrapped.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a number.
     */
    public long asLong() {
        throw new UnsupportedOperationException("Not a long: " + this);
    }

    /**
     * Returns the int value being represented by this wrapper.
     *
     * @return The number being wrapped.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a number.
     */
    public int asInt() {
        throw new UnsupportedOperationException("Not an int: " + this);
    }

    /**
     * Returns the double value being represented by this wrapper.
     *
     * @return The number being wrapped.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a number.
     */
    public double asDouble() {
        throw new UnsupportedOperationException("Not a double: " + this);
    }

    /**
     * Returns the float value being represented by this wrapper.
     *
     * @return The number being wrapped.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a number.
     */
    public float asFloat() {
        throw new UnsupportedOperationException("Not a float: " + this);
    }

    /**
     * Returns the boolean value being represented by this wrapper.
     *
     * @return The boolean value being wrapped.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a boolean value.
     */
    public boolean asBoolean() {
        throw new UnsupportedOperationException("Not a boolean: " + this);
    }

    /**
     * Returns the string value being represented by this wrapper.
     *
     * @return The string value being wrapped.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a string.
     */
    public String asString() {
        throw new UnsupportedOperationException("Not a string: " + this);
    }

    /**
     * Gets this value as a {@link JsonContainer container}, if applicable.
     *
     * @return This value as a {@link JsonContainer container}.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a container.
     */
    public JsonContainer asContainer() {
        throw new UnsupportedOperationException("Not a container: " + this);
    }

    /**
     * Gets this value as an {@link JsonObject object}, if applicable.
     *
     * @return This value as an {@link JsonObject object}.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a {@link JsonObject}.
     */
    public JsonObject asObject() {
        throw new UnsupportedOperationException("Not an object: " + this);
    }

    /**
     * Gets this value as an {@link JsonArray array}, if applicable.
     *
     * @return This value as an {@link JsonArray array}.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a {@link JsonArray}.
     */
    public JsonArray asArray() {
        throw new UnsupportedOperationException("Not an array: " + this);
    }

    /**
     * Filters data out of this wrapper using a filter expression.
     *
     * <p>For example, to acquire this value as a string (or else empty):
     *
     * <pre>{@code
     *   final Optional<String> s = value.filter(JsonFilter.STRING)
     * }</pre>
     *
     * @param filter An expression for filtering data out of this wrapper.
     * @param <T>    The type of data being filtered out.
     * @return The filtered data, or else {@link Optional#empty}.
     * @see JsonFilter
     */
    @ApiStatus.Experimental
    public <T> Optional<T> filter(final JsonFilter<T> filter) {
        return filter.applyOptional(this);
    }

    /**
     * Coerces this value in to a long, even if it is not a number.
     *
     * @return This value as a number.
     * @apiNote Experimental - The exact implementation of this value may
     *          change slightly.
     */
    @ApiStatus.Experimental
    public long intoLong() {
        return (long) this.intoDouble();
    }

    /**
     * Coerces this value in to an int, even if it is not a number.
     *
     * @return This value as a number.
     * @apiNote Experimental - The exact implementation of this value may
     *          change slightly.
     */
    @ApiStatus.Experimental
    public int intoInt() {
        return (int) this.intoDouble();
    }

    /**
     * Coerces this value in to a double, even if it is not a number.
     *
     * @return This value as a number.
     * @apiNote Experimental - The exact implementation of this value may
     *          change slightly.
     */
    @ApiStatus.Experimental
    public abstract double intoDouble();

    /**
     * Coerces this value in to a float, even if it is not a number.
     *
     * @return This value as a number.
     * @apiNote Experimental - The exact implementation of this value may
     *          change slightly.
     */
    @ApiStatus.Experimental
    public float intoFloat() {
        return (float) this.intoDouble();
    }

    /**
     * Coerces this value in to a boolean, even if it is not a boolean value.
     *
     * @return This value as a boolean value.
     * @apiNote Experimental - The exact implementation of this value may
     *          change slightly.
     */
    @ApiStatus.Experimental
    public boolean intoBoolean() {
        return this.intoDouble() != 0;
    }

    /**
     * Coerces this value in to a string, even if it is not a string value.
     *
     * @return This value as a string.
     * @apiNote Experimental - The exact implementation of this value may
     *          change slightly.
     */
    @ApiStatus.Experimental
    public String intoString() {
        return this.toString();
    }

    /**
     * Coerces this value into a container (usually an array), even if it is
     * not a container.
     *
     * @return This value as a {@link JsonContainer}.
     */
    public JsonContainer intoContainer() {
        return this.intoArray();
    }

    /**
     * Coerces this value into a JSON object, even if it is not an object.
     *
     * @return This value as a {@link JsonObject}.
     */
    public JsonObject intoObject() {
        return new JsonObject().add("value", this);
    }

    /**
     * Coerces this value into a JSON array, even if it is not an array.
     *
     * @return This value as a {@link JsonArray}.
     */
    public JsonArray intoArray() {
        return new JsonArray().add(this);
    }

    /**
     * Generates a {@link JsonReference} from this type, allowing it to be
     * formatted correctly before reprinting.
     */
    public JsonReference intoReference() {
        return new JsonReference(this);
    }

    /**
     * Indicates whether the given data wraps the same <em>value</em>, thus
     * ignoring its metadata.
     *
     * @param other The value being compared to.
     * @return <code>true</code>, if the two values contain the same primary data.
     */
    public boolean matches(final JsonValue other) {
        return this.equals(other);
    }

    /**
     * Converts this value into a string. For most values, this means
     * printing it as a regular, unformatted. JSON string.
     *
     * @return This value in string format.
     */
    @Override
    public String toString() {
        return this.toString(JsonFormat.JSON);
    }

    /**
     * Converts this value into a string in the given format.
     *
     * @param format The expected format in which to write this value.
     * @return This value as a JSON string, formatted or otherwise.
     */
    public String toString(final JsonFormat format) {
        final StringWriter sw = new StringWriter();
        try {
            switch (format) {
                case JSON:
                    new JsonWriter(sw, false).write(this);
                    break;
                case JSON_FORMATTED:
                    new JsonWriter(sw, true).write(this);
                    break;
                case XJS:
                    new XjsWriter(sw, false).write(this);
                    break;
                case XJS_FORMATTED:
                    new XjsWriter(sw, true).write(this);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Encoding error", e);
        }
        return sw.toString();
    }

    /**
     * Converts this value to a formatted XJS string using the given options.
     *
     * @param options Formatting options indicating how to output this value.
     * @return This value as a formatted XJS string.
     */
    public String toString(final JsonWriterOptions options) {
        final StringWriter sw = new StringWriter();
        try {
            new XjsWriter(sw, options).write(this);
        } catch (final IOException e) {
            throw new UncheckedIOException("Encoding error", e);
        }
        return sw.toString();
    }
}
