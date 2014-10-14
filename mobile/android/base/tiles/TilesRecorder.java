/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tiles;

import org.mozilla.gecko.background.tiles.TilesConstants;
import org.mozilla.gecko.background.tiles.TilesConstants.EventsContract;
import org.mozilla.gecko.background.tiles.TilesContentProvider;
import org.mozilla.gecko.background.tiles.TilesAlarmService;
import org.mozilla.gecko.util.ThreadUtils;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

/**
 * The browser interface to the TilesContentProvider.
 *
 * This class is not thread-safe, and must be accessed only from the UI thread.
 */
public class TilesRecorder {
    public static final String ACTION_CLICK = EventsContract.EVENT_CLICK;
    public static final String ACTION_PIN = EventsContract.EVENT_PIN;
    public static final String ACTION_UNPIN = EventsContract.EVENT_UNPIN;

    private static final String LOG_TAG = TilesRecorder.class.getSimpleName();

    private final ContentProviderClient providerClient;

    public TilesRecorder(final Context context) {
        ThreadUtils.assertOnUiThread();
        providerClient = context.getContentResolver().acquireContentProviderClient(TilesConstants.AUTHORITY);

        // Ensure the service has started and has an upload scheduled.
        final Intent serviceIntent = new Intent(context, TilesAlarmService.class);
        serviceIntent.setAction(Intent.ACTION_MAIN);
        context.startService(serviceIntent);
    }

    public void flush() {
        ThreadUtils.assertOnUiThread();

        final TilesContentProvider tilesProvider = (TilesContentProvider) providerClient.getLocalContentProvider();

        if (tilesProvider == null) {
            // We control whether the CP runs in a separate process. Since it doesn't, this
            // should never happen.
            throw new IllegalStateException("TilesUploadService is not running in the same process as TilesContentProvider");
        }

        tilesProvider.flushEventsToDisk();
    }

    public void destroy() {
        ThreadUtils.assertOnUiThread();

        providerClient.release();
    }

    public void recordAction(String action, int index, TilesSnapshot tiles) {
        ThreadUtils.assertOnUiThread();

        switch (action) {
        case ACTION_CLICK:
        case ACTION_PIN:
        case ACTION_UNPIN:
            final ContentValues values = new ContentValues();
            values.put(EventsContract.COLUMN_EVENT, action);
            values.put(EventsContract.COLUMN_INDEX, index);
            // BRN: Inserting tiles as a JSON string isn't ideal since the data format for the tiles
            // is disconnected from the tiles CP. But supporting true CP-style data would mean
            // separate JSON "tables", which would be a lot of extra work...
            // Perhaps TilesSnapshot can be a simple LinkedList-backed structure, and
            // TilesContentProvider can expose a method for queue -> JSON stringification.
            values.put(EventsContract.COLUMN_TILES, tiles.toString());
            insertValues(values);
            break;
        default:
            throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    public void recordView(TilesSnapshot tiles) {
        ThreadUtils.assertOnUiThread();

        final ContentValues values = new ContentValues();
        values.put(EventsContract.COLUMN_EVENT, EventsContract.EVENT_VIEW);
        values.put(EventsContract.COLUMN_TILES, tiles.toString());
        insertValues(values);
    }

    private void insertValues(ContentValues values) {
        try {
            providerClient.insert(TilesConstants.AUTHORITY_EVENTS_URI, values);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error inserting tiles values", e);
        }
    }
}
