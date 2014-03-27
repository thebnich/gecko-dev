package org.mozilla.gecko.autofill;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.mozilla.gecko.EventDispatcher;
import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.GeckoEvent;
import org.mozilla.gecko.GeckoRequest;
import org.mozilla.gecko.util.EventCallback;
import org.mozilla.gecko.util.NativeEventListener;
import org.mozilla.gecko.util.NativeJSObject;
import org.mozilla.gecko.util.ThreadUtils;

/**
 * Bridge between Gecko and the Java front-end for accessing autofill data.
 */
public class AutofillGeckoClient {
    private static AutofillListener autofillListener;

    public interface ValidationListener {
        void onSuccess();
        void onError(Map<String, String> errors);
    }

    public interface SubmitListener {
        void submit(PaymentAutofillEntry payment, AddressAutofillEntry billing, AddressAutofillEntry shipping);
        void cancel();
    }

    /**
     * All callbacks are executed on the UI thread.
     */
    public interface AutofillListener {
        /**
         * Called when the page has fired requestAutocomplete().
         *
         * The listener *must* respond to the request by calling either
         * {@link AutofillGeckoClient#submitAutofill(PaymentAutofillEntry, AddressAutofillEntry, AddressAutofillEntry, SubmitListener)
         * or {@link AutofillGeckoClient#cancelAutofill()}.
         */
        void onRequestAutofill(SubmitListener submitListener);

        /**
         * Called when autofill entries have been updated.
         */
        void onUpdated();
    }

    /**
     * All callbacks are executed on the UI thread.
     */
    public interface DataListener {
        void onData(AutofillEntries entries);
    }

