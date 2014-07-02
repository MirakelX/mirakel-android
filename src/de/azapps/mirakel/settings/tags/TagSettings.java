package de.azapps.mirakel.settings.tags;

import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import de.azapps.mirakel.helper.PreferencesHelper;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.settings.ColorPickerPref;

public class TagSettings extends PreferencesHelper {

    private EditTextPreference name;
    private CheckBoxPreference darkBackground;
    private ColorPickerPref background;
    private final Tag tag;

    public TagSettings(final TagsSettingsActivity c, final Tag tag) {
        super(c);
        this.tag = tag;
    }

    public TagSettings(final TagsSettingsFragment c, final Tag tag) {
        super(c);
        this.tag = tag;
    }

    public void setup() {
        this.name = (EditTextPreference) findPreference("tag_name");
        this.darkBackground = (CheckBoxPreference) findPreference("tag_dark_text");
        this.background = (ColorPickerPref) findPreference("tag_background_color");
        this.name.setSummary(this.tag.getName());
        this.name.setText(this.tag.getName());
        this.name
        .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                TagSettings.this.tag.setName((String) newValue);
                TagSettings.this.tag.save();
                TagSettings.this.name.setSummary(TagSettings.this.tag
                                                 .getName());
                return true;
            }
        });
        this.darkBackground.setChecked(this.tag.isDarkText());
        this.darkBackground
        .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                TagSettings.this.tag.setDarkText((Boolean) newValue);
                TagSettings.this.tag.save();
                return true;
            }
        });
        this.background.setColor(this.tag.getBackgroundColor());
        this.background.setOldColor(this.tag.getBackgroundColor());
        this.background
        .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                TagSettings.this.tag
                .setBackgroundColor(TagSettings.this.background
                                    .getColor());
                TagSettings.this.tag.save();
                return true;
            }
        });
    }

}
