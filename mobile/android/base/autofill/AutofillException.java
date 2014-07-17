package org.mozilla.gecko.autofill;

@SuppressWarnings("serial")
public class AutofillException extends RuntimeException {
    public AutofillException(Exception e) {
        super(e);
    }

    public AutofillException(String message) {
        super(message);
    }
}
