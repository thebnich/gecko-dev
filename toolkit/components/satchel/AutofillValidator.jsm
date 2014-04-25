/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
"use strict";

this.EXPORTED_SYMBOLS = ["AutofillValidator"];

const { classes: Cc, interfaces: Ci, utils: Cu } = Components;

Cu.import("resource://gre/modules/InputValidator.jsm");


let nonEmptyValidator = function (value) {
  return value.replace(/^\s+|\s+$/g, '') !== "";
};

let monthValidator = function (value) {
  let num = parseInt(value);
  return 1 <= num && num <= 12;
}

let positiveIntValidator = function (value) {
  let num = parseInt(value);
  return num > 0;
}

let addressAutocompleteFields = {
  "name": nonEmptyValidator,
  "tel": nonEmptyValidator, // BRN: add phone number validator
  "email": InputValidator.validateEmail.bind(InputValidator),
  "address-line1": nonEmptyValidator,
  "postal-code": nonEmptyValidator,
  "locality": nonEmptyValidator,
  "region": nonEmptyValidator,
  "country": nonEmptyValidator,
};

let paymentAutocompleteFields = {
  "cc-name": nonEmptyValidator,
  "cc-number": InputValidator.validateCreditCard.bind(InputValidator),
  "cc-exp-month": monthValidator,
  "cc-exp-year": positiveIntValidator,
  "cc-csc": positiveIntValidator,
};


function Validator() {}

/**
 * Returns an object containing validation results.
 * The returned object contains the following properties:
 *   valid: Boolean indicating whether the validation succeeded.
 *   fields: An object whose properties are the fields that failed to validate.
 *           Each property is mapped to an error string to be shown in the UI.
 */
Validator.prototype.validate = function (data) {
  let result = {
    valid: true,
    fields: {}
  };

  // BRN: localization
  for (let field in this.fieldList) {
    if (!data[field]) {
      result.valid = false;
      result.fields[field] = "Missing required field";
    } else if (!this.fieldList[field](data[field])) {
      result.valid = false;
      result.fields[field] = "Invalid entry";
    }
  }

  return result;
};

function PaymentValidator() {}
PaymentValidator.prototype = Object.create(Validator.prototype);
PaymentValidator.prototype.fieldList = paymentAutocompleteFields;

function AddressValidator() {}
AddressValidator.prototype = Object.create(Validator.prototype);
AddressValidator.prototype.fieldList = addressAutocompleteFields;


let paymentValidator = new PaymentValidator();
let addressValidator = new AddressValidator();
let AutofillValidator = {
  validatePayment: function (data) {
    return paymentValidator.validate(data);
  },

  validateAddress: function (data) {
    return addressValidator.validate(data);
  }
};
