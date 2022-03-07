package xjs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonValueTest {

    @Test
    public void valueOf_wrapsInteger() {
        assertEquals(12345678901L, JsonValue.valueOf(12345678901L).asLong());
    }

    @Test
    public void valueOf_wrapsDecimal() {
        assertEquals(1.2345678901, JsonValue.valueOf(1.2345678901).asDouble());
    }

    @Test
    public void valueOf_wrapsString() {
        assertEquals("Hello, World!", JsonValue.valueOf("Hello, World!").asString());
    }

    @Test
    public void valueOf_wrapsBoolean() {
        assertTrue(JsonValue.valueOf(true).asBoolean());
    }

    @Test
    public void valueOf_toleratesNullValues() {
        assertEquals(JsonLiteral.jsonNull(), JsonValue.valueOf(null));
    }

    @Test
    public void valueOf_returnsPrimitiveWrappers() {
        assertTrue(JsonValue.valueOf("value").isPrimitive());
    }

    @Test
    public void nonnull_wrapsWhenNull() {
        assertEquals(JsonLiteral.jsonNull(), JsonValue.nonnull(null));
    }

    @Test
    public void nonnull_doesNotWrapWhenNonnull() {
        final JsonValue value = JsonValue.valueOf("value");
        assertSame(value, JsonValue.nonnull(value));
    }

    @Test
    public void asType_doesNotSupportConversion() {
        assertThrows(UnsupportedOperationException.class,
            () -> JsonValue.valueOf("inconvertible").asInt());
    }

    @Test
    public void intoType_doesSupportTypeConversion() {
        assertEquals(5, JsonValue.valueOf("12345").intoInt());
    }

    @Test
    public void intoArray_generatesArray() {
        assertEquals(new JsonArray().add(1), JsonValue.valueOf(1).intoArray());
    }

    @Test
    public void intoObject_generatesObject() {
        assertEquals(new JsonObject().add("value", 1), JsonValue.valueOf(1).intoObject());
    }
}
