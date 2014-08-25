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

package de.azapps.mirakel.settings.generic_list;


import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import de.azapps.mirakel.model.ModelBase;

import static com.google.common.base.Optional.fromNullable;

public class GenericSettingsFragment<T extends ModelBase> extends PreferenceFragment {
    public static final String ARGUMENT_MODEL = "MODEL";

    public interface Callbacks<T extends ModelBase> {
        public void setUp(Optional<T> model, GenericSettingsFragment fragment);

        @NonNull
        public String getTitle(Optional<T> model);

        public int getPreferenceResource();
    }


    private Callbacks<T> mCallbacks;

    public Optional<T> getModel() {
        return model;
    }

    private Optional<T> model;


    public GenericSettingsFragment() {
        super();
    }


    public static <T extends ModelBase> GenericSettingsFragment<T> newInstance(T model) {
        GenericSettingsFragment<T> f = new GenericSettingsFragment<>();
        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_MODEL, model);
        f.setArguments(args);
        return f;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (Callbacks<T>) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement Callbacks");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(mCallbacks.getPreferenceResource());
        final Bundle b = getArguments();
        final ActionBar actionbar = getActivity().getActionBar();
        if (b != null && b.containsKey(ARGUMENT_MODEL)) {
            this.model = fromNullable((T) b.getParcelable(ARGUMENT_MODEL));
        } else {
            model = Optional.absent();
        }
        actionbar.setTitle(mCallbacks.getTitle(model));
        mCallbacks.setUp(model, this);
    }

    public void removePreference(final String which) {
        final Preference pref = findPreference(which);
        if (pref != null) {
            (this).getPreferenceScreen()
            .removePreference(pref);
        }
    }
}
