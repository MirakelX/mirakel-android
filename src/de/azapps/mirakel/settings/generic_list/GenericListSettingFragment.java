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

import android.app.Activity;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import de.azapps.mirakel.adapter.SimpleModelAdapter;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.settings.R;

public class GenericListSettingFragment<T extends ModelBase> extends ListFragment implements
    LoaderManager.LoaderCallbacks {
    public static interface Callbacks<T extends ModelBase> {
        public void selectItem(T model);
        public Uri getUri();
        public Class<T> getMyClass();
    }

    private SimpleModelAdapter<T> mAdapter;
    private Callbacks<T> mCallbacks;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new SimpleModelAdapter<T>(getActivity(), null, 0, mCallbacks.getMyClass());
        getListView().setDivider(new ColorDrawable(getResources().getColor(R.color.transparent)));
        setListAdapter(mAdapter);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (Callbacks<T>) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnItemSelectListener");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onListItemClick (ListView l, View v, int position, long id) {
        mCallbacks.selectItem(((SimpleModelAdapter.ViewHolder<T>) v.getTag()).getModel());
    }


    @Override
    public Loader onCreateLoader(int i, Bundle bundle) {
        return new MirakelQueryBuilder(getActivity()).toSupportCursorLoader(
                   mCallbacks.getUri());
    }


    @Override
    public void onLoadFinished(Loader loader, Object o) {
        mAdapter.swapCursor((Cursor) o);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mAdapter.swapCursor(null);
    }

}
