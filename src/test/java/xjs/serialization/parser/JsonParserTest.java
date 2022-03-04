package xjs.serialization.parser;

import org.junit.jupiter.api.Test;
import xjs.core.JsonValue;
import xjs.exception.SyntaxException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class JsonParserTest extends CommonParserTest {

    @Test
    public void parse_doesNotTolerate_trailingCommas() {
        assertThrows(SyntaxException.class,
            () -> this.parse("[1,2,3,]"));
    }

    @Test
    public void parse_doesNotTolerate_leadingCommas() {
        assertThrows(SyntaxException.class,
            () -> this.parse("[,1,2,3]"));
    }

    @Test
    public void parse_doesNotTolerate_unquotedStrings() {
        assertThrows(SyntaxException.class,
            () -> this.parse("{\"\":hello}"));
    }

    @Test
    public void parse_doesNotTolerate_nonStringKeys() {
        assertThrows(SyntaxException.class,
            () -> this.parse("{hello:\"world\"}"));
    }

    @Override
    protected JsonValue parse(final String json) throws IOException {
        return new JsonParser(json).parse();
    }
}
