package xjs.core;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xjs.serialization.writer.AbstractJsonWriter;
import xjs.transformer.JsonCollectors;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The JSON container is an abstraction around JSON objects and JSON arrays which exposes
 * their contents in a unified or generic context.
 *
 * <p>For example, to get the first number from any JSON container,
 *
 * <pre>{@code
 *   final Optional<JsonValue> first =
 *     container.values()
 *       .stream()
 *       .filter(JsonValue::isNumber)
 *       .findFirst();
 * }</pre>
 *
 * <p>To get an element by index:
 *
 * <pre>{@code
 *   final JsonValue first = container.get(0);
 * }</pre>
 *
 * <p>To format the container onto a single line and print it:
 *
 * <pre>{@code
 *   final String formatted =
 *     container.condense()
 *       .toString(JsonFormat.XJS_FORMATTED);
 * }</pre>
 *
 * <p>Or increment each number by 1:
 *
 * <pre>{@code
 *   container.references()
 *     .stream()
 *     .filter(ref ->
 *       ref.visit().isNumber())
 *     .forEach(ref ->
 *       ref.update(v ->
 *         Json.value(v.asDouble() + 1)));
 * }</pre>
 */
public abstract class JsonContainer extends JsonValue {

    protected final List<JsonReference> references;
    protected View<JsonValue> accessor;
    protected View<JsonValue> visitor;
    protected int linesTrailing;

    /**
     * Constructs a new container with no contents. To be used only by implementors.
     */
    protected JsonContainer() {
        this(new ArrayList<>());
    }

    /**
     * Constructs a new container from an existing set of {@link JsonReference references}.
     * To bne used only be implementors.
     *
     * @param references An existing set of references, thus preventing any access or
     *                   mutation.
     */
    protected JsonContainer(final List<JsonReference> references) {
        this.references = references;
        this.linesTrailing = -1;
    }

    /**
     * Gets the number of newline characters to be printed before the very end of this
     * container.
     *
     * <p>For example, an array with 0 lines trailing will be printed as follows:
     *
     * <pre>{@code
     *   []
     * }</pre>
     *
     * <p>However, an array with 2 lines trailing will be printed with a single empty
     * line after the final value:
     *
     * <pre>{@code
     *   [
     *     1, 2, 3
     *
     *   ]
     * }</pre>
     *
     * <p>Note that, when this value has not been configured, it will default to -1. In
     * this case, the formatting will be determined on a contextual basis by the
     * {@link AbstractJsonWriter writer}.
     *
     * @return The number of newline characters at the very end of this container.
     * @apiNote Experimental - may get removed or be implemented differently in a future
     *          release.
     */
    @ApiStatus.Experimental
    public int getLinesTrailing() {
        return this.linesTrailing;
    }

    /**
     * Sets the number of newline characters to be printed before the very end of this
     * container.
     *
     * @param linesTrailing The number of newline characters print.
     * @return <code>this</code>, for method chaining.
     * @see #getLinesTrailing()
     * @apiNote Experimental - may get removed or be implemented differently in a future
     *          release.
     */
    @ApiStatus.Experimental
    public JsonContainer setLinesTrailing(final int linesTrailing) {
        this.linesTrailing = linesTrailing;
        return this;
    }

    /**
     * Formats this array to display the given number of references on each line.
     *
     * <p>This operation is valid only when every expected value <b>already exists
     * inside of the container</b>. It is provided for convenience here, but due to its
     * nature, it may be moved into a dedicated JSON formatting utility in the future.
     *
     * <p>This is a {@link JsonReference#visit visiting} operation.
     *
     * @param lineLength The number of elements to output on each line.
     * @return <code>this</code>, for method chaining.
     * @apiNote Experimental - may get removed or be implemented differently in a future
     *          release.
     */
    @ApiStatus.Experimental
    public JsonContainer setLineLength(final int lineLength) {
        for (int i = 0; i < this.references.size(); i++) {
            if ((i % lineLength) == 0) {
                this.references.get(i).visit().setLinesAbove(1).setLinesBetween(0);
            } else {
                this.references.get(i).visit().setLinesAbove(0).setLinesBetween(0);
            }
        }
        return this;
    }

