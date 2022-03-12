package xjs.serialization.parser;

import xjs.core.JsonValue;

import java.io.File;
import java.io.IOException;

@FunctionalInterface
public interface ParsingFunction {
    JsonValue parse(final File file) throws IOException;
}