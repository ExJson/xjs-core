package xjs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonObjectTest {

    @Test
    public void add_insertsValue() {
        final JsonObject object = new JsonObject();
        object.add("value", 1234);
        assertEquals(1234, object.get("value").asInt());
    }

    @Test
    public void add_doesNotTrackAccess() {
        final JsonObject object = new JsonObject();
        object.add("value", 1234);
        assertFalse(object.getReference("value").isAccessed());
    }

    @Test
    public void addReference_insertsReference() {
        final JsonObject object = new JsonObject();
        final JsonReference reference = new JsonReference(JsonValue.valueOf(1234));
        object.addReference("value", reference);
        assertSame(reference, object.getReference("value"));
    }

    @Test
    public void addReference_doesNotTrackAccess() {
        final JsonObject object = new JsonObject();
        final JsonReference reference = new JsonReference(JsonValue.valueOf(1234));
        object.addReference("value", reference);
        assertFalse(object.getReference("value").isAccessed());
    }

    @Test
    public void set_updatesValue() {
        final JsonObject object = new JsonObject().add("value", 1);
        object.set("value", 2);
        assertTrue(object.getReference("value").isAccessed());
    }

    @Test
    public void set_toleratesInsertion() {
        final JsonObject object = new JsonObject();
        assertDoesNotThrow(() -> object.set("value", 2));
    }

    @Test
    public void addAll_addsValuesFromOtherObject() {
        final JsonObject object = new JsonObject();
        object.addAll(new JsonObject().add("", 1));
        assertTrue(object.contains(1));
    }

    @Test
    public void intoArray_dropsKeys() {
        final JsonObject object = new JsonObject().add("values", 1);
        assertEquals(new JsonArray().add(1), object.intoArray());
    }
}
