package org.mozilla.gecko.autofill;

import org.mozilla.gecko.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Adapter for displaying payment entries.
 */
public class PaymentAdapter extends ArrayAdapter<PaymentAutofillEntry> {
    private static final int RESOURCE_ID = R.layout.autofill_payment_entry;

    public PaymentAdapter(Context context) {
        super(context, RESOURCE_ID);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(RESOURCE_ID, parent, false);
        }

        final ViewGroup group = (ViewGroup) convertView;
        final PaymentAutofillEntry entry = getItem(position);
        ((TextView) group.findViewById(R.id.ccName)).setText(entry.name);
        ((TextView) group.findViewById(R.id.ccNumber)).setText(entry.num);

        return group;
    }
}
