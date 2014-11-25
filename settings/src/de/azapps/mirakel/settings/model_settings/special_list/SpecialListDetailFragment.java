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

package de.azapps.mirakel.settings.model_settings.special_list;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.google.common.base.Optional;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.MirakelContentObserver;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelDetailFragment;
import de.azapps.mirakel.settings.model_settings.generic_list.IDetailFragment;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.EditDialogFragment;
import de.azapps.mirakel.settings.model_settings.special_list.helper.SpecialListsConditionAdapter;

public class  SpecialListDetailFragment extends Fragment implements
    CompoundButton.OnCheckedChangeListener, EditDialogFragment.OnPropertyEditListener,
    IDetailFragment<SpecialList> {
    private ArrayList<Integer> backStack = new ArrayList<>();

    private SpecialListsConditionAdapter mAdapter;
    private MirakelContentObserver observer;

    protected SpecialList mItem;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SpecialListDetailFragment() {
    }



    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        mItem.setActive(isChecked);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.speciallist_condition_list, container, false);
        final DragSortListView listView = (DragSortListView)rootView.findViewById(R.id.speciallist_items);
        final Button add = (Button) rootView.findViewById(R.id.speciallist_add_condition);
        final List<Preference> preferences = getPrefernces();
        mAdapter = SpecialListsConditionAdapter.setUpListView(mItem, listView, getActivity(),
                   getFragmentManager(), backStack, this, add, preferences);
        return rootView;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(GenericModelDetailFragment.ARG_ITEM)) {
            mItem = getArguments().getParcelable(GenericModelDetailFragment.ARG_ITEM);
        } else {
            // Load the dummy content
            mItem = SpecialList.firstSpecialSafe();
        }
        getActivity().getActionBar().setTitle(mItem.getName());
    }


    @NonNull
    private List<Preference> getPrefernces() {
        List<Preference> preferences = new ArrayList<>();

        PreferenceCategory summary = new PreferenceCategory(getActivity());
        summary.setTitle(R.string.special_list_summary);
        preferences.add(summary);

        final EditTextPreference name = getNamePreference();
        preferences.add(name);

        final CheckBoxPreference active = getIsActivePreference();
        preferences.add(active);


        if (!MirakelCommonPreferences.isDebug()) {
            final Preference where = getWhereStringPreference();
            preferences.add(where);
        }

        PreferenceCategory defaultValues = new PreferenceCategory(getActivity());
        defaultValues.setTitle(R.string.special_lists_defaults);
        preferences.add(defaultValues);


        final Preference defList = getDefaultListPreference();
        preferences.add(defList);

        final Preference defDate = getDefaultDatePreference();
        preferences.add(defDate);

        PreferenceCategory conditions = new PreferenceCategory(getActivity());
        conditions.setTitle(R.string.special_lists_condition_title);
        preferences.add(conditions);


        return preferences;
    }

    private Preference getDefaultDatePreference() {
        final Preference defDate = new Preference(getActivity());
        defDate.setTitle(R.string.special_list_def_date);
        setDefaultDateSummary(defDate, getActivity(), mItem);
        defDate.setOnPreferenceChangeListener(null);
        defDate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            protected AlertDialog alert;

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final String[] items = getResources().getStringArray(R.array.special_list_def_date_picker);
                final int[] values = getResources().getIntArray(R.array.special_list_def_date_picker_val);
                int currentItem = 0;
                if (mItem.getDefaultDate() != null) {
                    final int ddate = mItem.getDefaultDate();
                    for (int i = 0; i < values.length; i++) {
                        if (values[i] == ddate) {
                            currentItem = i;
                        }
                    }
                }
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.special_list_def_date);
                builder.setSingleChoiceItems(items, currentItem,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                                        final int item) {
                        Integer date = values[item];
                        if (date == -1337) {
                            date = null;
                        }
                        mItem.setDefaultDate(date);
                        mItem.save();
                        setDefaultDateSummary(defDate, getActivity(), mItem);
                        mAdapter.notifyDataSetChanged();
                        alert.dismiss();

                    }
                });
                alert = builder.create();
                alert.show();
                return true;
            }
        });
        return defDate;
    }

    private Preference getDefaultListPreference() {
        final List<ListMirakel> lists = ListMirakel.all(false);
        final Preference defList = new Preference(getActivity());
        defList.setTitle(R.string.special_list_def_list);
        defList.setOnPreferenceChangeListener(null);
        setDefaultListSummary(defList, mItem);
        defList.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            protected AlertDialog alert;

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.special_list_def_list);
                final List<CharSequence> items = new ArrayList<>();
                final List<Long> list_ids = new ArrayList<>();
                int currentItem = 0, i = 1;
                items.add(getString(R.string.special_list_first));
                list_ids.add(null);
                for (final ListMirakel list : lists) {
                    if (list.getId() > 0) {
                        items.add(list.getName());
                        if (mItem.getDefaultList() == null) {
                            currentItem = 0;
                        } else {
                            if (mItem.getDefaultList().getId() == list.getId()) {
                                currentItem = i;
                            }
                        }
                        list_ids.add(list.getId());
                        ++i;
                    }
                }
                builder.setSingleChoiceItems(
                    items.toArray(new CharSequence[items.size()]), currentItem,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                                        final int item) {
                        final Long lid = list_ids.get(item);
                        if (lid == null) {
                            mItem.setDefaultList(Optional.<ListMirakel>absent());
                        } else {
                            mItem.setDefaultList(ListMirakel.get(lid));
                        }
                        mItem.save();
                        setDefaultListSummary(defList, mItem);
                        alert.dismiss();
                        mAdapter.notifyDataSetChanged();
                    }
                });
                alert = builder.create();
                alert.show();
                return true;
            }
        });
        return defList;
    }

    private Preference getWhereStringPreference() {
        final Preference where = new Preference(getActivity());
        Map<Uri, MirakelContentObserver.ObserverCallBack> doOnChange = new HashMap<>();
        doOnChange.put(SpecialList.URI, new MirakelContentObserver.ObserverCallBack() {
            @Override
            public void handleChange() {
                Optional<SpecialList> changed = SpecialList.getSpecial(mItem.getId());
                if (changed.isPresent() && !changed.get().equals(mItem)) {
                    mItem = changed.get();
                    where.setSummary(mItem.getWhereQueryForTasks().select("*").getQuery(SpecialList.URI));
                }
            }

            @Override
            public void handleChange(long id) {
                handleChange();
            }
        });
        observer = new MirakelContentObserver(new Handler(Looper.getMainLooper()), getActivity(),
                                              doOnChange);
        where.setTitle(R.string.special_list_where);
        where.setSummary(mItem.getWhereQueryForTasks().select("*").getQuery(Task.URI));
        where.setEnabled(false);
        return where;
    }

    private CheckBoxPreference getIsActivePreference() {
        final CheckBoxPreference active = new CheckBoxPreference(getActivity());
        active.setTitle(R.string.special_list_active);
        active.setChecked(mItem.isActive());
        active.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean a = !active.isChecked();
                mItem.setActive(a);
                mItem.save();
                active.setChecked(mItem.isActive());
                mAdapter.notifyDataSetChanged();
                return true;
            }
        });
        return active;
    }

    private EditTextPreference getNamePreference() {
        final EditTextPreference name = new EditTextPreference(getActivity());
        name.setTitle(R.string.special_list_name);
        name.setSummary(mItem.getName());
        name.setText(mItem.getName());
        name.setSummary(mItem.getName());
        name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final EditText input = new EditText(getActivity());
                input.setSingleLine(true);
                input.setText(mItem.getName());
                new AlertDialog.Builder(getActivity())
                .setTitle(R.string.special_list_name)
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mItem.setName(input.getText().toString());
                        mItem.save();
                        name.setSummary(mItem.getName());
                        mAdapter.notifyDataSetChanged();
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
                return true;
            }
        });
        return name;
    }

    private static void setDefaultListSummary(final @NonNull Preference defList,
            final @NonNull SpecialList list) {
        String summaryString = "";
        if (list.getDefaultList() != null) {
            summaryString = list.getDefaultList().getName();
        }
        defList.setSummary(summaryString);
    }

    private static void setDefaultDateSummary(final @NonNull Preference defDate,
            final @NonNull Context ctx, final @NonNull SpecialList list) {
        final int[] values = ctx.getResources().getIntArray(
                                 R.array.special_list_def_date_picker_val);
        for (int j = 0; j < values.length; j++) {
            if (list.getDefaultDate() == null) {
                defDate.setSummary(ctx.getResources().getStringArray(
                                       R.array.special_list_def_date_picker)[0]);
            } else if (values[j] == list.getDefaultDate()) {
                defDate.setSummary(ctx.getResources().getStringArray(
                                       R.array.special_list_def_date_picker)[j]);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (observer != null) {
            getActivity().getContentResolver().unregisterContentObserver(observer);
        }
    }

    @Override
    public void onEditFinish(@NonNull SpecialList list) {
        backStack.clear();
        mAdapter.setNewList(list, backStack);
    }

    @Override
    @NonNull
    public SpecialList getItem() {
        return mItem;
    }
}
