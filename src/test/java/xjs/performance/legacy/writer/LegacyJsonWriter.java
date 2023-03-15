package xjs.performance.legacy.writer;

import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.serialization.writer.JsonWriterOptions;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class LegacyJsonWriter extends LegacyAbstractJsonWriter {

    public LegacyJsonWriter(final File file, final boolean format) throws IOException {
        super(file, format);
    }

    public LegacyJsonWriter(final Writer writer, final boolean format) {
        super(writer, format);
    }

    public LegacyJsonWriter(final File file, final JsonWriterOptions options) throws IOException {
        super(file, options);
    }

    public LegacyJsonWriter(final Writer writer, final JsonWriterOptions options) {
        super(writer, options);
    }

    @Override
    public void write(final JsonValue value, final int level) throws IOException {
        final boolean condensed = this.isCondensed(value);
        JsonValue previous = null;

        switch (value.getType()) {
            case OBJECT:
                this.open(value.asObject(), condensed, '{');
                for (final JsonObject.Member member : value.asObject()) {
                    this.delimit(previous != null, member.getOnly().getLinesAbove());
                    this.writeLinesAbove(level + 1, value, previous, condensed, member.getOnly());
                    this.writeQuoted(member.getKey(), '"');
                    this.tw.write(':');
                    this.separate(level + 2, member.getOnly());
                    this.write(member.getOnly(), level + 1);
                    previous = member.getOnly();
                }
                this.close(value.asObject(), condensed, level, '}');
                break;
            case ARRAY:
                this.open(value.asArray(), condensed, '[');
                for (final JsonValue v : value.asArray().visitAll()) {
                    this.delimit(previous != null, v.getLinesAbove());
                    this.writeLinesAbove(level + 1, value, previous, condensed, v);
                    this.write(v, level + 1);
                    previous = v;
                }
                this.close(value.asArray(), condensed, level, ']');
                break;
            case NUMBER:
                this.writeNumber(value.asDouble());
                break;
            case STRING:
                this.writeQuoted(value.asString(), '"');
                break;
            default:
                this.tw.write(value.toString());
        }
    }
}
