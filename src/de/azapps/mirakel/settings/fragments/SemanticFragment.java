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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.common.base.Optional;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.custom_views.ExpandablePreference;
import de.azapps.mirakel.settings.custom_views.Settings;
import de.azapps.mirakel.settings.custom_views.SwipeLinearLayout;
import de.azapps.tools.OptionalUtils;
import de.azapps.widgets.DueDialog;


public class SemanticFragment extends MirakelPreferencesFragment<Settings> implements
    SwipeLinearLayout.OnItemRemoveListener {

    private static final String NULL_STR = "null";
    private Context mContext;
    private int dueDialogValue;
    private final List<ExpandablePreference> prefs = new ArrayList<>();
    private List<Semantic> semantics = new ArrayList<>();
    private boolean undo = false;


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
    public PreferenceScreen getPreferenceScreen() {
        prefs.clear();
        final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(mContext);
        for (final Semantic s : semantics) {
            final ExpandablePreference p = new ExpandablePreference(mContext, screen);
            p.setKey(SwipeLinearLayout.SWIPEABLE_VIEW + s.getName() + s.getId());
            screen.addPreference(p);
            p.setTitle(s.getName());
            setupItemPreferences(p, s);
            prefs.add(p);
        }
        return screen;
    }

    private void setupItemPreferences(final ExpandablePreference p, final Semantic item) {
        final PreferenceCategory general = new PreferenceCategory(mContext);
        general.setTitle(R.string.general);
        p.addItemFromInflater(general);

        final EditTextPreference semanticsCondition = new EditTextPreference(mContext);
        semanticsCondition.setKey("semantics_condition" + item.getId());
        semanticsCondition.setDialogTitle(R.string.settings_semantics_condition);
        semanticsCondition.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
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
        general.addItemFromInflater(semanticsCondition);



        final PreferenceCategory conditions = new PreferenceCategory(mContext);
        conditions.setTitle(R.string.settings_semantics_actions);
        p.addItemFromInflater(conditions);

        // Priority
        final ListPreference semanticsPriority = new ListPreference(mContext);
        semanticsPriority.setTitle(R.string.settings_semantics_priority);
        semanticsPriority.setDialogTitle(R.string.settings_semantics_priority);
        semanticsPriority.setKey("semantics_priority" + item.getId());
        semanticsPriority.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (NULL_STR.equals(newValue)) {
                    item.setPriority(null);
                    semanticsPriority.setValueIndex(0);
                    semanticsPriority.setSummary(semanticsPriority
                                                 .getEntries()[0]);
                } else {
                    item.setPriority(Integer.parseInt((String) newValue));
                    semanticsPriority.setValue((String)newValue);
                    semanticsPriority.setSummary((String)newValue);
                }
                item.save();
                return true;
            }
        });
        semanticsPriority.setEntries(R.array.priority_entries);
        semanticsPriority.setEntryValues(R.array.priority_entry_values);
        if (item.getPriority() == null) {
            semanticsPriority.setValueIndex(0);
            semanticsPriority.setSummary(getResources()
                                         .getStringArray(R.array.priority_entries)[0]);
        } else {
            semanticsPriority.setValue(item.getPriority()
                                       .toString());
            semanticsPriority
            .setSummary(semanticsPriority.getValue());
        }
        conditions.addItemFromInflater(semanticsPriority);

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
                        item.setDue(null);
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
                            item.setDue(val);
                            break;
                        case MONTH:
                            item.setDue(val * 30);
                            break;
                        case YEAR:
                            item.setDue(val * 365);
                            break;
                        case HOUR:
                        case MINUTE:
                        default:
                            // The other things aren't shown in
                            // the dialog so we haven't to care
                            // about them
                            break;
                        }
                        semanticsDue
                        .setSummary(updateDueStuff(item));
                        item.save();
                    }
                });
                dueDialog.show();
                return false;
            }
        });
        conditions.addItemFromInflater(semanticsDue);

        // Weekday
        final Integer weekday = item.getWeekday();
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
                item.setWeekday(weekday);
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
        if (weekday == null) {
            semanticsWeekday.setValueIndex(0);
        } else {
            semanticsWeekday.setValueIndex(weekday);
        }
        semanticsWeekday.setSummary(semanticsWeekday.getEntry());
        conditions.addItemFromInflater(semanticsWeekday);


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
        conditions.addItemFromInflater(semanticsList);
    }

    protected String updateDueStuff(final @NonNull Semantic item) {
        final Integer due = item.getDue();
        final String summary;
        if (due == null) {
            this.dueDialogValue = 0;
            summary = getString(R.string.semantics_no_due);
        } else if (((due % 365) == 0) && (due != 0)) {
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
        return summary;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.semantic_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.action_create_semantic) {
            semantics.add(Semantic.newSemantic(getString(R.string.semantic_new), null,
                                               null, Optional.<ListMirakel>absent(), null));
            mAdapter.updateScreen(getPreferenceScreen());
            prefs.get(prefs.size() - 1).setExanded(true);
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
            final Semantic s = semantics.remove(position);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final PreferenceScreen screen = getPreferenceScreen();
                    getActivity().getWindow().getDecorView().post(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.updateScreen(screen);
                        }
                    });
                }
            }).start();
            SnackbarManager.show(
                Snackbar.with(getActivity())
                .text(getActivity().getString(R.string.delete_semantic))
                .actionLabel(R.string.undo)
            .eventListener(new EventListener() {
                @Override
                public void onShow(final Snackbar snackbar) {
                    undo = false;
                }

                @Override
                public void onShown(final Snackbar snackbar) {

                }

                @Override
                public void onDismiss(final Snackbar snackbar) {

                }

                @Override
                public void onDismissed(final Snackbar snackbar) {
                    if (!undo) {
                        s.destroy();
                        mAdapter.updateScreen(getPreferenceScreen());
                    }
                }
            })
            .actionListener(new ActionClickListener() {
                @Override
                public void onActionClicked(final Snackbar snackbar) {
                    semantics.add(position, s);
                    undo = true;
                    mAdapter.updateScreen(getPreferenceScreen());
                }
            }));
        }

    }
}
