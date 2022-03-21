package xjs.core;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JsonObject extends JsonContainer implements JsonContainer.View<JsonObject.Member> {

    private final List<String> keys;
    private final transient HashIndexTable table;

    public JsonObject() {
        this.keys = new ArrayList<>();
        this.table = new HashIndexTable();
    }

    @ApiStatus.Experimental
    public JsonObject(final List<JsonReference> references) {
        this(listOfNumericKeys(references.size()), references);
    }

    protected JsonObject(final List<String> keys, final List<JsonReference> references) {
        super(references);
        this.keys = keys;
        this.table = new HashIndexTable();
        this.table.init(this.keys);
    }

    private static List<String> listOfNumericKeys(final int size) {
        final List<String> keys = new ArrayList<>(Math.max(size, 10));
        for (int i = 0; i < size; i++) {
            keys.add(String.valueOf(i));
        }
        return keys;
    }

    public List<String> keys() {
        return Collections.unmodifiableList(this.keys);
    }

    public JsonObject set(final String key, final long value) {
        return this.set(key, Json.value(value));
    }

    public JsonObject set(final String key, final double value) {
        return this.set(key, Json.value(value));
    }

    public JsonObject set(final String key, final boolean value) {
        return this.set(key, Json.value(value));
    }

    public JsonObject set(final String key, final String value) {
        return this.set(key, Json.value(value));
    }

    public JsonObject set(final String key, final @Nullable JsonValue value) {
        final int index = this.indexOf(key);
        if (index != -1) {
            this.references.get(index).update(og ->
                Json.nonnull(value).setDefaultMetadata(og));
        } else {
            this.table.add(key, this.keys.size());
            this.keys.add(key);
            this.references.add(new JsonReference(value).setAccessed(true));
        }
        return this;
    }

    /**
     * @throws NullPointerException if the value does not exist.
     */
    @SuppressWarnings("ConstantConditions")
    public JsonObject setComment(final String key, final String comment) {
        this.get(key).setComment(comment);
        return this;
    }

    public JsonObject setDefaults(final JsonObject defaultValues) {
        final Iterator<String> keyIterator = defaultValues.keys.iterator();
        final Iterator<JsonReference> referenceIterator = defaultValues.references.iterator();

        while (keyIterator.hasNext() && referenceIterator.hasNext()) {
            final String key = keyIterator.next();
            final JsonReference reference = referenceIterator.next();

            final JsonValue replaced = this.get(key);
            if (replaced == null) {
                this.add(key, reference.get());
            } else if (replaced instanceof JsonObject && reference.visit() instanceof JsonObject) {
                replaced.asObject().setDefaults(reference.get().asObject());
            }
        }
        return this;
    }

    public JsonObject add(final String key, final long value) {
        return this.add(key, Json.value(value));
    }

    public JsonObject add(final String key, final double value) {
        return this.add(key, Json.value(value));
    }

    public JsonObject add(final String key, final boolean value) {
        return this.add(key, Json.value(value));
    }

    public JsonObject add(final String key, final String value) {
        return this.add(key, Json.value(value));
    }

    public JsonObject add(final String key, final @Nullable JsonValue value) {
        this.table.add(key, this.keys.size());
        this.keys.add(key);
        this.references.add(new JsonReference(value));
        return this;
    }

    public JsonObject add(final String key, final long value, final String comment) {
        return this.add(key, Json.value(value), comment);
    }

    public JsonObject add(final String key, final double value, final String comment) {
        return this.add(key, Json.value(value), comment);
    }

    public JsonObject add(final String key, final boolean value, final String comment) {
        return this.add(key, Json.value(value), comment);
    }

    public JsonObject add(final String key, final String value, final String comment) {
        return this.add(key, Json.value(value), comment);
    }

    public JsonObject add(final String key, final @Nullable JsonValue value, final String comment) {
        return this.add(key, Json.nonnull(value).setComment(comment));
    }

    public JsonObject addAll(final JsonObject object) {
        final Iterator<String> keyIterator = object.keys.iterator();
        final Iterator<JsonReference> referenceIterator = object.references.iterator();
        while (keyIterator.hasNext() && referenceIterator.hasNext()) {
            this.addReference(keyIterator.next(), referenceIterator.next());
        }
        return this;
    }

    public JsonObject addReference(final String key, final JsonReference reference) {
        this.table.add(key, this.keys.size());
        this.keys.add(key);
        this.references.add(reference);
        return this;
    }

    public JsonObject set(final int index, final String key, final @Nullable JsonValue value) {
        if (index < 0 || index >= this.references.size()) {
            return this.add(key, value);
        }
        this.references.get(index).update(og ->
            Json.nonnull(value).setDefaultMetadata(og));
        this.keys.set(index, key);
        this.table.clear();
        this.table.init(this.keys);
        return this;
    }

    public JsonObject setKey(final int index, final String key) {
        if (index >= 0 && index < this.references.size()) {
            this.keys.set(index, key);
            this.table.clear();
            this.table.init(this.keys);
        }
        return this;
    }

    public JsonObject insert(final int index, final String key, final @Nullable JsonValue value) {
        this.references.add(index, new JsonReference(value).setAccessed(true));
        this.keys.add(index, key);
        this.table.clear();
        this.table.init(this.keys);
        return this;
    }

    public boolean has(final String key) {
        return this.indexOf(key) != -1;
    }

    public @Nullable JsonValue get(final String key) {
        final int index = this.indexOf(key);
        return index != -1 ? this.references.get(index).get() : null;
    }

    @ApiStatus.Experimental
    public <T> Optional<T> getOptional(final String key, final JsonFilter<T> f) {
        return this.getOptional(key).flatMap(f::applyOptional);
    }

    public Optional<JsonValue> getOptional(final String key) {
        return Optional.ofNullable(this.get(key));
    }

    public @Nullable JsonReference getReference(final String key) {
        final int index = this.indexOf(key);
        return index != -1 ? this.references.get(index) : null;
    }

    public JsonObject remove(final String key) {
        final int index = this.indexOf(key);
        if (index != -1) {
            this.references.remove(index);
            this.keys.remove(index);
            this.table.remove(index);
        }
        return this;
    }

    public int indexOf(final String key) {
        final int index = this.table.get(key);
        if (index != -1 && key.equals(this.keys.get(index))) {
            return index;
        }
        return this.keys.lastIndexOf(key);
    }

    @Override
    public View<Member> view() {
        return this;
    }

    @Override
    public JsonObject setLineLength(final int lineLength) {
        return (JsonObject) super.setLineLength(lineLength);
    }

    @Override
    public JsonObject condense() {
        return (JsonObject) super.condense();
    }

    @Override
    public JsonObject clear() {
        return (JsonObject) super.clear();
    }

    @Override
    public JsonObject remove(final JsonValue value) {
        final int index = this.indexOf(value);
        if (index != -1) {
            this.references.remove(index);
            this.keys.remove(index);
            this.table.remove(index);
        }
        return this;
    }

    @Override
    public JsonObject removeAll(final Iterable<JsonValue> values) {
        return (JsonObject) super.removeAll(values);
    }

    @Override
    public JsonObject setAllAccessed(final boolean accessed) {
        return (JsonObject) super.setAllAccessed(accessed);
    }

    public <T> Map<String, T> toMap(final Function<JsonValue, T> mapper) {
        final Map<String, T> map = new HashMap<>();
        final Iterator<String> keyIterator = this.keys.iterator();
        final Iterator<JsonReference> referenceIterator = this.references.iterator();

        while (keyIterator.hasNext() && referenceIterator.hasNext()) {
            map.put(keyIterator.next(), mapper.apply(referenceIterator.next().get()));
        }
        return map;
    }

    public Map<String, Object> toMap() {
        return this.toMap(JsonValue::unwrap);
    }

    @Override
    public JsonObject intoContainer() {
        return (JsonObject) super.intoContainer();
    }

    @Override
    public JsonObject shallowCopy() {
        final JsonObject copy = new JsonObject();
        final Iterator<String> keyIterator = this.keys.iterator();
        final Iterator<JsonReference> referenceIterator = this.references.iterator();

        while (keyIterator.hasNext() && referenceIterator.hasNext()) {
            final String key = keyIterator.next();
            final JsonReference reference = referenceIterator.next();
            final JsonValue value = reference.visit();

            if (value.isContainer()) {
                copy.add(key, value.asContainer().shallowCopy());
            } else {
                copy.addReference(key, reference);
            }
        }
        return copy;
    }

    @Override
    public JsonObject deepCopy() {
        return this.deepCopy(false);
    }

    @Override
    public JsonObject deepCopy(final boolean trackAccess) {
        final JsonObject copy = (JsonObject) new JsonObject().setDefaultMetadata(this);
        final Iterator<String> keyIterator = this.keys.iterator();
        final Iterator<JsonReference> referenceIterator = this.references.iterator();

        while (keyIterator.hasNext() && referenceIterator.hasNext()) {
            final String key = keyIterator.next();
            final JsonReference reference = referenceIterator.next();
            final JsonValue value = reference.visit();

            if (value.isContainer()) {
                copy.add(key, value.asContainer().deepCopy(trackAccess));
            } else {
                copy.addReference(key, reference.clone(trackAccess));
            }
        }
        return copy;
    }

    @Override
    public JsonObject unformatted() {
        final JsonObject copy = new JsonObject();
        final Iterator<String> keyIterator = this.keys.iterator();
        final Iterator<JsonReference> referenceIterator = this.references.iterator();

        while (keyIterator.hasNext() && referenceIterator.hasNext()) {
            final String key = keyIterator.next();
            final JsonReference reference = referenceIterator.next();

            copy.addReference(key, reference.clone(true)
                .mutate(reference.visit().unformatted()));
        }
        return copy;
    }

    @Override
    public final JsonType getType() {
        return JsonType.OBJECT;
    }

    @Override
    public Map<String, Object> unwrap() {
        return this.toMap();
    }

    @Override
    public final boolean isObject() {
        return true;
    }

    @Override
    public JsonObject asContainer() {
        return this;
    }

    @Override
    public JsonObject asObject() {
        return this;
    }

    @Override
    public JsonObject intoObject() {
        return this;
    }

    @Override
    public JsonArray intoArray() {
        return new JsonArray(this.references);
    }

    @Override
    public Iterator<Member> iterator() {
        return new MemberIterator();
    }

    private synchronized void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.table.clear();
        this.table.init(this.keys);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.keys.hashCode();
        result = 31 * result + this.references.hashCode();
        return result;
    }

    @Override
    public boolean matches(final JsonValue other) {
        if (!(other instanceof JsonObject)) {
            return false;
        }
        final JsonObject object = (JsonObject) other;
        if (this.size() != object.size()) {
            return false;
        }
        if (!this.keys.equals((object.keys))) {
            return false;
        }
        for (int i = 0; i < this.size(); i++) {
            if (!this.references.get(i).visit().matches(object.references.get(i).visit())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public JsonObject freeze() {
        return (JsonObject) super.freeze();
    }

    @Override
    public JsonObject freeze(final boolean recursive) {
        return new JsonObject(this.keys, this.freezeReferences(recursive));
    }

    private class MemberIterator implements Iterator<Member> {
        final Iterator<String> keys = JsonObject.this.keys.iterator();
        final Iterator<JsonReference> references = references().iterator();

        @Override
        public boolean hasNext() {
            return this.keys.hasNext() && this.references.hasNext();
        }

        @Override
        public Member next() {
            return new Member(this.keys.next(), this.references.next());
        }

        @Override
        public void remove() {
            this.references.remove();
            this.keys.remove();
        }
    }

    public static class Member extends Access {
        protected final String key;

        public Member(final String key, final JsonReference reference) {
            super(reference);
            this.key = key;
        }

        /**
         *
         * @param key
         * @param value
         * @apiNote Experimental - constructor is unnecessary and may get removed.
         */
        @ApiStatus.Experimental
        public Member(final String key, final JsonValue value) {
            super(new JsonReference(value));
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }

        @Override
        public int hashCode() {
            int result=1;
            result = 31 * result + this.key.hashCode();
            result = 31 * result + this.reference.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof Member) {
                final Member other = (Member) o;
                return this.key.equals(other.key) && this.reference.equals(other.reference);
            }
            return false;
        }

        @Override
        public String toString() {
            return "(" + this.key + "=" + this.reference + ")";
        }
    }

    private static class HashIndexTable {
        final byte[] indices = new byte[32];

        void init(final List<String> values) {
            for (int i = 0; i < values.size(); i++) {
                this.add(values.get(i), i);
            }
        }

        void add(final String key, final int index) {
            int slot = getSlot(key);
            if (index < 0xff) {
                // increment by 1, 0 stands for empty
                this.indices[slot] = (byte) (index + 1);
            } else {
                this.indices[slot] = 0;
            }
        }

        void remove(final int index) {
            for (int i = 0; i < this.indices.length; i++) {
                final int current = this.indices[i] & 0xff;
                if (current == index + 1) {
                    this.indices[i] = 0;
                } else if (current > index + 1) {
                    this.indices[i]--;
                }
            }
        }

        int get(final String key) {
            // subtract 1, 0 stands for empty
            return (this.indices[getSlot(key)] & 0xff) - 1;
        }

        private int getSlot(final String key) {
            return key.hashCode() & this.indices.length - 1;
        }

        private void clear() {
            Arrays.fill(this.indices, (byte) 0);
        }
    }
}
