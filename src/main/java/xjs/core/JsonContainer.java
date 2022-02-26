package xjs.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class JsonContainer extends JsonValue {

    protected final List<JsonReference> references;
    protected View<JsonValue> accessor;
    protected View<JsonValue> visitor;
    protected int lineLength;

    protected JsonContainer() {
        this(new ArrayList<>());
    }

    protected JsonContainer(final List<JsonReference> references) {
        this.references = references;
        this.lineLength = 0;
    }

    public int getLineLength() {
        return this.lineLength;
    }

    public JsonContainer setLineLength(final int lineLength) {
        this.lineLength = lineLength;
        return this;
    }

    public boolean isCondensed() {
        return this.lineLength == 0;
    }

    public JsonContainer setCondensed() {
        this.lineLength = 0;
        return this;
    }

    public Collection<JsonReference> references() {
        return Collections.unmodifiableCollection(this.references);
    }

    public boolean has(final int index) {
        return index >= 0 && index < this.references.size();
    }

    public JsonValue get(final int index) {
        return this.references.get(index).get();
    }

    public int getInt(final int index, final int defaultValue) {
        final JsonValue value = this.references.get(index).get();
        return value.isNumber() ? value.asInt() : defaultValue;
    }

    public long getLong(final int index, final long defaultValue) {
        final JsonValue value = this.references.get(index).get();
        return value.isNumber() ? value.asLong() : defaultValue;
    }

    public float getFloat(final int index, final float defaultValue) {
        final JsonValue value = this.references.get(index).get();
        return value.isNumber() ? value.asFloat() : defaultValue;
    }

    public double getDouble(final int index, final double defaultValue) {
        final JsonValue value = this.references.get(index).get();
        return value.isNumber() ? value.asDouble() : defaultValue;
    }

    public boolean getBoolean(final int index, final boolean defaultValue) {
        final JsonValue value = this.references.get(index).get();
        return value.isBoolean() ? value.asBoolean() : defaultValue;
    }

    public String getString(final int index, final String defaultValue) {
        final JsonValue value = this.references.get(index).get();
        return value.isString() ? value.asString() : defaultValue;
    }

    public Optional<JsonValue> getOptional(final int index) {
        if (index >= 0 && index < this.references.size()) {
            return Optional.of(this.references.get(index).get());
        }
        return Optional.empty();
    }

    public <T extends JsonValue> Optional<T> getOptional(final int index, final Class<T> type) {
        if (index >= 0 && index < this.references.size()) {
            JsonValue value = this.references.get(index).get();
            while (value instanceof JsonReference) {
                value = ((JsonReference) value).get();
            }
            if (type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
        }
        return Optional.empty();
    }

    public JsonReference getReference(final int index) {
        return this.references.get(index);
    }

    public int size() {
        return this.references.size();
    }

    public boolean isEmpty() {
        return this.references.isEmpty();
    }

    public boolean contains(final JsonValue value) {
        for (final JsonReference reference : this.references) {
            if (reference.visit().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(final Iterable<JsonValue> values) {
        for (final JsonValue value : values) {
            if (!this.contains(value)) {
                return false;
            }
        }
        return true;
    }

    public View<JsonValue> values() {
        if (this.accessor == null) {
            return this.accessor = new AccessingView();
        }
        return this.accessor;
    }

    public View<JsonValue> visitAll() {
        if (this.visitor == null) {
            return this.visitor = new VisitingView();
        }
        return this.visitor;
    }

    public abstract View<? extends Access> view();

    public JsonContainer clear() {
        this.references.clear();
        return this;
    }

    public JsonContainer remove(final JsonValue value) {
        final Iterator<JsonReference> iterator = this.references.iterator();
        while (iterator.hasNext()) {
            final JsonReference reference = iterator.next();
            if (reference.visit().equals(value)) {
                iterator.remove();
                return this;
            }
        }
        return this;
    }

    public JsonContainer removeAll(final Iterable<JsonValue> values) {
        values.forEach(this::remove);
        return this;
    }

    public JsonContainer setAllAccessed(final boolean accessed) {
        for (final JsonReference reference : this.references) {
            reference.setAccessed(accessed);

            final JsonValue referent = reference.visit();
            if (referent.isContainer()) {
                referent.asContainer().setAllAccessed(accessed);
            }
        }
        return this;
    }

    public int indexOf(final JsonValue value) {
        for (int i = 0; i < this.references.size(); i++) {
            if (this.references.get(i).visit().equals(value)) {
                return i;
            }
        }
        return -1;
    }

    public abstract JsonContainer shallowCopy();

    public abstract JsonContainer deepCopy();

    public abstract JsonContainer deepCopy(final boolean trackAccess);

    @Override
    public final boolean isContainer() {
        return true;
    }

    @Override
    public Number intoNumber() {
        return this.size();
    }

    @Override
    public long intoLong() {
        return this.size();
    }

    @Override
    public int intoInt() {
        return this.size();
    }

    @Override
    public double intoDouble() {
        return this.size();
    }

    @Override
    public float intoFloat() {
        return this.size();
    }

    @Override
    public boolean intoBoolean() {
        return this.size() > 0;
    }

    @Override
    public String intoString() {
        return this.toString();
    }

    @Override
    public JsonContainer intoContainer() {
        return this;
    }

    private class AccessingView implements View<JsonValue> {
        @Override
        public @NotNull Iterator<JsonValue> iterator() {
            return new AccessingValueIterator();
        }
    }

    private class AccessingValueIterator implements Iterator<JsonValue> {
        final Iterator<JsonReference> references = JsonContainer.this.references.iterator();

        @Override
        public boolean hasNext() {
            return this.references.hasNext();
        }

        @Override
        public JsonValue next() {
            return this.references.next().get();
        }

        @Override
        public void remove() {
            this.references.remove();
        }
    }

    private class VisitingView implements View<JsonValue> {
        @Override
        public @NotNull Iterator<JsonValue> iterator() {
            return new VisitingValueIterator();
        }
    }

    private class VisitingValueIterator implements Iterator<JsonValue> {
        final Iterator<JsonReference> references = JsonContainer.this.references.iterator();

        @Override
        public boolean hasNext() {
            return this.references.hasNext();
        }

        @Override
        public JsonValue next() {
            return this.references.next().visit();
        }

        @Override
        public void remove() {
            this.references.remove();
        }
    }

    public static abstract class Access {
        protected JsonReference reference;

        public Access(final JsonReference reference) {
            this.reference = reference;
        }

        public @NotNull JsonValue getValue() {
            return this.reference.get();
        }

        public Access setValue(final @Nullable JsonValue value) {
            this.reference.set(value);
            return this;
        }

        public @NotNull JsonValue visit() {
            return this.reference.visit();
        }

        public Access mutate(final JsonValue value) {
            this.reference.mutate(value);
            return this;
        }

        public JsonReference getReference() {
            return this.reference;
        }
    }

    public interface View<T> extends Iterable<T> {
        default Stream<T> stream() {
            return StreamSupport.stream(this.spliterator(), false);
        }
    }
}
