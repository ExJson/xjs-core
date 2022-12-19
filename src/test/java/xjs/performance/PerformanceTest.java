package xjs.performance;

import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.*;
import xjs.core.Json;
import xjs.core.JsonCopy;
import xjs.core.JsonValue;
import xjs.performance.experimental.util.ExperimentalInputStreamByteReader;
import xjs.performance.legacy.parser.LegacyXjsParser;
import xjs.performance.legacy.token.LegacyTokenStream;
import xjs.performance.legacy.token.LegacyTokenizer;
import xjs.performance.legacy.writer.LegacyJsonWriter;
import xjs.performance.legacy.writer.LegacyXjsWriter;
import xjs.serialization.parser.JsonParser;
import xjs.serialization.parser.XjsParser;
import xjs.serialization.token.TokenStream;
import xjs.serialization.token.Tokenizer;
import xjs.serialization.util.PositionTrackingReader;
import xjs.serialization.writer.JsonWriter;
import xjs.serialization.writer.XjsWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnusedReturnValue")
public class PerformanceTest {

    private static final String SIMPLE_XJS_SAMPLE =
        "[1234,5,6,7,'abc',\"def\",[[[['ghi',{},{}]]]],true,false,null,hello,world]";

    private static final String SIMPLE_JSON_SAMPLE =
        "[1234,5,6,7,\"abc\",\"def\",[[[[\"ghi\",{},{}]]]],true,false,null,\"hello\",\"world\"]";

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

    private static final JsonValue XJS_WRITING_SAMPLE =
        Json.parse(XJS_SAMPLE).copy(JsonCopy.UNFORMATTED | JsonCopy.COMMENTS);

    private static final JsonValue JSON_WRITING_SAMPLE =
        Json.parse(XJS_SAMPLE).copy(JsonCopy.UNFORMATTED);

    private static final String READER_INPUT_SAMPLE =
        XJS_SAMPLE.repeat(10);

    public static void main(final String[] args) throws Exception {
        Main.main(args);
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public JsonValue legacyXjsParsingSample() throws IOException {
        return new LegacyXjsParser(SIMPLE_XJS_SAMPLE).parse();
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public JsonValue xjsParsingSample() {
        return new XjsParser(SIMPLE_XJS_SAMPLE).parse();
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
    public String legacyXjsWritingSample() throws IOException {
        final StringWriter sw = new StringWriter();
        new LegacyXjsWriter(sw, true).write(XJS_WRITING_SAMPLE);
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

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String legacyJsonWritingSample() throws IOException {
        final StringWriter sw = new StringWriter();
        new LegacyJsonWriter(sw, true).write(JSON_WRITING_SAMPLE);
        return sw.toString();
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String stringReaderSample() throws IOException {
        return PositionTrackingReader.fromString(
            READER_INPUT_SAMPLE).readToEnd();
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String byteBufferSample_smallestBuffer() throws IOException {
        return new ExperimentalInputStreamByteReader(
            getReadingSampleIS(), 8, true).readToEnd();
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String byteBufferSample_mediumBuffer() throws IOException {
        return new ExperimentalInputStreamByteReader(
            getReadingSampleIS(), 128, true).readToEnd();
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String byteBufferSample_normalBuffer() throws IOException {
        return new ExperimentalInputStreamByteReader(
            getReadingSampleIS(), 1024, true).readToEnd();
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String charBufferSample_smallestBuffer() throws IOException {
        return PositionTrackingReader.fromIs(
            getReadingSampleIS(), 8, true).readToEnd();
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String charBufferSample_mediumBuffer() throws IOException {
        return PositionTrackingReader.fromIs(
            getReadingSampleIS(), 128, true).readToEnd();
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String charBufferSample_normalBuffer() throws IOException {
        return PositionTrackingReader.fromIs(
            getReadingSampleIS(), 1024, true).readToEnd();
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TokenStream stream_fromStandardReader() throws IOException {
        final Reader reader =
            new InputStreamReader(getReadingSampleIS());
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = new char[1024];
        int bytesRead;
        while ((bytesRead = reader.read(buffer, 0, buffer.length)) != -1) {
            sb.append(buffer, 0, bytesRead);
        }
        return Tokenizer.stream(sb.toString());
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TokenStream stream_fromPositionTrackingReader() throws IOException {
        final PositionTrackingReader reader =
            PositionTrackingReader.fromIs(getReadingSampleIS());
        return Tokenizer.stream(reader.readToEnd());
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public LegacyTokenStream stream_fromString() throws IOException {
        final Reader reader =
            new InputStreamReader(getReadingSampleIS());
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = new char[1024];
        int bytesRead;
        while ((bytesRead = reader.read(buffer, 0, buffer.length)) != -1) {
            sb.append(buffer, 0, bytesRead);
        }
        final LegacyTokenStream stream = LegacyTokenizer.stream(sb.toString());
        stream.forEach(t -> {});
        return stream;
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TokenStream stream_fromStreamReader() throws IOException {
        final TokenStream stream = Tokenizer.stream(getReadingSampleIS());
        stream.forEach(t -> {});
        return stream;
    }

    @Benchmark
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TokenStream stream_fromStringReader() throws IOException {
        final Reader reader =
            new InputStreamReader(getReadingSampleIS());
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = new char[1024];
        int bytesRead;
        while ((bytesRead = reader.read(buffer, 0, buffer.length)) != -1) {
            sb.append(buffer, 0, bytesRead);
        }
        final TokenStream stream = Tokenizer.stream(sb.toString());
        stream.forEach(t -> {});
        return stream;
    }

    private static InputStream getReadingSampleIS() {
        return new ByteArrayInputStream(
            READER_INPUT_SAMPLE.getBytes(StandardCharsets.UTF_8));
    }
}
