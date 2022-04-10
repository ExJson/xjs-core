package xjs.serialization.writer;

import xjs.core.JsonReference;
import xjs.core.JsonValue;

import java.io.IOException;
import java.io.Writer;

/**
 * Represents the entire procedure for serializing {@link JsonValue JSON values}
 * to the disk.
 */
@FunctionalInterface
public interface WritingFunction {

    /**
     * The main function being represented by this interface.
     *
     * @param writer    The writer accepting the serialized output.
     * @param reference The formatted value being written into the writer.
     * @param options   The options used to indicate output formatting.
     * @throws IOException If the given {@link Writer} throws an exception.
     * @see AbstractJsonWriter#AbstractJsonWriter(Writer, JsonWriterOptions)
     * @see AbstractJsonWriter#write(JsonValue)
     */
    void write(final Writer writer, final JsonReference reference, final JsonWriterOptions options) throws IOException;

    /**
     * Defaulted implementation allowing {@link JsonValue values} to be printed directly.
     *
     * @param writer  The writer accepting the serialized output.
     * @param value   The value being written into the writer.
     * @param options The options used to indicate output formatting.
     * @throws IOException If the given {@link Writer} throws an exception.
     * @see AbstractJsonWriter#AbstractJsonWriter(Writer, JsonWriterOptions)
     * @see AbstractJsonWriter#write(JsonValue)
     */
    default void write(final Writer writer, final JsonValue value, final JsonWriterOptions options) throws IOException {
        if (value.isContainer()) {
            this.write(writer, value.asContainer().unformatted().intoReference(), options);
        } else {
            this.write(writer, value.intoReference(), options);
        }
    }
}