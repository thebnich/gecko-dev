package org.mozilla.gecko.preferences;

import java.util.Collection;

import org.mozilla.gecko.autofill.AutofillEntries;
import org.mozilla.gecko.autofill.AutofillEntry;
import org.mozilla.gecko.autofill.AutofillGeckoClient;
import org.mozilla.gecko.autofill.AutofillGeckoClient.DataListener;
import org.mozilla.gecko.autofill.EditDialog.OnValidatedListener;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

public abstract class AutofillCategory<T extends AutofillEntry>
        extends PreferenceCategory
        implements DataListener {
    private static final String PREF_PREFIX = "android.not_a_preference.autofill.";

    public AutofillCategory(Context context) {
        super(context);
    }

    public AutofillCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutofillCategory(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);

        AutofillGeckoClient.getAutofillData(this);
    }

    @Override
    public void onData(AutofillEntries entries) {
        for (T entry : getCollection(entries)) {
            addEntryPreference(entry);
        }
    }

    @Override
    protected boolean onPrepareAddPreference(Preference preference) {
        super.onPrepareAddPreference(preference);

        final String key = PREF_PREFIX + getAddKey();
        if (key.equals(preference.getKey())) {
            preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final T entry = createEntry();
                    showDialog(entry, new OnValidatedListener() {
                        @Override
                        public void onValidated() {
                            addEntryPreference(entry);
                        }
                    });
                    return true;
                }
            });
        }

        return true;
    }

    private void addEntryPreference(final T entry) {
        Preference pref = new Preference(getContext());
        updateEntryPreference(pref, entry);

        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                showDialog(entry, new OnValidatedListener() {
                    @Override
                    public void onValidated() {
                        updateEntryPreference(preference, entry);
                    }
                });
                return true;
            }
        });

        addPreference(pref);
    }

    protected abstract String getAddKey();
    protected abstract Collection<T> getCollection(AutofillEntries entries);
    protected abstract T createEntry();
    protected abstract void showDialog(T entry, OnValidatedListener listener);
    protected abstract void updateEntryPreference(Preference pref, T entry);
}
