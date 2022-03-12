package xjs.serialization.writer;

import xjs.core.JsonValue;

import java.io.IOException;
import java.io.Writer;

@FunctionalInterface
public interface WritingFunction {
    void write(final Writer writer, final JsonValue value, final JsonWriterOptions options) throws IOException;
}