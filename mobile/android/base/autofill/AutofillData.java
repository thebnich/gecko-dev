package org.mozilla.gecko.autofill;

import java.util.ArrayList;
import java.util.Iterator;

import org.mozilla.gecko.util.NativeJSObject;

/**
 * Data requested by a requestAutocomplete invocation.
 *
 * When a request is handled, the sections, address sections, and fields are populated to match
 * the data being requested. The UI is reponsible for populating the data in each field. When the
 * data is submitted back to Gecko, the populated fields in this class are used to build a JSON
 * response.
 *
 * Examples of valid autocomplete attributes:
 *     section-blue shipping street-address home
 *         section: "blue"
 *         addressSection: "shipping"
 *         fieldName: "street-address"
 *         contactType: "home"
 *     billing name
 *         section: ""
 *         addressSection: "billing"
 *         fieldName: "name"
 *         contactType: ""
 *
 */
public class AutofillData implements Iterable<AutofillData.Section> {
    private final ArrayList<Section> sections = new ArrayList<Section>();

    public AutofillData(NativeJSObject request) {
        final NativeJSObject[] jsSections = request.getObjectArray("sections");
        for (NativeJSObject jsSection : jsSections) {
            final String name = jsSection.getString("name");
            final Section section = new Section(name);

            final NativeJSObject[] jsAddressSections = jsSection.getObjectArray("addressSections");
            for (NativeJSObject jsAddressSection : jsAddressSections) {
                final String addressType = jsAddressSection.getString("addressType");
                final AddressSection addressSection = new AddressSection(addressType);

                final NativeJSObject[] jsFields = jsAddressSection.getObjectArray("fields");
                for (NativeJSObject jsField : jsFields) {
                    final String fieldName = jsField.getString("fieldName");
                    final String contactType = jsField.getString("contactType");
                    final Field field = new Field(fieldName, contactType);

                    addressSection.addField(field);
                }

                section.addAddressSection(addressSection);
            }

            sections.add(section);
        }
    }

    /**
     * Data for a section, corresponding to the section-* autocomplete attribute.
     */
    public static class Section implements Iterable<AddressSection> {
        private final String name;
        private final ArrayList<AddressSection> addressSections = new ArrayList<AddressSection>();

        /**
         * @param name Name of this section. For a field with the autocomplete attribute "section-foo",
         *             the section name will be "foo". If no section is specified, the default empty
         *             string ("") is used.
         */
        public Section(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public Iterator<AddressSection> iterator() {
            return addressSections.iterator();
        }

        private void addAddressSection(AddressSection addressSection) {
            addressSections.add(addressSection);
        }
    }

    /**
     * Data for an address.
     */
    public static class AddressSection implements Iterable<Field> {
        private final String addressType;
        private final ArrayList<Field> fields = new ArrayList<Field>();

        /**
         * @param addressType The address type (e.g., "shipping"). If no address type is specified,
         *                    the default empty string ("") is used.
         */
        public AddressSection(String addressType) {
            this.addressType = addressType;
        }

        public String getAddressType() {
            return addressType;
        }

        @Override
        public Iterator<Field> iterator() {
            return fields.iterator();
        }

        private void addField(Field field) {
            fields.add(field);
        }
    }

    /**
     * Data for a single autocomplete field.
     */
    public static class Field {
        private final String fieldName;
        private final String contactType;
        private String value;

        /**
         * @param fieldName   The autocomplete field name (e.g., "street-address").
         * @param contactType The contact type, if applicable (e.g., "home"). If no contact type is
         *                    specified, the default empty string ("") is used.
         */
        public Field(String fieldName, String contactType) {
            this.fieldName = fieldName;
            this.contactType = contactType;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getContactType() {
            return contactType;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @Override
    public Iterator<Section> iterator() {
        return sections.iterator();
    }
}
