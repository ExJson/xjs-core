package xjs.serialization.util;

import org.junit.jupiter.api.Test;
import xjs.exception.SyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ImplicitStringUtilsTest {

    @Test
    public void isBalanced_emptyString_returnsTrue() {
        assertTrue(ImplicitStringUtils.isBalanced(""));
    }

    @Test
    public void isBalanced_wordTokens_returnsTrue() {
        assertTrue(ImplicitStringUtils.isBalanced("normal words"));
    }

    @Test
    public void isBalanced_singleContainer_returnsTrue() {
        assertTrue(ImplicitStringUtils.isBalanced("{}"));
    }

    @Test
    public void isBalanced_nestedContainers_returnsTrue() {
        assertTrue(ImplicitStringUtils.isBalanced("[{()()}]"));
    }

    @Test
    public void isBalanced_quotes_returnsTrue() {
        assertTrue(ImplicitStringUtils.isBalanced("''"));
    }

    @Test
    public void isBalanced_multilineQuote_returnsTrue() {
        assertTrue(ImplicitStringUtils.isBalanced("'''\ntext\n'''"));
    }

    @Test
    public void isBalanced_nestedEscapedQuotes_returnsTrue() {
        assertTrue(ImplicitStringUtils.isBalanced("('\\'')"));
    }

    @Test
    public void isBalanced_quotedContainers_returnsTrue() {
        assertTrue(ImplicitStringUtils.isBalanced("(')')"));
    }

    @Test
    public void isBalanced_escapedContainers_returnsTrue() {
        assertTrue(ImplicitStringUtils.isBalanced("(\\))"));
    }

    @Test
    public void isBalanced_inlineCommentedContainers_returnTrue() {
        assertTrue(ImplicitStringUtils.isBalanced("(/*)*/)"));
    }

    @Test
    public void isBalanced_lineCommentedContainers_returnTrue() {
        assertTrue(ImplicitStringUtils.isBalanced("(//)\n)"));
    }

    @Test
    public void isBalanced_betweenLines_returnsTrue() {
        assertTrue(ImplicitStringUtils.isBalanced("(\n{\n}\n)"));
    }

    @Test
    public void isBalanced_openerOnly_returnsFalse() {
        assertFalse(ImplicitStringUtils.isBalanced("("));
    }

    @Test
    public void isBalanced_closerOnly_returnsFalse() {
        assertFalse(ImplicitStringUtils.isBalanced("]"));
    }

    @Test
    public void isBalanced_oneNotClosed_returnsFalse() {
        assertFalse(ImplicitStringUtils.isBalanced("()[{(}]()"));
    }

    @Test
    public void isBalanced_oneNotOpened_returnsFalse() {
        assertFalse(ImplicitStringUtils.isBalanced("()[{)}]()"));
    }

    @Test
    public void isBalanced_closedOutOfOrder_returnsFalse() {
        assertFalse(ImplicitStringUtils.isBalanced("({)}"));
    }

    @Test
    public void isBalanced_unclosedQuoted_returnsFalse() {
        assertFalse(ImplicitStringUtils.isBalanced("'"));
    }

    @Test
    public void isBalanced_quotedNewline_returnsValues() {
        assertFalse(ImplicitStringUtils.isBalanced("'\n'"));
    }

    @Test
    public void selectValue_isolatesSingleWord() {
        assertEquals("word",
            ImplicitStringUtils.select("word", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_isolatesMultipleWords() {
        assertEquals("multiple words",
            ImplicitStringUtils.select("multiple words", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_isolatesSingleWord_toEndOfLine() {
        assertEquals("multiple",
            ImplicitStringUtils.select("multiple\nlines", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_isolatesSingleWord_toToFirstComma() {
        assertEquals("multiple",
            ImplicitStringUtils.select("multiple,lines", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_isolatesQuotedText() {
        assertEquals("'quoted,text'",
            ImplicitStringUtils.select("'quoted,text'", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_isolatesMultilineQuotedText() {
        assertEquals("'''\ntext\n'''",
            ImplicitStringUtils.select("'''\ntext\n'''", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_isolatesUntilBalanced() {
        assertEquals("balanced{\nunbalanced\n}",
            ImplicitStringUtils.select("balanced{\nunbalanced\n}\ngood", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_doesNotIncludeComma() {
        assertEquals("a{b}c",
            ImplicitStringUtils.select("a{b}c,", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_trimsWhitespace() {
        assertEquals("value{\n()()}",
            ImplicitStringUtils.select("value{\n()()}  ", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_trimsLineComment() {
        assertEquals("value{}",
            ImplicitStringUtils.select("value{}//comment", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_trimsInlineComment() {
        assertEquals("value{}",
            ImplicitStringUtils.select("value{}/*inline*/", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_doesNotTrimBalancedComment() {
        assertEquals("value{/**/}",
            ImplicitStringUtils.select("value{/**/}", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_doesNotTrimInnerComment() {
        assertEquals("value{}/*inner*/more",
            ImplicitStringUtils.select("value{}/*inner*/more", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_trimsWhitespaceBeforeComment() {
        assertEquals("value",
            ImplicitStringUtils.select("value # comment", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_allowsDelimiter_whenBalanced() {
        assertEquals("{value,}",
            ImplicitStringUtils.select("{value,}", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_isolatesMultipleLines_whenEscaped() {
        assertEquals("multiple\\\nlines",
            ImplicitStringUtils.select("multiple\\\nlines", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_canContainUnbalancedColon() {
        assertEquals("namespace:id",
            ImplicitStringUtils.select("namespace:id", 0, StringContext.VALUE));
    }

    @Test
    public void selectValue_requiresBalancedText() {
        assertThrows(SyntaxException.class, () ->
            ImplicitStringUtils.select("{", 0, StringContext.VALUE));
    }

    @Test
    public void selectKey_readsToFirstColon() {
        assertEquals("key",
            ImplicitStringUtils.select("key:value", 0, StringContext.KEY));
    }

    @Test
    public void selectKey_isolatesMultipleLines() {
        assertEquals("multiple\nlines",
            ImplicitStringUtils.select("multiple\nlines:", 0, StringContext.KEY));
    }

    @Test
    public void selectKey_mayContainComments() {
        assertEquals("l1#c\nl2",
            ImplicitStringUtils.select("l1#c\nl2:", 0, StringContext.KEY));
    }

    @Test
    public void selectKey_requiresColon() {
        assertThrows(SyntaxException.class, () ->
            ImplicitStringUtils.select("value", 0, StringContext.KEY));
    }

    @Test
    public void escape_reprintsBalancedString() {
        assertEquals("all good here {}",
            ImplicitStringUtils.escape("all good here {}", StringContext.VALUE));
    }

    @Test
    public void escapeValue_escapesNewline() {
        assertEquals("l1\\\nl2",
            ImplicitStringUtils.escape("l1\nl2", StringContext.VALUE));
    }

    @Test
    public void escapeKey_doesNotEscapeNewline() {
        assertEquals("l1\nl2",
            ImplicitStringUtils.escape("l1\nl2", StringContext.KEY));
    }

    @Test
    public void escapeValue_escapesComma() {
        assertEquals("v1\\,v2",
            ImplicitStringUtils.escape("v1,v2", StringContext.VALUE));
    }

    @Test
    public void escapeKey_escapesColon() {
        assertEquals("k1\\:k2",
            ImplicitStringUtils.escape("k1:k2", StringContext.KEY));
    }

    @Test
    public void escape_doesNotSupport_unbalancedText() {
        assertEquals("())",
            ImplicitStringUtils.escape("())", StringContext.VALUE));
    }

    @Test
    public void escape_doesNotAcknowledgeOrder() {
        assertEquals("[{]}",
            ImplicitStringUtils.escape("[{]}", StringContext.VALUE));
    }
}
