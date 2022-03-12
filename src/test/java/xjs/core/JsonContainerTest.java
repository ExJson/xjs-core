package xjs.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonContainerTest {

    @Test
    public void size_reportsNumReferences() {
        final List<JsonReference> references =
            List.of(new JsonReference(Json.value(1234)),
                new JsonReference(Json.value(5678)));

        final JsonContainer container = new JsonArray(references);
        assertEquals(references.size(), container.size());
    }

    @Test
    public void get_returnsReferent() {
        final JsonContainer container = new JsonArray().add(1);
        assertEquals(1, container.get(0).asInt());
    }

    @Test
    public void get_doesNotTolerate_outOfBounds() {
        final JsonContainer container = new JsonArray().add(1);
        assertThrows(IndexOutOfBoundsException.class,
            () -> container.get(1));
    }

    @Test
    public void getOptional_doesTolerate_outOfBounds() {
        final JsonContainer container = new JsonArray().add(1);
        assertTrue(container.getOptional(1).isEmpty());
    }

    @Test
    public void contains_searchesByValue() {
        final JsonContainer container = new JsonArray().add(1);
        assertTrue(container.contains(Json.value(1)));
    }

    @Test
    public void contains_canSearchByPrimitive() {
        final JsonContainer container = new JsonArray().add(1);
        assertTrue(container.contains(1));
    }

    @Test
    public void has_searchesByIndex() {
        final JsonContainer container = new JsonArray().add(1);
        assertTrue(container.has(0));
    }

    @Test
    public void forEachRecursive_inspectsRecursively() {
        final List<Integer> numbers = new ArrayList<>();
        final JsonArray array = new JsonArray()
            .add(new JsonArray().add(1).add(2).add(3))
            .add(new JsonArray().add(4).add(5).add(6));

        array.forEachRecursive(ref -> {
           if (ref.get().isNumber()) {
               numbers.add(ref.get().asInt());
           }
        });
        assertEquals(List.of(1, 2, 3, 4, 5, 6), numbers);
    }

    @Test
    public void clear_mutatesInPlace() {
        final JsonContainer container = new JsonArray().add(1);
        assertTrue(container.clear().isEmpty());
    }

    @Test
    public void intoContainer_returnsThis() {
        final JsonContainer container = new JsonArray().add(1);
        assertSame(container, container.intoContainer());
    }

    @Test
    public void values_tracksAccess() {
        final JsonContainer container = new JsonArray().add(1).add(2).add(3);
        container.values().forEach(value -> {});
        assertTrue(container.references().stream().allMatch(JsonReference::isAccessed));
    }

    @Test
    public void visitAll_doesNotTrackAccess() {
        final JsonContainer container = new JsonArray().add(1).add(2).add(3);
        container.visitAll().forEach(value -> {});
        assertFalse(container.references().stream().anyMatch(JsonReference::isAccessed));
    }

    @Test
    public void shallowCopy_addsExistingReferences() {
        final List<JsonReference> references =
            List.of(new JsonReference(Json.value(1234)),
                new JsonReference(Json.value(5678)));

        final JsonContainer container = new JsonArray(references);
        final JsonContainer copy = container.shallowCopy();

        for (int i = 0; i < references.size(); i++) {
            assertSame(references.get(i), copy.getReference(i));
        }
    }

    @Test
    public void shallowCopy_copiesRecursively() {
        final JsonContainer nested = new JsonArray();
        final JsonContainer container = new JsonArray().add(nested);
        final JsonContainer copy = container.shallowCopy();

        assertNotSame(nested, copy.get(0));
    }

    @Test
    public void shallowCopy_doesNotTrackAccess() {
        final JsonContainer container = new JsonArray().add(1).add(2).add(3);
        final JsonContainer copy = container.shallowCopy();

        assertFalse(copy.references().stream().anyMatch(JsonReference::isAccessed));
    }



    @Test
    public void deepCopy_clonesAllReferences() {
        final List<JsonReference> references =
            List.of(new JsonReference(Json.value(1234)),
                new JsonReference(Json.value(5678)));

        final JsonContainer container = new JsonArray(references);
        final JsonContainer copy = container.deepCopy(false);

        for (int i = 0; i < references.size(); i++) {
            assertNotSame(references.get(i), copy.getReference(i));
            assertEquals(references.get(i), copy.getReference(i));
        }
    }

    @Test
    public void deepCopy_copiesRecursively() {
        final JsonContainer nested = new JsonArray();
        final JsonContainer container = new JsonArray().add(nested);
        final JsonContainer copy = container.deepCopy();

        assertNotSame(nested, copy.get(0));
    }

    @Test
    public void deepCopy_doesNotTrackAccess() {
        final JsonContainer container = new JsonArray().add(1).add(2).add(3);
        final JsonContainer copy = container.deepCopy();

        assertFalse(copy.references().stream().anyMatch(JsonReference::isAccessed));
    }
}