    static {
        EventDispatcher.getInstance().registerGeckoThreadListener(new NativeEventListener() {
            @Override
            public void handleMessage(String event, NativeJSObject message, final EventCallback callback) {
                ThreadUtils.postToUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleAutofillRequest(callback);
                    }
                });
            }
        }, "Autofill:Prompt");
    }

    private AutofillGeckoClient() {}

    public static void setAutofillListener(final AutofillListener listener) {
        ThreadUtils.assertOnUiThread();

        if (listener != null && autofillListener != null) {
            throw new IllegalStateException("An AutofillListener has already been set");
        }

        autofillListener = listener;
    }

    /**
     * Requests autofill data from Gecko.
     *
     * @return {@link AutofillEntries} existing autofill entries
     */
    public static void getAutofillData(final DataListener listener) {
        final AutofillEntries autofillEntries = new AutofillEntries();

        GeckoAppShell.sendRequestToGecko(new GeckoRequest("Autofill:Get", null) {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    populatePaymentInfo(autofillEntries, response);
                    populateAddressInfo(autofillEntries, response);
                } catch (JSONException e) {
                    throw new AutofillException(e);
                }

                ThreadUtils.postToUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onData(autofillEntries);
                    }
                });
            }
        });
    }

    /**
     * Submits a payment edit for validation.
     * @param paymentEntry Entry containing new values to be updated
     */
    public static void submitPaymentEdit(PaymentAutofillEntry paymentEntry) {
        try {
            final JSONObject response = new JSONObject();
            response.put("guid", paymentEntry.guid);
            response.put("type", "payment");
            response.put("data", getPaymentJSON(paymentEntry));
            submitEdit(response);
        } catch (JSONException e) {
            throw new AutofillException(e);
        }
    }

    /**
     * Submits an address edit for validation.
     * @param addressEntry Entry containing new values to be updated
     */
    public static void submitAddressEdit(AddressAutofillEntry addressEntry) {
        try {
            final JSONObject response = new JSONObject();
            response.put("guid", addressEntry.guid);
            response.put("type", "address");
            response.put("data", getAddressJSON(addressEntry));
            submitEdit(response);
        } catch (JSONException e) {
            throw new AutofillException(e);
        }
    }

    private static JSONObject getAddressJSON(AddressAutofillEntry addressEntry) throws JSONException {
        final JSONObject entry = new JSONObject();
        entry.put("name", addressEntry.name);
        entry.put("address-line1", addressEntry.addressLine1);
        entry.put("address-line2", addressEntry.addressLine2);
        entry.put("locality", addressEntry.locality);
        entry.put("region", addressEntry.region);
        entry.put("postal-code", addressEntry.postalCode);
        entry.put("country", addressEntry.country);
        entry.put("tel", addressEntry.tel);
        entry.put("email", addressEntry.email);
        return entry;
    }

    private static JSONObject getPaymentJSON(PaymentAutofillEntry paymentEntry) throws JSONException {
        final JSONObject entry = new JSONObject();
        entry.put("cc-name", paymentEntry.name);
        entry.put("cc-number", paymentEntry.num);
        entry.put("cc-csc", paymentEntry.csc);
        entry.put("cc-exp-month", paymentEntry.expMonth);
        entry.put("cc-exp-year", paymentEntry.expYear);
        return entry;
    }

    private static void submitEdit(JSONObject data) {
        GeckoAppShell.sendEventToGecko(GeckoEvent.createBroadcastEvent("Autofill:Edit", data.toString()));
        if (autofillListener != null) {
            autofillListener.onUpdated();
        }
    }

    private static void handleAutofillRequest(final EventCallback callback) {
        if (autofillListener != null) {
            autofillListener.onRequestAutofill(new SubmitListener() {
                @Override
                public void submit(PaymentAutofillEntry payment, AddressAutofillEntry billing, AddressAutofillEntry shipping) {
                    submitAutofill(callback, payment, billing, shipping);
                }

                @Override
                public void cancel() {
                    cancelAutofill(callback);
                }
            });
        } else {
            cancelAutofill(callback);
        }
    }

    public static void validatePayment(PaymentAutofillEntry entry, final ValidationListener listener) {
        final JSONObject data = new JSONObject();

        try {
            data.put("payment", getPaymentJSON(entry));
        } catch (JSONException e) {
            throw new AutofillException(e);
        }

        validate(data, listener);
    }

    public static void validateAddress(final AddressAutofillEntry entry, final ValidationListener listener) {
        final JSONObject data = new JSONObject();

        try {
            data.put("address", getAddressJSON(entry));
        } catch (JSONException e) {
            throw new AutofillException(e);
        }

        validate(data, listener);
    }

    private static void validate(final JSONObject data, final ValidationListener listener) {
        GeckoAppShell.sendRequestToGecko(new GeckoRequest("Autofill:Validate", data) {
            @Override
            public void onResponse(final JSONObject response) {
                ThreadUtils.postToUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (response.getBoolean("valid")) {
                                listener.onSuccess();
                                return;
                            }

                            final HashMap<String, String> errors = new HashMap<String, String>();
                            final JSONObject fields = response.getJSONObject("fields");
                            final Iterator<?> it = fields.keys();
                            do {
                                final String field = (String) it.next();
                                errors.put(field, fields.getString(field));
                            }
                            while (it.hasNext());

                            listener.onError(errors);
                        } catch (JSONException e) {
                            throw new AutofillException(e);
                        }
                    }
                });
            }
        });
    }

    /**
     * BRN: Include "temporary" type for one-off submissions
     *
     * Submission JSON fields:
     *   payment:
     *     type: "saved"
     *       If type is "saved", the guid must be provided which matches a stored address entry.
     *     guid: <payment-guid>
     *       Used only if type is "saved".
     *   billing:
     *     type: "saved"
     *       If type is "saved", the guid must be provided which matches a stored address entry.
     *     guid: <billing-guid>
     *       Used only if type is "saved".
     *   shipping:
     *     type: "saved" OR "sameAsBilling"
     *       If type is "saved", the guid must be provided which matches a stored address entry.
     *       If type is "sameAsBilling", the billing details will be used for shipping.
     *     guid: <shipping-guid>
     *       Used only if type is "saved".
     */
    private static void submitAutofill(EventCallback callback,
                                       PaymentAutofillEntry payment,
                                       AddressAutofillEntry billing,
                                       AddressAutofillEntry shipping) {
        try {
            JSONObject response = new JSONObject();

            JSONObject paymentData = new JSONObject();
            paymentData.put("type", "saved");
            paymentData.put("guid", payment.guid);
            response.put("payment", paymentData);

            JSONObject billingData = new JSONObject();
            billingData.put("type", "saved");
            billingData.put("guid", billing.guid);
            response.put("billing", billingData);

            JSONObject shippingData = new JSONObject();
            if (shipping.guid == null) {
                shippingData.put("type", "sameAsBilling");
            } else {
                shippingData.put("type", "saved");
                shippingData.put("guid", shipping.guid);
            }
            response.put("shipping", shippingData);

            callback.sendSuccess(response);
        } catch (JSONException e) {
            throw new AutofillException(e);
        }
    }

    private static void cancelAutofill(EventCallback callback) {
        callback.sendSuccess(null);
    }

    private static void populatePaymentInfo(AutofillEntries autofillEntries, JSONObject data) throws JSONException {
        final JSONObject payment = data.getJSONObject("payment");
        final JSONArray entries = payment.getJSONArray("entries");
        final String paymentGuid = payment.getString("paymentGuid");
        for (int i = 0; i < entries.length(); i++) {
            final JSONObject entry = entries.getJSONObject(i);
            final JSONObject values = entry.getJSONObject("data");
            final PaymentAutofillEntry paymentEntry = new PaymentAutofillEntry(
                    entry.getString("guid"),
                    values.getString("cc-name"),
                    values.getString("cc-number"),
                    values.getString("cc-exp-month"),
                    values.getString("cc-exp-year"),
                    values.getString("cc-csc"));

            autofillEntries.payments.add(paymentEntry);
            if (paymentEntry.guid.equals(paymentGuid)) {
                autofillEntries.paymentIndex = i;
            }
        }
    }

    private static void populateAddressInfo(AutofillEntries autofillEntries, JSONObject data) throws JSONException {
        final JSONObject address = data.getJSONObject("address");
        final JSONArray entries = address.getJSONArray("entries");
        final String billingGuid = address.getString("billingGuid");
        final String shippingGuid = address.isNull("shippingGuid") ? null : address.getString("shippingGuid");
        for (int i = 0; i < entries.length(); i++) {
            final JSONObject entry = entries.getJSONObject(i);
            final JSONObject values = entry.getJSONObject("data");
            AddressAutofillEntry addressEntry = new AddressAutofillEntry(
                    entry.getString("guid"),
                    values.getString("name"),
                    values.getString("address-line1"),
                    values.optString("address-line2"),
                    values.getString("locality"),
                    values.getString("region"),
                    values.getString("postal-code"),
                    values.getString("country"),
                    values.getString("email"),
                    values.getString("tel"));

            autofillEntries.addresses.add(addressEntry);
            if (addressEntry.guid.equals(billingGuid)) {
                autofillEntries.billingIndex = i;
            }
            if (addressEntry.guid.equals(shippingGuid)) {
                autofillEntries.shippingIndex = i;
            }
        }
    }
}
