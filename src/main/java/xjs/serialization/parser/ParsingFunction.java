package xjs.serialization.parser;

import xjs.core.JsonValue;
import xjs.exception.SyntaxException;
import xjs.serialization.writer.ValueWriter;
import xjs.serialization.writer.WritingFunction;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Represents an entire procedure for converting {@link File files} into
 * {@link JsonValue JSON values}.
 */
@FunctionalInterface
public interface ParsingFunction {

    /**
     * The main function being represented by this interface.
     *
     * @param file The file being parsed.
     * @return The {@link JsonValue} being represented by the file.
     * @throws IOException If the underlying {@link FileReader} throws an exception.
     * @throws SyntaxException if the file is syntactically invalid.
     * @see JsonParser#JsonParser(File)
     * @see JsonParser#parse()
     */
    JsonValue parse(final File file) throws IOException;

    /**
     * Builds a WritingFunction when given a reference to the constructor
     * of any {@link ValueWriter}.
     *
     * @param c The constructor used to build a {@link ValueWriter}.
     * @return A reusable {@link WritingFunction}.
     */
    static ParsingFunction fromParser(final ValueParser.FileConstructor c) {
        return file -> {
            final ValueParser parser = c.construct(file);
            final JsonValue value = parser.parse();

            try {
                parser.close();
            } catch (final Exception e) {
                throw new IOException(e);
            }
            return value;
        };
    }
}