/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

"use strict";

this.EXPORTED_SYMBOLS = [
  "RequestAutocompleteUI",
];

const { classes: Cc, interfaces: Ci, utils: Cu, results: Cr } = Components;

Cu.import("resource://gre/modules/Messaging.jsm");
Cu.import("resource://gre/modules/Services.jsm");
Cu.import("resource://gre/modules/XPCOMUtils.jsm");

XPCOMUtils.defineLazyModuleGetter(this, "Task",
                                  "resource://gre/modules/Task.jsm");

/**
 * Dispatch a request to Java to handle the requestAutocomplete UI.
 */
this.RequestAutocompleteUI = function (aAutofillData) {
  this._autofillData = aAutofillData;
}

this.RequestAutocompleteUI.prototype = {
  _autofillData: null,

  show: Task.async(function* () {
    return yield Messaging.sendRequestToJava({
      type: "Autofill:Prompt",
      data: this._autofillData,
    });
  }),
};
