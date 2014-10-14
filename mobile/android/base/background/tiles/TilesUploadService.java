/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.tiles;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.entity.StringEntity;

import org.mozilla.gecko.background.tiles.TilesConstants.EventsContract;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.Resource;

import android.app.IntentService;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.RemoteException;
import android.util.Log;

/**
 * Service that uploads tiles data.
 *
 * This service queries the TilesContentProvider, then performs an upload if there is any cached
 * outgoing data and a working network connection.
 */
public class TilesUploadService extends IntentService {
  private static final String LOG_TAG = TilesUploadService.class.getSimpleName();

  private ContentProviderClient providerClient;
  private TilesContentProvider tilesProvider;

  public TilesUploadService() {
    super(LOG_TAG);
  }

  @Override
  public void onCreate() {
    Log.d(LOG_TAG, "onCreate");

    super.onCreate();

    providerClient = getContentResolver().acquireContentProviderClient(TilesConstants.AUTHORITY);
    if (providerClient == null) {
      // acquireContentProviderClient returns null only when there's no ContentProvider
      // associated with the given authority. In our case, that should be never.
      throw new IllegalStateException("Tiles CPC could not be acquired");
    }

    tilesProvider = (TilesContentProvider) providerClient.getLocalContentProvider();
    if (tilesProvider == null) {
      // We control whether the CP runs in a separate process. Since it doesn't, this
      // should never happen.
      throw new IllegalStateException("TilesUploadService is not running in the same process as TilesContentProvider");
    }
  }

  @Override
  public void onDestroy() {
    Log.d(LOG_TAG, "onDestroy");

    super.onDestroy();
    providerClient.release();
  }

  /**
   * Returns whether the device is currently able to transmit data.
   */
  protected boolean isConnected() {
    ConnectivityManager connectivity = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connectivity.getActiveNetworkInfo();
    if (networkInfo == null) {
      return false;
    }
    return networkInfo.isConnected();
  }

  private JSONObject getPayload() throws RemoteException, JSONException {
    final Cursor cursor = providerClient.query(TilesConstants.AUTHORITY_EVENTS_URI, null, null, null, null);

    // Return null if there is no data to upload.
    if (cursor.getCount() == 0) {
      return null;
    }

    final JSONArray batch = new JSONArray();

    try {
      while (cursor.moveToNext()) {
        final JSONObject event = new JSONObject();
        final String eventName = cursor.getString(cursor.getColumnIndexOrThrow(EventsContract.COLUMN_EVENT));

        switch (eventName) {
        case EventsContract.EVENT_CLICK:
        case EventsContract.EVENT_PIN:
        case EventsContract.EVENT_UNPIN:
          final int index = cursor.getInt(cursor.getColumnIndexOrThrow(EventsContract.COLUMN_INDEX));
          event.put(eventName, index);
          // Fall through.
        case EventsContract.EVENT_VIEW:
          final JSONObject timeSync = new JSONObject();
          final int clock = cursor.getInt(cursor.getColumnIndexOrThrow(EventsContract.COLUMN_CLOCK));
          final long realtime = cursor.getLong(cursor.getColumnIndexOrThrow(EventsContract.COLUMN_REALTIME));
          final long calcTime = cursor.getLong(cursor.getColumnIndexOrThrow(EventsContract.COLUMN_CALCTIME));
          final long minTime = cursor.getLong(cursor.getColumnIndexOrThrow(EventsContract.COLUMN_MINTIME));
          timeSync.put("clock", clock);
          timeSync.put("realtime", realtime);
          if (minTime > 0) {
            timeSync.put("mintime", minTime);
            if (calcTime > 0) {
              timeSync.put("calctime", calcTime);
            }
          }

          final String tilesJson = cursor.getString(cursor.getColumnIndexOrThrow(EventsContract.COLUMN_TILES));
          final JSONArray tiles = new JSONArray(tilesJson);
          event.put("ts", timeSync);
          event.put("tiles", tiles);
          break;
        default:
          throw new IllegalArgumentException("Unknown event: " + event);
        }

        batch.put(event);
      }
    } finally {
      cursor.close();
    }

    final JSONObject timeSync = new JSONObject();
    timeSync.put("clock", tilesProvider.getClock());
    timeSync.put("realtime", tilesProvider.getAndUpdateRealtime());

    final JSONObject payload = new JSONObject();
    payload.put("ts", timeSync);
    payload.put("batch", batch);
    return payload;
  }

  @Override
  public void onHandleIntent(Intent intent) {
    // Intent can be null. Bug 1025937.
    if (intent == null) {
      Log.d(LOG_TAG, "Short-circuiting on null intent.");
      return;
    }

    // BRN: Bail if metrics reporting is disabled.

    // Update the last realtime to help detect reboots.
    tilesProvider.getAndUpdateRealtime();

    final String action = intent.getAction();
    if (action == null) {
      Log.w(LOG_TAG, "Intent does not specify an action");
      return;
    }

    switch (action) {
    case TilesConstants.ACTION_UPLOAD: {
      Log.d(LOG_TAG, "Uploading tiles data");

      final boolean uploaded = uploadAndUpdateClocks();

      Log.d(LOG_TAG, "Upload complete, success=" + uploaded);
      // BRN: Optionallly do something (exponential backoff?) based on upload success.
      break;
    }

    default:
      Log.w(LOG_TAG, "Unexpected intent action: " + action);
    }
  }

