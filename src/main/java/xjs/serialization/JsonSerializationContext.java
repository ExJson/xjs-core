package xjs.serialization;

import xjs.core.JsonValue;
import xjs.serialization.parser.JsonParser;
import xjs.serialization.parser.XjsParser;
import xjs.serialization.writer.JsonWriter;
import xjs.serialization.writer.JsonWriterOptions;
import xjs.serialization.writer.XjsWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class JsonSerializationContext {

    private static final Map<String, ParsingFunction> PARSERS =
        new ConcurrentHashMap<>();

    private static final Map<String, WritingFunction> WRITERS =
        new ConcurrentHashMap<>();

    private static final Map<String, String> ALIASES =
        new ConcurrentHashMap<>();

    private static final AtomicReference<String> EOL =
        new AtomicReference<>(System.getProperty("line.separator"));

    private static final AtomicReference<JsonWriterOptions> DEFAULT_FORMATTING =
        new AtomicReference<>(new JsonWriterOptions());

    public static void addParser(final String format, final ParsingFunction parser) {
        PARSERS.put(format, parser);
    }

    public static void addWriter(final String format, final WritingFunction writer) {
        WRITERS.put(format, writer);
    }

    public static void registerAlias(final String alias, final String format) {
        ALIASES.put(alias, format);
    }

    public static String getEol() {
        return EOL.get();
    }

    public static void setEol(final String eol) {
        EOL.set(eol);
    }

    public static JsonWriterOptions getDefaultFormatting() {
        return new JsonWriterOptions(DEFAULT_FORMATTING.get());
    }

    public static void setDefaultFormatting(final JsonWriterOptions options) {
        DEFAULT_FORMATTING.set(options);
    }

    public static JsonValue autoParse(final File file) throws IOException {
        final String ext = getExtension(file);
        return PARSERS.get(ALIASES.getOrDefault(ext, ext)).parse(file);
    }

    public static void autoWrite(final File file, final JsonValue value) throws IOException {
        final String ext = getExtension(file);
        WRITERS.get(ALIASES.getOrDefault(ext, ext)).write(new FileWriter(file), value, DEFAULT_FORMATTING.get());
    }

    private static String getExtension(final File file) {
        final int index = file.getName().lastIndexOf('.');
        return index < 0 ? "xjs" : file.getName().substring(index + 1);
    }

    static {
        PARSERS.put("json", file -> new JsonParser(file).parse());
        PARSERS.put("xjs", file -> new XjsParser(file).parse());
        WRITERS.put("json", (w, v, o) -> new JsonWriter(w, o).write(v));
        WRITERS.put("xjs", (w, v, o) -> new XjsWriter(w, o).write(v));
    }

    @FunctionalInterface
    interface ParsingFunction {
        JsonValue parse(final File file) throws IOException;
    }

    @FunctionalInterface
    interface WritingFunction {
        void write(final Writer writer, final JsonValue value, final JsonWriterOptions options) throws IOException;
    }
}
