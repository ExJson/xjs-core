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
        assertEquals("true", write(Json.value(true)));
    }

    @Test
    public void write_printsFalse() {
        assertEquals("false", write(Json.value(false)));
    }

    @Test
    public void write_printsNull() {
        assertEquals("null", write(Json.value(null)));
    }

    @Test
    public void write_printsInteger() {
        assertEquals("1234", write(Json.value(1234)));
    }

    @Test
    public void write_printsDecimal() {
        assertEquals("12.34", write(Json.value(12.34)));
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
        assertEquals("\n\ntrue", write(Json.value(true).intoReference().setLinesAbove(2)));
    }

    @Test
    public void write_preservesWhitespaceBetween() {
        assertEquals("a:\n\n\n\n  1234",
            write(new JsonObject().addReference("a", Json.value(1234).intoReference().setLinesBetween(4))));
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
                .addReference("1", Json.value(1).intoReference().setLinesBetween(1))
                .add("2", 2)
                .addReference("a", new JsonArray()
                    .add(3)
                    .add(4)
                    .add(new JsonObject()
                        .add("5", 5)
                        .add("6", 6)
                        .condense())
                    .intoReference()
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
    public void write_withLineSpacing_overridesDefaultSpacing() {
        final JsonWriterOptions options = new JsonWriterOptions().setLineSpacing(2);
        final JsonObject object =
            new JsonObject()
                .add("1", "2")
                .add("3", "4")
                .add("5", new JsonArray().add(6).add(7).add(8).condense())
                .add("9", "0");
        final String expected = """

            1: '2'
              
            3: '4'
              
            5: [ 6, 7, 8 ]
              
            9: '0'
            """;
        assertEquals(expected, write(object, options));
    }

    @Test
    public void write_withMinAndMaxSpacing_overridesConfiguredSpacing_ignoringCondensed() throws IOException {
        final JsonWriterOptions options = new JsonWriterOptions().setMinSpacing(2).setMaxSpacing(2);
        final String input = """
            1: '2'
            3: '4'
            5: {
              6: [ 7, 8, 9 ]
            }""";
        final String expected = """

            1: '2'
            
            3: '4'
            
            5: {
              6: [ 7, 8, 9 ]
            }
            """;
        final JsonReference object = new XjsParser(input).parse();
        assertEquals(expected, write(object, options));
    }

    @Test
    public void write_withHeaderOnRootObject_addsImplicitEmptyLine() {
        final JsonReference object = new JsonObject().add("key", "value").intoReference();
        object.setComment(CommentType.HEADER, CommentStyle.LINE, "header");
        final String expected = """
            // header
            
            key: 'value'""";
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
            /* header */
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
            
            enter: {
              level: {
                two: [
                  // header
                  # header
                  yes
                  // header
                  yes
                ]
              }
            }
            
            b: '})](*&(*%#&)!'
            c: { : }
            d: [,,,]
            """;
        assertEquals(expected, write(new XjsParser(expected).parse()));
    }

    @Test
    public void write_printsHeaderComment() {
        final JsonReference r = new JsonString("v", StringType.IMPLICIT).intoReference();
        r.setComment(CommentType.HEADER, CommentStyle.HASH, "Header");

        assertEquals("# Header\nv", write(r));
    }

    @Test
    public void write_printsFooterComment() {
        final JsonReference r = new JsonObject().intoReference();
        r.setComment(CommentType.FOOTER, CommentStyle.LINE, "Footer");

        assertEquals("{}\n// Footer", write(r));
    }

    @Test
    public void write_printsEolComment() {
        final JsonReference r = new JsonString("v", StringType.IMPLICIT).intoReference();
        r.setComment(CommentType.EOL, CommentStyle.LINE_DOC, "Eol");

        assertEquals("v /// Eol", write(r));
    }

    @Test
    public void write_printsValueComment() {
        final JsonReference r = new JsonString("v", StringType.IMPLICIT).intoReference();
        r.setComment(CommentType.VALUE, CommentStyle.BLOCK, "Value");

        assertEquals("k: /* Value */ v", write(new JsonObject().addReference("k", r)));
    }

    @Test
    public void writeValueComment_withLinesBetween_coercesOntoNextLine() {
        final JsonReference r = new JsonString("v", StringType.IMPLICIT).intoReference();
        r.setComment(CommentType.VALUE, CommentStyle.BLOCK, "Value");
        r.setLinesBetween(1);

        assertEquals("k:\n  /* Value */\n  v", write(new JsonObject().addReference("k", r)));
    }

    @Test
    public void writeValueComment_withLinesBetween_andLinesBelow_doesNotInsertExtraLines() {
        final JsonReference r = new JsonString("v", StringType.IMPLICIT).intoReference();
        r.getComments().setData(CommentType.VALUE, "/* Value */\n");
        r.setLinesBetween(1);

        assertEquals("k:\n  /* Value */\n  v", write(new JsonObject().addReference("k", r)));
    }

    @Test
    public void write_printsInteriorComment() {
        final JsonReference r = new JsonArray().intoReference();
        r.setComment(CommentType.INTERIOR, CommentStyle.MULTILINE_DOC, "Interior");

        assertEquals("[ /** Interior */ ]", write(r));
    }

    @Test
    public void writeInteriorComment_withNewlineInComment_insertsLinesAround() {
        final JsonReference r = new JsonArray().intoReference();
        r.setComment(CommentType.INTERIOR, CommentStyle.MULTILINE_DOC, "Interior\nLine 2");

        assertEquals("[\n  /**\n   * Interior\n   * Line 2\n   */\n]", write(r));
    }

    @Test
    public void writeInteriorComment_withNewlineAboveComment_insertsLineBelow() {
        final JsonReference r = new JsonArray().intoReference();
        r.get().asArray().setLinesTrailing(1); // todo
        r.setComment(CommentType.INTERIOR, CommentStyle.MULTILINE_DOC, "Interior");

        assertEquals("[\n  /** Interior */\n]", write(r));
    }

    private static String write(final JsonValue value) {
        return write(value.intoReference(), new JsonWriterOptions());
    }

    private static String write(final JsonReference reference) {
        return write(reference, new JsonWriterOptions());
    }

    private static String write(final JsonValue value, final JsonWriterOptions options) {
        return write(value.intoReference(), options);
    }

    private static String write(final JsonReference reference, final JsonWriterOptions options) {
        final StringWriter sw = new StringWriter();
        final XjsWriter writer =
            options != null ? new XjsWriter(sw, options) : new XjsWriter(sw, false);
        try {
            writer.write(reference);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return sw.toString();
    }
}
