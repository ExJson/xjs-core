package xjs.transformer;

import xjs.core.*;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

public class JsonCollectors {

    private JsonCollectors() {}

    public static Collector<JsonContainer.Access, JsonArray, JsonArray> access() {
        return Collector.of(
            JsonArray::new,
            (array, access) -> array.addReference(access.getReference()),
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }

    public static Collector<JsonReference, JsonArray, JsonArray> reference() {
        return Collector.of(
            JsonArray::new,
            JsonArray::addReference,
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }

    public static Collector<JsonValue, JsonArray, JsonArray> value() {
        return Collector.of(
            JsonArray::new,
            JsonArray::add,
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }

    public static Collector<Object, JsonArray, JsonArray> any() {
        return Collector.of(
            JsonArray::new,
            (array, any) -> array.add("todo"), // Todo: JsonValue#valueOf(Object)
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }

    public static Collector<JsonObject.Member, JsonObject, JsonObject> member() {
        return Collector.of(
            JsonObject::new,
            (object, member) -> object.addReference(member.getKey(), member.getReference()),
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }

    public static Collector<Map.Entry<String, ?>, JsonObject, JsonObject> toObject() {
        return Collector.of(
            JsonObject::new,
            (object, entry) -> object.add(entry.getKey(), "todo"), // Todo: JsonValue#valueOf(Object)
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }

    public static <T> Collector<Map.Entry<String, T>, JsonObject, JsonObject> toObject(
            final Function<T, JsonValue> valueMapper) {
        return Collector.of(
            JsonObject::new,
            (object, entry) -> object.add(entry.getKey(), valueMapper.apply(entry.getValue())),
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }

    public static <T> Collector<T, JsonObject, JsonObject> toObject(
            final Function<T, String> keyMapper, final Function<T, JsonValue> valueMapper) {
        return Collector.of(
            JsonObject::new,
            (object, t) -> object.add(keyMapper.apply(t), valueMapper.apply(t)),
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }
}
