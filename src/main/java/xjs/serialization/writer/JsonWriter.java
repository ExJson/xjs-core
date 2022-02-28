package xjs.serialization.writer;

import xjs.core.JsonArray;
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
        boolean following = false;
        final boolean condensed = value.isContainer()
            && value.asContainer().size() > 0
            && value.asContainer().get(0).getLinesAbove() == 0;

        switch (value.getType()) {
            case OBJECT:
                this.open(condensed, '{');
                for (final JsonObject.Member member : value.asObject()) {
                    this.delimit(following, member.visit().getLinesAbove());
                    this.nl(level + 1, member.visit());
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
                    this.nl(level + 1, v);
                    this.write(v, level + 1);
                    following = true;
                }
                this.close(value.asArray(), condensed, level, ']');
                break;
            case INTEGER:
                this.writeInteger(value.asLong());
                break;
            case DECIMAL:
                this.writeDecimal(value.asDouble());
                break;
            case STRING:
                this.writeQuoted(value.asString(), '"');
                break;
            default:
                this.tw.write(value.toString());
        }
    }
}
