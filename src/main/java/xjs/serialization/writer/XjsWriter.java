package xjs.serialization.writer;

import xjs.core.JsonValue;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class XjsWriter extends AbstractJsonWriter {

    public XjsWriter(final File file, final boolean format) throws IOException {
        super(file, format);
    }

    public XjsWriter(final Writer writer, final boolean format) {
        super(writer, format);
    }

    public XjsWriter(final Writer writer, final JsonWriterOptions options) {
        super(writer, options);
    }

    @Override
    protected void write(final JsonValue value, final int level) throws IOException {

    }
}
