/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Watch for notifications to start the tile metrics background service.
 *
 * From the Android documentation: "Also note that as of Android 3.0 the user
 * needs to have started the application at least once before your application
 * can receive android.intent.action.BOOT_COMPLETED events."
 *
 * We really do want to launch on BOOT_COMPLETED, since it's possible for a user
 * to run Firefox, shut down the phone, then power it on again on the same day.
 * We want to schedule uploads in this case, even though they haven't launched
 * Firefox since boot.
 */
public class TilesBroadcastReceiver extends BroadcastReceiver {
  public static final String LOG_TAG = TilesBroadcastReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(LOG_TAG, "Received intent; starting TilesAlarmService");
    Intent intentService = new Intent(context, TilesAlarmService.class);
    intentService.setAction(Intent.ACTION_MAIN);
    context.startService(intentService);
  }
}