package xjs.core;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xjs.serialization.JsonSerializationContext;
import xjs.serialization.writer.XjsWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
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
    protected int linesAbove;
    protected int linesBetween;
    protected int flags;
    protected @Nullable CommentHolder comments;
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
        this.linesAbove = -1;
        this.linesBetween = -1;
        this.flags = JsonFlags.NULL;
        this.comments = null;
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
     * Gets the number of newline characters <em>above</em> this value.
     *
     * <p>For example, in the following JSON data:
     *
     * <pre>{@code
     *   {
     *     "a": 1,
     *
     *     "b": 2
     *   }
     * }</pre>
     *
     * <p><code>b</code> has <em>2</em> newlines above it.
     *
     * @return The number of newline characters.
     */
    public int getLinesAbove() {
        return this.linesAbove;
    }

    /**
     * Sets the number of newline characters above this value.
     *
     * @param linesAbove The number of newline characters above the value.
     * @return <code>this</code>, for method chaining.
     * @see #getLinesAbove()
     */
    public JsonReference setLinesAbove(final int linesAbove) {
        this.linesAbove = linesAbove;
        return this;
    }

    /**
     * Gets the number of newline characters between this value and its key, if
     * applicable.
     *
     * <p>For example, the in the following JSON data:
     *
     * <pre>{@code
     *   {
     *     "k":
     *       "v"
     *   }
     * }</pre>
     *
     * <p><code>k</code> has <em>1</em> line between.
     *
     * @return The number of newline characters between this value and its key.
     */
    public int getLinesBetween() {
        return this.linesBetween;
    }

    /**
     * Sets the number of newline characters between this value and its key, if
     * applicable.
     *
     * @param linesBetween The number of newline characters between this value
     *                     and its key.
     * @return <code>this</code>, for method chaining.
     * @see #getLinesBetween()
     */
    public JsonReference setLinesBetween(final int linesBetween) {
        this.linesBetween = linesBetween;
        return this;
    }

    /**
     * Gets all field flags used by any JEL expressions.
     *
     * @return All flags configured for this field, as an integer.
     */
    @MagicConstant(flagsFromClass = JsonFlags.class)
    public int getFlags() {
        return this.flags;
    }

    /**
     * Sets all field flags used by any JEL expressions.
     *
     * @param flags All flags configured for this field, as an integer.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference setFlags(
            final @MagicConstant(flagsFromClass = JsonFlags.class) int flags) {
        this.flags = flags;
        return this;
    }

    /**
     * Indicates whether any number of flags are set for this value.
     *
     * @param flag The flag or flags being queried.
     * @return Whether each flag in the given integer is present.
     */
    public boolean hasFlag(
            final @MagicConstant(flagsFromClass = JsonFlags.class) int flag) {
        return (this.flags & flag) == flag;
    }

    /**
     * Appends any number of flags to this value, to be used by JEL expressions.
     *
     * @param flag The flag or flags to be set.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference addFlag(
            final @MagicConstant(flagsFromClass = JsonFlags.class) int flag) {
        this.flags &= ~JsonFlags.NULL;
        this.flags |= flag;
        return this;
    }

    /**
     * Removes any number of flags from this value.
     *
     * @param flag The flag or flags to be unset.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference removeFlag(
            final @MagicConstant(flagsFromClass = JsonFlags.class) int flag) {
        this.flags &= ~flag;
        return this;
    }

    /**
     * Gets a handle on the comments used by this value. This handle can be to
     * append additional comments or view messages inside the comments already
     * configured for this value.
     *
     * @return The {@link CommentHolder} used by this value.
     */
    public CommentHolder getComments() {
        if (this.comments == null) {
            return this.comments = new CommentHolder();
        }
        return this.comments;
    }

    /**
     * Sets the entire handle on any comments appended to this value.
     *
     * @param comments The new comments and their data being appended.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference setComments(final @Nullable CommentHolder comments) {
        this.comments = comments;
        return this;
    }

    /**
     * Indicates whether any comments have been appended to this value.
     *
     * @return <code>true</code>, if there are any comments.
     */
    public boolean hasComments() {
        return this.comments != null && this.comments.hasAny();
    }

    /**
     * Indicates whether a specific type of comment has been appended to this
     * value.
     *
     * @param type The type of comment being queried.
     * @return <code>true</code>, if the given type of comment does exist.
     */
    public boolean hasComment(final CommentType type) {
        return this.comments != null && this.comments.has(type);
    }

    /**
     * Sets the <em>message</em> of the header comment attached to this value.
     *
     * <p>For example, when appending <code>"Header"</code> to this value, it
     * will be printed as follows:
     *
     * <pre>{@code
     *   // Header
     *   key: value
     * }</pre>
     *
     * @param text The message being appended to this value.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference setComment(final String text) {
        return this.setComment(CommentType.HEADER, JsonSerializationContext.getDefaultCommentStyle(), text);
    }

    /**
     * Sets the <em>message</em> for the given type of comment attached to this
     * value.
     *
     * @param type The type of comment being set.
     * @param text The message of the comment.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference setComment(final CommentType type, final String text) {
        return this.setComment(type, JsonSerializationContext.getDefaultCommentStyle(), text);
    }

    /**
     * Sets the <em>message</em> for the given type of comment, while also
     * selecting a specific style for the comment.
     *
     * <p>Callers should be aware that the <em>style</em> of this comment may
     * not be valid, depending on the format. In this case, it will simply be
     * overwritten, which may be expensive.
     *
     * @param type  The position of the comment being set.
     * @param style The comment style being appended.
     * @param text  The message of the comment being set.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference setComment(final CommentType type, final CommentStyle style, final String text) {
        this.getComments().set(type, style, text);
        return this;
    }

    /**
     * Sets a comment for this value while appending a number of empty lines at
     * the end.
     *
     * @param type  The position of the comment being set.
     * @param style The comment style being appended.
     * @param text  The message of the comment being set.
     * @param lines The number of new line characters to append.
     * @return <code>this</code>, for method chaining.
     * @see CommentHolder#set(CommentType, CommentStyle, String, int)
     */
    public JsonReference setComment(final CommentType type, final CommentStyle style, final String text, final int lines) {
        this.getComments().set(type, style, text, lines);
        return this;
    }

    /**
     * Inserts the given message as a comment <em>above</em> the existing header
     * comment.
     *
     * @param text The message to insert as a comment.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference prependComment(final String text) {
        this.getComments().prepend(CommentType.HEADER, JsonSerializationContext.getDefaultCommentStyle(), text);
        return this;
    }

    /**
     * Inserts the given message as a comment <em>above</em> the existing comment
     * at this position.
     *
     * @param type The type of comment being inserted.
     * @param text The message to insert as a comment.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference prependComment(final CommentType type, final String text) {
        this.getComments().prepend(type, JsonSerializationContext.getDefaultCommentStyle(), text);
        return this;
    }

    /**
     * Inserts the given message as a comment <em>below</em> the existing header
     * comment.
     *
     * @param text The message to insert as a comment.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference appendComment(final String text) {
        this.getComments().append(CommentType.HEADER, JsonSerializationContext.getDefaultCommentStyle(), text);
        return this;
    }

    /**
     * Inserts the given message as a comment <em>below</em> the existing comment
     * at this position.
     *
     * @param type The type of comment being inserted.
     * @param text The message to insert as a comment.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference appendComment(final CommentType type, final String text) {
        this.getComments().append(type, JsonSerializationContext.getDefaultCommentStyle(), text);
        return this;
    }

    /**
     * Gets the <em>message</em> of the comment for the given position.
     *
     * @param type The position of the comment being queried.
     * @return The message of the comment.
     */
    public String getComment(final CommentType type) {
        return this.getComments().get(type);
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
     * Transfers all metadata from the given reference into this reference,
     * <em>without</em> overwriting any custom settings.
     *
     * @param metadata The value containing metadata to be reused.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference setDefaultMetadata(final JsonReference metadata) {
        if (this.linesAbove < 0) this.linesAbove = metadata.linesAbove;
        if (this.linesBetween < 0) this.linesBetween = metadata.linesBetween;
        if (this.hasFlag(JsonFlags.NULL)) this.flags = metadata.flags;
        if (this.comments == null) this.comments = metadata.comments;
        return this;
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

    /**
     * Writes this value to the disk, selecting which format to use based on
     * the extension of this file.
     *
     * @param file The file being written into.
     * @throws IOException If the involved {@link FileWriter} throws an exception.
     */
    public void write(final File file) throws IOException {
        JsonSerializationContext.autoWrite(file, this);
    }

    /**
     * Writes this value into the given writer.
     *
     * @param writer The writer being written into.
     * @throws IOException If this writer throws an exception.
     */
    public void write(final Writer writer) throws IOException {
        new XjsWriter(writer, JsonSerializationContext.getDefaultFormatting()).write(this);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + this.referent.hashCode();
        result = 31 * result + this.linesAbove;
        result = 31 * result + this.linesBetween;
        result = 31 * result + this.flags;

        if (this.comments != null) {
            result = 31 * result + this.comments.hashCode();
        }
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
                && this.linesAbove == other.linesAbove
                && this.linesBetween == other.linesBetween
                && this.flags == other.flags
                && Objects.equals(this.comments, other.comments)
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
