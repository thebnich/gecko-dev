package org.mozilla.gecko.autofill;

public class AutofillException extends RuntimeException {
    public AutofillException(Exception e) {
        super(e);
    }

    public AutofillException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 1L;
}