    /**
     * Formats this container to place each element on the same line.
     *
     * <p>This operation is valid only when every expected value <b>already exists
     * inside of the array</b>. It is provided for convenience here, but due to its
     * nature, it may be moved into a dedicated JSON formatting utility in the future.
     *
     * @return <code>this</code>, for method chaining.
     * @apiNote Experimental - may get removed or be implemented differently in a future
     *          release.
     */
    @ApiStatus.Experimental
    public JsonContainer condense() {
        for (final JsonReference reference : this.references) {
            reference.visit().setLinesAbove(0).setLinesBetween(0);
        }
        return this;
    }

    /**
     * Returns a view of each reference in this container. Callers should be aware that
     * the return value of this method is <b>immutable</b>. However, callers may
     * <em>replace</em> values within this collection by mutating the references directly.
     *
     * <pre>{@code
     *   container.references()
     *     .get(0)
     *     .set(Json.value(1234))
     * }</pre>
     *
     * <p>Alternatively, the references can be collected into a <em>new</em> container:
     *
     * <pre>{@code
     *   final JsonContainer collected =
     *     container.references()
     *       .stream()
     *       .filter(ref -> ref.visit().isNumber())
     *       .collect(JsonCollectors.reference());
     * }</pre>
     *
     * @return An immutable view of the references being wrapped by this container.
     */
    public Collection<JsonReference> references() {
        return Collections.unmodifiableCollection(this.references);
    }

    /**
     * Performs a simple bounds check on the given index. If a value does exist, this
     * returns <code>true</code>, else it returns <code>false</code>.
     *
     * @param index The index of the value being inspected.
     * @return <code>true</code>, if the given index is in bounds.
     */
    public boolean has(final int index) {
        return index >= 0 && index < this.references.size();
    }

    /**
     * Returns whichever value is being stored at the given index, or else throws an
     * {@link IndexOutOfBoundsException}.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param index The index of the value being inspected.
     * @return Whichever {@link JsonValue} is at the give index.
     * @throws IndexOutOfBoundsException If the given index falls out of bounds.
     */
    public JsonValue get(final int index) {
        return this.references.get(index).get();
    }

    /**
     * Returns whatever data is stored at the given index, if it matches the filter.
     * If the value exists, and it matches the given filter, a value will be returned.
     * Otherwise, this method returns {@link Optional#empty}.
     *
     * <p>For example, to retrieve string value at index 0:
     *
     * <pre>{@code
     *   final String s = container.getOptional(0, JsonFilter.STRING).orElse("");
     * }</pre>
     *
     * <p>In the previous example, <b>only the happy path will be acknowledged</b>.
     * Missing values and unexpected data types will both simply return empty.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param index  The index of the value being inspected.
     * @param filter A filter to extra the type of data being expected.
     * @param <T>    The type of value being returned by this method.
     * @return The expected data, or else {@link Optional#empty}.
     */
    @ApiStatus.Experimental
    public <T> Optional<T> getOptional(final int index, final JsonFilter<T> filter) {
        return this.getOptional(index).flatMap(filter::applyOptional);
    }

    /**
     * Returns the value stored at the given index. If the given index is out of bounds,
     * returns {@link Optional#empty}.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param index The index of the value being inspected.
     * @return The expected value, or else {@link Optional#empty}.
     */
    public Optional<JsonValue> getOptional(final int index) {
        if (index >= 0 && index < this.references.size()) {
            return Optional.of(this.references.get(index).get());
        }
        return Optional.empty();
    }

    /**
     * Returns a <em>{@link JsonReference reference}</em> to the value at the given index,
     * or else throws an {@link IndexOutOfBoundsException}.
     *
     * @param index The index of the value being inspected.
     * @return A reference to this value, which can be reassigned or shared elsewhere.
     * @throws IndexOutOfBoundsException If the given index falls out of bounds.
     */
    public JsonReference getReference(final int index) {
        return this.references.get(index);
    }

