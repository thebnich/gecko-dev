package org.mozilla.gecko.autofill;

import org.mozilla.gecko.R;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Adapter for displaying address entries.
 */
public class AddressAdapter extends ArrayAdapter<AddressAutofillEntry> {
    private static final int RESOURCE_ID = R.layout.autofill_address_entry;

    public AddressAdapter(Context context) {
        super(context, RESOURCE_ID);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AddressAutofillEntry entry = getItem(position);

        if (convertView == null) {
            convertView = createView(parent, entry);
        }

        ViewGroup group = (ViewGroup) convertView;

        ((TextView) group.findViewById(R.id.address)).setText(getAddressLine(entry));
        ((TextView) group.findViewById(R.id.email)).setText(entry.email);
        ((TextView) group.findViewById(R.id.tel)).setText(entry.tel);

        return group;
    }

    protected View createView(ViewGroup parent, AddressAutofillEntry entry) {
        return LayoutInflater.from(getContext()).inflate(RESOURCE_ID, parent, false);
    }

    private String getAddressLine(AddressAutofillEntry entry) {
        // TODO: localize separators
        return entry.name + ", " +
               entry.addressLine1 + ", " +
               (TextUtils.isEmpty(entry.addressLine2) ? "" : entry.addressLine2 + ", ") +
               entry.locality + ", " +
               entry.region + " " +
               entry.postalCode;
    }
}
