package org.mozilla.gecko;

import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public abstract class GeckoRequest {
    private static final String LOGTAG = "GeckoRequest";
    private static final AtomicInteger sCurrentId = new AtomicInteger(0);

    private final int mId = sCurrentId.getAndIncrement();
    private final GeckoEvent mEvent;

    public GeckoRequest(String subject, Object data) {
        JSONObject message = new JSONObject();
        try {
            message.put("id", mId);
            message.put("data", data);
        } catch (JSONException e) {
            Log.e(LOGTAG, "JSON error", e);
        }

        mEvent = GeckoEvent.createBroadcastEvent(subject, message.toString());
    }

    int getId() {
        return mId;
    }

    GeckoEvent getGeckoEvent() {
        return mEvent;
    }

    public abstract void onResponse(JSONObject response);
}