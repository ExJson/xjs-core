package xjs.performance.legacy.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import xjs.core.*;
import xjs.exception.SyntaxException;
import xjs.serialization.parser.CommonParserTest;
import xjs.serialization.writer.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LegacyXjsParserTest extends CommonParserTest {

    @Test
    public void parse_ignoresTrailingCommas() throws IOException {
        assertTrue(new JsonArray().add(1).add(2).add(3)
            .matches(this.parse("[1,2,3,]")));
    }

    @Test
    public void parse_readsUnquotedStrings() throws IOException {
        assertEquals("hello", this.parse("hello").asString());
    }

    @Test
    public void parse_readsUnquotedKeys() throws IOException {
        assertEquals("key", this.parse("{key:value}").asObject().keys().get(0));
    }

    @Test
    public void parse_readsMultipleUnquotedKeys() throws IOException {
        assertTrue(new JsonObject().add("k1", "v1").add("k2", "v2")
            .matches(this.parse("{k1:v1,k2:v2}")));
    }

    @Test
    public void parse_readsOpenRoot() throws IOException {
        assertTrue(new JsonObject().add("a", 1).add("b", 2)
            .matches(this.parse("a:1,b:2")));
    }

    @Test
    public void parse_readsMultipleUnquotedValues() throws IOException {
        assertTrue(new JsonArray().add("a").add("b").add("c")
            .matches(this.parse("[a,b,c]")));
    }

    @Test
    public void singleComma_inArray_isEmptyImplicitString() throws IOException {
        assertTrue(new JsonArray().add("").add("")
            .matches(this.parse("[,,]")));
    }

    @Test
    public void singleComma_inObject_isEmptyImplicitString() throws IOException {
        assertTrue(new JsonObject().add("k", "").add("r", "")
            .matches(this.parse("k:,r:,")));
    }

    @Test
    public void singleColon_inObject_isEmptyImplicitKey() throws IOException {
        assertTrue(new JsonObject().add("", "")
            .matches(this.parse(":,")));
    }

    @Test
    public void emptyFile_isImplicitlyString() throws IOException {
        assertEquals(new JsonString("", StringType.IMPLICIT),
            this.parse("").trim());
    }

    @Test
    public void parseValue_readsUntilEndOfLine() throws IOException {
        assertTrue(new JsonObject().add("k", "v").add("r", "t")
            .matches(this.parse("k:v\nr:t")));
    }

    @Test
    public void parseKey_readsUntilColon() throws IOException {
        assertTrue(new JsonObject().add("k\n1\n2\n3", "v")
            .matches(this.parse("k\n1\n2\n3:v").unformatted()));
    }

    @Test
    public void parseValue_readsUntilTextIsBalanced() throws IOException {
        assertTrue(new JsonObject().add("k", "(\n1\n2\n3\n)")
            .matches(this.parse("k:(\n1\n2\n3\n)")));
    }

    @Test
    public void parseValue_cannotEndUnlessBalanced() {
        assertThrows(SyntaxException.class,
            () -> this.parse("k:("));
    }

    @Test
    public void parseValue_continuesReadingAfterEscape() throws IOException {
        assertTrue(new JsonObject().add("k", "v\nv")
            .matches(this.parse("k:v\\\nv")));
    }

    @Test
    public void parse_readsSingleQuotedString() throws IOException {
        assertEquals("", this.parse("''").asString());
    }

    @Test
    public void parse_readsMultilineString() throws IOException {
        assertEquals("test", this.parse("'''test'''").asString());
    }

    @Test
    public void parse_toleratesEmptyMultilineString() throws IOException {
        assertEquals("", this.parse("''''''").asString());
    }

    @Test
    public void multilineString_ignoresLeadingWhitespace() throws IOException {
        assertEquals("test", this.parse("'''  test'''").asString());
    }

    @Test
    public void multilineString_ignoresTrailingNewline() throws IOException {
        assertEquals("test", this.parse("'''test\n'''").asString());
    }

    @Test
    public void multilineString_preservesIndentation_bySubsequentLines() throws IOException {
        final String text = """
            multi:
              '''
              0
               1
                2
              '''
            """;
        assertEquals("0\n 1\n  2", this.parse(text).asObject().getAsserted("multi").asString());
    }

    @ParameterizedTest
    @CsvSource({"/*header*/", "#header", "//header"})
    public void parse_preservesHeaderComment_atTopOfFile(final String comment) throws IOException {
        assertEquals(comment,
            this.parse(comment + "\n{}").getComments().getData(CommentType.HEADER));
    }

    @ParameterizedTest
    @CsvSource({"/*footer*/", "#footer", "//footer"})
    public void parse_preservesFooterComment_atBottomOfFile(final String comment) throws IOException {
        assertEquals(comment,
            this.parse("{}\n" + comment).getComments().getData(CommentType.FOOTER));
    }

    @ParameterizedTest
    @CsvSource({"/*eol*/", "#eol", "//eol"})
    public void parse_preservesEolComment_afterClosingRootBrace(final String comment) throws IOException {
        assertEquals(comment,
            this.parse("{}" +  comment).getComments().getData(CommentType.EOL));
    }

    @ParameterizedTest
    @CsvSource({"/*header*/", "#header", "//header"})
    public void parse_preservesHeader_aboveValue(final String comment) throws IOException {
        assertEquals(comment,
            this.parse(comment + "\nk:v").asObject().get(0).getComments().getData(CommentType.HEADER));
    }

    @ParameterizedTest
    @CsvSource({"/*value*/", "#value", "//value"})
    public void parse_preservesValueComment_betweenKeyValue(final String comment) throws IOException {
        assertEquals(comment + "\n",
            this.parse("k:\n" + comment + "\nv").asObject().get(0).getComments().getData(CommentType.VALUE));
    }

    @ParameterizedTest
    @CsvSource({"/*eol*/", "#eol", "//eol"})
    public void parse_preservesEolComment_afterValue(final String comment) throws IOException {
        assertEquals(comment,
            this.parse("k:v" + comment).asObject().get(0).getComments().getData(CommentType.EOL));
    }

    @ParameterizedTest
    @CsvSource({"/*interior*/", "#interior", "//interior"})
    public void parse_preservesInteriorComment_inContainer(final String comment) throws IOException {
        assertEquals(comment,
            this.parse("{\n" + comment + "\n}").getComments().getData(CommentType.INTERIOR));
    }

    @ParameterizedTest
    @CsvSource({"/*comment*/", "#comment", "//comment"})
    public void parse_preservesNewlines_afterComments(final String comment) throws IOException {
        assertEquals(comment + "\n",
            this.parse("k1:v1\n" + comment + "\n\nk:v")
                .asObject().get(1).getComments().getData(CommentType.HEADER));
    }

    @Test
    public void parse_readsUntilLastEmptyLine_asHeader() throws IOException {
        final String header = """
            // header part 1
            // header part 2
            
            // header part 3""";
        final String json = """

            // comment of "key"
            key: value""";

        final JsonValue parsed = this.parse(header + "\n" + json);
        assertEquals(header, parsed.getComments().getData(CommentType.HEADER));
    }

    @Test
    public void parse_preservesEmptyLines_ignoringComments() throws IOException {
        final String json = """
             
             key:
               value
             
             another:
             
               # comment
               value
               
             k3: v3, k4: v4
               
               
             # and
             finally: value
             """;
        final String expected = """
             {
               "key":
                 "value",
               
               "another":
             
                 "value",
             
               "k3": "v3", "k4": "v4",
             
             
               "finally": "value"
             }""";
        final StringWriter sw = new StringWriter();
        final JsonWriter writer = new JsonWriter(sw, true);
        writer.write(this.parse(json));
        assertEquals(expected.replace("\r", ""), sw.toString().replace("\r", ""));
    }

    @Override
    protected JsonValue parse(final String json) throws IOException {
        return new LegacyXjsParser(json).parse();
    }
}
