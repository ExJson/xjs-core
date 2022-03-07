package xjs.serialization.writer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xjs.core.*;
import xjs.serialization.JsonSerializationContext;
import xjs.serialization.parser.XjsParser;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class XjsWriterTest {

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
    public void write_printsSingleQuotedString() {
        assertEquals("'test'", write(new JsonString("test", StringType.SINGLE)));
    }

    @Test
    public void write_printsDoubleQuotedString() {
        assertEquals("\"test\"", write(new JsonString("test", StringType.DOUBLE)));
    }

    @Test
    public void write_printsMultiString() {
        assertEquals("'''\n  test\n  '''", write(new JsonString("test", StringType.MULTI)));
    }

    @Test
    public void write_printsImplicitString() {
        assertEquals("test", write(new JsonString("test", StringType.IMPLICIT)));
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
    public void write_printsMultilineArray() {
        assertEquals("[\n  1\n  2\n  3\n]", write(new JsonArray().add(1).add(2).add(3)));
    }

    @Test
    public void write_printsVoidStrings_asCommas_withoutSpaces() {
        final JsonArray array = new JsonArray()
            .add(new JsonString("", StringType.IMPLICIT))
            .add(new JsonString("", StringType.IMPLICIT))
            .add(new JsonString("", StringType.IMPLICIT))
            .condense();
        assertEquals("[,,,]", write(array));
    }

    @Test
    public void write_printsCondensedObject() {
        assertEquals("1: 1, 2: 2, 3: 3",
            write(new JsonObject().add("1", 1).add("2", 2).add("3", 3).condense()));
    }

    @Test
    public void write_printsMultilineObject() {
        assertEquals("1: 1\n2: 2\n3: 3",
            write(new JsonObject().add("1", 1).add("2", 2).add("3", 3)));
    }

    @Test
    public void write_preservesWhitespaceAbove() {
        assertEquals("\n\ntrue", write(JsonValue.valueOf(true).setLinesAbove(2)));
    }

    @Test
    public void write_preservesWhitespaceBetween() {
        assertEquals("a:\n\n\n\n  1234",
            write(new JsonObject().add("a", JsonValue.valueOf(1234).setLinesBetween(4))));
    }

    @Test
    public void writeUnformatted_doesNotPrintWhitespace() {
        assertEquals("1:1,2:2,3:3",
            write(new JsonObject().add("1", 1).add("2", 2).add("3", 3), null));
    }

    @Test
    public void write_printsRecursively() {
        assertEquals("[\n  1\n  []\n]",
            write(new JsonArray().add(1).add(new JsonArray())));
    }

    @Test
    public void write_withoutAllowCondense_printsExpanded() {
        final JsonWriterOptions options = new JsonWriterOptions().setAllowCondense(false);
        assertEquals("[\n  1\n  2\n  3\n]",
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
            1:
              1
            2: 2
            
            a: [
              3
              4
              { 5: 5, 6: 6 }
            ]""";
        assertEquals(expected, write(object));
    }

    @Test
    public void parse_thenRewrite_preservesComplexFormatting() throws IOException {
        final String expected = """
            
            # Header
            1:
              # Value
              1
            2: // Value
             
              2 # E0l
              
            z:
            
              /// Value
              y
            
            a: /* Value */ [
              3, 4
              { 5: 5, 6: 6 /* eol */ }
              [ /* interior */ ]
              
              
              /**
               * Interior
               */
            
            ] # eol
            
            b: '})](*&(*%#&)!'
            c: { : }
            d: [,,,]
            """;
        assertEquals(expected, write(new XjsParser(expected).parse()));
    }

    @Test
    public void write_printsHeaderComment() {
        final JsonValue v = new JsonString("v", StringType.IMPLICIT);
        v.setComment(CommentType.HEADER, CommentStyle.HASH, "Header");

        assertEquals("# Header\nv", write(v));
    }

    @Test
    public void write_printsFooterComment() {
        final JsonValue v = new JsonObject();
        v.setComment(CommentType.FOOTER, CommentStyle.LINE, "Footer");

        assertEquals("{}\n// Footer", write(v));
    }

    @Test
    public void write_printsEolComment() {
        final JsonValue v = new JsonString("v", StringType.IMPLICIT);
        v.setComment(CommentType.EOL, CommentStyle.LINE_DOC, "Eol");

        assertEquals("v /// Eol", write(v));
    }

    @Test
    public void write_printsValueComment() {
        final JsonValue v = new JsonString("v", StringType.IMPLICIT);
        v.setComment(CommentType.VALUE, CommentStyle.BLOCK, "Value");

        assertEquals("k: /* Value */ v", write(new JsonObject().add("k", v)));
    }

    @Test
    public void writeValueComment_withLinesBetween_coercesOntoNextLine() {
        final JsonValue v = new JsonString("v", StringType.IMPLICIT);
        v.setComment(CommentType.VALUE, CommentStyle.BLOCK, "Value");
        v.setLinesBetween(1);

        assertEquals("k:\n  /* Value */\n  v", write(new JsonObject().add("k", v)));
    }

    @Test
    public void writeValueComment_withLinesBetween_andLinesBelow_doesNotInsertExtraLines() {
        final JsonValue v = new JsonString("v", StringType.IMPLICIT);
        v.getComments().setData(CommentType.VALUE, "/* Value */\n");
        v.setLinesBetween(1);

        assertEquals("k:\n  /* Value */\n  v", write(new JsonObject().add("k", v)));
    }

    @Test
    public void write_printsInteriorComment() {
        final JsonValue v = new JsonArray();
        v.setComment(CommentType.INTERIOR, CommentStyle.MULTILINE_DOC, "Interior");

        assertEquals("[ /** Interior */ ]", write(v));
    }

    @Test
    public void writeInteriorComment_withNewlineInComment_insertsLinesAround() {
        final JsonValue v = new JsonArray();
        v.setComment(CommentType.INTERIOR, CommentStyle.MULTILINE_DOC, "Interior\nLine 2");

        assertEquals("[\n  /**\n   * Interior\n   * Line 2\n   */\n]", write(v));
    }

    @Test
    public void writeInteriorComment_withNewlineAboveComment_insertsLineBelow() {
        final JsonArray v = new JsonArray();
        v.setLinesTrailing(1);
        v.setComment(CommentType.INTERIOR, CommentStyle.MULTILINE_DOC, "Interior");

        assertEquals("[\n  /** Interior */\n]", write(v));
    }

    private static String write(final JsonValue value) {
        return write(value, new JsonWriterOptions());
    }

    private static String write(final JsonValue value, final JsonWriterOptions options) {
        final StringWriter sw = new StringWriter();
        final XjsWriter writer =
            options != null ? new XjsWriter(sw, options) : new XjsWriter(sw, false);
        try {
            writer.write(value);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return sw.toString();
    }
}
