package com.clinic.enums;

public enum Access {
    NONE, READ, WRITE;

    /** WRITE implies READ. */
    public boolean covers(Access required) {
        return this.ordinal() >= required.ordinal();
    }
}
