/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
"use strict";

const { classes: Cc, interfaces: Ci, utils: Cu } = Components;

Cu.import("resource://gre/modules/AutofillContract.jsm");
Cu.import("resource://gre/modules/Promise.jsm");
Cu.import("resource://gre/modules/Task.jsm");
Cu.import("resource://gre/modules/XPCOMUtils.jsm");

XPCOMUtils.defineLazyServiceGetter(this, "AutofillUIGlue",
                                   "@mozilla.org/autofill-ui-glue;1",
                                   "nsIAutofillUIGlue");

XPCOMUtils.defineLazyModuleGetter(this, "AutofillProvider",
                                  "resource://gre/modules/AutofillProvider.jsm");

function AutofillController() {}

AutofillController.prototype = {
  _activeWindows: new WeakMap(),

  _parseAutocomplete: function (attr) {
    function* attrGenerator(attr) {
      attr = attr.toLowerCase();
      let regex = /\S+/g;
      let match;
      while (match = regex.exec(attr)) {
        yield match[0];
      }
    }

    let gen = attrGenerator(attr);

    function getToken() {
      let result = gen.next();
      if (result.done) {
        throw "Unexpected end of autocomplete attributes";
      }
      return result.value;
    }

    // Use shipping data by default.
    let type = "shipping";

    let token = getToken();
    if (token === "billing" || token === "shipping") {
      type = token;
      token = getToken();
    }

    let key;
    if (token in AutofillContract.addressAutocompleteFields) {
      key = token;
    } else if (token in AutofillContract.paymentAutocompleteFields) {
      type = "payment";
      key = token;
    } else {
      throw "Unknown autocomplete attribute";
    }

    // Abort if there are extra attributes we're not expecting.
    if (!gen.next().done) {
      throw "Unexpected autocomplete attribute";
    }

    return {
      key: key,
      type: type
    };
  },

  _submit: function (form, data) {
    function getEntryFromGuid(type, guid) {
      for (let entry of type.entries) {
        if (entry.guid === guid) {
          return entry;
        }
      }
      return null;
    }

    AutofillProvider.getDB(function (entries) {
      let selected = {};
      let guids = {};

      if (data.payment.type == "saved") {
        guids.payment = data.payment.guid;
        selected.payment = getEntryFromGuid(entries.payment, guids.payment).data;
      }

      if (data.billing.type == "saved") {
        guids.billing = data.billing.guid;
        selected.billing = getEntryFromGuid(entries.address, guids.billing).data;
      }

      if (data.shipping.type == "saved") {
        guids.shipping = data.shipping.guid;
        selected.shipping = getEntryFromGuid(entries.address, guids.shipping).data;
      } else if (data.shipping.type == "sameAsBilling") {
        guids.shipping = null;
        selected.shipping = selected.billing;
      }

      AutofillProvider.saveSelections(guids);

      // BRN: handle select fields restrictions
      let win = form.ownerDocument.defaultView;
      let els = form.querySelectorAll("input,select");
      for (let i = 0; i < els.length; ++i) {
        let el = els[i];
        let attr = el.getAttribute("autocomplete");

        if (!attr || el.disabled || el.readOnly) {
          continue;
        }

        let field;
        try {
          field = this._parseAutocomplete(attr);
        } catch (e) {
          // BRN: does this need to be logged?
          continue;
        }

        let value;

        // Handle special case fields (those that are optional or are a
        // composition of other fields).
        let type = selected[field.type];
        let addressLine2 = type["address-line2"];
        if (field.key == "street-address") {
          let addressLine1 = type["address-line1"];
          // BRN: localize separator
          value = addressLine1 + (addressLine2 ? ", " + addressLine2 : "");
        } else if (field.key == "address-line2" && !addressLine2) {
          // address-line2 is optional, so use empty string if not given.
          value = "";
        } else if (field.key == "cc-exp") {
          // BRN: localize separator
          value = type["cc-exp-month"] + "/" + type["cc-exp-year"];
        } else {
          value = type[field.key];
        }

        // Truncate field if maxlength is given.
        let maxLength = el.maxLength;
        if (maxLength >= 0) {
          value = value.substr(0, maxLength);
        }

        el.value = value;
        let evt = new win.Event("change", { bubbles: true });
        el.dispatchEvent(evt);
      }

      // Fire event on the form to indicate success.
      let evt = new win.Event("autocomplete", { bubbles: true });
      form.dispatchEvent(evt);
    }.bind(this));
  },

  requestAutocomplete: function (form) {
    let win = form.ownerDocument.defaultView;

    function dispatchError(reason) {
      let evt = new win.AutocompleteErrorEvent("autocompleteerror", { bubbles: true, reason: reason });
      form.dispatchEvent(evt);
    }

    function dispatchDisabled(message) {
      Cu.reportError("requestAutocomplete disabled: " + message);
      Task.spawn(function () {
        dispatchError("disabled");
      });
    }

    let dwu = win.QueryInterface(Components.interfaces.nsIInterfaceRequestor)
                 .getInterface(Components.interfaces.nsIDOMWindowUtils);

    if (!dwu.isHandlingUserInput) {
      dispatchDisabled("requestAutocomplete must be initiated by a trusted input event");
      return;
    }

    if (form.autocomplete === "off") {
      dispatchDisabled("form autocomplete attribute is off");
      return;
    }

    if (this._activeWindows.has(win)) {
      dispatchDisabled("another dialog is already active for this window");
      return;
    }

    this._activeWindows.set(win, true);

    let submit = (data) => this._submit(form, data);
    let cancel = () => dispatchError("cancel");
    let done = () => this._activeWindows.delete(win);
    new Promise((resolve, reject) => AutofillUIGlue.onRequestAutocomplete({ submit: resolve, cancel: reject }))
      .then(submit, cancel)
      .then(done);
  },

  classID: Components.ID("{ed9c2c3c-3f86-4ae5-8e31-10f71b0f19e6}"),
  QueryInterface: XPCOMUtils.generateQI([Ci.nsIAutofillController])
};

this.NSGetFactory = XPCOMUtils.generateNSGetFactory([AutofillController]);
