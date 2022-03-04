package xjs.core;

/**
 * A handful of modifiers providing alternate behaviors and QOL parsing
 * changes for JEL expressions.
 *
 * Todo: move to xjs-jel
 */
public final class JsonFlags {

    private JsonFlags() {}

    /**
     * This value is to be used only by the config itself and should
     * not be exposed to the parsing application.
     */
    public static final int VAR = 1;

    /**
     * This value has private scope and cannot be reexported to other
     * files.
     */
    public static final int PRIVATE = 1 << 1;

    /**
     * This value should only get added to an array.
     */
    public static final int ADD = 1 << 2;

    /**
     * This value should only get merged into an object and must be an
     * object.
     */
    public static final int MERGE = 1 << 3;

    /**
     * This value has been imported by some expression.
     */
    public static final int IMPORT = 1 << 4;

    /**
     * This value may contain expression-like data, but should be
     * evaluated.
     */
    public static final int NOINLINE = 1 << 5;

    /**
     * In addition to containing a regular RHS expression, this field
     * contains a config-like "meta" descriptor of the expression.
     */
    public static final int META = 1 << 6;

    /**
     * This field does not have flags and is a valid candidate to be
     * overwritten.
     */
    public static final int NULL = 1 << 31;

}
