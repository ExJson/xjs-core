package xjs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonArrayTest {

    @Test
    public void add_insertsValue() {
        final JsonArray array = new JsonArray();
        array.add(1234);
        assertEquals(1234, array.get(0).asInt());
    }

    @Test
    public void add_doesNotTrackAccess() {
        final JsonArray array = new JsonArray();
        array.add(1234);
        assertFalse(array.getReference(0).isAccessed());
    }

    @Test
    public void addReference_insertsReference() {
        final JsonArray array = new JsonArray();
        final JsonReference reference = new JsonReference(JsonValue.valueOf(1234));
        array.addReference(reference);
        assertSame(reference, array.getReference(0));
    }

    @Test
    public void addReference_doesNotTrackAccess() {
        final JsonArray array = new JsonArray();
        final JsonReference reference = new JsonReference(JsonValue.valueOf(1234));
        array.addReference(reference);
        assertFalse(array.getReference(0).isAccessed());
    }

    @Test
    public void set_updatesValue() {
        final JsonArray array = new JsonArray().add(1);
        array.set(0, 2);
        assertTrue(array.getReference(0).isAccessed());
    }

    @Test
    public void set_doesNotTolerateInsertion() {
        final JsonArray array = new JsonArray();
        assertThrows(IndexOutOfBoundsException.class,
            () -> array.set(0, 2));
    }

    @Test
    public void addAll_addsFromAnyContainer() {
        final JsonArray array = new JsonArray();
        array.addAll(new JsonObject().add("", 1));
        assertTrue(array.contains(1));
    }

    @Test
    public void intoObject_convertsIndicesIntoStrings() {
        final JsonArray array = new JsonArray().add(1);
        assertEquals(new JsonObject().add("0", 1), array.intoObject());
    }
}
