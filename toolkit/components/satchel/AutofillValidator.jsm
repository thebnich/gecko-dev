/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
"use strict";

this.EXPORTED_SYMBOLS = ["AutofillValidator"];

const { classes: Cc, interfaces: Ci, utils: Cu } = Components;

Cu.import("resource://gre/modules/AutofillContract.jsm");

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

  for (let field in this.fieldList) {
    if (this.fieldList[field] && !data[field]) {
      result.valid = false;
      // BRN: localization
      result.fields[field] = "Missing required field";
    }
  }

  return result;
};

// BRN: add validation for all payment fields
function PaymentValidator() {}
PaymentValidator.prototype = Object.create(Validator.prototype);
PaymentValidator.prototype.fieldList = AutofillContract.paymentAutocompleteFields;

// BRN: add validation for all address fields
function AddressValidator() {}
AddressValidator.prototype = Object.create(Validator.prototype);
AddressValidator.prototype.fieldList = AutofillContract.addressAutocompleteFields;


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
