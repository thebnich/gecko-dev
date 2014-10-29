package org.mozilla.gecko.background.tiles;

import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

public class TilesClockManager {
  private static final String LOG_TAG = TilesClockManager.class.getSimpleName();

  // The current clock ID, incremented on each boot.
  private volatile int clock;

  // The absolute server time of the last upload, or 0 if undefined. Nonzero as long as an upload
  // has ever occurred in the past, even if the last upload did not happen on the current clock.
  private volatile long lastUploadServerTime;

  // The realtime of the last upload, or 0 if undefined. Nonzero only if an upload has occurred on
  // the current clock. lastUploadRealtime being nonzero implies that lastUploadAbsTime is also nonzero.
  private volatile long lastUploadRealtime;

  private final SharedPreferences prefs;

  public TilesClockManager(SharedPreferences prefs) {
    this.prefs = prefs;

    clock = getAndUpdateClock();

    // Restore the previous upload absolute time and realtime if they happened on the same clock.
    // These allow us to set the calculated and minimum times for future uploads to improve accuracy.
    lastUploadServerTime = prefs.getLong(TilesConstants.PREF_LAST_UPLOAD_ABS_TIME, 0);
    if (lastUploadServerTime != 0) {
      final int lastUploadClock = prefs.getInt(TilesConstants.PREF_LAST_UPLOAD_CLOCK, 0);
      if (lastUploadClock == clock) {
        lastUploadRealtime = prefs.getLong(TilesConstants.PREF_LAST_UPLOAD_REALTIME, 0);
      }
    }
  }

  public long getCalculatedServerTime(long realtime) {
    // Use a combination of the device's realtime, the last upload device realtime, and the
    // last upload server absolute time to make a reasonable guess for the absolute time this
    // event occurred, relative to the server time.
    final long calcTime;
    if (lastUploadRealtime != 0) {
      calcTime = lastUploadServerTime + realtime - lastUploadRealtime;
    } else {
      calcTime = 0;
    }
    return calcTime;
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

  public long getLastUploadServerTime() {
    return lastUploadServerTime;
  }

  /**
   * Updates the clock and time, both in memory and shared prefs, based on an absolute server time.
   *
   * @param serverTime the absolute server time
   */
  public void updateLastUploadTimeForCurrentClock(long serverTime) {
    if (serverTime <= 0) {
      throw new IllegalArgumentException("Invalid time");
    }

    lastUploadServerTime = serverTime;
    lastUploadRealtime = SystemClock.elapsedRealtime();

    prefs.edit().putLong(TilesConstants.PREF_LAST_UPLOAD_ABS_TIME, lastUploadServerTime)
    .putLong(TilesConstants.PREF_LAST_UPLOAD_REALTIME, lastUploadRealtime)
    .putInt(TilesConstants.PREF_LAST_UPLOAD_CLOCK, clock)
    .apply();
  }

  /**
   * Fetches the last absolute server time recorded for the given clock.
   *
   * @return the last server time if there was previously an upload during the given clock period,
   *     or 0 otherwise
   */
  public long getMinimumTime(int clock) {
    final int lastClock = prefs.getInt(TilesConstants.PREF_LAST_UPLOAD_CLOCK, 0);
    if (lastClock != clock) {
      return 0;
    }

    return prefs.getLong(TilesConstants.PREF_LAST_UPLOAD_ABS_TIME, 0);
  }

  /**
   * Fetches the current clock ID while also recording it and the realtime in prefs.
   *
   * @return the current clock
   */
  private int getAndUpdateClock() {
    final long realtime = SystemClock.elapsedRealtime();
    int clock = prefs.getInt(TilesConstants.PREF_CLOCK, 0);

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
}
