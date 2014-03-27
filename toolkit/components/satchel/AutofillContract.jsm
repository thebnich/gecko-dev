/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
"use strict";

this.EXPORTED_SYMBOLS = ["AutofillContract"];

const { classes: Cc, interfaces: Ci, utils: Cu } = Components;

let AutofillContract = Object.freeze({
  // True indicates that this field is shown in the form UI and must have a value.
  addressAutocompleteFields: {
    "name": true,
    "tel": true,
    "email": true,
    "address-line1": true,
    "postal-code": true,
    "locality": true,
    "region": true,
    "country": true,

    // Not shown in the UI; formed by combining address-line1 and address-line2.
    "street-address": false,

    // Optional.
    "address-line2": false,
  },

  // True indicates that this field is shown in the form UI and must have a value.
  paymentAutocompleteFields: {
    "cc-name": true,
    "cc-number": true,
    "cc-exp-month": true,
    "cc-exp-year": true,
    "cc-csc": true,
    "cc-type": true,

    // Not shown in the UI; formed by combining cc-exp-month and cc-exp-year.
    "cc-exp": false,
  }
});
