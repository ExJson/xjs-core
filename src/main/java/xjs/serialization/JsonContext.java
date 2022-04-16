package xjs.serialization;

import xjs.core.CommentStyle;
import xjs.core.Json;
import xjs.core.JsonValue;
import xjs.exception.SyntaxException;
import xjs.serialization.parser.JsonParser;
import xjs.serialization.parser.ParsingFunction;
import xjs.serialization.parser.XjsParser;
import xjs.serialization.writer.JsonWriter;
import xjs.serialization.writer.JsonWriterOptions;
import xjs.serialization.writer.WritingFunction;
import xjs.serialization.writer.XjsWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A collection of settings and serializers for automatic parsing and writing.
 *
 * <p>This context may be used to configure the behavior of this library. For
 * example, to configure the default comment style or newline character used
 * by all provided serializers:
 *
 * <pre>{@code
 *   JsonContext.setDefaultCommentStyle(CommentStyle.HASH);
 *   JsonContext.setEol("\n");
 * }</pre>
 *
 * <p>Or the formatting options used by all default serializers:
 *
 * <pre>{@code
 *   JsonContext.setDefaultFormatting(
 *     new JsonWriterOptions()
 *       .setIndent("    ")
 *       .setMaxSpacing(3))
 * }</pre>
 *
 * <p>In addition, the context is provided as a way to configure the automatic
 * format selection of {@link Json} and {@link JsonValue}:
 *
 * <pre>{@code
 *   JsonContext.addParser("yaml", file -> new MyYamlParser(file).parse());
 *   JsonContext.addWriter("yaml" (w, v, o) -> new MyYamlWriter(w, o).write(v));
 * }</pre>
 */
public class JsonContext {

    private static final Map<String, ParsingFunction> PARSERS = new ConcurrentHashMap<>();
    private static final Map<String, WritingFunction> WRITERS = new ConcurrentHashMap<>();
    private static final Map<String, String> ALIASES = new ConcurrentHashMap<>();
    private static final ParsingFunction DEFAULT_PARSER = file -> new XjsParser(file).parse();
    private static final WritingFunction DEFAULT_WRITER = (w, v, o) -> new XjsWriter(w, o).write(v);
    private static volatile String eol = System.lineSeparator();
    private static volatile CommentStyle defaultCommentStyle = CommentStyle.LINE;
    private static volatile JsonWriterOptions defaultFormatting = new JsonWriterOptions();

    /**
     * Indicates whether the xjs-compat module is provided, enabling support for
     * Hjson, JSON-C, YAML, and other foreign serializers.
     */
    public static final boolean COMPAT_AVAILABLE;

    /**
     * Indicates whether the xjs-jel module is provided, enabling support for JEL
     * expressions and data processing.
     */
    public static final boolean JEL_AVAILABLE;

    /**
     * Indicates whether the xjs-transform module is provided, enabling support for
     * data transforms and formatters.
     */
    public static final boolean TRANSFORM_AVAILABLE;

    /**
     * Adds or replaces a parser for the given format.
     *
     * <p>Note that parsing functions would ideally not get reused for multiple formats.
     * For this purpose, use {@link #registerAlias}.
     *
     * @param format The file extension corresponding to this parser.
     * @param parser A function of {@link File} -> {@link JsonValue} throwing IOException
     */
    public static void addParser(final String format, final ParsingFunction parser) {
        PARSERS.put(format.toLowerCase(), parser);
    }

    /**
     * Adds or replaces a writer for the given format.
     *
     * <p>Note that writing functions would ideally not get reused for multiple formats.
     * For this purpose, use {@link #registerAlias}.
     *
     * @param format The file extension corresponding to the parser.
     * @param writer A consumer of ({@link Writer}, {@link JsonValue}, {@link JsonWriterOptions})
     */
    public static void addWriter(final String format, final WritingFunction writer) {
        WRITERS.put(format.toLowerCase(), writer);
    }

    /**
     * Registers an alias for some other format to the context.
     *
     * <p>For example, to register <code>yml</code> as an alias of <code>yaml</code>:
     *
     * <pre>{@code
     *   JsonContext.registerAlias("yml", "yaml");
     * }</pre>
     *
     * @param alias  An alias for the expected format.
     * @param format The expected format being configured.
     */
    public static void registerAlias(final String alias, final String format) {
        ALIASES.put(alias.toLowerCase(), format.toLowerCase());
    }

