package xjs.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public class JsonArray extends JsonContainer implements Iterable<JsonValue> {

    private ElementView view;

    public JsonArray() {}

    public JsonArray(final List<JsonReference> references) {
        super(references);
    }

    public JsonArray set(final int index, final long value) {
        return this.set(index, valueOf(value));
    }

    public JsonArray set(final int index, final double value) {
        return this.set(index, valueOf(value));
    }

    public JsonArray set(final int index, final boolean value) {
        return this.set(index, valueOf(value));
    }

    public JsonArray set(final int index, final String value) {
        return this.set(index, valueOf(value));
    }

    public JsonArray set(final int index, final @Nullable JsonValue value) {
        this.references.get(index).update(og ->
            nonnull(value).setDefaultMetadata(og));
        return this;
    }

    public JsonArray setComment(final int index, final String comment) {
        this.get(index).setComment(comment);
        return this;
    }

    public JsonArray add(final long value) {
        return this.add(valueOf(value));
    }

    public JsonArray add(final double value) {
        return this.add(valueOf(value));
    }

    public JsonArray add(final boolean value) {
        return this.add(valueOf(value));
    }

    public JsonArray add(final String value) {
        return this.add(valueOf(value));
    }

    public JsonArray add(final JsonValue value) {
        this.references.add(new JsonReference(value));
        return this;
    }

    public JsonArray add(final long value, final String comment) {
        return this.add(valueOf(value), comment);
    }

    public JsonArray add(final double value, final String comment) {
        return this.add(valueOf(value), comment);
    }

    public JsonArray add(final boolean value, final String comment) {
        return this.add(valueOf(value), comment);
    }

    public JsonArray add(final String value, final String comment) {
        return this.add(valueOf(value), comment);
    }

    public JsonArray add(final JsonValue value, final String comment) {
        return this.add(JsonValue.nonnull(value).setComment(comment));
    }

    public JsonArray addAll(final JsonContainer container) {
        for (final JsonReference reference : container.references) {
            this.add(reference.get());
        }
        return this;
    }

    public JsonArray addReference(final int index, final JsonReference reference) {
        this.references.add(index, reference);
        return this;
    }

    public JsonArray addReference(final JsonReference reference) {
        this.references.add(reference);
        return this;
    }

    @Override
    public View<Element> view() {
        if (this.view == null) {
            return this.view = new ElementView();
        }
        return this.view;
    }

    @Override
    public JsonArray setLineLength(final int lineLength) {
        return (JsonArray) super.setLineLength(lineLength);
    }

    @Override
    public JsonArray condense() {
        return (JsonArray) super.condense();
    }

    @Override
    public JsonArray clear() {
        return (JsonArray) super.clear();
    }

    @Override
    public JsonArray remove(final JsonValue value) {
        return (JsonArray) super.remove(value);
    }

    @Override
    public JsonArray removeAll(final Iterable<JsonValue> values) {
        return (JsonArray) super.removeAll(values);
    }

    @Override
    public JsonArray setAllAccessed(final boolean accessed) {
        return (JsonArray) super.setAllAccessed(accessed);
    }

    @Override
    public JsonArray intoContainer() {
        return (JsonArray) super.intoContainer();
    }

    @Override
    public JsonArray shallowCopy() {
        final JsonArray copy = new JsonArray();
        for (final JsonReference reference : this.references) {
            final JsonValue value = reference.visit();
            if (value.isContainer()) {
                copy.add(value.asContainer().shallowCopy());
            } else {
                copy.addReference(reference);
            }
        }
        return copy;
    }

    @Override
    public JsonArray deepCopy() {
        return this.deepCopy(false);
    }

    @Override
    public JsonArray deepCopy(final boolean trackAccess) {
        final JsonArray copy = (JsonArray) new JsonArray().setDefaultMetadata(this);
        for (final JsonReference reference : this.references) {
            final JsonValue value = reference.visit();
            if (value.isContainer()) {
                copy.add(value.asContainer().deepCopy(trackAccess));
            } else {
                copy.addReference(reference.clone(trackAccess));
            }
        }
        return copy;
    }

    @Override
    public JsonArray unformatted() {
        final JsonArray copy = new JsonArray();
        for (final JsonReference reference : this.references) {
            copy.addReference(reference.clone(true)
                .mutate(reference.visit().unformatted()));
        }
        return copy;
    }

    @Override
    public JsonType getType() {
        return JsonType.ARRAY;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public JsonArray asContainer() {
        return this;
    }

    @Override
    public JsonArray asArray() {
        return this;
    }

    @Override
    public JsonObject intoObject() {
        return new JsonObject(this.references);
    }

    @Override
    public JsonArray intoArray() {
        return this;
    }

    @Override
    public Iterator<JsonValue> iterator() {
        return this.values().iterator();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + this.references.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof JsonArray) {
            return this.references.equals(((JsonArray) o).references)
                && super.metadataEquals((JsonArray) o);
        }
        return false;
    }

    @Override
    public String toString() {
        return "<Missing implementation>"; // todo
    }

    private class ElementView implements View<Element> {
        @Override
        public @NotNull ElementIterator iterator() {
            return new ElementIterator();
        }
    }

    private class ElementIterator implements Iterator<Element> {
        final Iterator<JsonReference> references = references().iterator();
        int index = 0;

        @Override
        public boolean hasNext() {
            return this.references.hasNext();
        }

        @Override
        public Element next() {
            final JsonReference reference = this.references.next();
            return new Element(index++, reference);
        }

        @Override
        public void remove() {
            this.references.remove();
        }
    }

    public static class Element extends Access {
        private final int index;

        public Element(final int index, final JsonReference reference) {
            super(reference);
            this.index = index;
        }

        public Element(final int index, final JsonValue value) {
            super(new JsonReference(value));
            this.index = index;
        }

        public int getIndex() {
            return this.index;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + this.index;
            result = 31 * result + this.reference.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof Element) {
                final Element other = (Element) o;
                return this.index == other.index && this.reference.equals(other.reference);
            }
            return false;
        }

        @Override
        public String toString() {
            return "([" + this.index + "]=" + this.reference + ")";
        }
    }
}
