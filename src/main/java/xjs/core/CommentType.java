package xjs.core;

/**
 * This class represents the specific type of comment to be used in conjunction with a {@link JsonValue}.
 * Comments can be placed by calling {@link CommentHolder#set(CommentType, CommentStyle, String)}
 * or another such variant.
 *
 * // todo: define specific expectations for newline characters at end of comments
 */
public enum CommentType {

    /**
     * Indicates that a comment precedes the value that it is paired with, to be placed one line
     * before the value. In the case of parent or root objects, this type indicates that the comment
     * is a header inside the json file.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   // Header comment
     *   key: value
     *
     * }</pre>
     */
    HEADER,

    /**
     * Indicates that a comment follows the value that it is paired with, to be placed at the end
     * of the line.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   key: value // EOL comment
     *
     * }</pre>
     */
    EOL,

    /**
     * Indicates that a comment is to be written on the following line after its value. For example,
     * In the case of parent or root objects, this type indicates that the comment is a footer at the
     * very bottom of the json file.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   {
     *     key: value
     *   }
     *   // Footer comment
     *
     * }</pre>
     *
     * <p>Note that a footer comment can be placed after any regular value, but upon being re-parsed,
     * it will be paired with the next value as a header.
     */
    FOOTER,

    /**
     * Indicates that a comment is to after a key, but before the value in a JSON object member.
     * This is most commonly seen when a value is written on the following line after its key.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   key:
     *     // Value comment
     *     value
     *
     * }</pre>
     */
    VALUE,

    /**
     * Indicates that a comment falls anywhere else in association with this value. This usually
     * implies that the value is inside an empty object or array.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   key: [
     *     // Interior comment
     *   ]
     *
     * }</pre>
     */
    INTERIOR
}