    /**
     * Returns the number of values in this container.
     *
     * @return The number of values in this container.
     */
    public int size() {
        return this.references.size();
    }

    /**
     * Returns whether this container has no values in it.
     *
     * @return <code>true</code>, if the container has no values.
     */
    public boolean isEmpty() {
        return this.references.isEmpty();
    }

    /**
     * Returns whether any of the values in this container matches the given integer.
     *
     * @param value The value being queried in this container.
     * @return <code>true</code>, if a match is found.
     * @see #contains(JsonValue)
     */
    public boolean contains(final long value) {
        return this.contains(Json.value(value));
    }

    /**
     * Returns whether any of the values in this container matches the given decimal.
     *
     * @param value The value being queried in this container.
     * @return <code>true</code>, if a match is found.
     * @see #contains(JsonValue)
     */
    public boolean contains(final double value) {
        return this.contains(Json.value(value));
    }

    /**
     * Returns whether any of the values in this container matches the given boolean.
     *
     * @param value The value being queried in this container.
     * @return <code>true</code>, if a match is found.
     * @see #contains(JsonValue)
     */
    public boolean contains(final boolean value) {
        return this.contains(Json.value(value));
    }

    /**
     * Returns whether any of the values in this container matches the given string.
     *
     * @param value The value being queried in this container.
     * @return <code>true</code>, if a match is found.
     * @see #contains(JsonValue)
     */
    public boolean contains(final @Nullable String value) {
        return this.contains(Json.value(value));
    }

    /**
     * Returns whether any of the values in this container equals the input.
     *
     * <p>Note that, due to the contract of {@link Object#equals}, whether a match is found
     * is sensitive to the metadata associated with the values.
     *
     * <p>To demonstrate, the following is an assertion of the expected behaviors:
     *
     * <pre>{@code
     *   assert Json.array(1, 2, 3).contains(1);
     *   assert !Json.parse("[1, 2, 3]").asArray().contains(1);
     *   assert Json.parse("[1, 2, 3]").asArray().unformatted().contains(1);
     * }</pre>
     *
     * @param value The value being queried in this container.
     * @return <code>true</code>, if an exact match is found.
     */
    public boolean contains(final JsonValue value) {
        return this.indexOf(value) >= 0;
    }