  /**
   * Uploads pending tiles data to the server.
   *
   * @return whether the data was successfully uploaded
   */
  private boolean uploadAndUpdateClocks() {
    // Don't do anything if the device can't talk to the server.
    if (!isConnected()) {
      Log.d(LOG_TAG, "Device is not connected; skipping upload");
      return false;
    }

    try {
      final JSONObject payload = getPayload();
      if (payload == null) {
        Log.d(LOG_TAG, "No queued events to upload");
        return true;
      }

      final long serverTime = uploadData(payload);

      // The upload succeeded, so delete the flushed events file.
      providerClient.delete(TilesConstants.AUTHORITY_EVENTS_URI, null, null);

      if (serverTime == 0) {
        // The upload succeeded, but the HTTP response didn't include the server time.
        // That means we can't update our clock parameters.
        Log.w(LOG_TAG, "Server response did not include date");
        return true;
      }

      Log.d(LOG_TAG, "Server time is: " + serverTime);
      tilesProvider.updateLastUploadTimeForCurrentClock(serverTime);
      return true;
    } catch (JSONException e) {
      Log.e(LOG_TAG, "Error building tiles payload", e);
    } catch (RemoteException e) {
      Log.e(LOG_TAG, "Error accessing tiles ContentProvider", e);
    } catch (TilesUploadException e) {
      Log.e(LOG_TAG, "Error uploading data", e);
    }

    return false;
  }

  private long uploadData(JSONObject payload) throws TilesUploadException {
    try {
      final URI uri = new URI(TilesConstants.SERVER_SPEC);
      final BaseResource resource = new BaseResource(uri);
      final TilesResourceDelegate delegate = new TilesResourceDelegate(resource);
      resource.delegate = delegate;

      final StringEntity entity = stringEntityWithContentTypeApplicationJSON(payload.toString());
      resource.post(entity);

      return delegate.getResponseDate();
    } catch (UnsupportedEncodingException | URISyntaxException e) {
      throw new TilesUploadException(e);
    }
  }

  private static StringEntity stringEntityWithContentTypeApplicationJSON(String s) throws UnsupportedEncodingException {
    StringEntity e = new StringEntity(s, "UTF-8");
    e.setContentType("application/json");
    return e;
  }

  private static class TilesResourceDelegate extends BaseResourceDelegate {
    // BaseResource has an unusual API where exceptions must be handled by the methods in the
    // delegate. Store the exception here so we can throw it later.
    private TilesUploadException uploadException;

    private long responseDate = 0;

    public TilesResourceDelegate(Resource resource) {
      super(resource);
    }

    @Override
    public String getUserAgent() {
      return TilesConstants.USER_AGENT;
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      final int status = response.getStatusLine().getStatusCode();

      switch (status) {
      case 200:
      case 201:
        Log.d(LOG_TAG, "Upload succeeded with status " + status);

        final Header header = response.getFirstHeader("Date");
        if (header == null) {
          Log.w(LOG_TAG, "No date header in HTTP response");
          break;
        }

        final String date = header.getValue();
        if (date == null) {
          Log.w(LOG_TAG, "No date value in HTTP response");
          break;
        }

        // RFC 2616 specifies three different valid date formats. Since we own the server, it should
        // be safe to assume we're using the preferred format.
        final String template = "EEE, dd MMM yyyy HH:mm:ss zzz";
        final SimpleDateFormat format = new SimpleDateFormat(template, Locale.ENGLISH);
        try {
          responseDate = format.parse(date).getTime();
          Log.d(LOG_TAG, "Got response date: " + responseDate);
        } catch (ParseException e) {
          Log.w(LOG_TAG, "Date parse failed", e);
        }
        break;
      default:
        uploadException = new TilesUploadException("Upload failed with status " + status);
      }

      BaseResource.consumeEntity(response);
    }

    @Override
    public void handleHttpProtocolException(ClientProtocolException e) {
      uploadException = new TilesUploadException(e);
    }

    @Override
    public void handleHttpIOException(IOException e) {
      uploadException = new TilesUploadException(e);
    }

    @Override
    public void handleTransportException(GeneralSecurityException e) {
      uploadException = new TilesUploadException(e);
    }

    /**
     * Gets the Date from the server response, or TIME_UNDEFINED if it wasn't given.
     *
     * @return the response date
     * @throws TilesUploadException if the upload wasn't successful
     */
    public long getResponseDate() throws TilesUploadException {
      if (uploadException != null) {
        throw uploadException;
      }

      return responseDate;
    }
  }
}
