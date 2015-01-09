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

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;

import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsConjunctionList;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.EditDialogFragment;
import de.azapps.mirakel.settings.model_settings.special_list.helper.SpecialListsConditionAdapter;

public class ConjunctionFragment extends BasePropertyFragement<SpecialListsConjunctionList>
    implements EditDialogFragment.OnPropertyEditListener {

    private static final String LIST_KEY = "LIST";
    private static final String BACKSTACK_KEY = "BACK";

    private ArrayList<Integer> mBackstack;
    private SpecialList mList;
    private SpecialListsConditionAdapter mAdapter;

    public static ConjunctionFragment newInstance(@NonNull final SpecialListsConjunctionList property,
            @NonNull final SpecialList list, @NonNull final ArrayList<Integer> backstack) {
        final ConjunctionFragment fragment = new ConjunctionFragment();
        final Bundle args = new Bundle();
        args.putParcelable(PROPERTY_KEY, property);
        args.putIntegerArrayList(BACKSTACK_KEY, backstack);
        args.putParcelable(LIST_KEY, list);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@NonNull final Bundle extras) {
        super.onCreate(extras);
        if ((getArguments() != null) && getArguments().containsKey(LIST_KEY) &&
            getArguments().containsKey(BACKSTACK_KEY)) {
            mList = getArguments().getParcelable(LIST_KEY);
            mBackstack = getArguments().getIntegerArrayList(BACKSTACK_KEY);
        } else {
            throw new IllegalArgumentException("No property passed");
        }
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.speciallist_condition_list, null);

        final DragSortListView listView = (DragSortListView)rootView.findViewById(R.id.speciallist_items);
        final Button add = (Button)rootView.findViewById(R.id.speciallist_add_condition);

        mAdapter = SpecialListsConditionAdapter.setUpListView(mList, listView, getActivity(),
                   getChildFragmentManager(), mBackstack, this, add, new ArrayList<Preference>());
        return rootView;
    }

    @Override
    public void onEditFinish(@NonNull final SpecialList list) {
        mBackstack.remove(mBackstack.size() - 1);
        property = mAdapter.setNewList(list, mBackstack);
    }
}
