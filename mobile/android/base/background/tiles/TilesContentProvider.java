/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.tiles;

import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import org.mozilla.gecko.background.tiles.TilesConstants.EventsContract;

/**
 * To reduce I/O, inserts to this ContentProvider are held in memory until flushed. To
 * accomplish this, we need to abuse the ContentProvider's lifecycle (according to Dianne
 * Hackborne, transactions should be committed immediately upon insert [1]).
 *
 * This CP will stay alive as long as the activity's process is alive, meaning the lifetime
 * of the queued data is directly tied to the activity's lifetime. The process may be killed
 * any moment after the activity's onPause, so flushEventsToDisk should be (synchronously!)
 * called there to minimize data loss.
 *
 * Note that since we're relying on explicit flushes to write out the queue, we're still
 * susceptible to data loss if Fennec crashes.
 *
 * [1] https://groups.google.com/d/msg/android-developers/PD-XxFn1hvI/vjoHVTJILPAJ
 */
public class TilesContentProvider extends ContentProvider {
  private static final String LOG_TAG = "GeckoTilesProvider";
  private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
  private static final int PATH_EVENT = 0;

  private static final String[] COLUMNS = {
    EventsContract.COLUMN_EVENT,
    EventsContract.COLUMN_INDEX,
    EventsContract.COLUMN_TILES,
    EventsContract.COLUMN_CLOCK,
    EventsContract.COLUMN_REALTIME,
    EventsContract.COLUMN_CALCTIME,
    EventsContract.COLUMN_MINTIME,
  };

  static {
    URI_MATCHER.addURI(TilesConstants.AUTHORITY, "events", PATH_EVENT);
  }

  private final ConcurrentLinkedQueue<TileEvent> recordedEvents = new ConcurrentLinkedQueue<>();
  private volatile SharedPreferences prefs;
  private volatile int clock;

  // The absolute server time of the last upload, or 0 if undefined. Nonzero as long as an upload
  // has ever occurred in the past, even if the last upload did not happen on the current clock.
  private volatile long lastUploadAbsTime;

  // The realtime of the last upload, or 0 if undefined. Nonzero only if an upload has occurred on
  // the current clock. lastUploadRealtime being nonzero implies that lastUploadAbsTime is also nonzero.
  private volatile long lastUploadRealtime;

  @Override
  public boolean onCreate() {
    prefs = getContext().getSharedPreferences(TilesConstants.PREFS_BRANCH, 0);

    final int lastClock = prefs.getInt(TilesConstants.PREF_CLOCK, 0);
    final long realtime = SystemClock.elapsedRealtime();
    clock = getAndUpdateClock(prefs, lastClock, realtime);

    // Restore the previous upload absolute time and realtime if they happened on the same clock.
    // These allow us to set the calculated and minimum times for future uploads to improve accuracy.
    lastUploadAbsTime = prefs.getLong(TilesConstants.PREF_LAST_UPLOAD_ABS_TIME, 0);
    if (lastUploadAbsTime != 0) {
      final int lastUploadClock = prefs.getInt(TilesConstants.PREF_LAST_UPLOAD_CLOCK, 0);
      if (lastUploadClock == clock) {
        lastUploadRealtime = prefs.getLong(TilesConstants.PREF_LAST_UPLOAD_REALTIME, 0);
      }
    }

    return true;
  }

  @Override
  public String getType(Uri uri) {
    return null;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    Log.d(LOG_TAG, "Query on URI: " + uri);

    if (projection != null || selection != null || selectionArgs != null || sortOrder != null) {
      throw new UnsupportedOperationException("Custom queries not supported");
    }

    final MatrixCursor cursor = new MatrixCursor(COLUMNS);

    TileEvent event;
    while ((event = recordedEvents.poll()) != null) {
      // BRN: Write these to disk instead of using them directly.
      cursor.addRow(new Object[] { event.event, event.index, event.tiles, clock, event.realtime,
                                   event.calcTime, event.minTime });
    }

    // BRN: ...and then read all events from disk to build the cursor.

    return cursor;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    Log.d(LOG_TAG, "Insert on URI: " + uri);

    final TileEvent event;
    final String tiles = values.getAsString(EventsContract.COLUMN_TILES);
    final String eventName = values.getAsString(EventsContract.COLUMN_EVENT);

    // Use a combination of the device's realtime, the last upload device realtime, and the
    // last upload server absolute time to make a reasonable guess for the absolute time this
    // event occurred, relative to the server time.
    final long realtime = SystemClock.elapsedRealtime();
    final long calcTime;
    if (lastUploadRealtime != 0) {
      calcTime = lastUploadAbsTime + realtime - lastUploadRealtime;
    } else {
      calcTime = 0;
    }

    switch (eventName) {
    case EventsContract.EVENT_CLICK:
    case EventsContract.EVENT_PIN:
    case EventsContract.EVENT_UNPIN:
      final int index = values.getAsInteger(EventsContract.COLUMN_INDEX);
      event = new TileEvent(eventName, index, tiles, realtime, calcTime, lastUploadAbsTime);
      break;
    case EventsContract.EVENT_VIEW:
      event = new TileEvent(eventName, -1, tiles, realtime, calcTime, lastUploadAbsTime);
      break;
    default:
      throw new IllegalArgumentException("Unknown event: " + eventName);
    }

    recordedEvents.add(event);

    // We don't support querying individual entries, so just return null.
    return null;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    Log.d(LOG_TAG, "Delete on URI: " + uri);

    if (selection != null || selectionArgs != null) {
      throw new UnsupportedOperationException("Custom deletions not supported");
    }

    // BRN: Delete the event file.

    return 1;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    throw new UnsupportedOperationException();
  }

