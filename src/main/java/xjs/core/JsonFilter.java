package xjs.core;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The JSON filter is an experimental data validation API intended for extracting very
 * basic data from {@link JsonValue JSON values}. Essentially, its purpose is to
 *
 * <ol>
 *   <li>Inspect a value</li>
 *   <li>If needed, validate it</li>
 *   <li>Extract data from it</li>
 * </ol>
 *
 * <p>This API builds on the "as / is" pattern outlined in the main {@link JsonValue}
 * specifications. For example:
 *
 * <pre>{@code
 *   // Reads text from a value if the value is a string
 *   final String s =
 *     value.isString() ? value.asString() : null;
 * }</pre>
 *
 * <p>Essentially, a filter can be used as a functional replacement in the following way:
 *
 * <pre>{@code
 *   final Optional<String> s =
 *     value.filter(JsonFilter.STRING);
 * }</pre>
 *
 * <p>The "as / is" pattern does not support extracting unique data from {@link JsonValue}
 * child classes. For example, to acquire a {@link StringType} when given a JSON value,
 * regular <code>instanceof</code> checks are required, thus breaking the usual paradigm:
 *
 * <pre>{@code
 *   final StringType type =
 *     value instanceof JsonString
 *       ? ((JsonString) value).getStringType)
 *       : null;
 * }</pre>
 *
 * <p>The JSON filter is capable of resolving this issue, as follows:
 *
 * <pre>{@code
 *   final Optional<StringType> type =
 *     type.filter(JsonFilter.STRING_TYPE);
 * }</pre>
 *
 * <p>Finally, JSON filters may be combined and operated on to form simple validations:
 *
 * <pre>{@code
 *   static final JsonFilter<JsonObject> ENABLED_OBJECT =
 *     OBJECT.given(BOOLEAN.forKey("enabled").is(true));
 *
 *   static JsonObject getObject(final JsonValue value) {
 *     return value.filter(ENABLED_OBJECT)
 *       .orElseThrow(AssertionError::new);
 *   }
 * }</pre>
 *
 * <p><b>Note</b>: callers should be aware that this API is highly experimental. It has the
 * unfortunate side effect of destroying invalid data, meaning there's no way to tell <em>why
 * </em> a filter returns no value. Currently, it forms a bit of a band-aid which will either
 * be removed or hopefully replaced in a future pre-release.
 *
 * @param <T> The return type of the filter function. In other words, its <em>yield</em>.
 * @apiNote Experimental - may get removed or be implemented differently in a future
 *          release.
 */
@FunctionalInterface
@ApiStatus.Experimental
public interface JsonFilter<T> extends Function<JsonValue, T> {

    /**
     * Primitive filter used for casting {@link JsonValue values} into strings.
     */
    JsonFilter<String> STRING = v -> v.isString() ? v.asString() : null;

    /**
     * Primitive filter used for casting {@link JsonValue values} into numbers.
     */
    JsonFilter<Number> NUMBER = v -> v.isNumber() ? v.asDouble() : null;

    /**
     * Primitive filter used for casting {@link JsonValue values} into booleans.
     */
    JsonFilter<Boolean> BOOLEAN = v -> v.isBoolean() ? v.asBoolean() : null;

    /**
     * Primitive filter used for casting {@link JsonValue values} into {@link JsonContainer containers}.
     */
    JsonFilter<JsonContainer> CONTAINER = v -> v.isContainer() ? v.asContainer() : null;

    /**
     * Primitive filter used for casting {@link JsonValue values} into {@link JsonObject objects}.
     */
    JsonFilter<JsonObject> OBJECT = v -> v.isObject() ? v.asObject() : null;

    /**
     * Primitive filter used for casting {@link JsonValue values} into {@link JsonArray arrays}.
     */
    JsonFilter<JsonArray> ARRAY = v -> v.isArray() ? v.asArray() : null;

    /**
     * Primitive identity filter mostly intended for checking if a value is present.
     *
     * <p>For example, to check if an object contains a given key:
     *
     * <pre>{@code
     *   OBJECT.given(VALUE.forKey("value").isPresent())
     * }</pre>
     */
    JsonFilter<JsonValue> VALUE = v -> v;

    /**
     * A filter used for extracting {@link StringType string types} from {@link JsonString JSON strings}.
     *
     * <p>Note that this filter also treats _any_ non-string, primitive value as implicit, as this is
     * how they would typically be written.
     */
    JsonFilter<StringType> STRING_TYPE = v -> {
        if (v instanceof JsonString) return ((JsonString) v).getStringType();
        return v.isPrimitive() ? StringType.IMPLICIT : null;
    };

    /**
     * This method is the most essential function in the {@link JsonFilter} API. In short,
     * it combines a <code>filter</code> and a <code>map</code> operation into a single
     * step. This can then be reused for an {@link Optional} type or a {@link Stream} by
     * deferring to {@link #applyOptional} or {@link #applyStream}, respectively.
     *
     * <p>For example,
     *
     * <pre>{@code
     *   static @Nullable JsonString asString(final JsonValue value) {
     *     return JsonType.STRING.apply(value);
     *   }
     * }</pre>
     *
     * @param value The value being filtered by the interface.
     * @return <code>T</code>, if possible, or else <code>null</code>.
     */
    @Override @Nullable T apply(final JsonValue value);

    /**
     * Variant of {@link #apply} which is safe to call for {@link Optional} types.
     *
     * <p>For example,
     *
     * <pre>{@code
     *   static Optional<JsonString> asString(final Optional<JsonValue> value) {
     *     return value.flatMap(JsonFilter.STRING::applyOptional);
     *   }
     * }</pre>
     *
     * @param value The value being filtered by the interface.
     * @return <code>T</code>, if possible, or else {@link Optional#empty}.
     */
    default Optional<T> applyOptional(final JsonValue value) {
        return Optional.ofNullable(this.apply(value));
    }

