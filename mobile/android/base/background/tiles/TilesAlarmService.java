/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.tiles;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * Alarm service for scheduling uploads.
 *
 * This service simply ensures that a TilesUploadService intent is pending in the AlarmManager.
 */
public class TilesAlarmService extends IntentService {
  private static final String LOG_TAG = TilesAlarmService.class.getSimpleName();

  public TilesAlarmService() {
    super(LOG_TAG);
  }

  public void onCreate() {
    Log.d(LOG_TAG, "onCreate");
    super.onCreate();
  }

  @Override
  public void onDestroy() {
    Log.d(LOG_TAG, "onDestroy");
    super.onDestroy();
  }

  @Override
  public void onHandleIntent(Intent intent) {
    // Intent can be null. Bug 1025937.
    if (intent == null) {
      Log.d(LOG_TAG, "Short-circuiting on null intent.");
      return;
    }

    // BRN: Bail if metrics reporting is disabled.

    final String action = intent.getAction();
    if (action == null) {
      Log.w(LOG_TAG, "Intent does not specify an action");
      return;
    }

    switch (action) {
    case Intent.ACTION_MAIN: {
      // ACTION_MAIN will be fired multiple times; once from a (delayed) intent
      // fired when the device starts, and once every time Fennec starts.
      // There are no guarantees about which will occur first. scheduleUpload()
      // aborts if an alarm is already scheduled, so any ACTION_MAIN intents
      // after the first are effectively ignored.
      scheduleUpload();
      break;
    }

    default:
      Log.w(LOG_TAG, "Unexpected intent action: " + action);
    }
  }

  private void scheduleUpload() {
    final Intent uploadIntent = new Intent(this, TilesUploadService.class);
    uploadIntent.setAction(TilesConstants.ACTION_UPLOAD);

    // Abort if there's already an upload PendingIntent. Yes, this really is the only way to see if
    // an alarm is already scheduled (see https://code.google.com/p/android/issues/detail?id=3776).
    final PendingIntent existingIntent = PendingIntent.getService(this, 0, uploadIntent, PendingIntent.FLAG_NO_CREATE);
    if (existingIntent != null) {
      Log.d(LOG_TAG, "Tiles upload already scheduled");
      return;
    }

    final long realtime = SystemClock.elapsedRealtime();
    final AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    final PendingIntent pendingIntent = PendingIntent.getService(this, 0, uploadIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    // BRN: Change this to setInexactRepeating for production.
    Log.d(LOG_TAG, "Scheduling inexact tiles upload at " + realtime);
    alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, realtime, TilesConstants.UPLOAD_INTERVAL_MSEC, pendingIntent);
  }
}
