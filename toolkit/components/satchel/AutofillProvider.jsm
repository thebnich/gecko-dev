/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
"use strict";

this.EXPORTED_SYMBOLS = ["AutofillProvider"];

const { classes: Cc, interfaces: Ci, utils: Cu } = Components;

Cu.import("resource://gre/modules/AutofillValidator.jsm");

// BRN: Dummy values used until storage is implemented
var dummyDB = {
  address: {
    mtime: 123,
    billingGuid: "Ag8dal2k",
    shippingGuid: null, // null = use billing
    entries: [
      {
        guid: "Ag8dal2k",
        mtime: 123,
        data: {
          "name": "Brian Nicholson",
          "email": "bnicholson@mozilla.com",
          "tel": "408-555-5555",
          "address-line1": "650 Castro St.",
          "address-line2": "Suite 300",
          "locality": "Mountain View",
          "region": "CA",
          "country": "US",
          "postal-code": "94041"
        }
      },
      {
        guid: "909lZ2jl",
        mtime: 789,
        data: {
          "name": "Brian Nicholson",
          "email": "brn@mozilla.com",
          "tel": "804-555-5555",
          "address-line1": "1234 Peach Tree Ln.",
          "locality": "Richmond",
          "region": "VA",
          "country": "US",
          "postal-code": "23230"
        }
      }
    ]
  },
  payment: {
    mtime: 456,
    paymentGuid: "fha811ZY",
    entries: [
      {
        guid: "fha811ZY",
        mtime: 456,
        data: {
          "cc-name": "Brian R Nicholson",
          "cc-number": "4111-1111-1111-1111",
          "cc-exp-month": "01",
          "cc-exp-year": "2015",
          "cc-csc": "123",
          "cc-type": "Visa"
        }
      }
    ]
  }
};

let AutofillProvider = {
  _cachedData: null,

  /*
   * Updates an entry with the given GUID. If an entry with this GUID does not
   * exist, it is created.
   *
   * Accepts a newEntry parameter with these fields:
   *   guid: GUID of the entry being updated or added
   *   type: The entry type, either "payment" or "address"
   *   data: An object containing all of the updated fields for this entry
   */
  update: function (newEntry) {
    function getEntryFromGuid(type, guid) {
      for (let entry of type.entries) {
        if (entry.guid === guid) {
          return entry;
        }
      }
      return null;
    }

    // The caller is responsible for doing validation before calling update, but
    // do it here again to prevent storing bad data.
    let validation;
    if (newEntry.type == "payment") {
      validation = AutofillValidator.validatePayment(newEntry.data);
    } else if (newEntry.type == "address") {
      validation = AutofillValidator.validateAddress(newEntry.data);
    } else {
      throw "Unknown update type: " + newEntry.type;
    }
    if (!validation.valid) {
      throw "Validation failed: " + JSON.stringify(validation.fields);
    }

    this.getAll(function (cache) {
      let entry = getEntryFromGuid(cache[newEntry.type], newEntry.guid);
      let add = false;
      if (!entry) {
        // Non-existent entry; create a new one.
        entry = {
          guid: newEntry.guid,
          mtime: Date.now()
        };
        add = true;
      }

      // Update the stored entry with newly entered values.
      // BRN: rename these
      entry.data = newEntry.data;

      if (add) {
        cache[newEntry.type].entries.push(entry);
      }

      // Write the in-memory cache to disk.
      this._writeCache(cache);
    }.bind(this));
  },

  /*
   * Saves the given set of GUIDs as default selections. "payment", "billing",
   * and "shipping" are keys on the guids argument whose GUIDs will be stored.
   * If any of these keys are omitted, the database will not be updated for
   * that key. A null value for "shipping" indicates that "Use billing address"
   * should be the default selection.
   */
  saveSelections: function (guids) {
    this.getAll(function (data) {
      if ("payment" in guids) {
        data.payment.paymentGuid = guids.payment;
      }
      if ("billing" in guids) {
        data.address.billingGuid = guids.billing;
      }
      if ("shipping" in guids) {
        data.address.shippingGuid = guids.shipping;
      }
      this._writeData(data);
    }.bind(this));
  },

  _writeData: function (data) {
    // BRN: Save data to JSON on disk.
  },

  getAll: function (callback) {
    if (!this._cachedData) {
      // BRN: Pull user and payment info from database
      this._cachedData = dummyDB;
    }

    callback(this._cachedData);
  }
};
