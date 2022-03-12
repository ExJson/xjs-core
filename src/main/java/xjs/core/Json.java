package xjs.core;

import org.jetbrains.annotations.Nullable;
import xjs.serialization.JsonSerializationContext;
import xjs.serialization.parser.XjsParser;
import xjs.transformer.JsonCollectors;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public final class Json {

    private Json() {}

    public static JsonNumber value(final long value) {
        return new JsonNumber(value);
    }

    public static JsonNumber value(final double value) {
        return new JsonNumber(value);
    }

    public static JsonLiteral value(final boolean value) {
        return value ? JsonLiteral.jsonTrue() : JsonLiteral.jsonFalse();
    }

    public static JsonValue value(final @Nullable String value) {
        return value != null ? JsonString.auto(value) : JsonLiteral.jsonNull();
    }

    public static JsonValue nonnull(final @Nullable JsonValue value) {
        return value != null ? value : JsonLiteral.jsonNull();
    }

    public static JsonArray array() {
        return new JsonArray();
    }

    public static JsonArray array(final Collection<?> collection) {
        return collection.stream().collect(JsonCollectors.any());
    }

    public static JsonArray array(final long... values) {
        return LongStream.of(values).mapToObj(Json::value).collect(JsonCollectors.value());
    }

    public static JsonArray array(final double... values) {
        return DoubleStream.of(values).mapToObj(Json::value).collect(JsonCollectors.value());
    }

    public static JsonArray array(final String... values) {
        return Stream.of(values).map(Json::value).collect(JsonCollectors.value());
    }

    public static JsonObject object() {
        return new JsonObject();
    }

    public static JsonObject object(final Map<?, ?> map) {
        return map.entrySet().stream().collect(JsonCollectors.toObject());
    }

    public static JsonValue any(final @Nullable Object unknown) {
        if (unknown == null) return JsonLiteral.jsonNull();
        if (unknown instanceof Number) return value(((Number) unknown).doubleValue());
        if (unknown instanceof Boolean) return value((boolean) unknown);
        if (unknown instanceof String) return value((String) unknown);
        if (unknown instanceof Enum) return value(((Enum<?>) unknown).name());
        if (unknown instanceof Collection) return array((Collection<?>) unknown);
        if (unknown instanceof Map) return object((Map<?, ?>) unknown);
        if (unknown.getClass().isArray()) return arrayPointer(unknown);
        throw new UnsupportedOperationException("Unsupported type: " + unknown.getClass());
    }

    public static JsonArray any(final Object... unknowns) {
        return Stream.of(unknowns).collect(JsonCollectors.any());
    }

    private static JsonArray arrayPointer(final Object unknown) {
        final JsonArray array = new JsonArray();
        final int length = Array.getLength(unknown);
        for (int i = 0; i < length; i++) {
            array.add(any(Array.get(unknown, i)));
        }
        return array;
    }

    public static JsonValue parse(final File file) throws IOException {
        return JsonSerializationContext.autoParse(file);
    }

    public static JsonValue parse(final String xjs) {
        try {
            return new XjsParser(xjs).parse();
        } catch (final IOException unreachable) {
            throw new IllegalStateException(unreachable);
        }
    }

    public static void view(final File file, final Consumer<JsonValue> f) throws IOException {
        update(file, value -> { f.accept(value); return value; });
    }

    public static void update(final File file, final UnaryOperator<JsonValue> f) throws IOException {
        f.apply(parse(file)).write(file);
    }
}
