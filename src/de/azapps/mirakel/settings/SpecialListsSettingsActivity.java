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

package de.azapps.mirakel.settings;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.github.machinarius.preferencefragment.PreferenceFragment;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.azapps.mirakel.helper.ListDialogHelpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsContentProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDoneProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDueProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsFileProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsListProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsNameProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsPriorityProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsProgressProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsReminderProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsSetProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsStringProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsSubtaskProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsTagProperty;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.settings.generic_list.GenericListSettingActivity;
import de.azapps.mirakel.settings.generic_list.GenericSettingsFragment;
import de.azapps.widgets.DueDialog;

public class SpecialListsSettingsActivity extends GenericListSettingActivity<SpecialList> {

    enum SET_TYPE {
        LIST, PRIO, TAG
    }

    @Override
    protected void createModel() {
        SpecialList specialList = SpecialList.newSpecialList(
                                      getString(R.string.special_lists_new),
                                      new HashMap<String, SpecialListsBaseProperty>(), true);
        selectItem(specialList);
    }

    @NonNull
    @Override
    public String getTitle(Optional<SpecialList> model) {
        if (model.isPresent()) {
            return model.get().getName();
        } else {
            return getString(R.string.no_special_list_selected);
        }
    }

    @Override
    public int getPreferenceResource() {
        return R.xml.settings_special_list;
    }

    @Override
    public Uri getUri() {
        return MirakelInternalContentProvider.SPECIAL_LISTS_URI;
    }

    @Override
    public Class<SpecialList> getMyClass() {
        return SpecialList.class;
    }

