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
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsConjunctionList;
import de.azapps.mirakel.model.list.meta.SpecialListsDoneProperty;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.custom_views.SwipeLinearLayout;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.EditDialogFragment;
import de.azapps.mirakel.settings.model_settings.special_list.helper.SpecialListsViewHelper;

public class ConjunctionFragment extends BasePropertyFragement<SpecialListsConjunctionList>
    implements EditDialogFragment.OnPropertyEditListener, View.OnClickListener,
    SwipeLinearLayout.OnUndoListener {

    private static final String LIST_KEY = "LIST";
    private static final String BACKSTACK_KEY = "BACK";
    public static final int NEW_PROPERTY = -1;

    private ArrayList<Integer> mBackstack;
    private SpecialList mList;
    private SwipeLinearLayout listView;
    private LayoutInflater mInflater;

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

    private static SpecialListsConjunctionList getRootProperty(@NonNull final
            Optional<SpecialListsBaseProperty> where,
            @NonNull final List<Integer> backStack) {
        if (where.isPresent()) {
            if (where.get() instanceof SpecialListsConjunctionList) {
                SpecialListsConjunctionList currentProperty = (SpecialListsConjunctionList) where.get();
                for (int i = 0; i < backStack.size(); i++) {
                    if (backStack.get(i) == NEW_PROPERTY) {
                        final SpecialListsConjunctionList newList = new SpecialListsConjunctionList(
                            (currentProperty.getConjunction() ==
                             SpecialListsConjunctionList.CONJUNCTION.AND) ? SpecialListsConjunctionList.CONJUNCTION.OR :
                            SpecialListsConjunctionList.CONJUNCTION.AND, new ArrayList<SpecialListsBaseProperty>());
                        backStack.set(i, currentProperty.getChilds().size());
                        currentProperty.getChilds().add(newList);
                        return newList;
                    }
                    if (currentProperty.getChilds().get(backStack.get(i)) instanceof SpecialListsConjunctionList) {
                        currentProperty = (SpecialListsConjunctionList) currentProperty.getChilds().get(backStack.get(i));
                    } else {
                        final List<SpecialListsBaseProperty> childs = new ArrayList<>(1);
                        childs.add(currentProperty.getChilds().get(backStack.get(i)));
                        currentProperty = new SpecialListsConjunctionList((currentProperty.getConjunction() ==
                                SpecialListsConjunctionList.CONJUNCTION.AND) ? SpecialListsConjunctionList.CONJUNCTION.OR :
                                SpecialListsConjunctionList.CONJUNCTION.AND, childs);
                        break;
                    }
                }
                return currentProperty;
            }
        }
        return new SpecialListsConjunctionList(SpecialListsConjunctionList.CONJUNCTION.AND,
                                               new ArrayList<SpecialListsBaseProperty>(0));
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        mInflater = inflater;
        final View rootView = inflater.inflate(R.layout.speciallist_condition_list, null);

        listView = (SwipeLinearLayout)rootView.findViewById(R.id.speciallist_items);
        final View add = rootView.findViewById(R.id.speciallist_add_condition);
        add.setOnClickListener(this);
        final ImageView addImage = (ImageView)rootView.findViewById(R.id.speciallist_add_condition_image);
        addImage.setImageDrawable(ThemeManager.getColoredIcon(R.drawable.ic_plus_white_24dp,
                                  ThemeManager.getAccentThemeColor()));
        property = getRootProperty(mList.getWhere(),
                                   mBackstack);
        addImage.setOnClickListener(this);
        final Button addButon = (Button)rootView.findViewById(R.id.speciallist_add_condition_button);
        addButon.setOnClickListener(this);
        updateConditions();
        listView.setOnUndoListener(this);
        return rootView;
    }

    private void updateConditions() {
        int index = 0;
        for (final SpecialListsBaseProperty child : property.getChilds()) {
            listView.addView(generateView(child, index), index++);
        }
    }

    @Override
    public void onEditFinish(@NonNull final SpecialList list) {
        mBackstack.remove(mBackstack.size() - 1);
        mList = list;
        listView.removeViews(0, listView.getChildCount() - 1);
        property = getRootProperty(mList.getWhere(),
                                   mBackstack);
        updateConditions();
    }

    @Override
    public void onClick(final View v) {
        final SpecialListsDoneProperty property = new SpecialListsDoneProperty(false);
        mBackstack.add(NEW_PROPERTY);
        listView.addView(generateView(property, mBackstack.size() - 2), listView.getChildCount() - 1);
        listView.requestLayout();
        listView.getParent().invalidateChild(listView, new Rect());
        EditDialogFragment.newInstance(mList, property, mBackstack, this,
                                       this.property).show(((AppCompatActivity)
                                               getActivity()).getSupportFragmentManager(), "editdialog");

    }

    private View generateView(final SpecialListsBaseProperty property, final int index) {
        final View v = new SpecialListsViewHelper(property, getActivity()).getView(null, mInflater, null);
        v.setTag(SwipeLinearLayout.SWIPEABLE_VIEW, 42);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mBackstack.add(index);
                EditDialogFragment.newInstance(mList, property, mBackstack,
                                               ConjunctionFragment.this, ConjunctionFragment.this.property).show(((AppCompatActivity)
                                                       getActivity()).getSupportFragmentManager(), "editdialog");
            }
        });
        return v;
    }


    @Override
    public void onRemove(final int index) {
        if (index < property.getChilds().size()) {
            this.property.getChilds().remove(index);
        }
    }
}
