package org.mozilla.gecko.autofill;

/**
 * Fields used in a billing or shipping autofill entry.
 */
public class AddressAutofillEntry extends AutofillEntry {
    public String name;
    public String addressLine1;
    public String addressLine2;
    public String locality;
    public String region;
    public String country;
    public String postalCode;
    public String email;
    public String tel;

    public AddressAutofillEntry(String guid) {
        super(guid);
    }

    public AddressAutofillEntry(String guid, String name, String addressLine1, String addressLine2, String locality,
                        String region, String country, String postalCode, String email, String tel) {
        super(guid);
        this.name = name;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.locality = locality;
        this.region = region;
        this.country = country;
        this.postalCode = postalCode;
        this.email = email;
        this.tel = tel;
    }
}
