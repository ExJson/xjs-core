package xjs.serialization.parser;

import org.junit.jupiter.api.Test;
import xjs.core.JsonArray;
import xjs.core.JsonObject;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.exception.SyntaxException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class CommonParserTest {

    @Test
    public final void parse_readsTrue() throws IOException {
        assertTrue(this.parseValue("true").isTrue());
    }

    @Test
    public final void parse_readsFalse() throws IOException {
        assertTrue(this.parseValue("false").isFalse());
    }

    @Test
    public final void parse_readsNull() throws IOException {
        assertTrue(this.parseValue("null").isNull());
    }

    @Test
    public final void parse_readsInteger() throws IOException {
        assertEquals(1234, this.parseValue("1234").asInt());
    }

    @Test
    public final void parse_readsDecimal() throws IOException {
        assertEquals(12.34, this.parseValue("12.34").asDouble());
    }

    @Test
    public final void parse_readsExponent() throws IOException {
        assertEquals(12.3e4, this.parseValue("12.3e4").asDouble());
    }

    @Test
    public final void parse_readsQuotedString() throws IOException {
        assertEquals("Hello, World!", this.parseValue("\"Hello, World!\"").asString());
    }

    @Test
    public final void parse_readsEmptyArray() throws IOException {
        assertEquals(new JsonArray(), this.parseValue("[]").asArray().unformatted());
    }

    @Test
    public final void parse_readsEmptyObject() throws IOException {
        assertEquals(new JsonObject(), this.parseValue("{}").asObject().unformatted());
    }

    @Test
    public final void parse_readsCondensedArray() throws IOException {
        final JsonArray parsed = this.parseValue("[1,2,3]").asArray();
        assertEquals(List.of(1, 2, 3), parsed.toList(JsonValue::asInt));
    }

    @Test
    public final void parse_readsMultilineArray() throws IOException {
        final JsonArray parsed = this.parseValue("[\n1,\n2,\n3\n]").asArray();
        assertEquals(List.of(1, 2, 3), parsed.toList(JsonValue::asInt));
    }

    @Test
    public final void parse_readsCondensedObject() throws IOException {
        final JsonObject parsed = this.parseValue("{\"1\":1,\"2\":2,\"3\":3}").asObject();
        assertEquals(Map.of("1", 1, "2", 2, "3", 3), parsed.toMap(JsonValue::asInt));
    }

    @Test
    public final void parse_readsMultilineObject() throws IOException {
        final JsonObject parsed = this.parseValue("{\n\"1\":1,\n\"2\":2,\n\"3\":3\n}").asObject();
        assertEquals(Map.of("1", 1, "2", 2, "3", 3), parsed.toMap(JsonValue::asInt));
    }

    @Test
    public final void parse_preservesWhitespaceAbove() throws IOException {
        assertEquals(2, this.parse("\n\ntrue").getLinesAbove());
    }

    @Test
    public final void parse_preservesWhitespaceBetween() throws IOException {
        assertEquals(3, this.parseValue("{\"\":\n\n\n\"\"}")
            .asObject().getReference(0).getLinesBetween());
    }

    @Test
    public final void parse_parsesRecursively() throws IOException {
        assertEquals(new JsonArray().add(1).add(new JsonArray()),
            this.parseValue("[1,[]]").asArray().unformatted());
    }

    @Test
    public final void parse_doesNotTolerate_unbalancedContainers() {
        assertThrows(SyntaxException.class,
            () -> this.parse("[1,2,3"));
    }

    @Test
    public final void parse_doesNotTolerate_newLinesInStrings() {
        assertThrows(SyntaxException.class,
            () -> this.parse("\"Hello,\nWorld!\""));
    }

    @Test
    public final void parse_doesNotTolerate_unknownEscapeSequences() {
        assertThrows(SyntaxException.class,
            () -> this.parse("\"\\y\""));
    }

    protected abstract JsonReference parse(final String json) throws IOException;

    protected JsonValue parseValue(final String json) throws IOException {
        return this.parse(json).get();
    }
}
