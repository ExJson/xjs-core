package xjs.serialization.parser;

import org.junit.jupiter.api.Test;
import xjs.core.JsonArray;
import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.exception.SyntaxException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonParserTest {

    @Test
    public void parse_readsTrue() throws IOException {
        assertTrue(new JsonParser("true").parse().isTrue());
    }

    @Test
    public void parse_readsFalse() throws IOException {
        assertTrue(new JsonParser("false").parse().isFalse());
    }

    @Test
    public void parse_readsNull() throws IOException {
        assertTrue(new JsonParser("null").parse().isNull());
    }

    @Test
    public void parse_readsInteger() throws IOException {
        assertEquals(1234, new JsonParser("1234").parse().asInt());
    }

    @Test
    public void parse_readsDecimal() throws IOException {
        assertEquals(12.34, new JsonParser("12.34").parse().asDouble());
    }

    @Test
    public void parse_readsExponent() throws IOException {
        assertEquals(12.3e4, new JsonParser("12.3e4").parse().asDouble());
    }

    @Test
    public void parse_readsQuotedString() throws IOException {
        assertEquals("Hello, World!", new JsonParser("\"Hello, World!\"").parse().asString());
    }

    @Test
    public void parse_readsEmptyObject() throws IOException {
        assertEquals(new JsonObject(), new JsonParser("{}").parse().asObject());
    }

    @Test
    public void parse_readsEmptyArray() throws IOException {
        assertEquals(new JsonArray(), new JsonParser("[]").parse().asArray());
    }

    @Test
    public void parse_readsCondensedArray() throws IOException {
        final JsonArray parsed = new JsonParser("[1,2,3]").parse().asArray();
        assertEquals(List.of(1, 2, 3), parsed.toList(JsonValue::asInt));
    }

    @Test
    public void parse_readsMultiLineArray() throws IOException {
        final JsonArray parsed = new JsonParser("[\n1,\n2,\n3\n]").parse().asArray();
        assertEquals(List.of(1, 2, 3), parsed.toList(JsonValue::asInt));
    }

    @Test
    public void parse_readsCondensedObject() throws IOException {
        final JsonObject parsed = new JsonParser("{\"1\":1,\"2\":2,\"3\":3}").parse().asObject();
        assertEquals(Map.of("1", 1, "2", 2, "3", 3), parsed.toMap(JsonValue::asInt));
    }

    @Test
    public void parse_readsMultiLineObject() throws IOException {
        final JsonObject parsed = new JsonParser("{\n\"1\":1,\n\"2\":2,\n\"3\":3\n}").parse().asObject();
        assertEquals(Map.of("1", 1, "2", 2, "3", 3), parsed.toMap(JsonValue::asInt));
    }

    @Test
    public void parse_preservesWhitespaceAbove() throws IOException {
        assertEquals(2, new JsonParser("\n\ntrue").parse().getLinesAbove());
    }

    @Test
    public void parse_preservesWhitespaceBetween() throws IOException {
        assertEquals(3, new JsonParser("{\"\":\n\n\n\"\"}")
            .parse().asObject().getReference(0).getLinesBetween());
    }

    @Test
    public void parse_parsesRecursively() throws IOException {
        assertEquals(new JsonArray().add(1).add(new JsonArray()),
            new JsonParser("[1,[]]").parse().asArray().unformatted());
    }

    @Test
    public void parse_doesNotTolerate_trailingCommas() {
        assertThrows(SyntaxException.class,
            () -> new JsonParser("[1,2,3,]").parse());
    }

    @Test
    public void parse_doesNotTolerate_leadingCommas() {
        assertThrows(SyntaxException.class,
            () -> new JsonParser("[,1,2,3]").parse());
    }

    @Test
    public void parse_doesNotTolerate_unquotedStrings() {
        assertThrows(SyntaxException.class,
            () -> new JsonParser("{\"\":hello}").parse());
    }

    @Test
    public void parse_doesNotTolerate_unbalancedContainers() {
        assertThrows(SyntaxException.class,
            () -> new JsonParser("[1,2,3").parse());
    }

    @Test
    public void parse_doesNotTolerate_newLinesInStrings() {
        assertThrows(SyntaxException.class,
            () -> new JsonParser("\"Hello,\nWorld!\"").parse());
    }

    @Test
    public void parse_doesNotTolerate_unknownEscapeSequences() {
        assertThrows(SyntaxException.class,
            () -> new JsonParser("\"\\y\"").parse());
    }

    @Test
    public void parse_doesNotTolerate_nonStringKeys() {
        assertThrows(SyntaxException.class,
            () -> new JsonParser("{hello:\"world\"}").parse());
    }
}
