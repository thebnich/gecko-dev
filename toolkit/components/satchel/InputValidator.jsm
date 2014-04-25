/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
"use strict";

this.EXPORTED_SYMBOLS = ["InputValidator"];

const { classes: Cc, interfaces: Ci, utils: Cu } = Components;

// http://en.wikipedia.org/wiki/Bank_card_number#Issuer_identification_number_.28IIN.29
let cardTypes = {
  "amex": /^3[47][0-9]{13}$/,
  "discover": /^6(?:011[0-9]{12}|4[4-9][0-9]{13}|5[0-9]{14})$/,
  "mastercard": /^5[0-5][0-9]{14}$/,
  "visa": /^4(?:[0-9]{12}|[0-9]{15})$/,
};

let InputValidator = {
  validateEmail: function (email) {
    // Regex given by the HTML standard for valid email addresses:
    // http://www.whatwg.org/specs/web-apps/current-work/multipage/states-of-the-type-attribute.html#valid-e-mail-address
    let regex = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/;
    return regex.test(email);
  },

  // Returns the type of credit card or null if the number is invalid.
  getCreditCardType: function (number) {
    let card = null;
    for (let type in cardTypes) {
      if (cardTypes[type].test(number)) {
        card = type;
      }
    }

    if (!card) {
      return null;
    }

    // Implements the Luhn checksum algorithm as described at
    // http://wikipedia.org/wiki/Luhn_algorithm
    let len = number.length;
    let total = 0;
    for (let i = 0; i < len; i++) {
        let ch = parseInt(number[len - i - 1]);
        if (i % 2 == 1) {
            // Double it, add digits together if > 10
            ch *= 2;
            if (ch > 9)
                ch -= 9;
        }
        total += ch;
    }

    return (total % 10 === 0) ? card : null;
  },

  validateCreditCard: function (number) {
    return this.getCreditCardType(number) != null;
  },
};
