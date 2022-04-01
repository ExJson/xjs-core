package xjs.core;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

/**
 * The JSON reference is an accessor to a {@link JsonValue} which can be made
 * available to multiple containers. It is primarily intended to facilitate
 * JEL expressions, but may also be useful for general-purpose data transforms.
 *
 * <p>For example, to increment each number in a {@link JsonArray JSON array}:
 *
 * <pre>{@code
 *   array.references()
 *     .stream()
 *     .filter(ref ->
 *       ref.visit().isNumber())
 *     .forEach(ref ->
 *       ref.apply(n ->
 *         Json.value(n.asDouble() + 1)));
 * }</pre>
 *
 * <p>To share references between containers, thus synchronizing changes between
 * them:
 *
 * <pre>{@code
 *   final JsonArray a1 = Json.array(1, 2, 3);
 *   final JsonArray a2 = Json.array(4, 5, 6);
 *
 *   a1.setReference(0, a2.getReference(0));
 *   a1.set(0, 7);
 *
 *   assert Json.array(7, 2, 3).equals(a1);
 *   assert Json.array(7, 5, 6).equals(a2);
 * }</pre>
 *
 * <h2>A note about future API design:</h2>
 *
 * <p>In the future, this API will almost certainly change due to its odd ergonomic
 * behaviors. The distinction between {@link #get} and {@link #visit} in itself is
 * fine, but additional variants of {@link #set}, {@link #update}, and any other
 * accessors needed here mean that we have to arbitrarily double its vernacular.
 *
 * <p>In the future, we may attach a lazily-initialized <code>Visitor</code> to this
 * object. Its API might work something like this:
 *
 * <pre>{@code
 *   final JsonReference reference = new JsonReference(Json.value(1234));
 *   // Get accessing:
 *   final JsonValue accessed = reference.get();
 *   // Get visiting:
 *   final JsonValue visited = reference.visitor().get();
 * }</pre>
 *
 * <p>However, such an API also has its limitations, as it requires a bit of
 * significant code duplication for each of the accessors. For this reason, a new
 * design is still being worked out.
 */
public class JsonReference {

    protected JsonValue referent;
    protected boolean accessed;
    protected boolean mutable;

    /**
     * Construct a new reference when given a value to wrap.
     *
     * <p>Note that this value may be <code>null</code>, in which case it will
     * simply be wrapped as {@link JsonLiteral#jsonNull}.
     *
     * @param referent The value being wrapped.
     */
    public JsonReference(final @Nullable JsonValue referent) {
        this.referent = Json.nonnull(referent);
        this.accessed = false;
        this.mutable = true;
    }

    /**
     * Returns the value being wrapped by this object.
     *
     * <p>Calling this method implies that the referent is required by the application
     * in some way. For example, to be {@link JsonValue#unwrap unwrapped} and treated
     * as raw data, <em>not</em> JSON data. The alternative would be to {@link #visit}
     * the data, which implies that we are using it to update formatting or simply
     * find a value which we <em>do</em> need.
     *
     * <p>We call this an <em>"accessing"</em> operation.
     *
     * <p>Quite literally, this means that the value will be flagged as "accessed,"
     * which can be reflected upon at a later time to provide diagnostics to the end
     * user or to investigate potential optimizations regarding unused values.
     *
     * @return The referent
     * @apiNote Experimental - This method may get renamed at some point before release.
     */
    @ApiStatus.Experimental
    public @NotNull JsonValue get() {
        this.accessed = true;
        return this.referent;
    }

    /**
     * Points this reference toward a different {@link JsonValue}.
     *
     * <p>This is an {@link #get accessing} operation.
     *
     * @param referent The new referent being wrapped by this object.
     * @return <code>this</code>, for method chaining.
     * @throws UnsupportedOperationException If this reference is immutable.
     * @apiNote Experimental - This method may get renamed at some point before release.
     * @see #get
     */
    @ApiStatus.Experimental
    public JsonReference set(final @Nullable JsonValue referent) {
        this.checkMutable();
        this.referent = Json.nonnull(referent);
        this.accessed = true;
        return this;
    }

