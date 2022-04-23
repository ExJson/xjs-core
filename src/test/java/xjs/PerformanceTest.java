package xjs;

import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.*;
import xjs.core.Json;
import xjs.core.JsonCopy;
import xjs.core.JsonValue;
import xjs.serialization.parser.JsonParser;
import xjs.serialization.parser.XjsParser;
import xjs.serialization.writer.JsonWriter;
import xjs.serialization.writer.XjsWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnusedReturnValue")
public class PerformanceTest {

    private static final String XJS_SAMPLE = """
        // Comment
        a: 1 # Comment
        b:
          /* Comment */
          2
        c: [ 3a, 3b, 3c ]
        d: { da: 4a, db: 4b }
        e: complex (
          balanced value
          [ this must ]
          be validated
          { before }
          it can be printed
          /* safely */
        )
        """;

    private static final JsonValue XJS_WRITING_SAMPLE =
        Json.parse(XJS_SAMPLE).copy(JsonCopy.UNFORMATTED | JsonCopy.COMMENTS);

    private static final String JSON_SAMPLE = """
        {
          "a": 1,
          "b":
            2,
          "c": [ "3a", "3b", "3c" ],
          "d": { "da": "4a", "db": "4b" },
          "e": "(Multiline text\\njust a few lines\\nnothing special\\"\\nnothing required)"
        }
        """;

    private static final JsonValue JSON_WRITING_SAMPLE =
        Json.parse(XJS_SAMPLE).copy(JsonCopy.UNFORMATTED);

    public static void main(final String[] args) throws Exception {
        Main.main(args);
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public JsonValue xjsParsingSample() throws IOException {
        return new XjsParser(XJS_SAMPLE).parse();
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String xjsWritingSample() throws IOException {
        final StringWriter sw = new StringWriter();
        new XjsWriter(sw, true).write(XJS_WRITING_SAMPLE);
        return sw.toString();
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public JsonValue jsonParsingSample() throws IOException {
        return new JsonParser(JSON_SAMPLE).parse();
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String jsonWritingSample() throws IOException {
        final StringWriter sw = new StringWriter();
        new JsonWriter(sw, true).write(JSON_WRITING_SAMPLE);
        return sw.toString();
    }
}