  /**
   * Flush recorded events to disk.
   *
   * Data is flushed to a global (not per-profile) file since the tiles server
   * is not profile-aware.
   */
  public void flushEventsToDisk() {
    Log.d(LOG_TAG, "Flushing tiles events to disk");
    // BRN: TODO
  }

  /**
   * Gets a counter that is incremented on each boot.
   *
   * This counter is a unique identifier used as a reference for saved realtime values.
   *
   * @return the clock ID
   */
  public int getClock() {
    return clock;
  }

  /**
   * Fetches the realtime clock while also recording the value in prefs.
   *
   * @return the current realtime
   */
  public long getAndUpdateRealtime() {
    final long realtime = SystemClock.elapsedRealtime();
    prefs.edit().putLong(TilesConstants.PREF_LAST_REALTIME, realtime).apply();
    return realtime;
  }

  /**
   * Updates the clock and time, both in memory and shared prefs, based on an absolute server time.
   *
   * @param absTime the absolute server time
   */
  public void updateLastUploadTimeForCurrentClock(long absTime) {
    if (absTime <= 0) {
      throw new IllegalArgumentException("Invalid time");
    }

    lastUploadAbsTime = absTime;
    lastUploadRealtime = SystemClock.elapsedRealtime();

    prefs.edit().putLong(TilesConstants.PREF_LAST_UPLOAD_ABS_TIME, lastUploadAbsTime)
                .putLong(TilesConstants.PREF_LAST_UPLOAD_REALTIME, lastUploadRealtime)
                .putInt(TilesConstants.PREF_LAST_UPLOAD_CLOCK, clock)
                .apply();
  }

  /**
   * Fetches the current clock ID while also recording it and the realtime in prefs.
   *
   * @return the current clock
   */
  public static int getAndUpdateClock(SharedPreferences prefs, int clock, long realtime) {
    // Get the current clock counter, incrementing it if we rebooted.
    final long lastRealtime = prefs.getLong(TilesConstants.PREF_LAST_REALTIME, Long.MAX_VALUE);
    final SharedPreferences.Editor prefsEditor = prefs.edit();

    if (realtime < lastRealtime) {
      // If the current realtime is less than the last realtime, assume we've rebooted.
      clock++;
      prefsEditor.putInt(TilesConstants.PREF_CLOCK, clock);
    }

    prefsEditor.putLong(TilesConstants.PREF_LAST_REALTIME, realtime).apply();
    Log.d(LOG_TAG, "Using clock: " + clock);
    return clock;
  }

  /**
   * Fetches the last absolute server time recorded for the given clock.
   *
   * @return the last server time if there was previously an upload during the given clock period,
   *         or 0 otherwise
   */
  public static long getMinimumTime(SharedPreferences prefs, int clock) {
    final int lastClock = prefs.getInt(TilesConstants.PREF_LAST_UPLOAD_CLOCK, 0);
    if (lastClock != clock) {
      return 0;
    }

    return prefs.getLong(TilesConstants.PREF_LAST_UPLOAD_ABS_TIME, 0);
  }

  private static class TileEvent {
    public final String event;
    public final int index;
    public final String tiles;
    public final long realtime;
    public final long calcTime;
    public final long minTime;

    public TileEvent(String event, int index, String tiles, long realtime, long calcTime, long minTime) {
      this.event = event;
      this.index = index;
      this.tiles = tiles;
      this.realtime = realtime;
      this.calcTime = calcTime;
      this.minTime = minTime;
    }
  }
}
