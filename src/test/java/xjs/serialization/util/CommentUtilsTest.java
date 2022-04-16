package xjs.serialization.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xjs.core.CommentStyle;
import xjs.serialization.JsonContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class CommentUtilsTest {

    @BeforeAll
    static void setup() {
        JsonContext.setEol("\n");
    }

    @AfterAll
    static void teardown() {
        JsonContext.setEol(System.getProperty("line.separator"));
    }

    @Test
    public void format_generatesHashComment() {
        final String comment = "here's a comment";
        final String expected = "# here's a comment";

        assertEquals(expected, CommentUtils.format(CommentStyle.HASH, comment));
    }

    @Test
    public void format_generatesLineComment() {
        final String comment = "here's another comment";
        final String expected = "// here's another comment";

        assertEquals(expected, CommentUtils.format(CommentStyle.LINE, comment));
    }

    @Test
    public void format_generatesBlockComment() {
        final String comment = "here's a block comment";
        final String expected = "/* here's a block comment */";

        assertEquals(expected, CommentUtils.format(CommentStyle.BLOCK, comment));
    }

    @Test
    public void format_generatesMultilineHash() {
        final String comment = "here's a comment\nwith multiple lines";
        final String expected = "# here's a comment\n# with multiple lines";

        assertEquals(expected, CommentUtils.format(CommentStyle.HASH, comment));
    }

    @Test
    public void format_generatesMultilineLine() {
        final String comment = "here's a comment\nwith multiple lines";
        final String expected = "// here's a comment\n// with multiple lines";

        assertEquals(expected, CommentUtils.format(CommentStyle.LINE, comment));
    }

    @Test
    public void format_generatesMultilineBlock() {
        final String comment = "here's a block comment\nwith multiple lines";
        final String expected = "/*\n * here's a block comment\n * with multiple lines\n */";

        assertEquals(expected, CommentUtils.format(CommentStyle.BLOCK, comment));
    }

    @Test
    public void strip_stripsHashComment() {
        final String comment = "# hashed comment";
        final String expected = "hashed comment";

        assertEquals(expected, CommentUtils.strip(comment));
    }

    @Test
    public void strip_stripsLineComment() {
        final String comment = "// line comment";
        final String expected = "line comment";

        assertEquals(expected, CommentUtils.strip(comment));
    }

    @Test
    public void strip_stripsBlockComment() {
        final String comment = "/* block comment */";
        final String expected = "block comment";

        assertEquals(expected, CommentUtils.strip(comment));
    }

    @Test
    public void strip_stripsMultilineHash() {
        final String comment = "# hashed comment\n# second line";
        final String expected = "hashed comment\nsecond line";

        assertEquals(expected, CommentUtils.strip(comment));
    }

    @Test
    public void strip_stripsMultilineLine() {
        final String comment = "// line comment\n// second line";
        final String expected = "line comment\nsecond line";

        assertEquals(expected, CommentUtils.strip(comment));
    }

    @Test
    public void strip_stripsMultilineBlock() {
        final String comment = "/* block comment\n* second line\n */";
        final String expected = "block comment\nsecond line";

        assertEquals(expected, CommentUtils.strip(comment));
    }

    @Test
    public void strip_stripsComplexComment() {
        final String comment = "/* block comment\n* second line\n /*\n# third line\n// fourth line";
        final String expected = "block comment\nsecond line\nthird line\nfourth line";

        assertEquals(expected, CommentUtils.strip(comment));
    }

    @Test
    public void format_strip_preservesExactText() {
        final String comment = "Hello, World!";
        final String formatted = CommentUtils.format(CommentStyle.HASH, comment);

        assertEquals(comment, CommentUtils.strip(formatted));
    }
}
