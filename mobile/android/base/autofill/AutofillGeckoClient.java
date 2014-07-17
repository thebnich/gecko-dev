package org.mozilla.gecko.autofill;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.mozilla.gecko.EventDispatcher;
import org.mozilla.gecko.autofill.AutofillData.AddressSection;
import org.mozilla.gecko.autofill.AutofillData.Field;
import org.mozilla.gecko.autofill.AutofillData.Section;
import org.mozilla.gecko.util.EventCallback;
import org.mozilla.gecko.util.NativeEventListener;
import org.mozilla.gecko.util.NativeJSObject;
import org.mozilla.gecko.util.ThreadUtils;

/**
 * Bridge between Gecko and the Java front-end for accessing autofill data.
 */
public class AutofillGeckoClient {
    private static AutofillListener autofillListener;

    public interface SubmitListener {
        void submit();
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
         * {@link SubmitListener#submit()} or {@link SubmitListener#cancel()}. The fields in
         * requestData must be updated with autofill data before calling submit; this data will be
         * used to build a response to Gecko.
         */
        void onRequestAutofill(AutofillData requestData, SubmitListener submitListener);
    }

    static {
        // Create a static listener that is registered for the lifetime of the application.
        EventDispatcher.getInstance().registerGeckoThreadListener(new NativeEventListener() {
            @Override
            public void handleMessage(String event, NativeJSObject message, final EventCallback callback) {
                final AutofillData requestData = new AutofillData(message.getObject("data"));
                ThreadUtils.postToUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleAutofillRequest(requestData, callback);
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
     * Handles a requestAutocomplete request.
     *
     * If there is no listener registered to handle requests, an error response is immediately sent.
     * This can happen if the page issues an rAc request and there's no browser Activity alive.
     *
     * @param requestData The set of fields and sections being requested
     * @param callback    Callback from the NativeEventListener used to send a response to Gecko
     */
    private static void handleAutofillRequest(final AutofillData requestData, final EventCallback callback) {
        ThreadUtils.assertOnUiThread();

        if (autofillListener != null) {
            autofillListener.onRequestAutofill(requestData, new SubmitListener() {
                @Override
                public void submit() {
                    submitAutofill(requestData, callback);
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

    /**
     * Submits a requestAutocomplete response.
     *
     * @param data     The set of populated fields that are used to fill the form. This is the same
     *                 AutofillData instance given to the request handler. The request handler
     *                 should set the value of each field before calling this function.
     * @param callback Callback from the NativeEventListener used to send a response to Gecko
     */
    private static void submitAutofill(AutofillData data, EventCallback callback) {
        try {
            JSONArray jsonFields = new JSONArray();

            for (Section section : data) {
                for (AddressSection addressSection : section) {
                    for (Field field : addressSection) {
                        final String value = field.getValue();
                        if (value == null) {
                            continue;
                        }

                        final JSONObject jsonField = new JSONObject();
                        jsonField.put("section", section.getName());
                        jsonField.put("addressType", addressSection.getAddressType());
                        jsonField.put("contactType", field.getContactType());
                        jsonField.put("fieldName", field.getFieldName());
                        jsonField.put("value", value);
                        jsonFields.put(jsonField);
                    }
                }
            }

            final JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("fields", jsonFields);

            callback.sendSuccess(jsonResponse);
        } catch (JSONException e) {
            throw new AutofillException(e);
        }
    }

    /**
     * Cancels a requestAutocomplete request.
     *
     * @param callback Callback from the NativeEventListener used to send a response to Gecko
     */
    private static void cancelAutofill(EventCallback callback) {
        callback.sendError("cancel");
    }
}
