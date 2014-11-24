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
package de.azapps.mirakel.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class MirakelArrayAdapter<T> extends ArrayAdapter<T> {
    private static final String TAG = "MirakelArrayAdapter";
    protected Context context;
    protected boolean darkTheme;
    private final List<T> data;
    protected int layoutResourceId;
    private List<Boolean> selected;
    protected int selectedCount;

    public MirakelArrayAdapter(final Context context,
                               final int textViewResourceId, final List<T> data) {
        super(context, textViewResourceId, textViewResourceId, data);
        this.layoutResourceId = textViewResourceId;
        this.data = data;
        this.context = context;
        this.darkTheme = MirakelCommonPreferences.isDark();
        this.selected = new ArrayList<Boolean>();
        for (int i = 0; i < data.size(); i++) {
            this.selected.add(false);
        }
        this.selectedCount = 0;
    }

    public void addToEnd(final T el) {
        this.data.add(el);
        this.selected.add(false);
    }

    public void addToHead(final T el) {
        this.data.add(0, el);
        this.selected.add(false);
    }

    public List<T> getData() {
        return this.data;
    }

    public void remove(final int location) {
        this.data.remove(location);
        this.selected.remove(location);
    }

    public void addToData(final int location, final T item) {
        this.data.add(location, item);
        this.selected.add(location, false);
    }

    public void addToData(final T item) {
        this.data.add(item);
        this.selected.add(false);
    }

    @NonNull
    public Optional<T> getDataAt(final int location) {
        if (location < data.size()) {
            return of(this.data.get(location));
        }
        return absent();
    }

    public boolean isSelectedAt(final int location) {
        return this.selected.get(location);
    }

    public void changeData(final List<T> newData) {
        this.data.clear();
        this.data.addAll(newData);
        while (this.data.size() > this.selected.size()) {
            this.selected.add(false);
        }
    }

    @Override
    public int getCount() {
        return this.data.size();
    }

    public List<T> getSelected() {
        final List<T> selectedList = new ArrayList<T>();
        for (int i = 0; i < this.data.size(); i++) {
            if (this.selected.get(i)) {
                selectedList.add(this.data.get(i));
            }
        }
        return selectedList;
    }

    public int getSelectedCount() {
        return this.selectedCount;
    }

    public void resetSelected() {
        Log.d(TAG, "reset selected");
        this.selected = new ArrayList<Boolean>();
        for (int i = 0; i < this.data.size(); i++) {
            this.selected.add(false);
        }
        notifyDataSetChanged();
        this.selectedCount = 0;
    }

    public void setSelected(final int position, final boolean selected) {
        this.selected.set(position, selected);
        this.selectedCount += selected ? 1 : -1;
        notifyDataSetChanged();
    }

}
