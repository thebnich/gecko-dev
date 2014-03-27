package org.mozilla.gecko.autofill;

/**
 * Fields used in a payment autofill entry.
 */
public class PaymentAutofillEntry extends AutofillEntry {
    public String name;
    public String num;
    public String expMonth;
    public String expYear;
    public String csc;

    public PaymentAutofillEntry(String guid) {
        super(guid);
    }

    public PaymentAutofillEntry(String guid, String name, String num, String expMonth, String expYear, String csc) {
        super(guid);
        this.name = name;
        this.num = num;
        this.expMonth = expMonth;
        this.expYear = expYear;
        this.csc = csc;
    }
}
