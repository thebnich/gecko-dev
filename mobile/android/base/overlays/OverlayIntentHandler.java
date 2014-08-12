/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

 package org.mozilla.gecko.overlays;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * A service to receive requests from overlays to perform actions.
 **/
public class OverlayIntentHandler extends Service {

    // Not a bound service.
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
