package xjs.serialization.writer;

import xjs.core.JsonObject;
import xjs.core.JsonValue;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class JsonWriter extends AbstractJsonWriter {

    public JsonWriter(final File file, final boolean format) throws IOException {
        super(file, format);
    }

    public JsonWriter(final Writer writer, final boolean format) {
        super(writer, format);
    }

    public JsonWriter(final Writer writer, final JsonWriterOptions options) {
        super(writer, options);
    }

    @Override
    public void write(final JsonValue value, final int level) throws IOException {
        final boolean condensed = this.isCondensed(value);
        boolean following = false;

        switch (value.getType()) {
            case OBJECT:
                this.open(condensed, '{');
                for (final JsonObject.Member member : value.asObject()) {
                    this.delimit(following, member.visit().getLinesAbove());
                    this.writeLinesAbove(level + 1, !following, condensed, member.visit());
                    this.writeQuoted(member.getKey(), '"');
                    this.tw.write(':');
                    this.separate(level + 2, member.visit());
                    this.write(member.visit(), level + 1);
                    following = true;
                }
                this.close(value.asObject(), condensed, level, '}');
                break;
            case ARRAY:
                this.open(condensed, '[');
                for (final JsonValue v : value.asArray().visitAll()) {
                    this.delimit(following, v.getLinesAbove());
                    this.writeLinesAbove(level + 1, !following, condensed, v);
                    this.write(v, level + 1);
                    following = true;
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