    /**
     * Variant of {@link #apply} which is safe to call for {@link Stream} types.
     *
     * <p>For example,
     *
     * <pre>{@code
     *   static Stream<JsonString> asString(final Stream<JsonValue> value) {
     *     return value.flatMap(JsonFilter.STRING::applyStream);
     *   }
     * }</pre>
     *
     * @param value The value being filtered by the interface.
     * @return <code>T</code>, if possible, or else {@link Optional#empty}.
     */
    default Stream<T> applyStream(final JsonValue value) {
        final T t = this.apply(value);
        return t != null ? Stream.of(t) : Stream.empty();
    }

    /**
     * Generates a function filtering into a different type of {@link JsonValue}.
     *
     * @param type The output type's class
     * @param <V>  The output type itself
     * @return A function of <code>JsonValue -> @Nullable V</code>
     */
    static <V extends JsonValue> JsonFilter<V> intoType(final Class<V> type) {
        return v -> type.isInstance(v) ? type.cast(v) : null;
    }

    /**
     * Applies the given validation to a value before returning it.
     *
     * <p>For example,
     *
     * <pre>{@code
     *   final JsonFilter<Number> POSITIVE_NUMBER =
     *     NUMBER.given(n -> n.intValue() > 0);
     * }</pre>
     *
     * @param condition A condition which must pass for the value to return.
     * @return A new filter with added conditions.
     */
    default JsonFilter<T> given(final Predicate<T> condition) {
        return v -> {
            final T t = this.apply(v);
            return t != null && condition.test(t) ? t : null;
        };
    }

    /**
     * Switches to an alternate filter if the current filter fails.
     *
     * <p>For example, to get a string type from value, or else {@link StringType#NONE}:
     *
     * <pre>{@code
     *   intoType(JsonString.class).map(JsonString::getStringType).or(v -> StringType.NONE)
     * }</pre>
     *
     * @param other Another filter to default to if this filter fails.
     * @return A meta filter being either one of two types.
     */
    default JsonFilter<T> or(final JsonFilter<T> other) {
        return v -> {
            final T t = this.apply(v);
            return t != null ? t : other.apply(v);
        };
    }

    /**
     * Generates a new filter which applies the given function to the output.
     *
     * @param mapper A function for converting <code>T</code> into <code>U</code>.
     * @param <U>    The new type of filter being returned.
     * @return A new filter with transformed output.
     */
    default <U> JsonFilter<U> map(final Function<T, U> mapper) {
        return v -> {
            final T t = this.apply(v);
            return t != null ? mapper.apply(t) : null;
        };
    }

    /**
     * Generates a {@link Getter getter} for checking keys in objects, using this
     * filter as the filter of the value in the object.
     *
     * <p>For example, to check for a boolean in an object:
     *
     * <pre>{@code
     *   OBJECT.given(BOOLEAN.forKey("key).isPresent())
     * }</pre>
     *
     * @param key The key being matched in the object.
     * @return A {@link Getter} used mostly for generating object predicates.
     */
    default Getter<JsonObject, T> forKey(final String key) {
        return o -> {
            final JsonValue v = o.get(key);
            return v != null ? this.apply(v) : null;
        };
    }

    /**
     * Generates a {@link Getter getter} for checking values by index in containers,
     * using this filter as the filter of the value in the container.
     *
     * <p>for example, to check for a STRING as the first value in a container:
     *
     * <pre>{@code
     *   CONTAINER.given(STRING.forIndex(0).isPresent())
     * }</pre>
     *
     * @param index The index of the value being expected.
     * @return A {@link Getter} used mostly for generating container predicates.
     */
    default Getter<JsonContainer, T> forIndex(final int index) {
        return a -> {
            final JsonValue v = a.has(index) ? a.get(index) : null;
            return v != null ? this.apply(v) : null;
        };
    }

    /**
     * A transitional type used for creating {@link JsonObject} and {@link JsonContainer}
     * predicates.
     *
     * @param <T> The input type and source of data.
     * @param <U> The type of data being acquired from <code>T</code>.
     */
    @FunctionalInterface
    interface Getter<T, U> {

        /**
         * The main function forming this interface--a literal <em>getter</em>.
         *
         * @param t The data source, usually some kind of {@link JsonContainer}.
         * @return Some instance of <code>U</code>, or else <code>null</code>.
         */
        @Nullable U get(final T t);

        /**
         * Returns <code>true</code> if the <code>U</code> is equal to <code>expected</code>.
         *
         * @param expected The value being compared to.
         * @return A predicate for comparing data in an instance of <code>T</code>.
         */
        default Predicate<T> is(final U expected) {
            return t -> {
                final U u = this.get(t);
                return u != null && u.equals(expected);
            };
        }

        /**
         * Returns <code>true</code> if the <code>U</code> passes the given predicate.
         *
         * @param predicate A predicate for checking the result of {@link #get}.
         * @return A predicate for comparing data in an instance of <code>T</code>.
         */
        default Predicate<T> matches(final Predicate<U> predicate) {
            return t -> {
                final U u = this.get(t);
                return u != null && predicate.test(u);
            };
        }

        /**
         * Returns <code>true</code> only if {@link #get} returns a nonnull value.
         *
         * @return A predicate for comparing data in an instance of <code>T</code>.
         */
        default Predicate<T> isPresent() {
            return t -> this.get(t) != null;
        }
    }
}
