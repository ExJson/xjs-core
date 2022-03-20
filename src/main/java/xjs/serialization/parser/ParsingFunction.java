package xjs.serialization.parser;

import xjs.core.JsonValue;
import xjs.exception.SyntaxException;

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
     * @see AbstractJsonParser#AbstractJsonParser(File)
     * @see AbstractJsonParser#parse()
     */
    JsonValue parse(final File file) throws IOException;
}