package org.mozilla.gecko.autofill;

import org.mozilla.gecko.R;
import org.mozilla.gecko.autofill.AutofillGeckoClient.ValidationListener;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * Dialog for editing fields of a billing or shipping autofill entry.
 */
public class AddressEditDialog extends EditDialog<AddressAutofillEntry> {
    private final Context context;

    public AddressEditDialog(Context context, OnValidatedListener listener) {
        super(context, listener);
        this.context = context;
    }

    @Override
    protected ViewGroup createDialogView(AddressAutofillEntry entry) {
        final ViewGroup view = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.autofill_address_edit, null);

        ((EditText) view.findViewById(R.id.name)).setText(entry.name);
        ((EditText) view.findViewById(R.id.addressLine1)).setText(entry.addressLine1);
        ((EditText) view.findViewById(R.id.addressLine2)).setText(entry.addressLine2);
        ((EditText) view.findViewById(R.id.locality)).setText(entry.locality);
        ((EditText) view.findViewById(R.id.region)).setText(entry.region);
        ((EditText) view.findViewById(R.id.postalCode)).setText(entry.postalCode);
        ((EditText) view.findViewById(R.id.country)).setText(entry.country);
        ((EditText) view.findViewById(R.id.tel)).setText(entry.tel);
        ((EditText) view.findViewById(R.id.email)).setText(entry.email);

        return view;
    }

    protected AddressAutofillEntry createEntry() {
        return new AddressAutofillEntry(null);
    }

    @Override
    protected void updateEntryFromInputs(View view, AddressAutofillEntry entry) {
        entry.name = ((EditText) view.findViewById(R.id.name)).getText().toString();
        entry.addressLine1 = ((EditText) view.findViewById(R.id.addressLine1)).getText().toString();
        entry.addressLine2 = ((EditText) view.findViewById(R.id.addressLine2)).getText().toString();
        entry.locality = ((EditText) view.findViewById(R.id.locality)).getText().toString();
        entry.region = ((EditText) view.findViewById(R.id.region)).getText().toString();
        entry.postalCode = ((EditText) view.findViewById(R.id.postalCode)).getText().toString();
        entry.country = ((EditText) view.findViewById(R.id.country)).getText().toString();
        entry.tel = ((EditText) view.findViewById(R.id.tel)).getText().toString();
        entry.email = ((EditText) view.findViewById(R.id.email)).getText().toString();
    }

    @Override
    protected void validate(AddressAutofillEntry entry, ValidationListener listener) {
        AutofillGeckoClient.validateAddress(entry, listener);
    }

    @Override
    protected void submit(AddressAutofillEntry entry) {
        AutofillGeckoClient.submitAddressEdit(entry);
    }
}
