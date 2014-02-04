// -*- Mode: js2; tab-width: 2; indent-tabs-mode: nil; js2-basic-offset: 2; js2-skip-preprocessor-directives: t; -*-
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

"use strict";

this.EXPORTED_SYMBOLS = [ "JavaRequest" ];

const { utils: Cu } = Components;

Cu.import("resource://gre/modules/Messaging.jsm");
Cu.import("resource://gre/modules/Services.jsm");

this.JavaRequest = {
  _requestListeners: {},

  addListener: function (aListener, aMessage) {
    if (aMessage in this._requestListeners) {
      Cu.reportError("Error in addListener: A listener already exists for message " + aMessage);
      return;
    }

    if (typeof aListener.onRequest !== "function") {
      Cu.reportError("Error in addListener: Listener must have an onRequest method for message " + aMessage);
      return;
    }

    this._requestListeners[aMessage] = aListener;

    Services.obs.addObserver(this, aMessage, false);
  },

  observe: function (aSubject, aTopic, aData) {
    let wrapper = JSON.parse(aData);
    let listener = this._requestListeners[aTopic];
    listener.onRequest(aTopic, wrapper.data, function (response) {
      sendMessageToJava({
        type: "Gecko:Request" + wrapper.id,
        response: response
      });
    });
  },

  removeListener: function (aListener, aMessage) {
    if (!(aMessage in this._requestListeners)) {
      Cu.reportError("Error in removeListener: There is no observer for message " + aMessage);
      return;
    }

    if (this._requestListeners[aMessage] !== aListener) {
      Cu.reportError("Error in removeListener: Given listener is not observing message " + aMessage);
      return;
    }

    delete this._requestListeners[aMessage];
    Services.obs.removeObserver(this, aMessage);
  }
};
