/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.settings;

import android.net.Uri;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.settings.ColorPickerPref;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.generic_list.GenericListSettingActivity;
import de.azapps.mirakel.settings.generic_list.GenericSettingsFragment;

public class TagsSettingsActivity extends GenericListSettingActivity<Tag> {
    @Override
    protected void createModel() {
        Tag tag = Tag.newTag(getString(R.string.tag_new));
        selectItem(tag);
    }


    @NonNull
    @Override
    public String getTitle(Optional<Tag> model) {
        if (model.isPresent()) {
            return model.get().getName();
        } else {
            return getString(R.string.no_tag_selected);
        }
    }

    @Override
    public int getPreferenceResource() {
        return R.xml.settings_tag;
    }

    @Override
    public Uri getUri() {
        return MirakelInternalContentProvider.TAG_URI;
    }

    @Override
    public Class<Tag> getMyClass() {
        return Tag.class;
    }

    @Override
    public void setUp(Optional<Tag> model, GenericSettingsFragment fragment) {
        if (!model.isPresent()) {
            return;
        }
        final Tag tag = model.get();
        final EditTextPreference name = (EditTextPreference) fragment.findPreference("tag_name");
        final CheckBoxPreference darkBackground = (CheckBoxPreference)
                fragment.findPreference("tag_dark_text");
        final ColorPickerPref background = (ColorPickerPref)
                                           fragment.findPreference("tag_background_color");
        name.setSummary(tag.getName());
        name.setText(tag.getName());
        name
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                tag.setName((String) newValue);
                tag.save();
                name.setSummary(tag.getName());
                return true;
            }
        });
        darkBackground.setChecked(tag.isDarkText());
        darkBackground
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                tag.setDarkText((Boolean) newValue);
                tag.save();
                return true;
            }
        });
        background.setColor(tag.getBackgroundColor());
        background.setOldColor(tag.getBackgroundColor());
        background
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                tag.setBackgroundColor(background.getColor());
                tag.save();
                return true;
            }
        });
    }
}
