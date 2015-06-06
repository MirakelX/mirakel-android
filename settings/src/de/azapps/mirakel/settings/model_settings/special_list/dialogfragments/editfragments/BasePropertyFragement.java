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
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;

public abstract class BasePropertyFragement<T extends SpecialListsBaseProperty> extends Fragment {

    public final static String PROPERTY_KEY = "PROPERTY";

    @NonNull
    protected T property;

    protected BasePropertyFragement() {

    }

    @Override
    public void onCreate(@NonNull final Bundle extras) {
        super.onCreate(extras);
        if ((getArguments() != null) && getArguments().containsKey(PROPERTY_KEY)) {
            property = getArguments().getParcelable(PROPERTY_KEY);
        } else {
            throw new IllegalArgumentException("No property passed");
        }
    }


    public T getProperty() {
        return property;
    }

    public void setProperty(@NonNull final T property) {
        this.property = property;
    }


    protected static<S extends BasePropertyFragement> S setInitialArguments(final S fragment,
            final SpecialListsBaseProperty property) {
        final Bundle args = new Bundle();
        args.putParcelable(PROPERTY_KEY, property);
        fragment.setArguments(args);
        return fragment;
    }

}