    /**
     * Applies the given transformation to the referent of this object.
     *
     * <p>This is an {@link #get accessing} operation.
     *
     * @param updater An expression transforming the wrapped {@link JsonValue}.
     * @return <code>this</code>, for method chaining.
     * @throws UnsupportedOperationException If this reference is immutable.
     * @apiNote Experimental - This method may get renamed at some point before release.
     * @see #get
     */
    @ApiStatus.Experimental
    public JsonReference update(final UnaryOperator<JsonValue> updater) {
        return this.set(updater.apply(this.referent));
    }

    /**
     * Returns the value being wrapped by this object.
     *
     * <p>Calling this method does not imply that the referent is required by the
     * application. Instead, the value is being used for the purpose of <em>reflection
     * </em>. For example, to inspect or update formatting options or scan for matching
     * values. Philosophically speaking, this means that the value could be removed
     * without changing the behavior of the application in any significant way.
     *
     * <p>We call this a <em>"visiting"</em> operation.
     *
     * <p>Literally speaking, this operation avoids updating this value's access flags,
     * which means the value will still be in an "unused" state. This can be reflected
     * upon at a later time to provide diagnostics to the end user or to investigate
     * potential optimizations regarding unused values.
     *
     * @return The referent
     * @apiNote Experimental - This method may get renamed at some point before release.
     */
    @ApiStatus.Experimental
    public JsonValue visit() {
        return this.referent;
    }

    /**
     * Visiting counterpart of {@link #set}.
     *
     * <p>This is a {@link #visit visiting} operation.
     *
     * @param referent The new referent being wrapped by this object.
     * @return <code>this</code>, for method chaining.
     * @throws UnsupportedOperationException If this reference is immutable.
     * @apiNote Experimental - This method may get renamed at some point before release.
     * @see #visit
     */
    @ApiStatus.Experimental
    public JsonReference mutate(final @Nullable JsonValue referent) {
        this.checkMutable();
        this.referent = Json.nonnull(referent);
        return this;
    }

    /**
     * Visiting counterpart of {@link #update}.
     *
     * <p>This is a {@link #visit visiting} operation.
     *
     * @param updater An expression transforming the wrapped {@link JsonValue}.
     * @return <code>this</code>, for method chaining.
     * @throws UnsupportedOperationException If this reference is immutable.
     * @apiNote Experimental - This method may get renamed at some point before release.
     * @see #visit
     */
    @ApiStatus.Experimental
    public JsonReference apply(final UnaryOperator<JsonValue> updater) {
        return this.mutate(updater.apply(this.referent));
    }

    /**
     * Indicates whether this reference has been {@link #get accessed}.
     *
     * @return <code>true</code>, if the value has been accessed.
     */
    public boolean isAccessed() {
        return this.accessed;
    }

    /**
     * Overrides the access flag for this reference.
     *
     * @param accessed Whether the value has been {@link #get accessed}.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference setAccessed(final boolean accessed) {
        this.accessed = accessed;
        return this;
    }

    /**
     * Indicates whether this reference may be updated.
     *
     * @return <code>true</code>, if the reference may be updated.
     */
    public boolean isMutable() {
        return this.mutable;
    }

    /**
     * Freezes this reference into an immutable state.
     *
     * <p><b>This operation is permanent</b>.
     *
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference freeze() {
        this.mutable = false;
        return this;
    }

    private void checkMutable() {
        if (!this.mutable) {
            throw new UnsupportedOperationException("Reference is immutable: " + this);
        }
    }

    /**
     * Generates a mutable clone of this reference.
     *
     * @param trackAccess Whether to additionally persist access tracking.
     * @return A copy of this reference.
     */
    public JsonReference clone(final boolean trackAccess) {
        final JsonReference clone = new JsonReference(this.referent);
        return trackAccess ? clone.setAccessed(this.accessed) : clone;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + this.referent.hashCode();
        if (this.accessed) result *= 17;
        if (this.mutable) result *= 31;
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof JsonReference) {
            final JsonReference other = (JsonReference) o;
            return this.referent.equals(other.referent)
                && this.accessed == other.accessed
                && this.mutable == other.mutable;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.referent.toString();
    }
}
