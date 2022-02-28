package xjs.core;

public abstract class JsonBoolean extends JsonValue {

    private JsonBoolean() {}

    public static JsonTrue jsonTrue() {
        return new JsonTrue();
    }

    public static JsonFalse jsonFalse() {
        return new JsonFalse();
    }

    public static JsonBoolean get(final boolean value) {
        return value ? new JsonTrue() : new JsonFalse();
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o.getClass().equals(this.getClass())) {
            return this.metadataEquals((JsonBoolean) o);
        }
        return false;
    }

    public static class JsonTrue extends JsonBoolean {

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
        public JsonTrue deepCopy(final boolean trackAccess) {
            return (JsonTrue) new JsonTrue().setDefaultMetadata(this);
        }

        @Override
        public JsonTrue unformatted() {
            return new JsonTrue();
        }

        @Override
        public String toString() {
            return "true";
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + 1;
        }
    }

    public static class JsonFalse extends JsonBoolean {

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
        public JsonFalse deepCopy(final boolean trackAccess) {
            return (JsonFalse) new JsonFalse().setDefaultMetadata(this);
        }

        @Override
        public JsonValue unformatted() {
            return new JsonFalse();
        }

        @Override
        public String toString() {
            return "false";
        }
    }
}