    @Override
    public void setUp(Optional<SpecialList> model, final GenericSettingsFragment fragment) {
        if (!model.isPresent()) {
            return;
        }
        final SpecialList specialList = model.get();
        final EditTextPreference name = (EditTextPreference) fragment.findPreference("special_list_name");
        name.setText(specialList.getName());
        name.setSummary(specialList.getName());
        name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @SuppressLint("NewApi")
            @Override
            public boolean onPreferenceChange(final Preference preference,
                                              final Object newValue) {
                if (newValue != null && !((String) newValue).equals("")) {
                    specialList.setName(newValue
                                        .toString());
                    specialList.save();
                    name.setSummary(specialList
                                    .getName());
                    name.setText(specialList.getName());
                }
                return false;
            }
        });
        final CheckBoxPreference active = (CheckBoxPreference)
                                          fragment.findPreference("special_list_active");
        active.setChecked(specialList.isActive());
        active.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference,
                                              final Object newValue) {
                specialList
                .setActive((Boolean) newValue);
                specialList.save();
                return true;
            }
        });
        if (!MirakelCommonPreferences.isDebug()) {
            fragment.getPreferenceScreen().removePreference(
                fragment.findPreference("special_lists_where"));
        } else {
            fragment.findPreference("special_lists_where").setSummary(
                specialList.getWhereQueryForTasks().select("*").getQuery(Task.URI));
        }
        final List<ListMirakel> lists = ListMirakel.all(false);
        final Preference sortBy = fragment.findPreference("special_default_sort");
        sortBy.setOnPreferenceChangeListener(null);
        sortBy.setSummary(getResources().getStringArray(
                              R.array.task_sorting_items)[specialList.getSortBy().getShort()]);
        sortBy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                SpecialList sl = (SpecialList) ListDialogHelpers
                                 .handleSortBy(SpecialListsSettingsActivity.this,
                                               specialList, sortBy);
                return true;
            }
        });
        final Preference defList = fragment.findPreference("special_default_list");
        defList.setOnPreferenceChangeListener(null);
        String summary = "";
        if (specialList.getDefaultList() != null) {
            summary = specialList.getDefaultList().getName();
        }
        defList.setSummary(summary);
        defList.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                SpecialList sl = ListDialogHelpers
                                 .handleDefaultList(SpecialListsSettingsActivity.this,
                                                    specialList, lists,
                                                    defList);
                return true;
            }
        });
        final Preference defDate = fragment.findPreference("special_default_due");
        final int[] values = getResources().getIntArray(
                                 R.array.special_list_def_date_picker_val);
        for (int j = 0; j < values.length; j++) {
            if (specialList.getDefaultDate() == null) {
                defDate.setSummary(getResources().getStringArray(
                                       R.array.special_list_def_date_picker)[0]);
            } else if (values[j] == specialList.getDefaultDate()) {
                defDate.setSummary(getResources().getStringArray(
                                       R.array.special_list_def_date_picker)[j]);
            }
        }
        defDate.setOnPreferenceChangeListener(null);
        defDate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                SpecialList sl = ListDialogHelpers
                                 .handleDefaultDate(SpecialListsSettingsActivity.this,
                                                    specialList, defDate);
                return true;
            }
        });
        // Where
        final Preference taskName = fragment.findPreference("special_where_name");
        taskName.setSummary(getFieldText("name", specialList));
        final Preference list = fragment.findPreference("special_where_list");
        list.setSummary(getFieldText("list_id", specialList));
        final Preference done = fragment.findPreference("special_where_done");
        done.setSummary(getFieldText("done", specialList));
        final Preference content = fragment.findPreference("special_where_content");
        content.setSummary(getFieldText("content", specialList));
        final Preference prio = fragment.findPreference("special_where_prio");
        prio.setSummary(getFieldText("priority", specialList));
        final Preference due = fragment.findPreference("special_where_due");
        due.setSummary(getFieldText("due", specialList));
        final Preference reminder = fragment.findPreference("special_where_reminder");
        reminder.setSummary(getFieldText("reminder", specialList));
        final Preference progress = fragment.findPreference("special_where_progress");
        progress.setSummary(getFieldText(Task.PROGRESS, specialList));
        final Preference subtask = fragment.findPreference("special_where_subtask");
        subtask.setSummary(getFieldText(Task.SUBTASK_TABLE, specialList));
        final Preference file = fragment.findPreference("special_where_file");
        file.setSummary(getFieldText(FileMirakel.TABLE, specialList));
        final Preference tag = fragment.findPreference("special_where_tag");
        tag.setSummary(getFieldText(Tag.TABLE, specialList));
        done.setOnPreferenceChangeListener(null);
        done.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final CharSequence[] SortingItems = {
                    SpecialListsSettingsActivity.this
                    .getString(R.string.nothing),
                    SpecialListsSettingsActivity.this
                    .getString(R.string.done),
                    SpecialListsSettingsActivity.this
                    .getString(R.string.undone)
                };
                int defVal = 0;
                final SpecialListsDoneProperty prop = (SpecialListsDoneProperty)
                                                      specialList
                                                      .getWhere().get(Task.DONE);
                if (prop != null) {
                    if (!prop.getDone()) {
                        defVal = 2;
                    } else {
                        defVal = 1;
                    }
                }
                new AlertDialog.Builder(SpecialListsSettingsActivity.this)
                .setTitle(
                    SpecialListsSettingsActivity.this
                    .getString(R.string.select_by))
                .setSingleChoiceItems(SortingItems, defVal,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int item) {
                        if (prop == null) {
                            setNewWhere(
                                new SpecialListsDoneProperty(
                                    item == 1),
                                item == 1 || item == 2,
                                Task.DONE, done, specialList, fragment);
                        } else {
                            prop.setDone(item == 1);
                            setNewWhere(prop, item == 1
                                        || item == 2, Task.DONE,
                                        done, specialList, fragment);
                        }
                        dialog.dismiss(); // Ugly
                    }
                }).show();
                return true;
            }
        });
        reminder.setOnPreferenceChangeListener(null);
        reminder.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final CharSequence[] SortingItems = {
                    SpecialListsSettingsActivity.this
                    .getString(R.string.nothing),
                    SpecialListsSettingsActivity.this
                    .getString(R.string.reminder_set),
                    SpecialListsSettingsActivity.this
                    .getString(R.string.reminder_unset)
                };
                int defVal = 0;
                final SpecialListsReminderProperty prop = (SpecialListsReminderProperty)
                        specialList
                        .getWhere().get(Task.REMINDER);
                if (prop != null) {
                    if (!prop.isSet()) {
                        defVal = 2;
                    } else {
                        defVal = 1;
                    }
                }
                new AlertDialog.Builder(SpecialListsSettingsActivity.this)
                .setTitle(
                    SpecialListsSettingsActivity.this
                    .getString(R.string.select_by))
                .setSingleChoiceItems(SortingItems, defVal,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int item) {
                        if (prop == null) {
                            setNewWhere(
                                new SpecialListsReminderProperty(
                                    item == 1),
                                item == 1 || item == 2,
                                Task.REMINDER, reminder, specialList, fragment);
                        } else {
                            prop.setIsSet(item == 1);
                            setNewWhere(prop, item == 1
                                        || item == 2,
                                        Task.REMINDER, reminder, specialList, fragment);
                        }
                        dialog.dismiss(); // Ugly
                    }
                }).show();
                return false;
            }
        });
        list.setOnPreferenceChangeListener(null);
        list.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            private boolean[] mSelectedItems;
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final List<ListMirakel> lists = ListMirakel.all(true);
                int loc = 0;
                for (final ListMirakel list : lists) {
                    if (list.getId() == specialList
                        .getId()) {
                        lists.remove(loc);
                        break;
                    }
                    loc++;
                }
                final CharSequence[] SortingItems = new String[lists.size() + 1];
                final int[] values = new int[lists.size() + 1];
                this.mSelectedItems = new boolean[SortingItems.length];
                final SpecialListsListProperty prop = (SpecialListsListProperty)
                                                      specialList
                                                      .getWhere().get(Task.LIST_ID);
                values[0] = 0;
                this.mSelectedItems[0] = prop == null ? false : prop
                                         .isNegated();
                SortingItems[0] = SpecialListsSettingsActivity.this
                                  .getString(R.string.inverte);
                for (int i = 0; i < lists.size(); i++) {
                    values[i + 1] = (int)lists.get(i).getId();
                    SortingItems[i + 1] = lists.get(i).getName();
                    this.mSelectedItems[i + 1] = prop == null ? false : prop
                                                 .getContent().contains(lists.get(i).getId());
                }
                new AlertDialog.Builder(SpecialListsSettingsActivity.this)
                .setTitle(
                    SpecialListsSettingsActivity.this
                    .getString(R.string.select_by))
                .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int id) {
                        setSetProperty(list, values, prop,
                                       mSelectedItems, Task.LIST_ID,
                                       SET_TYPE.LIST, specialList, fragment);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setMultiChoiceItems(SortingItems, this.mSelectedItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which,
                        final boolean isChecked) {
                        mSelectedItems[which] = isChecked;
                    }
                }).show();
                return false;
            }
        });
        tag.setOnPreferenceChangeListener(null);
        tag.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            private boolean[] mSelectedItems;
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final List<Tag> tags = Tag.all();
                final CharSequence[] SortingItems = new String[tags.size() + 1];
                final int[] values = new int[tags.size() + 1];
                this.mSelectedItems = new boolean[SortingItems.length];
                final SpecialListsTagProperty prop = (SpecialListsTagProperty) specialList
                                                     .getWhere().get(Tag.TABLE);
                values[0] = 0;
                this.mSelectedItems[0] = prop == null ? false : prop
                                         .isNegated();
                SortingItems[0] = SpecialListsSettingsActivity.this
                                  .getString(R.string.inverte);
                for (int i = 0; i < tags.size(); i++) {
                    values[i + 1] = (int)tags.get(i).getId();
                    SortingItems[i + 1] = tags.get(i).getName();
                    this.mSelectedItems[i + 1] = prop == null ? false : prop
                                                 .getContent().contains(tags.get(i).getId());
                }
                new AlertDialog.Builder(SpecialListsSettingsActivity.this)
                .setTitle(
                    SpecialListsSettingsActivity.this
                    .getString(R.string.select_by))
                .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int id) {
                        setSetProperty(tag, values, prop,
                                       mSelectedItems, Tag.TABLE,
                                       SET_TYPE.TAG, specialList, fragment);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setMultiChoiceItems(SortingItems, this.mSelectedItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which,
                        final boolean isChecked) {
                        mSelectedItems[which] = isChecked;
                    }
                }).show();
                return false;
            }
        });
        prio.setOnPreferenceChangeListener(null);
        prio.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            private boolean[] mSelectedItems;
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final CharSequence[] SortingItems = new String[6];
                final int[] values = new int[SortingItems.length];
                this.mSelectedItems = new boolean[SortingItems.length];
                final SpecialListsPriorityProperty prop = (SpecialListsPriorityProperty)
                        specialList
                        .getWhere().get(Task.PRIORITY);
                values[0] = -5;
                this.mSelectedItems[0] = prop != null && prop
                                         .isNegated();
                SortingItems[0] = SpecialListsSettingsActivity.this
                                  .getString(R.string.inverte);
                for (int i = 1; i < SortingItems.length; i++) {
                    SortingItems[i] = i - 3 + "";
                    values[i] = i - 3;
                    this.mSelectedItems[i] = prop != null && prop
                                             .getContent().contains(i - 3);
                }
                new AlertDialog.Builder(SpecialListsSettingsActivity.this)
                .setTitle(
                    SpecialListsSettingsActivity.this
                    .getString(R.string.select_by))
                .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int id) {
                        setSetProperty(prio, values, prop,
                                       mSelectedItems, Task.PRIORITY,
                                       SET_TYPE.PRIO, specialList, fragment);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setMultiChoiceItems(SortingItems, this.mSelectedItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which,
                        final boolean isChecked) {
                        mSelectedItems[which] = isChecked;
                    }
                }).show();
                return false;
            }
        });
        content.setOnPreferenceChangeListener(null);
        content.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final SpecialListsStringProperty prop = (SpecialListsStringProperty)
                                                        specialList
                                                        .getWhere().get(Task.CONTENT);
                final View dialogView = getView(R.layout.content_name_dialog);
                final EditText search = (EditText) dialogView
                                        .findViewById(R.id.where_like);
                final CheckBox negated = (CheckBox) dialogView
                                         .findViewById(R.id.where_like_inverte);
                setUpStringProperty(prop, dialogView, search, negated);
                new AlertDialog.Builder(SpecialListsSettingsActivity.this)
                .setTitle(
                    SpecialListsSettingsActivity.this
                    .getString(R.string.select_by))
                .setNegativeButton(android.R.string.cancel, null)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        setStringProperty(content, prop,
                                          dialogView, search, negated,
                                          true, specialList, fragment);
                    }
                }).show();
                return false;
            }
        });
        taskName.setOnPreferenceChangeListener(null);
        taskName.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final SpecialListsStringProperty prop = (SpecialListsStringProperty)
                                                        specialList
                                                        .getWhere().get(ModelBase.NAME);
                final View dialogView = getView(R.layout.content_name_dialog);
                final EditText search = (EditText) dialogView
                                        .findViewById(R.id.where_like);
                final CheckBox negated = (CheckBox) dialogView
                                         .findViewById(R.id.where_like_inverte);
                setUpStringProperty(prop, dialogView, search, negated);
                new AlertDialog.Builder(SpecialListsSettingsActivity.this)
                .setTitle(
                    SpecialListsSettingsActivity.this
                    .getString(R.string.select_by))
                .setNegativeButton(android.R.string.cancel, null)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        setStringProperty(taskName, prop,
                                          dialogView, search, negated,
                                          false, specialList, fragment);
                    }
                }).show();
                return false;
            }
        });
        due.setOnPreferenceChangeListener(null);
        due.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final SpecialListsDueProperty prop = (SpecialListsDueProperty) specialList
                                                     .getWhere().get(Task.DUE);
                DueDialog.VALUE day = DueDialog.VALUE.DAY;
                if (prop != null) {
                    switch (prop.getUnit()) {
                    case DAY:
                        day = DueDialog.VALUE.DAY;
                        break;
                    case MONTH:
                        day = DueDialog.VALUE.MONTH;
                        break;
                    case YEAR:
                        day = DueDialog.VALUE.YEAR;
                        break;
                    default:
                        break;
                    }
                }
                final DueDialog dueDialog = new DueDialog(
                    SpecialListsSettingsActivity.this, false);
                dueDialog.setTitle(SpecialListsSettingsActivity.this
                                   .getString(R.string.select_by));
                dueDialog.setValue(prop == null ? 0 : prop.getLenght(), day);
                dueDialog.setNegativeButton(android.R.string.cancel, null);
                dueDialog.setNeutralButton(R.string.no_date,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                                        final int which) {
                        setNewWhere(prop, false, Task.DUE, due, specialList, fragment);
                    }
                });
                dueDialog.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                                        final int which) {
                        final int val = dueDialog.getValue();
                        SpecialListsDueProperty.Unit t;
                        switch (dueDialog.getDayYear()) {
                        case MONTH:
                            t = SpecialListsDueProperty.Unit.MONTH;
                            break;
                        case YEAR:
                            t = SpecialListsDueProperty.Unit.YEAR;
                            break;
                        case DAY:
                            t = SpecialListsDueProperty.Unit.DAY;
                            break;
                        default:
                            // The other things aren't
                            // shown in
                            // the dialog so we haven't to care
                            // about them
                            return;
                        }
                        if (prop == null) {
                            setNewWhere(new SpecialListsDueProperty(t,
                                                                    val), true, Task.DUE, due, specialList, fragment);
                        } else {
                            prop.setUnit(t);
                            prop.setLenght(val);
                            setNewWhere(prop, true, Task.DUE, due, specialList, fragment);
                        }
                    }
                });
                dueDialog.show();
                return false;
            }
        });
        if (Build.VERSION.SDK_INT >= 11) {
            progress.setOnPreferenceChangeListener(null);
            progress.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    final SpecialListsProgressProperty prop = (SpecialListsProgressProperty)
                            specialList
                            .getWhere().get(Task.PROGRESS);
                    final View v = SpecialListsSettingsActivity.this
                                   .getLayoutInflater().inflate(
                                       R.layout.progress_dialog, null);
                    final NumberPicker operationPicker = (NumberPicker) v
                                                         .findViewById(R.id.progress_op);
                    final NumberPicker valuePicker = (NumberPicker) v
                                                     .findViewById(R.id.progress_value);
                    final String[] operations = { ">=", "=", "<=" };
                    operationPicker.setMaxValue(2);
                    operationPicker.setMinValue(0);
                    operationPicker.setDisplayedValues(operations);
                    valuePicker.setMinValue(0);
                    valuePicker.setMaxValue(100);
                    valuePicker.setWrapSelectorWheel(false);
                    if (prop != null) {
                        operationPicker.setValue(prop.getOperation().ordinal());
                        valuePicker.setValue(prop.getValue());
                    } else {
                        operationPicker.setValue(0);// >=
                        valuePicker.setValue(50);
                    }
                    new AlertDialog.Builder(SpecialListsSettingsActivity.this)
                    .setView(v)
                    .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            final DialogInterface dialog,
                            final int which) {
                            if (prop == null) {
                                setNewWhere(
                                    new SpecialListsProgressProperty(
                                        valuePicker
                                        .getValue(),
                                        SpecialListsProgressProperty.OPERATION
                                        .values()[operationPicker
                                                  .getValue()]),
                                    true, Task.PROGRESS,
                                    progress, specialList, fragment);
                            } else {
                                prop.setValue(valuePicker
                                              .getValue());
                                prop.setOperation(SpecialListsProgressProperty.OPERATION
                                                  .values()[operationPicker
                                                            .getValue()]);
                                setNewWhere(prop, true,
                                            Task.PROGRESS, progress, specialList, fragment);
                            }
                        }
                    })
                    .setNegativeButton(R.string.remove,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            final DialogInterface dialog,
                            final int which) {
                            setNewWhere(null, false,
                                        Task.PROGRESS, progress, specialList, fragment);
                        }
                    })
                    .setTitle(
                        SpecialListsSettingsActivity.this
                        .getString(R.string.select_by))
                    .show();
                    return false;
                }
            });
        } else {
            // dont provide this on android 2.x
            fragment.removePreference("special_where_progress");
        }
        subtask.setOnPreferenceChangeListener(null);
        subtask.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final SpecialListsSubtaskProperty prop = (SpecialListsSubtaskProperty)
                        specialList
                        .getWhere().get(Task.SUBTASK_TABLE);
                final boolean[] checked = new boolean[2];
                if (prop != null) {
                    checked[0] = prop.isNegated();
                    checked[1] = prop.isParent();
                }
                new AlertDialog.Builder(SpecialListsSettingsActivity.this)
                .setTitle(R.string.select_by)
                .setMultiChoiceItems(
                    R.array.special_lists_subtask_options, checked,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which,
                        final boolean isChecked) {
                        checked[which] = isChecked;
                    }
                })
                .setNegativeButton(R.string.remove,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        setNewWhere(null, false,
                                    Task.SUBTASK_TABLE, subtask, specialList, fragment);
                    }
                })
                .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        if (prop == null) {
                            setNewWhere(
                                new SpecialListsSubtaskProperty(
                                    checked[0],
                                    checked[1]), true,
                                Task.SUBTASK_TABLE, subtask, specialList, fragment);
                        } else {
                            prop.setParent(checked[1]);
                            prop.setNegated(checked[0]);
                            setNewWhere(prop, true,
                                        Task.SUBTASK_TABLE, subtask, specialList, fragment);
                        }
                    }
                }).show();
                return false;
            }
        });
        file.setOnPreferenceChangeListener(null);
        file.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final SpecialListsFileProperty prop = (SpecialListsFileProperty)
                                                      specialList
                                                      .getWhere().get(FileMirakel.TABLE);
                int checked = 0;
                if (prop != null) {
                    if (prop.getDone()) {
                        checked = 1;
                    } else {
                        checked = 2;
                    }
                }
                new AlertDialog.Builder(SpecialListsSettingsActivity.this)
                .setTitle(R.string.select_by)
                .setSingleChoiceItems(R.array.file_choice, checked,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        if (which == 0) {
                            setNewWhere(null, false,
                                        FileMirakel.TABLE, file, specialList, fragment);
                        } else {
                            if (prop == null) {
                                setNewWhere(
                                    new SpecialListsFileProperty(
                                        which == 1),
                                    true,
                                    FileMirakel.TABLE, file, specialList, fragment);
                            } else {
                                prop.setDone(which == 1);
                                setNewWhere(prop, true,
                                            FileMirakel.TABLE, file, specialList, fragment);
                            }
                        }
                        dialog.dismiss();
                    }
                }).show();
                return false;
            }
        });
    }

    protected void setStringProperty(final Preference pref,
                                     final SpecialListsStringProperty prop, final View dialogView,
                                     final EditText search, final CheckBox negated,
                                     final boolean isContent, final SpecialList specialList, final PreferenceFragment fragment) {
        SpecialListsStringProperty.Type t = SpecialListsStringProperty.Type.CONTAINS;
        final int id = ((RadioGroup) dialogView
                        .findViewById(R.id.where_like_radio)).getCheckedRadioButtonId();
        if (id == R.id.where_like_begin) {
            t = SpecialListsStringProperty.Type.BEGIN;
        } else if (id == R.id.where_like_end) {
            t = SpecialListsStringProperty.Type.END;
        }
        final String searchString = search.getText().toString().trim();
        if (prop == null) {
            setNewWhere(
                isContent ? new SpecialListsContentProperty(
                    negated.isChecked(), searchString, t.ordinal())
                : new SpecialListsNameProperty(negated.isChecked(),
                                               searchString, t.ordinal()),
                searchString.length() != 0, isContent ? Task.CONTENT
                : ModelBase.NAME, pref, specialList, fragment);
        } else {
            prop.setType(t);
            prop.setSearchString(searchString);
            prop.setNegated(negated.isChecked());
            setNewWhere(prop, searchString.length() > 0,
                        isContent ? Task.CONTENT : ModelBase.NAME, pref, specialList, fragment);
        }
    }

    protected void setUpStringProperty(final SpecialListsStringProperty prop,
                                       final View dialogView, final EditText search, final CheckBox negated) {
        final RadioButton contain = (RadioButton) dialogView
                                    .findViewById(R.id.where_like_contain);
        contain.setChecked(prop == null || prop.getType() == SpecialListsStringProperty.Type.CONTAINS);
        contain.setText(getString(
                            R.string.where_like_contain_text, ""));
        final RadioButton begin = (RadioButton) dialogView
                                  .findViewById(R.id.where_like_begin);
        begin.setChecked(prop != null && prop.getType() == SpecialListsStringProperty.Type.BEGIN);
        begin.setText(getString(R.string.where_like_begin_text,
                                ""));
        final RadioButton end = (RadioButton) dialogView
                                .findViewById(R.id.where_like_end);
        end.setChecked(prop != null && prop.getType() == SpecialListsStringProperty.Type.END);
        end.setText(getString(R.string.where_like_end_text, ""));
        negated.setChecked(prop != null && prop.isNegated());
        search.setText(prop == null ? "" : prop.getSearchString());
    }

    protected void setSetProperty(final Preference list, final int[] values,
                                  SpecialListsSetProperty prop, final boolean[] mSelectedItems,
                                  final String key, final SET_TYPE setType, final  SpecialList specialList,
                                  final PreferenceFragment fragment) {
        final List<Integer> content = prop == null ? new ArrayList<Integer>()
                                      : prop.getContent();
        for (int i = 1; i < mSelectedItems.length; i++) {
            if (mSelectedItems[i] && !content.contains(values[i])) {
                content.add(values[i]);
            } else if (content.indexOf(values[i]) != -1) {
                // bad hack to remove element due to overloading template
                // function
                content.remove(content.indexOf(values[i]));
            }
        }
        if (prop == null) {
            switch (setType) {
            case LIST:
                prop = new SpecialListsListProperty(mSelectedItems[0], content);
                break;
            case PRIO:
                prop = new SpecialListsPriorityProperty(mSelectedItems[0],
                                                        content);
                break;
            case TAG:
                prop = new SpecialListsTagProperty(mSelectedItems[0], content);
                break;
            }
            setNewWhere(prop, !content.isEmpty() && content.size() > 0, key,
                        list, specialList, fragment);
        } else {
            prop.setNegated(mSelectedItems[0]);
            prop.setContent(content);
            setNewWhere(prop, !content.isEmpty() && content.size() > 0, key,
                        list, specialList, fragment);
        }
    }

    protected void setNewWhere(final SpecialListsBaseProperty prop,
                               final boolean add, final String key, final Preference pref, final SpecialList specialList,
                               final PreferenceFragment fragment) {
        final Map<String, SpecialListsBaseProperty> where = specialList
                .getWhere();
        where.remove(key);
        if (add) {
            where.put(key, prop);
        }
        specialList.setWhere(where);
        specialList.save();
        pref.setSummary(getFieldText(key, specialList));
        if (MirakelCommonPreferences.isDebug()) {
            fragment.findPreference("special_lists_where").setSummary(
                specialList.getWhereQueryForTasks().select("*").getQuery(Task.URI));
        }
    }

    protected String getFieldText(final String queryPart, SpecialList specialList) {
        final Map<String, SpecialListsBaseProperty> where = specialList
                .getWhere();
        if (where.containsKey(queryPart)) {
            final SpecialListsBaseProperty prop = where.get(queryPart);
            return prop.getSummary(this);
        }
        return getString(R.string.empty);
    }


    private View getView(final int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return getLayoutInflater().inflate(id, null);
        }
        return View.inflate(new ContextThemeWrapper(this,
                            R.style.Dialog), id, null);
    }

}
