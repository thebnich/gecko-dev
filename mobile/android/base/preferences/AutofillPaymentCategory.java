package org.mozilla.gecko.preferences;

import java.util.Collection;

import org.mozilla.gecko.autofill.AutofillEntries;
import org.mozilla.gecko.autofill.EditDialog.OnValidatedListener;
import org.mozilla.gecko.autofill.PaymentAutofillEntry;
import org.mozilla.gecko.autofill.PaymentEditDialog;
import org.mozilla.gecko.sync.Utils;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class AutofillPaymentCategory extends AutofillCategory<PaymentAutofillEntry> {
    private static final String ADD_KEY = "payment_add";

    public AutofillPaymentCategory(Context context) {
        super(context);
    }

    public AutofillPaymentCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutofillPaymentCategory(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected String getAddKey() {
        return ADD_KEY;
    }

    @Override
    protected Collection<PaymentAutofillEntry> getCollection(AutofillEntries entries) {
        return entries.payments;
    }

    @Override
    protected PaymentAutofillEntry createEntry() {
        return new PaymentAutofillEntry(Utils.generateGuid());
    }

    @Override
    protected void showDialog(PaymentAutofillEntry entry, OnValidatedListener listener) {
        new PaymentEditDialog(getContext(), listener).showDialog(entry);
    }

    @Override
    protected void updateEntryPreference(Preference pref, PaymentAutofillEntry entry) {
        pref.setTitle(entry.num);
    }
}
