package org.mozilla.gecko.autofill;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;

import org.mozilla.gecko.R;
import org.mozilla.gecko.autofill.AutofillData.AddressSection;
import org.mozilla.gecko.autofill.AutofillData.Field;
import org.mozilla.gecko.autofill.AutofillData.Section;
import org.mozilla.gecko.autofill.AutofillGeckoClient.AutofillListener;
import org.mozilla.gecko.autofill.AutofillGeckoClient.SubmitListener;
import org.mozilla.gecko.util.ThreadUtils;

public class AutofillDialog implements AutofillListener {
    private final Context context;
    private AlertDialog dialog;
    private SubmitListener submitListener;

    private static final HashMap<String, String> DUMMY_DB = new HashMap<String, String>();

    static {
        // TODO: Replace with autofill storage module (bug 1018304).
        DUMMY_DB.put("name", "Mozzy La");
        DUMMY_DB.put("street-address", "331 E Evelyn Ave");
        DUMMY_DB.put("address-level2", "Mountain View");
        DUMMY_DB.put("address-level1", "CA");
        DUMMY_DB.put("country", "US");
        DUMMY_DB.put("postal-code", "94041");
        DUMMY_DB.put("email", "email@example.org");
    }

    public AutofillDialog(Context context) {
        this.context = context;

        AutofillGeckoClient.setAutofillListener(this);
    }

    @Override
    public void onRequestAutofill(AutofillData requestData, SubmitListener submitListener) {
        ThreadUtils.assertOnUiThread();

        this.submitListener = submitListener;
        showDialog(requestData);
    }

    private void showDialog(final AutofillData requestData) {
        ThreadUtils.assertOnUiThread();

        // This view is being inflated for a dialog, so no root exists yet.
        @SuppressLint("InflateParams")
        final View dialogView = LayoutInflater.from(context).inflate(R.layout.autofill_dummy_dialog, null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
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
                for (Section section : requestData) {
                    for (AddressSection addressSection : section) {
                        for (Field field : addressSection) {
                            final String fieldName = field.getFieldName();
                            if (!DUMMY_DB.containsKey(fieldName)) {
                                continue;
                            }

                            field.setValue(DUMMY_DB.get(fieldName));
                        }
                    }
                }

                submitListener.submit();
                dialog.dismiss();
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                submitListener = null;
                dialog = null;
            }
        });
    }

    public void destroy() {
        ThreadUtils.assertOnUiThread();

        if (dialog != null) {
            submitListener.cancel();
            dialog.dismiss();
        }

        AutofillGeckoClient.setAutofillListener(null);
    }
}
