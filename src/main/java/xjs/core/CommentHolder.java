package xjs.core;

import xjs.serialization.util.CommentUtils;

import java.util.Objects;

public class CommentHolder {

    private String headerData;
    private String eolData;
    private String footerData;
    private String valueData;
    private String interiorData;

    public boolean hasAny() {
        return this.headerData != null
            && this.eolData != null
            && this.footerData != null
            && this.valueData != null
            && this.interiorData != null;
    }

    public String get(final CommentType type) {
        return CommentUtils.strip(this.getData(type));
    }

    public String getData(final CommentType type) {
        switch (type) {
            case HEADER: return this.headerData;
            case EOL: return this.eolData;
            case FOOTER: return this.footerData;
            case VALUE: return this.valueData;
            default: return this.interiorData;
        }
    }

    public void set(final CommentType type, final CommentStyle style, final String text) {
        this.setData(type, CommentUtils.format(style, text));
    }

    public void append(final CommentType type, final CommentStyle style, final String text) {
        final String data = this.getData(type);
        if (data.isEmpty()) {
            this.setData(type, CommentUtils.format(style, text));
        } else {
            this.setData(type, data + "\n" + CommentUtils.format(style, text));
        }
    }

    public void prepend(final CommentType type, final CommentStyle style, final String text) {
        final String data = this.getData(type);
        if (data.isEmpty()) {
            this.setData(type, CommentUtils.format(style, text));
        } else {
            this.setData(type, CommentUtils.format(style, text) + "\n" + data);
        }
    }

    public void setData(final CommentType type, final String data) {
        switch (type) {
            case HEADER: this.headerData = data; break;
            case EOL: this.eolData = data; break;
            case FOOTER: this.footerData = data; break;
            case VALUE: this.valueData = data; break;
            default: this.interiorData = data;
        }
    }

    @Override
    public int hashCode() {
        int result = 0;
        if (this.headerData != null) result = 31 * result + this.headerData.hashCode();
        if (this.eolData != null) result = 31 * result + this.eolData.hashCode();
        if (this.footerData != null) result = 31 * result + this.footerData.hashCode();
        if (this.valueData != null) result = 31 * result + this.valueData.hashCode();
        if (this.interiorData != null) result = 31 * result + this.interiorData.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof CommentHolder) {
            final CommentHolder other = (CommentHolder) o;
            return Objects.equals(this.headerData, other.headerData)
                && Objects.equals(this.eolData, other.eolData)
                && Objects.equals(this.footerData, other.footerData)
                && Objects.equals(this.valueData, other.valueData)
                && Objects.equals(this.interiorData, other.interiorData);
        }
        return false;
    }
}
