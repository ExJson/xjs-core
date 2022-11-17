package xjs.serialization.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import xjs.exception.SyntaxException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PositionTrackingReaderTest {

    private static final int MEDIUM_STRING = 1024;
    private static final int LARGE_STRING = 2048;
    private static final int GIANT_STRING = 16_384;

    private static final int[] STRING_SIZES = {
        MEDIUM_STRING, LARGE_STRING, GIANT_STRING
    };

    private static final int MINIMUM_BUFFER = 8;
    private static final int SMALL_BUFFER = 128;
    private static final int NORMAL_BUFFER = 1024;
    private static final int LARGE_BUFFER = 2048;

    private static final int[] BUFFER_SIZES = {
        MINIMUM_BUFFER, SMALL_BUFFER, NORMAL_BUFFER, LARGE_BUFFER
    };

    private static final String[] SPECIAL_CHARS = {
        "!", "@", "#", "$", "%", "^", "&", "*", "(", ")",
        "{", "}", "[", "]", ",", "\"", "'", ":", "<", ">",
        "\uD83D\uDE00", "\uD83E\uDD17", "\uD83E\uDD28"
    };

    @RepeatedTest(25)
    public void reader_readsExactString() {
        for (final Sample sample : TestData.generateMixedSet()) {
            for (PositionTrackingReader reader : sample.getAllReaders()) {
                assertEquals(sample.text, parseFullText(reader),
                    reader.getClass().getSimpleName());
            }
        }
    }

    @Test
    public void reader_capturesFullText() {
        for (final Sample sample : TestData.generateMixedSet()) {
            for (PositionTrackingReader reader : sample.getAllReaders()) {
                assertEquals(sample.text, getFullOutput(reader),
                    reader.getClass().getSimpleName());
            }
        }
    }

    @Test
    public void reader_tracksIndexExactly() throws IOException {
        final Sample sample = Sample.generate(GIANT_STRING, NORMAL_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            int index = 0;
            do {
                assertEquals(index++, reader.index,
                    reader.getClass().getSimpleName());
                reader.read();
            } while (!reader.isEndOfText());
        }
    }

    @Test
    public void reader_tracksColumnExactly() throws IOException {
        final String columns = "0123\n012345\n01234\n".repeat(100);
        final Sample sample = new Sample(columns, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.read();
            do {
                if (reader.current != '\n') {
                    final int expectedColumn =
                        Integer.parseInt("" + (char) reader.current);
                    assertEquals(expectedColumn, reader.getColumn(),
                        reader.getClass().getSimpleName());
                }
                reader.read();
            } while (!reader.isEndOfText());
        }
    }

    @Test
    public void reader_tracksLinesExactly() throws IOException {
        final String columns = "0123\n012345\n01234\n".repeat(100);
        final Sample sample = new Sample(columns, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            do {
                reader.read();
            } while (!reader.isEndOfText());
            assertEquals(300, reader.line,
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void readIf_doesNotAdvanceOnMismatch() throws IOException {
        final Sample sample = new Sample("abc", NORMAL_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.read();
            assertFalse(reader.readIf('x'));
            assertTrue(reader.readIf('a'));
            assertFalse(reader.readIf('y'));
            assertTrue(reader.readIf('b'));
            assertFalse(reader.readIf('z'));
            assertTrue(reader.readIf('c'));
            assertTrue(reader.isEndOfText());
        }
    }

    @Test
    public void expect_throwsOnMismatch() throws IOException {
        final Sample sample = new Sample("abc", NORMAL_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.read();
            assertFalse(reader.readIf('x'));
            assertTrue(reader.readIf('a'));
            assertThrows(SyntaxException.class, () ->
                reader.expect('y'));
        }
    }

    @Test
    public void capture_capturesExactly() throws IOException {
        final Sample sample = new Sample("1234567891011", MINIMUM_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.read();
            assertTrue(reader.readIf('1'));
            assertTrue(reader.readIf('2'));
            assertTrue(reader.readIf('3'));
            reader.startCapture();
            while (!reader.isEndOfText()) {
                reader.read();
            }
            assertEquals("4567891011", reader.endCapture(),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void pauseCapture_doesNotMangleCapture() throws IOException {
        final Sample sample = new Sample("1234567891011", MINIMUM_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.read();
            assertTrue(reader.readIf('1'));

            reader.startCapture();
            assertTrue(reader.readIf('2'));
            assertTrue(reader.readIf('3'));

            reader.pauseCapture();
            assertTrue(reader.readIf('4'));
            assertTrue(reader.readIf('5'));

            reader.startCapture();
            while (!reader.isEndOfText()) {
                reader.read();
            }
            assertEquals("2367891011", reader.endCapture(),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void endCaptureEarly_doesNotMangleCapture() throws IOException {
        final Sample sample = new Sample("1234567891011", MINIMUM_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.read();
            assertTrue(reader.readIf('1'));

            reader.startCapture();
            assertTrue(reader.readIf('2'));
            assertTrue(reader.readIf('3'));
            assertTrue(reader.readIf('4'));
            assertTrue(reader.readIf('5'));
            assertTrue(reader.readIf('6'));

            assertEquals("23456", reader.endCapture(),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void readNumber_readsFullNumber() throws IOException {
        final Sample sample = new Sample("123456789", MINIMUM_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.read();
            assertEquals(123456789, reader.readNumber());
        }
    }

    @ParameterizedTest
    @ValueSource(chars = {'"', '\"'})
    public void readQuoted_readsFullQuote(final char quote) throws IOException {
        final String text = "'Hello, world!'";
        final Sample sample = new Sample(quote + text + quote, MINIMUM_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.read();
            assertEquals(text, reader.readQuoted(quote));
        }
    }

    @ParameterizedTest
    @ValueSource(chars = {'"', '\''})
    public void readQuoted_readsEscapeChars(final char quote) throws IOException {
        final String text = "\\t\\n\\u1234";
        final Sample sample = new Sample(quote + text + quote, MINIMUM_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.read();
            assertEquals("\t\n\u1234", reader.readQuoted(quote));
        }
    }

    private static String parseFullText(final PositionTrackingReader reader) {
        final StringBuilder sb = new StringBuilder();
        try {
            reader.read();
            while (reader.current != -1) {
                sb.append((char) reader.current);
                reader.read();
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    private static String getFullOutput(final PositionTrackingReader reader) {
        reader.captureFullText();
        try {
            do {
                reader.read();
            } while (reader.current != -1);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        return reader.getFullText();
    }

    private record TestData(List<Sample> samples) implements Iterable<Sample> {
        static TestData generateMixedSet() {
            final List<Sample> samples = new ArrayList<>();
            for (final int stringSize : STRING_SIZES) {
                for (final int bufferSize : BUFFER_SIZES) {
                    samples.add(Sample.generate(stringSize, bufferSize));
                }
            }
            return new TestData(samples);
        }

        @NotNull
        @Override
        public Iterator<Sample> iterator() {
            return this.samples.iterator();
        }
    }

    private record Sample(String text, int bufferSize) {
        static Sample generate(final int stringSize, final int bufferSize) {
            return new Sample(generateString(stringSize), bufferSize);
        }

        static String generateString(final int stringSize) {
            return generateString(stringSize, RandomUtils.nextInt(1, stringSize));
        }

        static String generateString(final int stringSize, final int specialChars) {
            final StringBuilder sb = new StringBuilder();
            final int numGroups = specialChars + 1;
            final int groupSize = (stringSize / numGroups) - 1;
            for (int i = 0; i < numGroups; i++) {
                sb.append(RandomStringUtils.random(groupSize, true, true));
                sb.append(SPECIAL_CHARS[RandomUtils.nextInt(0, SPECIAL_CHARS.length)]);
            }
            return sb.toString();
        }

        List<PositionTrackingReader> getAllReaders() {
            return List.of(
                    this.getStringReader(),
                    this.getCharBufferedReader());
        }

        PositionTrackingReader getStringReader() {
            return PositionTrackingReader.fromString(this.text);
        }

        PositionTrackingReader getCharBufferedReader() {
            return PositionTrackingReader.fromIs(this.getInputStream(), this.bufferSize);
        }

        InputStream getInputStream() {
            return new ByteArrayInputStream(this.text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
