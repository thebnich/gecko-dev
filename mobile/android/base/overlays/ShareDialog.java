/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.overlays;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;
import org.mozilla.gecko.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import org.mozilla.gecko.sync.setup.activities.WebURLFinder;

/**
 * A transparent activity that displays the share overlay.
 */
public class ShareDialog extends Activity {
    private static final String LOGTAG = "GeckoShareDialog";

    private String url;
    private String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        // The URL is usually hiding somewhere in the extra text. Extract it.
        String extraText = intent.getStringExtra(Intent.EXTRA_TEXT);
        String pageUrl = (new WebURLFinder(extraText)).bestWebURL();

        if (TextUtils.isEmpty(pageUrl)) {
            Log.e(LOGTAG, "Unable to process shared intent. No URL found!");

            // Display toast notifying the user of failure (most likely a developer who screwed up
            // trying to send a share intent).
            Toast toast = Toast.makeText(this, getResources().getText(R.string.overlay_share_no_url), Toast.LENGTH_SHORT);
            toast.show();

            return;
        }

        setContentView(R.layout.overlay_share_dialog);

        // If provided, we use the subject text to give us something nice to display.
        // If not, we wing it with the URL.
        // TODO: Consider polling Fennec databases to find better information to display.
        // TODO: Extract the name of Sync's default send target device somehow and maybe use it?
        String subjectText = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (subjectText != null) {
            ((TextView) findViewById(R.id.title)).setText(subjectText);
        }

        // Assign the derived url/title to fields.
        title = subjectText;
        url = pageUrl;

        // Set the subtitle text on the view and cause it to marquee if it's too long (which it will
        // be, since it's a URL).
        TextView subtitleView = (TextView) findViewById(R.id.subtitle);
        subtitleView.setText(pageUrl);
        subtitleView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        subtitleView.setSingleLine(true);
        subtitleView.setMarqueeRepeatLimit(5);
        subtitleView.setSelected(true);

        // Commence the slide-up animation.
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.overlay_slide_up);
        findViewById(R.id.sharedialog).startAnimation(anim);
    }

    /**
     * Helper method to get an overlay service intent populated with the data held in this dialog
     * for a given request type.
     */
    private Intent getServiceIntent(String requestType) {
        Intent serviceIntent = new Intent(this, OverlayIntentHandler.class);
        serviceIntent.setAction(requestType);

        serviceIntent.putExtra(OverlayIntentConstants.URL, url);
        serviceIntent.putExtra(OverlayIntentConstants.TITLE, title);

        return serviceIntent;
    }

    /*
     * Button handlers. Send intents to the background service responsible for processing requests
     * on Fennec in the background. (a nice extensible mechanism for "doing stuff without properly
     * launching Fennec").
     */
    public void sendTab(View v) {
        // This one is mildly annoying, as we dispatch an intent to dispatch an intent. I claim the
        // clarity gained from a unified mechanism outweighs the small cost of the minor crazy.
        startService(getServiceIntent(OverlayIntentConstants.SEND_TAB));

        // Since send-tab takes over the whole screen, we don't bother to prettily animate away.
        finish();
    }

    public void addReadingListItem(View v) {
        startService(getServiceIntent(OverlayIntentConstants.ADD_TO_READING_LIST));
        slideOut();
        showToast(getResources().getString(R.string.reading_list_added));
    }

    public void addBookmark(View v) {
        startService(getServiceIntent(OverlayIntentConstants.ADD_BOOKMARK));
        slideOut();
        showToast(getResources().getString(R.string.bookmark_added));
    }

    private void showToast(String s) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.overlay_share_toast,
                (ViewGroup) findViewById(R.id.overlay_share_toast));

        TextView text = (TextView) layout.findViewById(R.id.overlay_toast_message);
        text.setText(s);

        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.BOTTOM, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    private void slideOut() {
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.overlay_slide_down);
        findViewById(R.id.sharedialog).startAnimation(anim);

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Unused. I can haz Miranda method?
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Unused.
            }
        });
    }

    /**
     * Close the dialog if back is pressed.
     */
    @Override
    public void onBackPressed() {
        slideOut();
    }

    /**
     * Close the dialog if the background is tapped.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        slideOut();
        return true;
    }
}
