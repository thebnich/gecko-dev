/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.overlays;

/**
 * Constant values used to configure intents relating to the overlay service.
 */
public class OverlayIntentConstants {
    // Intent actions
    public static final String ADD_BOOKMARK = "org.mozilla.gecko.overlays.intents.ADD_BOOKMARK";
    public static final String ADD_TO_READING_LIST = "org.mozilla.gecko.overlays.intents.ADD_TO_READING_LIST";
    public static final String SEND_TAB = "org.mozilla.gecko.overlays.intents.SEND_TAB";
    public static final String OPEN_IN_BACKGROUND = "org.mozilla.gecko.overlays.intents.OPEN_IN_BACKGROUND";

    // Intent extra keys
    public static final String URL = "URL";
    public static final String TITLE = "TITLE";
}