    /**
     * Gets the <em>default</em> newline character configured the provided serializers.
     *
     * @return The default newline character, usually {@link System#lineSeparator()}
     */
    public static synchronized String getEol() {
        return eol;
    }

    /**
     * Sets the <em>default</em> newline character configured for the provided serializers.
     *
     * @param eol The default newline character, e.g. <code>\n</code>
     */
    public static synchronized void setEol(final String eol) {
        JsonContext.eol = eol;
    }

    /**
     * Gets the <em>default</em> newline character used by {@link JsonValue#setComment(String)}.
     *
     * @return The configured {@link CommentStyle}.
     */
    public static synchronized CommentStyle getDefaultCommentStyle() {
        return defaultCommentStyle;
    }

    /**
     * Sets the <em>default</em> newline character used by {@link JsonValue#setComment(String)}.
     *
     * @param style The configured {@link CommentStyle}.
     */
    public static synchronized void setDefaultCommentStyle(final CommentStyle style) {
        defaultCommentStyle = style;
    }

    /**
     * Gets the <em>default</em> formatting options used by the provided serializers.
     *
     * @return The default {@link JsonWriterOptions}.
     */
    public static synchronized JsonWriterOptions getDefaultFormatting() {
        return new JsonWriterOptions(defaultFormatting);
    }

    /**
     * Sets the <em>default</em> formatting options used by the provided serializers.
     *
     * @param options The default {@link JsonWriterOptions}.
     */
    public static synchronized void setDefaultFormatting(final JsonWriterOptions options) {
        defaultFormatting = options;
    }

    /**
     * Indicates whether the given file is extended with a known format or alias.
     *
     * @param file The file being tested.
     * @return <code>true</code>, if the extension is recognized by the context.
     */
    public static boolean isKnownFormat(final File file) {
        final String ext = getExtension(file);
        return PARSERS.containsKey(ext) || ALIASES.containsKey(ext);
    }

    /**
     * Parses the given file automatically based on its extension.
     *
     * <p>This method is the delegate of {@link Json#parse(File)}.
     *
     * @param file The file being parsed as some kind of JSON file or superset.
     * @return The {@link JsonValue} represented by the file.
     * @throws IOException If the underlying {@link FileReader} throws an exception.
     * @throws SyntaxException If the contents of the file are syntactically invalid.
     */
    public static JsonValue autoParse(final File file) throws IOException {
        return PARSERS.getOrDefault(getFormat(file), DEFAULT_PARSER).parse(file);
    }

    /**
     * Writes the given file automatically based on its extension.
     *
     * <p>This method is the delegate of {@link JsonValue#write(File)}.
     *
     * @param file  The file being written as some kind of JSON file or superset.
     * @param value The {@link JsonValue} to be represented by the file.
     * @throws IOException If the underlying {@link FileWriter} throws an exception.
     */
    public static void autoWrite(final File file, final JsonValue value) throws IOException {
        final Writer writer = new FileWriter(file);
        WRITERS.getOrDefault(getFormat(file), DEFAULT_WRITER).write(writer, value, defaultFormatting);
        writer.flush();
    }

    private static String getFormat(final File file) {
        final String ext = getExtension(file);
        return ALIASES.getOrDefault(ext, ext);
    }

    private static String getExtension(final File file) {
        final int index = file.getName().lastIndexOf('.');
        return index < 0 ? "xjs" : file.getName().substring(index + 1).toLowerCase();
    }

    static {
        PARSERS.put("json", file -> new JsonParser(file).parse());
        PARSERS.put("xjs", DEFAULT_PARSER);
        WRITERS.put("json", (w, v, o) -> new JsonWriter(w, o).write(v));
        WRITERS.put("xjs", DEFAULT_WRITER);

        COMPAT_AVAILABLE = isClassAvailable("xjs.serialization.XjsCompat");
        JEL_AVAILABLE = isClassAvailable("xjs.jel.JelContext");
        TRANSFORM_AVAILABLE = isClassAvailable("xjs.transform.JsonTransformer");
    }

    private static boolean isClassAvailable(final String name) {
        try {
            Class.forName(name);
            return true;
        } catch (final ClassNotFoundException ignored) {}
        return false;
    }
}
