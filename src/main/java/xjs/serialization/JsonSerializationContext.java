package xjs.serialization;

import xjs.core.CommentStyle;
import xjs.core.JsonValue;
import xjs.serialization.parser.JsonParser;
import xjs.serialization.parser.ParsingFunction;
import xjs.serialization.parser.XjsParser;
import xjs.serialization.writer.JsonWriter;
import xjs.serialization.writer.JsonWriterOptions;
import xjs.serialization.writer.WritingFunction;
import xjs.serialization.writer.XjsWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsonSerializationContext {

    private static final Map<String, ParsingFunction> PARSERS = new ConcurrentHashMap<>();
    private static final Map<String, WritingFunction> WRITERS = new ConcurrentHashMap<>();
    private static final Map<String, String> ALIASES = new ConcurrentHashMap<>();
    private static volatile String eol = System.lineSeparator();
    private static final ParsingFunction DEFAULT_PARSER = file -> new JsonParser(file).parse();
    private static final WritingFunction DEFAULT_WRITER = (w, v, o) -> new XjsWriter(w, o).write(v);
    private static volatile CommentStyle defaultCommentStyle = CommentStyle.LINE;
    private static volatile JsonWriterOptions defaultFormatting = new JsonWriterOptions();

    public static void addParser(final String format, final ParsingFunction parser) {
        PARSERS.put(format, parser);
    }

    public static void addWriter(final String format, final WritingFunction writer) {
        WRITERS.put(format, writer);
    }

    public static void registerAlias(final String alias, final String format) {
        ALIASES.put(alias, format);
    }

    public static synchronized String getEol() {
        return eol;
    }

    public static synchronized void setEol(final String eol) {
        JsonSerializationContext.eol = eol;
    }

    public static synchronized CommentStyle getDefaultCommentStyle() {
        return defaultCommentStyle;
    }

    public static synchronized void setDefaultCommentStyle(final CommentStyle style) {
        defaultCommentStyle = style;
    }

    public static synchronized JsonWriterOptions getDefaultFormatting() {
        return new JsonWriterOptions(defaultFormatting);
    }

    public static synchronized void setDefaultFormatting(final JsonWriterOptions options) {
        defaultFormatting = options;
    }

    public static boolean isKnownFormat(final File file) {
        final String ext = getExtension(file);
        return PARSERS.containsKey(ext) || ALIASES.containsKey(ext);
    }

    public static JsonValue autoParse(final File file) throws IOException {
        return PARSERS.getOrDefault(getFormat(file), DEFAULT_PARSER).parse(file);
    }

    public static void autoWrite(final File file, final JsonValue value) throws IOException {
        final Writer writer = new FileWriter(file);
        WRITERS.getOrDefault(getFormat(file), DEFAULT_WRITER).write(writer, value, getDefaultFormatting());
        writer.flush();
    }

    private static String getFormat(final File file) {
        final String ext = getExtension(file);
        return ALIASES.getOrDefault(ext, ext);
    }

    private static String getExtension(final File file) {
        final int index = file.getName().lastIndexOf('.');
        return index < 0 ? "xjs" : file.getName().substring(index + 1);
    }

    static {
        PARSERS.put("json", file -> new JsonParser(file).parse());
        PARSERS.put("xjs", DEFAULT_PARSER);
        WRITERS.put("json", (w, v, o) -> new JsonWriter(w, o).write(v));
        WRITERS.put("xjs", DEFAULT_WRITER);
    }
}