    /**
     * Returns whether each value in the given iterable can also be found in this container.
     *
     * @param values An iterable of {@link JsonValue values} to be queried against.
     * @return <code>true</code>, only if <b>exact</b> matches are found for each value.
     * @see #contains(JsonValue)
     */
    public boolean containsAll(final Iterable<JsonValue> values) {
        for (final JsonValue value : values) {
            if (!this.contains(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Exposes a {@link View view} of the values in this container.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @return An {@link AccessingView accessing view} which may be iterated or streamed.
     */
    public View<JsonValue> values() {
        if (this.accessor == null) {
            return this.accessor = new AccessingView();
        }
        return this.accessor;
    }

    /**
     * Variant of {@link #values} intended for reflective purposes. This may be needed for
     * any user wishing to track which values do and do not get used by their application.
     *
     * <p>This is a {@link JsonReference#visit visiting} operation.
     *
     * @return An {@link AccessingView accessing view} which may be iterated or streamed.
     */
    public View<JsonValue> visitAll() {
        if (this.visitor == null) {
            return this.visitor = new VisitingView();
        }
        return this.visitor;
    }

    /**
     * Exposes a {@link View view} of each element in this container along with its accessor,
     * if applicable.
     *
     * Iterating in this manor <em>does</em> allow values to be mutated concurrently.
     *
     * @return A {@link View view} of each element in this container and its accessor(s).
     * @see Access
     */
    @ApiStatus.Experimental
    public abstract View<? extends Access> view();

    /**
     * Recursively executes some action for each value in this container. In other words, for
     * any container nested <em>inside</em> of this container, each of its elements will also
     * be exposed to the consumer.
     *
     * <p>Because this method exposes references to the consumer instead of raw values, this
     * means the data may be mutated in place at any level in the container. For example:
     *
     * <pre>{@code
     *   // Transform numbers into strings of numbers.
     *   container.forEachRecursive(ref -> {
     *      if (ref.visit().isNumber()) {
     *          ref.apply(JsonValue::intoString);
     *      }
     *   });
     * }</pre>
     *
     * <p>This is a {@link JsonReference#visit visiting} operation.
     *
     * @param fn An action to execute for each value in this container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonContainer forEachRecursive(final Consumer<JsonReference> fn) {
        this.references.forEach(ref -> {
            fn.accept(ref);
            if (ref.visit().isContainer()) {
                ref.visit().asContainer().forEachRecursive(fn);
            }
        });
        return this;
    }

    /**
     * Removes every reference from this container. The container will be empty after this
     * operation completes.
     *
     * @return <code>this</code>, for method chaining.
     */
    public JsonContainer clear() {
        this.references.clear();
        return this;
    }

    /**
     * Removes a single reference from this container if its contents match the given value.
     *
     * @param value The value being compared against.
     * @return <code>this</code>, for method chaining.
     */
    public JsonContainer remove(final JsonValue value) {
        final int index = this.indexOf(value);
        if (index >= 0) {
            this.references.remove(index);
        }
        return this;
    }

    /**
     * Removes a collection of references from this container.
     *
     * @param values An iterable providing values to be removed.
     * @return <code>this</code>, for method chaining.
     */
    public JsonContainer removeAll(final Iterable<JsonValue> values) {
        values.forEach(this::remove);
        return this;
    }

    /**
     * Recursively updates the access flags for every reference in this container.
     *
     * @param accessed Whether each reference should be marked as accessed.
     * @return <code>this</code>, for method chaining.
     */
    public JsonContainer setAllAccessed(final boolean accessed) {
        return this.forEachRecursive(ref -> ref.setAccessed(accessed));
    }

    /**
     * Gets the index of the first matching element in the container, or else -1.
     *
     * @param value The value being compared against.
     * @return The index of the first matching element, or else -1.
     */
    public int indexOf(final JsonValue value) {
        for (int i = 0; i < this.references.size(); i++) {
            if (this.references.get(i).visit().matches(value)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Converts this container into a list of type <code>T</code>.
     *
     * @param mapper A mapper for converting {@link JsonValue JSON values} into <code>T</code>.
     * @param <T>    The type of list being returned by this method.
     * @return A list containing the transformed values.
     */
    public <T> List<T> toList(final Function<JsonValue, T> mapper) {
        return this.values().stream().map(mapper).collect(Collectors.toList());
    }

    /**
     * Override indicating that JSON containers are <em>not</em> primitive types.
     *
     * @return <code>false</code>, always.
     */
    @Override
    public boolean isPrimitive() {
        return false;
    }

    /**
     * Override indicating that all {@link JsonContainer} implementations <em>are</em> containers.
     *
     * @return <code>true</code>, always.
     */
    @Override
    public final boolean isContainer() {
        return true;
    }

    /**
     * Override allowing this type to be cast into a JSON container.
     *
     * @return <code>this</code>, always.
     */
    @Override
    public JsonContainer asContainer() {
        return this;
    }

    /**
     * Allows any {@link JsonContainer} type to be coerced into a number value from its size.
     *
     * <p><b>Note:</b> this implementation is very likely to change in a future release.
     *
     * @return The size of this array.
     * @apiNote Experimental - may get removed or be implemented differently in a future
     *          release.
     */
    @Override
    @ApiStatus.Experimental
    public double intoDouble() {
        return this.size();
    }

    /**
     * Override returning this value directly instead of constructing a new container.
     *
     * @return <code>this</code>, always
     */
    @Override
    public JsonContainer intoContainer() {
        return this;
    }

    /**
     * Override requiring all {@link JsonContainer} implementations to manually implement this
     * behavior.
     *
     * @return Either <code>this</code> or a new {@link JsonObject}.
     */
    @Override
    @ApiStatus.Experimental
    public abstract JsonObject intoObject();

    /**
     * Override requiring all {@link JsonContainer} implementations to manually implement this
     * behavior.
     *
     * @return Either <code>this</code> or a new {@link JsonArray}.
     */
    @Override
    @ApiStatus.Experimental
    public abstract JsonArray intoArray();

    /**
     * Generates a shallow copy of this container. This operation yields a new
     * container housing the exact same references. Any other containers in this
     * object will be shallow copied recursively, but regular values will simply
     * be reused and are not safe to be mutually updated.
     *
     * @return A <em>shallow</em> copy of this container.
     */
    public abstract JsonContainer shallowCopy();

    /**
     * Generates a deep copy of this container. This operation yields a new
     * container housing the exact same references. Any other containers in this
     * object will be deep copied recursively. However, the new object will be
     * entirely flagged as {@link JsonReference#setAccessed unused}.
     *
     * @return a <em>deep</em> copy of this container.
     */
    public abstract JsonContainer deepCopy();

    /**
     * Generates a deep copy of this container. This operation yields a new
     * container housing the exact same references. Any other containers in this
     * object will be deep copied recursively.
     *
     * @param trackAccess Whether to additionally copy access flags.
     * @return a <em>deep</em> copy of this container.
     */
    public abstract JsonContainer deepCopy(final boolean trackAccess);

    /**
     * Generates a deep copy of this container without persisting any formatting
     * info or similar metadata.
     *
     * @return a <em>deep</em>, unformatted copy of this container.
     */
    public abstract JsonContainer unformatted();

    /**
     * Override indicating that subclasses must still implement this method.
     *
     * @param other The value being compared to.
     * @return <code>true</code>, if the two values match.
     */
    @Override
    public abstract boolean matches(final JsonValue other);

    /**
     * Generates an immutable view of this container in which values may not be added
     * or replaced.
     *
     * @return The frozen equivalent of this container.
     */
    public JsonContainer freeze() {
        return this.freeze(false);
    }

    /**
     * Generates an immutable view of this container in which values may not be added
     * or replaced.
     *
     * @param recursive Whether to freeze this container recursively.
     * @return The frozen equivalent of this container.
     */
    public abstract JsonContainer freeze(final boolean recursive);

    /**
     * Generates a list of JSON path strings indicating which paths <em>have not</em>
     * been used.
     *
     * @return A list of JSON path strings for each unused value.
     */
    public List<String> getUnusedPaths() {
        return this.getUsedPaths(false);
    }

    /**
     * Generates a list of JSON path strings indicating which paths <em>have</em>
     * been used.
     *
     * @return A list of JSON path strings for each used value.
     */
    public List<String> getUsedPaths() {
        return this.getUsedPaths(true);
    }

    /**
     * Generates a list of JSON path strings indicating which paths have or have not
     * been used.
     *
     * @return A list of JSON path strings for each used or unused value.
     */
    public abstract List<String> getUsedPaths(final boolean used);

    /**
     * Generates an immutable list of the references in this container.
     *
     * @param recursive Whether to freeze references recursively.
     * @return The frozen list.
     */
    protected List<JsonReference> freezeReferences(final boolean recursive) {
        final List<JsonReference> frozen =
            this.references.stream()
                .map(ref -> {
                    final JsonReference clone = ref.clone(true);
                    if (recursive && ref.visit().isContainer()) {
                        ref.apply(value -> value.asContainer().freeze(true));
                    }
                    return clone.freeze();
                })
                .collect(Collectors.toList());
        return Collections.unmodifiableList(frozen);
    }

    /**
     * A {@link View view} of this container which {@link JsonReference#get accesses}
     * each value as it gets returned to the consumer. This implies that every value
     * in the container is needed (i.e. not technically <em>ignored</em>) by the
     * application, which is significant for diagnostic purposes.
     */
    private class AccessingView implements View<JsonValue> {
        @Override
        public @NotNull Iterator<JsonValue> iterator() {
            return new AccessingValueIterator();
        }
    }

    /**
     * Counterpart to {@link AccessingView} used for the actual iteration of JSON values.
     */
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

    /**
     * A {@link View view} of this container which {@link JsonReference#visit visits}
     * each value as it gets returned to the consumer. This implies that the actions
     * are intended for the purpose of reflecting on the values in the container. For
     * example, to search for a value or reformat the container.
     */
    private class VisitingView implements View<JsonValue> {
        @Override
        public @NotNull Iterator<JsonValue> iterator() {
            return new VisitingValueIterator();
        }
    }

    /**
     * Counterpart to {@link VisitingView} used for the actual iteration of JSON values.
     */
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

    /**
     * This object represents a handle on a single value in this container, as well as
     * its accessor--e.g. its index or key--if applicable. It may be used to reflect on
     * the contents of a container and, additionally, can be {@link Collector collected}
     * in to a new container via {@link JsonCollectors}.
     *
     * <p><b>Note</b>: The exact implementation of this type is a particular source of
     * <em>bloat</em> being investigated for removal (or consolidation) in a future preview.
     * Outside of {@link JsonObject.Member}, <b>its use should generally be avoided.</b>
     *
     * @apiNote Experimental - may get removed or be implemented differently in a future
     *          release.
     */
    @ApiStatus.Experimental
    public static abstract class Access {
        protected JsonReference reference;

        /**
         * Constructs a new value accessor by providing its only essential data piece: the
         * {@link JsonReference}.
         *
         * @param reference The reference being wrapped by this accessor.
         * @apiNote This implementation is somewhat superfluous. It is possible that it
         *          will either be expanded upon to provide additional functionality in the
         *          future. Otherwise, it will simply be removed.
         */
        protected Access(final JsonReference reference) {
            this.reference = reference;
        }

        /**
         * Returns the value being wrapped by this accessor.
         *
         * <p>This is an {@link JsonReference#get accessing} operation.
         *
         * @return The value being wrapped.
         */
        public @NotNull JsonValue getValue() {
            return this.reference.get();
        }

        /**
         * Updates the value being wrapped by this accessor. This operation will cascade and
         * consequently replace the original value in the parent container.
         *
         * <p>This is an {@link JsonReference#get accessing} operation.
         *
         * @param value The new value to be wrapped.
         * @return <code>this</code>, for method chaining.
         */
        public Access setValue(final @Nullable JsonValue value) {
            this.reference.set(value);
            return this;
        }

        /**
         * Returns the value being wrapped by this accessor <em>without</em> updating its
         * access flags.
         *
         * <p>This is a {@link JsonReference#visit visiting} operation.
         *
         * @return The value being wrapped.
         */
        public @NotNull JsonValue visit() {
            return this.reference.visit();
        }

        /**
         * Updates the value being wrapped by this accessor <em>without</em> updating its
         * access flags.
         *
         * <p>This is a {@link JsonReference#visit visiting} operation.
         *
         * @param value The new value to be wrapped.
         * @return <code>this</code>, for method chaining.
         */
        public Access mutate(final @Nullable JsonValue value) {
            this.reference.mutate(value);
            return this;
        }

        /**
         * Directly exposes the reference to the value being wrapped by this accessor.
         *
         * @return A reference to the wrapped value.
         */
        public JsonReference getReference() {
            return this.reference;
        }
    }

    /**
     * An accessor to this container which may be {@link Iterator iterated} or
     * {@link Stream streamed}. Its main purpose is not to transform this container,
     * but to <em>inspect</em> its contents. In other words, to <em>view</em> them.
     *
     * @param <T> The type of value being exposed by the view.
     */
    public interface View<T> extends Iterable<T> {
        default Stream<T> stream() {
            return StreamSupport.stream(this.spliterator(), false);
        }
    }
}
