package xjs.core;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

public class JsonReference{

    protected JsonValue referent;
    protected boolean accessed;

    public JsonReference(final @Nullable JsonValue referent) {
        this.referent = Json.nonnull(referent);
        this.accessed = false;
    }

    public @NotNull JsonValue get() {
        this.accessed = true;
        return this.referent;
    }

    public JsonReference set(final @Nullable JsonValue referent) {
        this.referent = Json.nonnull(referent);
        this.accessed = true;
        return this;
    }

    /**
     *
     * @param updater
     * @return <code>this</code>, for method chaining.
     * @apiNote Experimental - This method may get renamed at some point before release.
     */
    @ApiStatus.Experimental
    public JsonReference update(final UnaryOperator<JsonValue> updater) {
        return this.set(updater.apply(this.referent));
    }

    public JsonValue visit() {
        return this.referent;
    }

    /**
     *
     * @param referent
     * @return <code>this</code>, for method chaining.
     * @apiNote Experimental - This method may get renamed at some point before release.
     */
    @ApiStatus.Experimental
    public JsonReference mutate(final @Nullable JsonValue referent) {
        this.referent = Json.nonnull(referent);
        return this;
    }

    /**
     *
     * @param updater
     * @return <code>this</code>, for method chaining.
     * @apiNote Experimental - This method may get renamed at some point before release.
     */
    @ApiStatus.Experimental
    public JsonReference apply(final UnaryOperator<JsonValue> updater) {
        return this.mutate(updater.apply(this.referent));
    }

    public boolean isAccessed() {
        return this.accessed;
    }

    public JsonReference setAccessed(final boolean accessed) {
        this.accessed = accessed;
        return this;
    }

    public JsonReference clone(final boolean trackAccess) {
        final JsonReference clone = new JsonReference(this.referent);
        return trackAccess ? clone.setAccessed(this.accessed) : clone;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + this.referent.hashCode();
        result = 31 * result + (this.accessed ? 1 : 0);

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
                && this.accessed == other.accessed;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.referent.toString();
    }
}
