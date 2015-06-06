/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.settings.model_settings.tag;

import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.settings.ColorPickerPref;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelDetailFragment;

public class TagDetailFragment extends GenericModelDetailFragment<Tag> {

    @Override
    protected boolean hasMenu() {
        return !MirakelCommonPreferences.isTablet();
    }

    @NonNull
    @Override
    protected Tag getDummyItem() {
        return Tag.newTag(getString(R.string.tag_new));
    }

    @Override
    protected int getResourceId() {
        return R.xml.settings_tag;
    }

    @Override
    protected void setUp() {
        final Tag tag = mItem;
        final EditTextPreference name = (EditTextPreference) findPreference("tag_name");
        final ColorPickerPref background = (ColorPickerPref) findPreference("tag_background_color");
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
                updateList();
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
