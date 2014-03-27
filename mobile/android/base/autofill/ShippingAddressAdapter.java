package org.mozilla.gecko.autofill;

import org.mozilla.gecko.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Adapter for displaying shipping address entries.
 */
public class ShippingAddressAdapter extends AddressAdapter {
    private AddressAutofillEntry useBilling = new AddressAutofillEntry(null);

    public ShippingAddressAdapter(Context context) {
        super(context);
    }

    @Override
    public int getCount() {
        return super.getCount() + 1;
    }

    @Override
    public AddressAutofillEntry getItem(int position) {
        if (position == 0) {
            return useBilling;
        }
        return super.getItem(position - 1);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewGroup group = (ViewGroup) super.getView(position, convertView, parent);

        // This is a hack that reuses autofill_address_entry for the
        // "use billing address for shipping" item. Using a separate view
        // sounds better on the surface, but there are some issues:
        //   1) spinners don't support multiple view types
        //   2) it's difficult to make the custom view's height match the others
        if (position == 0) {
            // BRN: localize
            ((TextView) group.findViewById(R.id.address)).setText("Use billing address for shipping");
        }

        return group;
    }
}
