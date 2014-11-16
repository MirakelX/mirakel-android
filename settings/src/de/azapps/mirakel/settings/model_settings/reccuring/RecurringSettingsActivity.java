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

package de.azapps.mirakel.settings.model_settings.reccuring;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;

import com.google.common.base.Optional;

import java.util.Calendar;

import de.azapps.mirakel.adapter.SimpleModelAdapter;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelListActivity;

import static com.google.common.base.Optional.of;

public class RecurringSettingsActivity extends GenericModelListActivity<Recurring> {


    @Override
    protected boolean isSupport() {
        return false;
    }

    @NonNull
    @Override
    protected Optional<android.app.Fragment> getDetailFragment() {
        return of((android.app.Fragment)new RecurringDetailFragment());
    }

    @NonNull
    @Override
    protected Class<? extends GenericModelListActivity> getSelf() {
        return RecurringSettingsActivity.class;
    }


    @NonNull
    @Override
    protected Recurring getDefaultItem() {
        return Recurring.getSafeFirst();
    }

    @Override
    protected void createItem(@NonNull final Context ctx) {
        onItemSelected(Recurring.newRecurring(getString(R.string.new_recurring), 0, 0,
                                              0, 0, 1, true, Optional.<Calendar>absent(), Optional.<Calendar>absent(), false, false,
                                              new SparseBooleanArray()));
    }

    @NonNull
    @Override
    public SimpleModelAdapter<Recurring> getAdapter() {
        return new SimpleModelAdapter<>(this, new MirakelQueryBuilder(this).query(Recurring.URI), 0,
                                        Recurring.class);
    }

}
