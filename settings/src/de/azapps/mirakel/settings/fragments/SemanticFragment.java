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

package de.azapps.mirakel.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Optional;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.helper.AnalyticsWrapperBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.adapter.ExpandableSettingsAdapter;
import de.azapps.mirakel.settings.custom_views.ExpandablePreference;
import de.azapps.mirakel.settings.custom_views.Settings;
import de.azapps.mirakel.settings.custom_views.SwipeLinearLayout;
import de.azapps.tools.OptionalUtils;
import de.azapps.widgets.DueDialog;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;


public class SemanticFragment extends MirakelPreferencesFragment<Settings> implements
    SwipeLinearLayout.OnItemRemoveListener {

    private static final String NULL_STR = "null";
    private Context mContext;
    private int dueDialogValue;
    private final List<ExpandablePreference> prefs = new ArrayList<>();
    private List<Semantic> semantics = new ArrayList<>();
    @Nullable
    private Semantic deletedSemantic;
    private PreferenceScreen mScreen;
    private boolean hasDivider = false;
    @NonNull
    private Optional<Pair<Integer, Snackbar>> mSnackBar = absent();


    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (mContext == null) {
            mContext = activity;
        }
        semantics = Semantic.all();
    }

    @NonNull
    @Override
    public Settings getItem() {
        return Settings.TASK_TEMPLATES;
    }

    @Override
    protected void setAdapter(final PreferenceScreen preferenceScreen) {
        if (recyclerView != null) {
            final ExpandableSettingsAdapter mAdapter = new ExpandableSettingsAdapter(preferenceScreen);
            mAdapter.setRemoveListener(getRemoveListener());
            if (!hasDivider) {
                recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), null));
                hasDivider = true;
            }
            recyclerView.swapAdapter(mAdapter, false);
        }
    }

    @Override
    public void onDestroy() {
        if (deletedSemantic != null) {
            deletedSemantic.destroy();
            deletedSemantic = null;
        }
        super.onDestroy();
    }

    private void setupItemPreferences(final ExpandablePreference p, final Semantic item) {
        final PreferenceCategory general = new PreferenceCategory(mContext);
        general.setTitle(R.string.general);
        p.addChild(general);

        final EditTextPreference semanticsCondition = new EditTextPreference(mContext);
        semanticsCondition.setKey("semantics_condition" + item.getId());
        semanticsCondition.setDialogTitle(R.string.settings_semantics_condition);
        semanticsCondition.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                item.setCondition((String) newValue);
                semanticsCondition.setSummary((String)newValue);
                semanticsCondition.setText((String)newValue);
                item.save();
                p.setTitle(item.getName());
                return true;
            }
        });
        semanticsCondition.setText(item.getCondition());
        semanticsCondition.setSummary(item.getCondition());
        semanticsCondition.setTitle(R.string.settings_semantics_condition);
        general.addPreference(semanticsCondition);



        final PreferenceCategory conditions = new PreferenceCategory(mContext);
        conditions.setTitle(R.string.settings_semantics_actions);
        p.addChild(conditions);

        // Priority
        final ListPreference semanticsPriority = new ListPreference(mContext);
        semanticsPriority.setTitle(R.string.settings_semantics_priority);
        semanticsPriority.setDialogTitle(R.string.settings_semantics_priority);
        semanticsPriority.setKey("semantics_priority" + item.getId());
        semanticsPriority.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (NULL_STR.equals(newValue)) {
                    item.setPriority(Optional.<Integer>absent());
                    semanticsPriority.setValueIndex(0);
                    semanticsPriority.setSummary(semanticsPriority
                                                 .getEntries()[0]);
                } else {
                    item.setPriority(of(Integer.parseInt((String) newValue)));
                    semanticsPriority.setValue((String)newValue);
                    semanticsPriority.setSummary((String)newValue);
                }
                item.save();
                return true;
            }
        });
        semanticsPriority.setEntries(R.array.priority_entries);
        semanticsPriority.setEntryValues(R.array.priority_entry_values);
        if (!item.getPriority().isPresent()) {
            semanticsPriority.setValueIndex(0);
            semanticsPriority.setSummary(getResources()
                                         .getStringArray(R.array.priority_entries)[0]);
        } else {
            semanticsPriority.setValue(item.getPriority().get()
                                       .toString());
            semanticsPriority
            .setSummary(semanticsPriority.getValue());
        }
        conditions.addPreference(semanticsPriority);

        // Due
        final Preference semanticsDue = new Preference(mContext);
        semanticsDue.setTitle(R.string.settings_semantics_due);
        semanticsDue.setKey("semantics_due" + item.getId());
        semanticsDue.setSummary(updateDueStuff(item));
        semanticsDue
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final DueDialog dueDialog = new DueDialog(
                    mContext, false);
                dueDialog.setTitle(semanticsDue.getTitle());
                dueDialog.setValue(dueDialogValue);
                dueDialog.setNegativeButton(android.R.string.cancel,
                                            null);
                dueDialog.setNeutralButton(R.string.no_date,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        item.setDue(Optional.<Integer>absent());
                        semanticsDue.setSummary(updateDueStuff(item));
                        item.save();
                    }
                });
                dueDialog.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        final int val = dueDialog.getValue();
                        final DueDialog.VALUE dayYear = dueDialog
                                                        .getDayYear();
                        switch (dayYear) {
                        case DAY:
                            item.setDue(of(val));
                            break;
                        case MONTH:
                            item.setDue(of(val * 30));
                            break;
                        case YEAR:
                            item.setDue(of(val * 365));
                            break;
                        case HOUR:
                        case MINUTE:
                        default:
                            // The other things aren't shown in
                            // the dialog so we haven't to care
                            // about them
                            break;
                        }
                        semanticsDue.setSummary(updateDueStuff(item));
                        item.save();
                    }
                });
                dueDialog.show();
                return false;
            }
        });
        conditions.addPreference(semanticsDue);

        // Weekday
        final Optional<Integer> weekday = item.getWeekday();
        final ListPreference semanticsWeekday = new ListPreference(mContext);
        semanticsWeekday.setTitle(R.string.settings_semantics_weekday);
        semanticsWeekday.setDialogTitle(R.string.settings_semantics_weekday);
        semanticsWeekday.setKey("semantics_weekday" + item.getId());
        semanticsWeekday.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Integer weekday = Integer.parseInt((String) newValue);
                if (weekday == 0) {
                    weekday = null;
                }
                item.setWeekday(fromNullable(weekday));
                semanticsWeekday.setValue((String)newValue);
                semanticsWeekday.setSummary(semanticsWeekday.getEntry());
                item.save();
                return true;
            }
        });

        semanticsWeekday.setEntries(R.array.weekdays);
        final CharSequence[] weekdaysNum = {"0", "1", "2", "3", "4", "5", "6",
                                            "7"
                                           };
        semanticsWeekday.setEntryValues(weekdaysNum);
        if (weekday.isPresent()) {
            semanticsWeekday.setValueIndex(weekday.get());
        } else {
            semanticsWeekday.setValueIndex(0);
        }
        semanticsWeekday.setSummary(semanticsWeekday.getEntry());
        conditions.addPreference(semanticsWeekday);


        // List
        final ListPreference semanticsList = new ListPreference(mContext);
        semanticsList.setTitle(R.string.settings_semantics_list);
        semanticsList.setDialogTitle(R.string.settings_semantics_list);
        semanticsList.setKey("semantics_list" + item.getId());
        semanticsList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                if (NULL_STR.equals(newValue)) {
                    item.setList(Optional.<ListMirakel>absent());
                    semanticsList.setValueIndex(0);
                    semanticsList.setSummary(semanticsList.getEntries()[0]);
                } else {
                    final Optional<ListMirakel> newList = ListMirakel.get(Integer.parseInt((String) newValue));
                    item.setList(newList);
                    semanticsList.setValue((String)newValue);
                    OptionalUtils.withOptional(newList, new OptionalUtils.Procedure<ListMirakel>() {
                        @Override
                        public void apply(final ListMirakel input) {
                            semanticsList.setSummary(input.getName());
                        }
                    });
                }
                item.save();
                return true;
            }
        });
        final List<ListMirakel> lists = ListMirakel.all(false);
        final CharSequence[] listEntries = new CharSequence[lists.size() + 1];
        final CharSequence[] listValues = new CharSequence[lists.size() + 1];
        listEntries[0] = getString(R.string.semantics_no_list);
        listValues[0] = NULL_STR;
        for (int i = 0; i < lists.size(); i++) {
            listValues[i + 1] = String.valueOf(lists.get(i).getId());
            listEntries[i + 1] = lists.get(i).getName();
        }
        semanticsList.setEntries(listEntries);
        semanticsList.setEntryValues(listValues);
        if (!item.getList().isPresent()) {
            semanticsList.setValueIndex(0);
            semanticsList.setSummary(getString(R.string.semantics_no_list));
        } else {
            final ListMirakel listMirakel = item.getList().get();
            semanticsList.setValue(String.valueOf(listMirakel
                                                  .getId()));
            semanticsList.setSummary(listMirakel.getName());
        }
        conditions.addPreference(semanticsList);
    }

    protected String updateDueStuff(final @NonNull Semantic item) {
        final Optional<Integer> dueOptional = item.getDue();
        final String summary;
        if (!dueOptional.isPresent()) {
            this.dueDialogValue = 0;
            summary = getString(R.string.semantics_no_due);
        } else {
            final Integer due = dueOptional.get();
            if (((due % 365) == 0) && (due != 0)) {
                this.dueDialogValue = due / 365;
                summary = this.dueDialogValue
                          + " "
                          + getResources().getQuantityString(
                              R.plurals.due_year, this.dueDialogValue);
            } else if (((due % 30) == 0) && (due != 0)) {
                this.dueDialogValue = due / 30;
                summary = this.dueDialogValue
                          + " "
                          + getResources().getQuantityString(
                              R.plurals.due_month, this.dueDialogValue);
            } else {
                this.dueDialogValue = due;
                summary = this.dueDialogValue
                          + " "
                          + getResources().getQuantityString(
                              R.plurals.due_day, this.dueDialogValue);
            }
        }
        return summary;
    }
    @NonNull
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View v = super.onCreateView(inflater, container, savedInstanceState);
        recyclerView.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(
                                           android.R.drawable.divider_horizontal_dark)));
        return v;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_about);
        mScreen = getPreferenceScreen();
        mScreen.removeAll();
        for (final Semantic s : semantics) {
            final ExpandablePreference p = new ExpandablePreference(mContext, mScreen);
            p.setKey(SwipeLinearLayout.SWIPEABLE_VIEW + s.getName() + s.getId());
            mScreen.addPreference(p);
            p.setTitle(s.getName());
            setupItemPreferences(p, s);
            prefs.add(p);
        }
        updateScreen(mScreen);
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsWrapperBase.setScreen(this);
    }


    @Override
    protected boolean hasMenu() {
        return true;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.semantic_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.action_create_semantic) {
            final Semantic semantic = Semantic.newSemantic(getString(R.string.semantic_new), null,
                                      null, Optional.<ListMirakel>absent(), null);
            AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.CREATED_SEMANTIC);
            semantics.add(semantic);
            final ExpandablePreference p = new ExpandablePreference(mContext, mScreen);
            p.setKey(SwipeLinearLayout.SWIPEABLE_VIEW + semantic.getName() + semantic.getId());
            p.setTitle(semantic.getName());
            mScreen.addPreference(p);
            setAdapter(mScreen);
            p.setExpanded(true);
            setupItemPreferences(p, semantic);
            recyclerView.smoothScrollToPosition(prefs.size());
            return true;
        } else {
            return false;
        }
    }

    @Override
    @Nullable
    protected SwipeLinearLayout.OnItemRemoveListener getRemoveListener() {
        return this;
    }

    @Override
    public void onRemove(final int position, final int index) {
        if (position < semantics.size()) {
            if (mSnackBar.isPresent() && (deletedSemantic != null)) {
                removeSemantic(mSnackBar.get().first);
            }
            deletedSemantic = semantics.remove(position);
            final long deleteSemanticId = deletedSemantic.getId();
            mSnackBar = of(new Pair<>(position, Snackbar.with(getActivity())
                                      .text(getActivity().getString(R.string.delete_semantic))
                                      .actionLabel(R.string.undo)
                                      .eventListener(new SnackbarEventListener(deleteSemanticId, position))
            .actionListener(new ActionClickListener() {
                @Override
                public void onActionClicked(final Snackbar snackbar) {
                    semantics.add(position, deletedSemantic);
                    deletedSemantic = null;
                    updateScreen(mScreen);
                }
            })));
            SnackbarManager.show(mSnackBar.get().second);
        }

    }

    private void removeSemantic(final int position) {
        final Preference p = mScreen.getPreference(position);
        mScreen.removePreference(p);
        if (deletedSemantic != null) {
            deletedSemantic.destroy();
            deletedSemantic = null;
        }
    }

    private class SnackbarEventListener implements EventListener {
        private final long deleteSemanticId;
        private final int position;

        public SnackbarEventListener(final long deleteSemanticId, final int position) {
            this.deleteSemanticId = deleteSemanticId;
            this.position = position;
        }

        @Override
        public void onShow(final Snackbar snackbar) {

        }

        @Override
        public void onShowByReplace(final Snackbar snackbar) {

        }

        @Override
        public void onShown(final Snackbar snackbar) {

        }

        @Override
        public void onDismiss(final Snackbar snackbar) {

        }

        @Override
        public void onDismissByReplace(final Snackbar snackbar) {

        }

        @Override
        public void onDismissed(final Snackbar snackbar) {
            if ((deletedSemantic != null) && (deleteSemanticId == deletedSemantic.getId())) {
                removeSemantic(position);
                updateScreen(mScreen);
            }
            mSnackBar = absent();
        }
    }
}
