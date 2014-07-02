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
package de.azapps.mirakel.settings.recurring;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakel.settings.R;

public class RecurringActivity extends ListSettings {

    protected Recurring recurring;

    @SuppressLint("NewApi")
    @Override
    protected OnClickListener getAddOnClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(final View v) {
                newRecurring();
                clickOnLast();
                invalidateHeaders();
            }
        };
    }

    protected Recurring newRecurring(final boolean temporary) {
        return Recurring.newRecurring(getString(R.string.new_recurring), 0, 0,
                                      0, 0, 1, true, null, null, temporary, false,
                                      new SparseBooleanArray());// TODO add option for exakt...
    }

    protected Recurring newRecurring() {
        return newRecurring(false);
    }

    @Override
    protected int getSettingsRessource() {
        return R.xml.settings_recurring;
    }

    @Override
    protected void setupSettings() {
        this.recurring = Recurring.get(getIntent().getIntExtra("id", 0));
        new RecurringSettings(this, this.recurring).setup();
    }

    @Override
    protected List<Pair<Integer, String>> getItems() {
        final List<Recurring> allRecurrences = Recurring.all();
        final List<Pair<Integer, String>> items = new ArrayList<Pair<Integer, String>>();
        for (final Recurring r : allRecurrences) {
            items.add(new Pair<Integer, String>(r.getId(), r.getLabel()));
        }
        return items;
    }

    @Override
    protected Class<?> getDestClass() {
        return RecurringActivity.class;
    }

    @Override
    protected Class<?> getDestFragmentClass() {
        return RecurringFragment.class;
    }

    @Override
    protected int getTitleRessource() {
        return R.string.recurring;
    }

    @Override
    protected OnClickListener getHelpOnClickListener() {
        return null;
    }

    @SuppressLint("NewApi")
    @Override
    public OnClickListener getDelOnClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(final View v) {
                RecurringActivity.this.recurring.destroy();
                if (Build.VERSION.SDK_INT < 11 || !onIsMultiPane()) {
                    finish();
                } else {
                    try {
                        if (getHeader().size() > 0) {
                            onHeaderClick(getHeader().get(0), 0);
                        }
                        invalidateHeaders();
                    } catch (final Exception e) {
                        finish();
                    }
                }
            }
        };
    }

    public void setReccuring(final Recurring r) {
        this.recurring = r;
    }

}
