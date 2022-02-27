package xjs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonReferenceTest {

    @Test
    public void new_wrapsValue() {
        final JsonValue value = JsonValue.valueOf(1234);
        assertSame(value, new JsonReference(value).get());
    }

    @Test
    public void new_toleratesNullValues() {
        assertEquals(JsonNull.instance(), new JsonReference(null).get());
    }

    @Test
    public void referent_cannotBeNull() {
        assertNotNull(new JsonReference(null).set(null).get());
    }

    @Test
    public void get_tracksAccess() {
        final JsonReference reference = new JsonReference(null);
        reference.get();
        assertTrue(reference.isAccessed());
    }

    @Test
    public void visit_doesNotTrackAccess() {
        final JsonReference reference = new JsonReference(null);
        reference.visit();
        assertFalse(reference.isAccessed());
    }

    @Test
    public void set_tracksAccess() {
        final JsonReference reference = new JsonReference(null);
        reference.set(null);
        assertTrue(reference.isAccessed());
    }

    @Test
    public void mutate_doesNotTrackAccess() {
        final JsonReference reference = new JsonReference(null);
        reference.mutate(null);
        assertFalse(reference.isAccessed());
    }

    @Test
    public void update_transformsValue() {
        final JsonReference reference = new JsonReference(null);
        reference.update(JsonValue::intoArray);
        assertTrue(reference.get().isArray());
    }

    @Test
    public void update_tracksAccess() {
        final JsonReference reference = new JsonReference(null);
        reference.update(JsonValue::intoArray);
        assertTrue(reference.isAccessed());
    }

    @Test
    public void apply_transformsValue() {
        final JsonReference reference = new JsonReference(null);
        reference.apply(JsonValue::intoArray);
        assertTrue(reference.get().isArray());
    }

    @Test
    public void apply_doesNotTrackAccess() {
        final JsonReference reference = new JsonReference(null);
        reference.apply(JsonValue::intoArray);
        assertFalse(reference.isAccessed());
    }

    @Test
    public void reference_canBeCoerced_intoReferent() {
        final JsonReference reference = new JsonReference(JsonValue.valueOf("test"));
        assertEquals("test", reference.asString());
    }

    @Test
    public void clone_createsNewInstance() {
        final JsonReference reference = new JsonReference(null);
        assertNotSame(reference, reference.clone(false));
    }

    @Test
    public void clone_copiesMetadata() {
        final JsonReference reference =
            new JsonReference(null)
                .setLinesAbove(10)
                .setLinesBetween(20)
                .setFlags(2);

        final JsonReference clone = reference.clone(false);
        assertEquals(reference.getLinesAbove(), clone.getLinesAbove());
        assertEquals(reference.getLinesBetween(), clone.getLinesBetween());
        assertEquals(reference.getFlags(), clone.getFlags());
    }
}
