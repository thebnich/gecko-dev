/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

"use strict";

const { classes: Cc, interfaces: Ci, utils: Cu } = Components;

Cu.import("resource://gre/modules/Messaging.jsm");
Cu.import("resource://gre/modules/XPCOMUtils.jsm");

function AutofillUIGlue() {}

AutofillUIGlue.prototype = {
  onRequestAutocomplete: function onRequestAutocomplete(callback) {
    sendMessageToJava({ type: "Autofill:Prompt" }, function (data) {
      if (data) {
        callback.submit(data);
      } else {
        callback.cancel();
      }
    });
  },

  classID: Components.ID("{747c653e-c543-499b-9126-e8e033f7b1da}"),
  QueryInterface: XPCOMUtils.generateQI([Ci.nsIAutofillUIGlue])
};

this.NSGetFactory = XPCOMUtils.generateNSGetFactory([AutofillUIGlue]);
