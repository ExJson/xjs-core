package xjs.serialization.writer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xjs.core.JsonArray;
import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.serialization.JsonSerializationContext;
import xjs.serialization.parser.JsonParser;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class JsonWriterTest {

    @BeforeAll
    static void setup() {
        JsonSerializationContext.setEol("\n");
    }

    @AfterAll
    static void teardown() {
        JsonSerializationContext.setEol(System.getProperty("line.separator"));
    }

    @Test
    public void write_printsTrue() {
        assertEquals("true", write(JsonValue.valueOf(true)));
    }

    @Test
    public void write_printsFalse() {
        assertEquals("false", write(JsonValue.valueOf(false)));
    }

    @Test
    public void write_printsNull() {
        assertEquals("null", write(JsonValue.valueOf(null)));
    }

    @Test
    public void write_printsInteger() {
        assertEquals("1234", write(JsonValue.valueOf(1234)));
    }

    @Test
    public void write_printsDecimal() {
        assertEquals("12.34", write(JsonValue.valueOf(12.34)));
    }

    @Test
    public void write_printsQuotedString() {
        assertEquals("\"test\"", write(JsonValue.valueOf("test")));
    }

    @Test
    public void write_printsEmptyArray() {
        assertEquals("[]", write(new JsonArray()));
    }

    @Test
    public void write_printsEmptyObject() {
        assertEquals("{}", write(new JsonObject()));
    }

    @Test
    public void write_printsCondensedArray() {
        assertEquals("[ 1, 2, 3 ]", write(new JsonArray().add(1).add(2).add(3).condense()));
    }

    @Test
    public void write_writesMultilineArray() {
        assertEquals("[\n  1,\n  2,\n  3\n]", write(new JsonArray().add(1).add(2).add(3)));
    }

    @Test
    public void write_printsCondensedObject() {
        assertEquals("{ \"1\": 1, \"2\": 2, \"3\": 3 }",
            write(new JsonObject().add("1", 1).add("2", 2).add("3", 3).condense()));
    }

    @Test
    public void write_printsMultilineObject() {
        assertEquals("{\n  \"1\": 1,\n  \"2\": 2,\n  \"3\": 3\n}",
            write(new JsonObject().add("1", 1).add("2", 2).add("3", 3)));
    }

    @Test
    public void write_preservesWhitespaceAbove() {
        assertEquals("\n\ntrue", write(JsonValue.valueOf(true).setLinesAbove(2)));
    }

    @Test
    public void write_preservesWhitespaceBetween() {
        assertEquals("{\n  \"a\":\n\n\n\n    1234\n}",
            write(new JsonObject().add("a", JsonValue.valueOf(1234).setLinesBetween(4))));
    }

    @Test
    public void writeUnformatted_doesNotPrintWhitespace() {
        assertEquals("{\"1\":1,\"2\":2,\"3\":3}",
            write(new JsonObject().add("1", 1).add("2", 2).add("3", 3), null));
    }

    @Test
    public void write_printsRecursively() {
        assertEquals("[\n  1,\n  []\n]",
            write(new JsonArray().add(1).add(new JsonArray())));
    }

    @Test
    public void write_withoutAllowCondense_printsExpanded() {
        final JsonWriterOptions options = new JsonWriterOptions().setAllowCondense(false);
        assertEquals("[\n  1,\n  2,\n  3\n]",
            write(new JsonArray().add(1).add(2).add(3).condense(), options));
    }

    @Test
    public void write_preservesComplexFormatting() {
        final JsonObject object =
            new JsonObject()
                .add("1", JsonValue.valueOf(1).setLinesBetween(1))
                .add("2", 2)
                .add("a", new JsonArray()
                    .add(3)
                    .add(4)
                    .add(new JsonObject()
                        .add("5", 5)
                        .add("6", 6)
                        .condense())
                    .setLinesAbove(2));
        final String expected = """
            {
              "1":
                1,
              "2": 2,
            
              "a": [
                3,
                4,
                { "5": 5, "6": 6 }
              ]
            }""";
        assertEquals(expected, write(object));
    }

    @Test
    public void parse_thenRewrite_preservesComplexFormatting() throws IOException {
        final String expected = """
            {
              
              "1":
                1,
              "2":
               
                2,
            
              "a": [
                3, 4,
                { "5": 5, "6": 6 }
              ]
            }""";
        assertEquals(expected, write(new JsonParser(expected).parse()));
    }

    private static String write(final JsonValue value) {
        return write(value, new JsonWriterOptions());
    }
    
    private static String write(final JsonValue value, final JsonWriterOptions options) {
        final StringWriter sw = new StringWriter();
        final JsonWriter writer =
            options != null ? new JsonWriter(sw, options) : new JsonWriter(sw, false);
        try {
            writer.write(value);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return sw.toString();
    }
}
