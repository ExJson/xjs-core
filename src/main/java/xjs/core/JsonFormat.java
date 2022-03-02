package xjs.core;

public enum JsonFormat {

    /**
     * Prints unformatted, regular JSON with no whitespace.
     *
     * <p>No formatting options will be preserved.
     */
    JSON,

    /**
     * Pretty prints regular JSON with whitespace.
     *
     * <p>Some formatting options will be preserved.
     */
    JSON_FORMATTED,

    /**
     * Prints unformatted XJS with minimal whitespace.
     *
     * <p>No formatting options will be preserved.
     */
    XJS,

    /**
     * Pretty prints XJS with whitespace.
     *
     * <p>Some formatting options will be preserved.
     */
    XJS_FORMATTED
}
