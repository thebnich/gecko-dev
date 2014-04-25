package org.mozilla.gecko.autofill;

import org.mozilla.gecko.R;
import org.mozilla.gecko.autofill.AutofillGeckoClient.CardTypeListener;
import org.mozilla.gecko.autofill.AutofillGeckoClient.ValidationListener;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

public class PaymentEditDialog extends EditDialog<PaymentAutofillEntry> {
    private final Context mContext;

    public PaymentEditDialog(Context context, OnValidatedListener listener) {
        super(context, listener);
        mContext = context;
    }

    @Override
    protected ViewGroup createDialogView(PaymentAutofillEntry entry) {
        final ViewGroup view = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.autofill_payment_edit, null);

        ((EditText) view.findViewById(R.id.ccName)).setText(entry.name);
        ((EditText) view.findViewById(R.id.ccExpMonth)).setText(entry.expMonth);
        ((EditText) view.findViewById(R.id.ccExpYear)).setText(entry.expYear);
        ((EditText) view.findViewById(R.id.ccCsc)).setText(entry.csc);

        final EditText ccNumber = (EditText) view.findViewById(R.id.ccNumber);
        ccNumber.setText(entry.num);

        final TextView ccType = (TextView) view.findViewById(R.id.ccType);
        ccNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                AutofillGeckoClient.getCreditCardType(s.toString(), new CardTypeListener() {
                    @Override
                    public void onCardType(String cardType) {
                        ccType.setText(cardType);
                    }
                });
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    protected PaymentAutofillEntry createEntry() {
        return new PaymentAutofillEntry(null);
    }

    @Override
    protected void updateEntryFromInputs(View view, PaymentAutofillEntry entry) {
        entry.name = ((EditText) view.findViewById(R.id.ccName)).getText().toString();
        entry.num = ((EditText) view.findViewById(R.id.ccNumber)).getText().toString();
        entry.expMonth = ((EditText) view.findViewById(R.id.ccExpMonth)).getText().toString();
        entry.expYear = ((EditText) view.findViewById(R.id.ccExpYear)).getText().toString();
        entry.csc = ((EditText) view.findViewById(R.id.ccCsc)).getText().toString();
    }

    @Override
    protected void validate(PaymentAutofillEntry entry, ValidationListener listener) {
        AutofillGeckoClient.validatePayment(entry, listener);
    }

    @Override
    protected void submit(PaymentAutofillEntry entry) {
        AutofillGeckoClient.submitPaymentEdit(entry);
    }
}
