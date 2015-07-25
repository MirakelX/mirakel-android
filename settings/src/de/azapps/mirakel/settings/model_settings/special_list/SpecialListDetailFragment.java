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

package de.azapps.mirakel.settings.model_settings.special_list;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import com.google.common.base.Optional;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.azapps.material_elements.utils.AnimationHelper;
import de.azapps.material_elements.utils.SnackBarEventListener;
import de.azapps.mirakel.helper.AnalyticsWrapperBase;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsConjunctionList;
import de.azapps.mirakel.model.list.meta.SpecialListsDoneProperty;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.custom_views.SwipeLinearLayout;
import de.azapps.mirakel.settings.custom_views.SwitchCompatPreference;
import de.azapps.mirakel.settings.fragments.MirakelPreferencesFragment;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelDetailFragment;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.EditDialogFragment;

import static com.google.common.base.Optional.of;

public class  SpecialListDetailFragment extends MirakelPreferencesFragment<SpecialList> implements
    CompoundButton.OnCheckedChangeListener, EditDialogFragment.OnPropertyEditListener,
    SwipeLinearLayout.OnItemRemoveListener {


    protected SpecialList mItem;

    private Context mContext = null;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SpecialListDetailFragment() {
    }


    @Override
    protected boolean isFabVisible() {
        return true;
    }

    @Override
    protected boolean hasMenu() {
        return !MirakelCommonPreferences.isTablet();
    }

    @Override
    protected void configureFab(final FloatingActionButton fab) {
        super.configureFab(fab);
        fab.setImageResource(R.drawable.ic_plus_white_24dp);
    }

    @Override
    protected void onFABClicked() {
        final SpecialListsDoneProperty property = new SpecialListsDoneProperty(false);
        final Optional<SpecialListsBaseProperty> where = mItem.getWhere();

        final SpecialListsConjunctionList specialListsBaseProperty;
        if (!where.isPresent()) {
            specialListsBaseProperty = new SpecialListsConjunctionList(property,
                    SpecialListsConjunctionList.CONJUNCTION.AND);
        } else if (where.get() instanceof SpecialListsConjunctionList) {
            specialListsBaseProperty = (SpecialListsConjunctionList) where.get();
            specialListsBaseProperty.getChilds().add(property);
        } else {
            specialListsBaseProperty = new SpecialListsConjunctionList(where.get(),
                    SpecialListsConjunctionList.CONJUNCTION.AND);
            specialListsBaseProperty.getChilds().add(property);
        }
        mItem.setWhere(Optional.<SpecialListsBaseProperty>of(specialListsBaseProperty));
        mItem.save();
        updateScreen(getPreferenceScreen());
        final ArrayList<Integer> backStack = new ArrayList<>();
        backStack.add(specialListsBaseProperty.getChilds().size() - 1);
        EditDialogFragment.newInstance(mItem, property, backStack,
                                       SpecialListDetailFragment.this, specialListsBaseProperty).show(((AppCompatActivity)
                                               mContext).getSupportFragmentManager(), "editdialog");
    }


    @Override
    @Nullable
    protected SwipeLinearLayout.OnItemRemoveListener getRemoveListener() {
        return this;
    }


    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        mItem.setActive(isChecked);
    }

    @Override
    public PreferenceScreen getPreferenceScreen() {
        final PreferenceScreen pf = getPreferenceManager().createPreferenceScreen(mContext);
        return getPrefernces(pf);
    }

    public void onCreate(Bundle savedInstanceState) {
        if (getArguments().containsKey(GenericModelDetailFragment.ARG_ITEM)) {
            mItem = getArguments().getParcelable(GenericModelDetailFragment.ARG_ITEM);
        } else {
            // Load the dummy content
            mItem = SpecialList.firstSpecialSafe();
        }
        AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.CREATED_META_LIST);
        super.onCreate(savedInstanceState);
    }


    @NonNull
    private PreferenceScreen getPrefernces(final @NonNull PreferenceScreen pf) {

        final PreferenceCategory summary = new PreferenceCategory(mContext);
        summary.setTitle(R.string.special_list_summary);
        pf.addItemFromInflater(summary);

        final EditTextPreference name = getNamePreference();
        summary.addItemFromInflater(name);

        final SwitchPreference active = getIsActivePreference();
        summary.addItemFromInflater(active);


        if (MirakelCommonPreferences.isEnabledDebugMenu()) {
            final Preference where = getWhereStringPreference();
            summary.addItemFromInflater(where);
        }

        final PreferenceCategory defaultValues = new PreferenceCategory(mContext);
        defaultValues.setTitle(R.string.special_lists_defaults);
        pf.addItemFromInflater(defaultValues);


        final Preference defList = getDefaultListPreference();
        defaultValues.addItemFromInflater(defList);

        final Preference defDate = getDefaultDatePreference();
        defaultValues.addItemFromInflater(defDate);

        final PreferenceCategory conditions = new PreferenceCategory(mContext);
        conditions.setTitle(R.string.special_lists_condition_title);
        pf.addItemFromInflater(conditions);
        if (mItem.getWhere().isPresent()) {
            setupConditions(conditions, mItem.getWhere().get(), true, new ArrayList<Integer>());
        }
        return pf;
    }

    private void setupConditions(final @NonNull PreferenceGroup conditions,
                                 final @NonNull SpecialListsBaseProperty specialListsBaseProperty, final boolean first,
                                 final @NonNull ArrayList<Integer> backStack) {
        if (first && (specialListsBaseProperty instanceof SpecialListsConjunctionList)) {
            for (int i = 0; i < ((SpecialListsConjunctionList) specialListsBaseProperty).getChilds().size();
                 i++) {
                final ArrayList<Integer> localBackstack = new ArrayList<>(backStack);
                localBackstack.add(i);
                setupConditions(conditions, ((SpecialListsConjunctionList)
                                             specialListsBaseProperty).getChilds().get(i),
                                false, localBackstack);
            }
        } else {
            final Preference p = new Preference(mContext);
            p.setTitle(specialListsBaseProperty.getTitle(mContext));
            p.setSummary(specialListsBaseProperty.getSummary(mContext));
            p.setKey(SwipeLinearLayout.SWIPEABLE_VIEW + String.valueOf(UUID.randomUUID()));
            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final SpecialListsConjunctionList base;
                    final Optional<SpecialListsBaseProperty> where = mItem.getWhere();
                    if (!where.isPresent()) {
                        base = new SpecialListsConjunctionList(SpecialListsConjunctionList.CONJUNCTION.AND,
                                                               new ArrayList<SpecialListsBaseProperty>(0));
                    } else if (where.get() instanceof SpecialListsConjunctionList) {
                        base = (SpecialListsConjunctionList) where.get();
                    } else {
                        base = new SpecialListsConjunctionList(where.get(), SpecialListsConjunctionList.CONJUNCTION.AND);
                    }

                    EditDialogFragment.newInstance(mItem, specialListsBaseProperty, backStack,
                                                   SpecialListDetailFragment.this, base).show(((AppCompatActivity)
                                                           getActivity()).getSupportFragmentManager(), "editdialog");
                    return true;
                }
            });
            conditions.addItemFromInflater(p);
        }
    }

    @Override
    protected void handleDelete() {
        mItem.destroy();
    }

    private Preference getDefaultDatePreference() {
        final ListPreference defDate = new ListPreference(mContext);
        defDate.setKey("special_lists_def_date");
        defDate.setTitle(R.string.special_list_def_date);
        setDefaultDateSummary(defDate, mContext, mItem);
        defDate.setEntries(R.array.special_list_def_date_picker);
        final int [] values = getResources().getIntArray(R.array.special_list_def_date_picker_val);
        final String [] valueStrings = new String[values.length];
        int currentItem = 0;
        final Integer ddate = mItem.getDefaultDate().or(SpecialList.NULL_DATE_VALUE);
        for (int i = 0; i < values.length; i++) {
            valueStrings[i] = String.valueOf(values[i]);
            if (((Integer) values[i]).equals(ddate)) {
                currentItem = i;
            }
        }
        defDate.setEntryValues(valueStrings);
        defDate.setValueIndex(currentItem);
        defDate.setDialogTitle(R.string.special_list_def_date);
        defDate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                Optional<Integer> newDate = of(Integer.valueOf((String) newValue));
                if (newDate.get() == SpecialList.NULL_DATE_VALUE) {
                    newDate = Optional.absent();
                }
                mItem.setDefaultDate(newDate);
                mItem.save();
                setDefaultDateSummary(defDate, mContext, mItem);
                return true;
            }
        });
        return defDate;
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (mContext == null) {
            mContext = activity;
        }
    }

    private Preference getDefaultListPreference() {
        final List<ListMirakel> lists = ListMirakel.all(false);
        final ListPreference defList = new ListPreference(mContext);
        defList.setKey("special_lists_def_list");
        defList.setDialogTitle(R.string.special_list_def_list);
        defList.setTitle(R.string.special_list_def_list);
        defList.setOnPreferenceChangeListener(null);
        setDefaultListSummary(defList, mItem);
        final List<CharSequence> items = new ArrayList<>(lists.size());
        final List<String> listIds = new ArrayList<>(lists.size());
        int current = 0;
        final ListMirakel dList = mItem.getDefaultList();
        for (final ListMirakel l : lists) {
            if (!l.isSpecial()) {
                items.add(l.getName());
                listIds.add(String.valueOf(l.getId()));
                if (dList.equals(l)) {
                    current = items.size() - 1;
                }
            }
        }
        defList.setEntryValues(listIds.toArray(new String[listIds.size()]));
        defList.setEntries(items.toArray(new CharSequence[items.size()]));
        defList.setValueIndex(current);
        defList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                mItem.setDefaultList(ListMirakel.get(Long.valueOf((String) newValue)));
                mItem.save();
                setDefaultListSummary(defList, mItem);
                return true;
            }
        });
        return defList;
    }

    private Preference getWhereStringPreference() {
        final Preference where = new Preference(mContext);
        where.setKey("special_lists_where");
        where.setTitle(R.string.special_list_where);
        where.setSummary(mItem.getWhereQueryForTasks().select("*").toString(Task.URI));
        where.setEnabled(false);
        return where;
    }

    private SwitchPreference getIsActivePreference() {
        final SwitchPreference active = new SwitchCompatPreference(getActivity());
        active.setKey("special_lists_active");
        active.setTitle(R.string.special_list_active);
        active.setChecked(mItem.isActive());
        active.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean a = !active.isChecked();
                mItem.setActive(a);
                mItem.save();
                return true;
            }
        });
        return active;
    }

    private EditTextPreference getNamePreference() {
        final EditTextPreference name = new EditTextPreference(getActivity());
        name.setKey("special_lists_name");
        name.setDialogTitle(R.string.special_list_name);
        name.setTitle(R.string.special_list_name);
        name.setSummary(mItem.getName());
        name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                mItem.setName((String) newValue);
                mItem.save();
                name.setSummary((String) newValue);
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
            if (!list.getDefaultDate().isPresent()) {
                defDate.setSummary(ctx.getResources().getStringArray(
                                       R.array.special_list_def_date_picker)[0]);
            } else if (values[j] == list.getDefaultDate().get()) {
                defDate.setSummary(ctx.getResources().getStringArray(
                                       R.array.special_list_def_date_picker)[j]);
            }
        }
    }

    @Override
    public void onEditFinish(@NonNull final SpecialList list) {
        mItem = list;
        updateScreen(getPreferenceScreen());
    }

    @Override
    @NonNull
    public SpecialList getItem() {
        return mItem;
    }

    @Override
    public void onRemove(final int position, final int index) {
        final Optional<SpecialListsBaseProperty> where = mItem.getWhere();
        if (where.isPresent()) {
            final ActionClickListener undo;
            if (where.get() instanceof SpecialListsConjunctionList) {
                final SpecialListsBaseProperty old = ((SpecialListsConjunctionList) where.get()).getChilds().remove(
                        index - 1);
                mItem.setWhere(where);
                mItem.save();
                undo = new ActionClickListener() {
                    @Override
                    public void onActionClicked(final Snackbar snackbar) {
                        ((SpecialListsConjunctionList) where.get()).getChilds().add(index - 1, old);
                        mItem.setWhere(where);
                        mItem.save();
                        updateScreen(getPreferenceScreen());
                    }
                };
            } else {
                final SpecialListsBaseProperty old = where.get();
                mItem.setWhere(Optional.<SpecialListsBaseProperty>absent());
                mItem.save();
                undo = new ActionClickListener() {
                    @Override
                    public void onActionClicked(final Snackbar snackbar) {
                        mItem.setWhere(of(old));
                        mItem.save();
                        updateScreen(getPreferenceScreen());
                    }
                };
            }
            SnackbarManager.show(
                Snackbar.with(getActivity())
                .text(getActivity().getString(R.string.delete_condition))
                .actionLabel(R.string.undo)
                .actionListener(undo)
                .eventListener(snackBarListener));
        }
    }

    private final LocalEventListener snackBarListener = new LocalEventListener();

    private class LocalEventListener extends SnackBarEventListener {
        @Override
        public void onShow(final Snackbar snackbar) {
            super.onShow(snackbar);
            if (((FrameLayout.LayoutParams)snackbar.getLayoutParams()).bottomMargin == 0) {
                AnimationHelper.moveViewUp(getActivity(), mFab, snackbar.getHeight());
            }
        }

        @Override
        public void onDismiss(final Snackbar snackbar) {
            super.onDismiss(snackbar);
            if (((FrameLayout.LayoutParams)snackbar.getLayoutParams()).bottomMargin == 0) {
                AnimationHelper.moveViewDown(getActivity(), mFab, snackbar.getHeight());
            }
        }
    }
}
