package org.mozilla.gecko.autofill;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Set of all existing autofill entries.
 */
public class AutofillEntries {
    public final Collection<PaymentAutofillEntry> payments = new ArrayList<PaymentAutofillEntry>();
    public int paymentIndex = -1;

    public final Collection<AddressAutofillEntry> addresses = new ArrayList<AddressAutofillEntry>();
    public int billingIndex = -1;
    public int shippingIndex = -1;
}
