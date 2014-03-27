package org.mozilla.gecko.preferences;

import java.util.Collection;

import org.mozilla.gecko.autofill.AddressAutofillEntry;
import org.mozilla.gecko.autofill.AddressEditDialog;
import org.mozilla.gecko.autofill.AutofillEntries;
import org.mozilla.gecko.autofill.EditDialog.OnValidatedListener;
import org.mozilla.gecko.sync.Utils;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class AutofillAddressCategory extends AutofillCategory<AddressAutofillEntry> {
    private static final String ADD_KEY = "address_add";

    public AutofillAddressCategory(Context context) {
        super(context);
    }

    public AutofillAddressCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutofillAddressCategory(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected String getAddKey() {
        return ADD_KEY;
    }

    @Override
    protected Collection<AddressAutofillEntry> getCollection(AutofillEntries entries) {
        return entries.addresses;
    }

    @Override
    protected AddressAutofillEntry createEntry() {
        return new AddressAutofillEntry(Utils.generateGuid());
    }

    @Override
    protected void showDialog(AddressAutofillEntry entry, OnValidatedListener listener) {
        new AddressEditDialog(getContext(), listener).showDialog(entry);
    }

    @Override
    protected void updateEntryPreference(Preference pref, AddressAutofillEntry entry) {
        pref.setTitle(entry.addressLine1);
    }
}
