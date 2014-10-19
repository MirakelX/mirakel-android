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

package de.azapps.mirakel.settings.model_settings.special_list.helper;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.cocosw.undobar.UndoBarController;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsConjunctionList;
import de.azapps.mirakel.model.list.meta.SpecialListsDoneProperty;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.EditDialogFragment;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class SpecialListsConditionAdapter extends ArrayAdapter<SpecialListsViewHelper> {

    public static final int NEW_PROPERTY = -1;
    private static final String TAG = "SpecialListsConditionAdapter";
    private final LayoutInflater mInflater;

    @NonNull
    public List<SpecialListsViewHelper> getData() {
        return data;
    }

    @NonNull
    private List<SpecialListsViewHelper> data;

    @NonNull
    private Optional<UndoBarController.UndoBar> undo = absent();

    private int preferencesCount;

    public SpecialListsConditionAdapter(final Context context, final int resource,
                                        @NonNull final List<SpecialListsViewHelper> objects) {
        super(context, resource, objects);
        mInflater = (LayoutInflater)context.getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
        data = objects;
        preferencesCount = Collections2.filter(data, new Predicate<SpecialListsViewHelper>() {
            @Override
            public boolean apply(SpecialListsViewHelper input) {
                return input.getPreference().isPresent();
            }
        }).size();
    }

    private static SpecialListsConjunctionList getRootProperty(@NonNull final
            Optional<SpecialListsBaseProperty> where, @NonNull final List<SpecialListsBaseProperty> conditions,
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
                conditions.addAll(currentProperty.getChilds());
                return currentProperty;
            } else {
                conditions.add(where.get());
            }
        }
        return new SpecialListsConjunctionList(SpecialListsConjunctionList.CONJUNCTION.AND,
                                               new ArrayList<SpecialListsBaseProperty>(0));
    }

    public static SpecialListsConditionAdapter setUpListView(final @NonNull SpecialList list,
            final @NonNull DragSortListView listView, final @NonNull Activity activity,
            final @NonNull FragmentManager fm,
            final @NonNull ArrayList<Integer>backStack,
            @NonNull final EditDialogFragment.OnPropertyEditListener listener, @NonNull final Button add,
            final @NonNull List<Preference> topPrefernces) {

        final List<SpecialListsBaseProperty> conditions = new ArrayList<>();
        final SpecialListsConjunctionList rootProperty = getRootProperty(list.getWhere(), conditions,
                backStack);
        final List<SpecialListsViewHelper> data = new ArrayList<>(Collections2.transform(topPrefernces,
        new Function<Preference, SpecialListsViewHelper>() {
            @Override
            public SpecialListsViewHelper apply(Preference input) {
                return new SpecialListsViewHelper(input, activity);
            }
        }));
        data.addAll(getTransformedCondition(activity, conditions));
        final SpecialListsConditionAdapter adapter = new SpecialListsConditionAdapter(activity, 0, data);
        listView.setAdapter(adapter);
        listView.setDragEnabled(true);
        listView.setParts(topPrefernces.size());
        listView.setRemoveListener(new DragSortListView.RemoveListener() {
            @Override
            public void remove(final int which) {
                adapter.cancleUndo(backStack);
                final SpecialListsBaseProperty property = adapter.getData().get(
                            which).getCondition().get();// only conditions can be removed, so its safe to call this here
                final SpecialList listToSave = EditDialogFragment.execOnTree(list, backStack,
                new EditDialogFragment.WorkOnTree() {
                    @Override
                    public void onTreeExists(final int position,
                                             @NonNull final SpecialListsConjunctionList currentProperty) {
                        currentProperty.getChilds().remove(which - adapter.preferencesCount);
                    }

                    @NonNull
                    @Override
                    public Optional<SpecialListsBaseProperty> onTreeNotExists() {
                        return absent();
                    }
                }, 0);
                listToSave.save();
                adapter.setNewList(listToSave, backStack);
                backStack.add(which - adapter.preferencesCount);

                final UndoBarController.UndoBar undo = new UndoBarController.UndoBar(activity);
                adapter.setUndo(of(undo));
                undo.message(activity.getString(R.string.Undo)).listener(
                new UndoBarController.AdvancedUndoListener() {
                    @Override
                    public void onHide(final Parcelable parcelable) {
                        adapter.cancleUndo(backStack);
                    }

                    @Override
                    public void onClear() {

                    }

                    @Override
                    public void onUndo(final Parcelable parcelable) {
                        final SpecialList listToSave = EditDialogFragment.execOnTree(list, backStack,
                        new EditDialogFragment.WorkOnTree() {
                            @Override
                            public void onTreeExists(final int position,
                                                     @NonNull final SpecialListsConjunctionList currentProperty) {
                                final List<SpecialListsBaseProperty> childs = new ArrayList<>();
                                final List<SpecialListsBaseProperty> oldChilds = currentProperty.getChilds();
                                boolean added = false;
                                for (int j = 0; j < oldChilds.size(); j++) {
                                    if (!added && (j == backStack.get(position))) {
                                        childs.add(property);
                                        added = true;
                                    }
                                    childs.add(oldChilds.get(j));
                                }
                                if (!added) {
                                    childs.add(property);
                                }
                                currentProperty.setChilds(childs);
                            }

                            @NonNull
                            @Override
                            public Optional<SpecialListsBaseProperty> onTreeNotExists() {
                                return of(property);
                            }
                        }, 1);
                        listToSave.save();
                        adapter.cancleUndo(backStack);
                        adapter.setNewList(listToSave, backStack);
                    }
                }).show();

            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final SpecialListsViewHelper item = adapter.getItem(position);
                if (item.getType() == SpecialListsViewHelper.Type.CONDITION) {
                    adapter.cancleUndo(backStack);
                    backStack.add(position - adapter.preferencesCount);
                    final SpecialListsBaseProperty property = item.getCondition().get();
                    EditDialogFragment.newInstance(list, property, backStack, listener, rootProperty).show(fm,
                            "editdialog");
                } else {
                    final Preference pref = item.getPreference().get();
                    pref.getOnPreferenceClickListener().onPreferenceClick(pref);
                }
            }
        });


        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.cancleUndo(backStack);
                final SpecialListsDoneProperty property = new SpecialListsDoneProperty(false);
                backStack.add(NEW_PROPERTY);
                EditDialogFragment.newInstance(list, property, backStack, listener, rootProperty).show(fm,
                        "editdialog");
            }
        });
        return adapter;
    }

    @NonNull
    private static Collection<SpecialListsViewHelper> getTransformedCondition(
        final @NonNull Context ctx, final @NonNull List<SpecialListsBaseProperty> conditions) {
        return Collections2.transform(conditions,
        new Function<SpecialListsBaseProperty, SpecialListsViewHelper>() {
            @Override
            public SpecialListsViewHelper apply(SpecialListsBaseProperty input) {
                return new SpecialListsViewHelper(input, ctx);
            }
        });
    }

    private void cancleUndo(final @NonNull List<Integer> backStack) {
        if (isUndo()) {
            undo.get().clear();
            undo = absent();
            backStack.remove(backStack.size() - 1);
        }
    }


    public SpecialListsConjunctionList setNewList(@NonNull final SpecialList list,
            @NonNull final List<Integer> backStack) {
        final List<SpecialListsBaseProperty> conditions = new ArrayList<>();
        final SpecialListsConjunctionList rootProperty = getRootProperty(list.getWhere(), conditions,
                backStack);
        final List<SpecialListsViewHelper> newData = new ArrayList<>(Collections2.filter(data,
        new Predicate<SpecialListsViewHelper>() {
            @Override
            public boolean apply(SpecialListsViewHelper input) {
                return input.getType() == SpecialListsViewHelper.Type.PREFERENCE;
            }
        }));
        clear();
        newData.addAll(getTransformedCondition(getContext(), conditions));
        addAll(newData);
        notifyDataSetChanged();
        data = newData;
        return rootProperty;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        if (position >= getCount()) {
            return new View(getContext());
        }
        final SpecialListsViewHelper item = getItem(position);
        return item.getView(convertView, mInflater, parent);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(final int position) {
        final SpecialListsViewHelper item = getItem(position);
        return !((item.getType() == SpecialListsViewHelper.Type.PREFERENCE) &&
                 (item.getPreference().get() instanceof PreferenceCategory));
    }

    public void setUndo(@NonNull final Optional<UndoBarController.UndoBar> undo) {
        this.undo = undo;
    }

    private boolean isUndo() {
        return undo.isPresent();
    }

    @NonNull
    public Optional<UndoBarController.UndoBar> getUndo() {
        return undo;
    }

}
