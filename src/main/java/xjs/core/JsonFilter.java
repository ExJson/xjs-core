package xjs.core;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

@FunctionalInterface
public interface JsonFilter<T> extends Function<JsonValue, T> {

    JsonFilter<String> STRING = v -> v.isString() ? v.asString() : null;
    JsonFilter<Number> NUMBER = v -> v.isNumber() ? v.asDouble() : null;
    JsonFilter<Boolean> BOOLEAN = v -> v.isBoolean() ? v.asBoolean() : null;
    JsonFilter<JsonContainer> CONTAINER = v -> v.isContainer() ? v.asContainer() : null;
    JsonFilter<JsonObject> OBJECT = v -> v.isObject() ? v.asObject() : null;
    JsonFilter<JsonArray> ARRAY = v -> v.isArray() ? v.asArray() : null;

    JsonFilter<StringType> STRING_TYPE = v -> {
        if (v instanceof JsonString) return ((JsonString) v).getStringType();
        return v.isPrimitive() ? StringType.IMPLICIT : null;
    };

    @Override @Nullable T apply(final JsonValue value);

    default Optional<T> applyOptional(final JsonValue value) {
        return Optional.ofNullable(this.apply(value));
    }
}
