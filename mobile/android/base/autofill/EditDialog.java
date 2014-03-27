package org.mozilla.gecko.autofill;

import java.util.Map;

import org.mozilla.gecko.R;
import org.mozilla.gecko.autofill.AutofillGeckoClient.ValidationListener;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Generic dialog bridge that supports updating and validating entries.
 *
 * @param <T> entry type being edited
 */
public abstract class EditDialog<T extends AutofillEntry> {
    private final Context context;
    private OnValidatedListener listener;

    public EditDialog(Context context, OnValidatedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public interface OnValidatedListener {
        public void onValidated();
    }

    public void showDialog(final T entry) {
        final ViewGroup dialogView = createDialogView(entry);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);

        // Set an empty listener that gets overridden below. We can't set
        // the listener here since we need to prevent the dialog from being
        // dismissed when OK is clicked.
        builder.setPositiveButton(R.string.button_ok, null);
        builder.setNegativeButton(R.string.button_cancel, null);

        final AlertDialog dialog = builder.show();

        // Override dialog button listener, preventing it from dismissing the dialog.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View buttonView) {
                // Create an entry used to validate the form fields. We don't want to modify the
                // entry passed into showDialog yet since the validation may fail.
                final T candidateEntry = createEntry();
                updateEntryFromInputs(dialogView, candidateEntry);
                validate(candidateEntry, new ValidationListener() {
                    @Override
                    public void onSuccess() {
                        // Our candidate was successfully validated; update our actual entry
                        // and submit it.
                        updateEntryFromInputs(dialogView, entry);
                        submit(entry);
                        listener.onValidated();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(final Map<String, String> errors) {
                        showErrors(dialogView, errors);
                    }
                });
            }
        });
    }

    private void showErrors(final ViewGroup group, final Map<String, String> errors) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ViewGroup) {
                showErrors((ViewGroup) child, errors);
            } else if (child instanceof AutofillEditText) {
                final AutofillEditText editText = (AutofillEditText) child;
                final String fieldName = editText.getFieldName();
                if (errors.containsKey(fieldName)) {
                    editText.setError(errors.get(fieldName));
                }
            }
        }
    }

    protected abstract ViewGroup createDialogView(T entry);
    protected abstract T createEntry();
    protected abstract void updateEntryFromInputs(View view, T entry);
    protected abstract void validate(T entry, ValidationListener listener);
    protected abstract void submit(T entry);
}
