package xjs.serialization;

import java.util.concurrent.atomic.AtomicReference;

public class JsonSerializationContext {

    private static final AtomicReference<String> EOL =
        new AtomicReference<>(System.getProperty("line.separator"));

    public static String getEol() {
        return EOL.get();
    }

    public static void setEol(final String eol) {
        EOL.set(eol);
    }
}
