package xjs.core;

public abstract class JsonBoolean extends JsonValue {

    private JsonBoolean() {}

    public static JsonTrue jsonTrue() {
        return JsonTrue.INSTANCE;
    }

    public static JsonFalse jsonFalse() {
        return JsonFalse.INSTANCE;
    }

    public static JsonBoolean get(final boolean value) {
        return value ? JsonTrue.INSTANCE : JsonFalse.INSTANCE;
    }

    @Override
    public final JsonType getType() {
        return JsonType.BOOLEAN;
    }

    @Override
    public final boolean isPrimitive() {
        return true;
    }

    @Override
    public final boolean isBoolean() {
        return true;
    }

    @Override
    public JsonContainer intoContainer() {
        return this.intoArray();
    }

    @Override
    public JsonObject intoObject() {
        return new JsonObject().add("value", this);
    }

    @Override
    public JsonArray intoArray() {
        return new JsonArray().add(this);
    }

    public static class JsonTrue extends JsonBoolean {

        static final JsonTrue INSTANCE = new JsonTrue();

        private JsonTrue() {}

        @Override
        public boolean isTrue() {
            return true;
        }

        @Override
        public boolean asBoolean() {
            return true;
        }

        @Override
        public Number intoNumber() {
            return 1;
        }

        @Override
        public long intoLong() {
            return 1L;
        }

        @Override
        public int intoInt() {
            return 1;
        }

        @Override
        public double intoDouble() {
            return 1.0;
        }

        @Override
        public float intoFloat() {
            return 1.0F;
        }

        @Override
        public boolean intoBoolean() {
            return true;
        }

        @Override
        public String intoString() {
            return "true";
        }

        @Override
        public String toString() {
            return "true";
        }
    }

    public static class JsonFalse extends JsonBoolean {

        static final JsonFalse INSTANCE = new JsonFalse();

        private JsonFalse() {}

        @Override
        public boolean isFalse() {
            return true;
        }

        @Override
        public boolean asBoolean() {
            return false;
        }

        @Override
        public Number intoNumber() {
            return 0;
        }

        @Override
        public long intoLong() {
            return 0L;
        }

        @Override
        public int intoInt() {
            return 0;
        }

        @Override
        public double intoDouble() {
            return 0.0;
        }

        @Override
        public float intoFloat() {
            return 0.0F;
        }

        @Override
        public boolean intoBoolean() {
            return false;
        }

        @Override
        public String intoString() {
            return "false";
        }

        @Override
        public String toString() {
            return "false";
        }
    }
}
