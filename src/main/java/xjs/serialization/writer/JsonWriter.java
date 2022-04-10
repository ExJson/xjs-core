package xjs.serialization.writer;

import xjs.core.JsonObject;
import xjs.core.JsonReference;
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
    public void write(final JsonReference reference, final int level) throws IOException {
        final JsonValue value = reference.visit();
        final boolean condensed = this.isCondensed(reference);
        boolean following = false;

        switch (value.getType()) {
            case OBJECT:
                this.open(condensed, '{');
                for (final JsonObject.Member member : value.asObject()) {
                    this.delimit(following, member.getReference().getLinesAbove());
                    this.writeLinesAbove(level + 1, !following, condensed, member.getReference());
                    this.writeQuoted(member.getKey(), '"');
                    this.tw.write(':');
                    this.separate(level + 2, member.getReference());
                    this.write(member.getReference(), level + 1);
                    following = true;
                }
                this.close(reference, condensed, level, '}');
                break;
            case ARRAY:
                this.open(condensed, '[');
                for (final JsonReference r : value.asArray().references()) {
                    this.delimit(following, r.getLinesAbove());
                    this.writeLinesAbove(level + 1, !following, condensed, r);
                    this.write(r, level + 1);
                    following = true;
                }
                this.close(reference, condensed, level, ']');
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
