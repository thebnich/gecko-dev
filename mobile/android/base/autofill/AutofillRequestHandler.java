package org.mozilla.gecko.autofill;

import org.mozilla.gecko.R;
import org.mozilla.gecko.autofill.AutofillGeckoClient.AutofillListener;
import org.mozilla.gecko.autofill.AutofillGeckoClient.DataListener;
import org.mozilla.gecko.autofill.AutofillGeckoClient.SubmitListener;
import org.mozilla.gecko.preferences.GeckoPreferences;
import org.mozilla.gecko.util.ThreadUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;

/**
 * Handles requestAutocomplete request by showing the
 * autofill dialog with selectors for each entry type.
 */
public class AutofillRequestHandler implements AutofillListener, DataListener {
    private final Activity activity;
    private AlertDialog dialog;
    private SubmitListener submitListener;

    public AutofillRequestHandler(Activity activity) {
        this.activity = activity;

        AutofillGeckoClient.setAutofillListener(this);
    }

    // BRN: we should make sure only the selected tab can show a dialog
    @Override
    public void onRequestAutofill(SubmitListener submitListener) {
        ThreadUtils.assertOnUiThread();

        this.submitListener = submitListener;
        loadDialog();
    }

    // If the autofill values change (e.g., while in the prefs screen), update the dialog.
    // BRN: Currently just a brute force method to kill the existing dialog and create a new one.
    // This is a crappy approach, and we should rebuild the existing dialog instead of throwing away
    // the existing one.
    //   * If we get this event, do we really want to refetch the entire data set? how can we improve this?
    //   * If the user has entries selected in the dialog, make sure not to lose their selection
    @Override
    public void onUpdated() {
        ThreadUtils.assertOnUiThread();

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            loadDialog();
        }
    }

    @Override
    public void onData(AutofillEntries entries) {
        showDialog(entries);
    }

    private void loadDialog() {
        ThreadUtils.assertOnUiThread();

        AutofillGeckoClient.getAutofillData(this);
    }

    private void showDialog(AutofillEntries entries) {
        ThreadUtils.assertOnUiThread();

        if (dialog != null && dialog.isShowing()) {
            // Don't show another prompt if one is already active.
            // This should never happen since we have a similar guard in Gecko.
            throw new AutofillException("Autofill dialog is already being displayed");
        }

        final View dialogView = LayoutInflater.from(activity).inflate(R.layout.autofill_dialog, null);
        final Spinner paymentView = (Spinner) dialogView.findViewById(R.id.payment);
        final Spinner billingView = (Spinner) dialogView.findViewById(R.id.billing);
        final Spinner shippingView = (Spinner) dialogView.findViewById(R.id.shipping);
        bindViews(activity, paymentView, billingView, shippingView, entries);

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);

        // Set an empty listener that gets overridden below. We can't set
        // the listener here since we need to prevent the dialog from being
        // dismissed when OK is clicked.
        builder.setPositiveButton(R.string.button_ok, null);
        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                submitListener.cancel();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                submitListener.cancel();
            }
        });

        dialog = builder.create();
        dialog.show();

        // Override dialog button listener, preventing it from dismissing the dialog.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View buttonView) {
                // BRN: validate each of these as needed
                final PaymentAutofillEntry payment = (PaymentAutofillEntry) paymentView.getSelectedItem();
                final AddressAutofillEntry billing = (AddressAutofillEntry) billingView.getSelectedItem();
                final AddressAutofillEntry shipping = (AddressAutofillEntry) shippingView.getSelectedItem();
                submitListener.submit(payment, billing, shipping);
                dialog.dismiss();
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // BRN: onUpdated needs to be fixed before we can set this to null
                // submitListener = null;

                dialog = null;
            }
        });
    }

    private static void bindViews(final Activity activity,
                                    Spinner paymentView,
                                    Spinner billingView,
                                    Spinner shippingView,
                                    AutofillEntries entries) {
        ThreadUtils.assertOnUiThread();

        final PaymentAdapter paymentAdapter = new PaymentAdapter(activity);
        paymentAdapter.addAll(entries.payments);
        paymentView.setAdapter(paymentAdapter);
        paymentView.setSelection(entries.paymentIndex);

        final AddressAdapter billingAdapter = new AddressAdapter(activity);
        billingAdapter.addAll(entries.addresses);
        billingView.setAdapter(billingAdapter);
        billingView.setSelection(entries.billingIndex);

        final AddressAdapter shippingAdapter = new ShippingAddressAdapter(activity);
        shippingAdapter.addAll(entries.addresses);
        shippingView.setAdapter(shippingAdapter);
        // "Use billing address" is prepended to the shipping address list, so adjust the index by 1.
        // -1 means to use the billing address, and our first view (index 0) is the "Use Billing Address" view.
        shippingView.setSelection(entries.shippingIndex + 1);
    }

    public void destroy() {
        ThreadUtils.assertOnUiThread();

        if (dialog != null && dialog.isShowing()) {
            submitListener.cancel();
            dialog.dismiss();
        }

        AutofillGeckoClient.setAutofillListener(null);
    }
}
