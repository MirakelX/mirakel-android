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

package de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.editfragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.azapps.mirakel.model.list.meta.SpecialListsSetProperty;
import de.azapps.mirakel.settings.R;

public abstract class SetPropertyFragment<T extends SpecialListsSetProperty> extends
    BasePropertyFragement<T> {

    @NonNull
    protected abstract Map<String, Integer> getElements();

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.edit_set_property_fragment, null);

        ListView listView = (ListView)rootView.findViewById(R.id.set_property_listView);
        listView.setAdapter(new PropertyAdapter(getActivity(), getElements(),
                                                new HashSet<>(property.getContent())));

        CheckBox negated = (CheckBox)rootView.findViewById(R.id.set_property_invert);
        negated.setChecked(property.isSet());
        negated.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                property.setIsNegated(isChecked);
            }
        });

        return rootView;
    }

    protected class PropertyAdapter extends ArrayAdapter<String> implements
        CompoundButton.OnCheckedChangeListener {

        private Set<Integer> checked;
        private Map<String, Integer> data;

        public PropertyAdapter(Context context, Map<String, Integer> objects, Set<Integer> checked) {
            super(context, 0, new ArrayList(objects.keySet()));
            this.checked = checked;
            this.data = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position >= data.size()) {
                return null;
            }
            CheckBox box;
            if (convertView == null || !(convertView instanceof CheckBox)) {
                box = new CheckBox(getContext());
            } else {
                box = (CheckBox)convertView;
            }
            String text = getItem(position);
            box.setText(text);
            box.setChecked(checked.contains(data.get(text)));
            box.setOnCheckedChangeListener(this);

            return box;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                checked.add(data.get(buttonView.getText()));
            } else {
                checked.remove(data.get(buttonView.getText()));
            }
            property.setContent(new ArrayList<>(checked));
        }
    }

}
