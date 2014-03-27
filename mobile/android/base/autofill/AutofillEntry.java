package org.mozilla.gecko.autofill;

public abstract class AutofillEntry {
    public String guid;

    public AutofillEntry(String guid) {
        this.guid = guid;
    }
}
