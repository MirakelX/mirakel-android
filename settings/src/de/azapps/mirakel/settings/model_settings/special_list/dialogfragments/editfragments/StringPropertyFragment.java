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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import de.azapps.mirakel.model.list.meta.SpecialListsStringProperty;
import de.azapps.mirakel.settings.R;

public class StringPropertyFragment extends BasePropertyFragement<SpecialListsStringProperty> {

    private EditText searchString;
    private RadioGroup type;
    private RadioButton begin;
    private RadioButton contain;
    private RadioButton end;

    public static StringPropertyFragment newInstance(SpecialListsStringProperty property) {
        return setInitialArguments(new StringPropertyFragment(), property);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_name_dialog, null);
        searchString = (EditText)rootView.findViewById(R.id.where_like);
        searchString.setText(property.getSearchString());
        searchString.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                property.setSearchString(s.toString());
                updateText();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // nothing
            }
        });

        type = (RadioGroup)rootView.findViewById(R.id.where_like_radio);
        switch (property.getType()) {
        case BEGIN:
            type.check(R.id.where_like_begin);
            break;
        case END:
            type.check(R.id.where_like_end);
            break;
        case CONTAINS:
            type.check(R.id.where_like_contain);
            break;
        }

        type.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.where_like_begin) {
                    property.setType(SpecialListsStringProperty.Type.BEGIN);
                } else if (checkedId == R.id.where_like_end) {
                    property.setType(SpecialListsStringProperty.Type.END);
                } else if (checkedId == R.id.where_like_contain) {
                    property.setType(SpecialListsStringProperty.Type.CONTAINS);
                }
            }
        });

        CheckBox negated = (CheckBox)rootView.findViewById(R.id.where_like_inverte);

        negated.setChecked(property.isSet());
        negated.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                property.setIsNegated(isChecked);
            }
        });

        begin = (RadioButton)rootView.findViewById(R.id.where_like_begin);
        contain = (RadioButton)rootView.findViewById(R.id.where_like_contain);
        end = (RadioButton)rootView.findViewById(R.id.where_like_end);
        updateText();
        return rootView;
    }

    private void updateText() {
        begin.setText(getActivity().getString(R.string.where_like_begin_text,
                                              "\"" + property.getSearchString() + "\""));
        contain.setText(getActivity().getString(R.string.where_like_contain_text,
                                                "\"" + property.getSearchString() + "\""));
        end.setText(getActivity().getString(R.string.where_like_end_text,
                                            "\"" + property.getSearchString() + "\""));
    }
}
