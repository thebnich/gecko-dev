/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tiles;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 *  An array of the visible tiles, sent with each tiles event.
 *  Ordered left-to right, top-to-bottom.
 */
public class TilesSnapshot {
    public static final int ID_UNDEFINED = -1;

    private static final String LOGTAG = TilesSnapshot.class.getSimpleName();
    private static final String JSON_ID = "id";
    private static final String JSON_PINNED = "pin";
    private static final String JSON_POSITION = "pos";

    private final JSONArray tiles = new JSONArray();

    public void add(int id, boolean pinned, int position) {
        final JSONObject tile = new JSONObject();

        try {
            if (id != ID_UNDEFINED) {
                tile.put(JSON_ID, id);
            }
            if (pinned) {
                tile.put(JSON_PINNED, true);
            }
            if (position != tiles.length()) {
                tile.put(JSON_POSITION, position);
            }
        } catch (JSONException e) {
            Log.e(LOGTAG, "Error serializing tile", e);
        }

        tiles.put(tile);
    }

    /**
     * Gets the snapshot serialized as a JSON string.
     *
     * @return The stringified JSON for this set of tiles
     */
    @Override
    public String toString() {
        return tiles.toString();
    }
}
