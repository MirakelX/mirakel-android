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
package de.azapps.mirakel.settings.special_list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.helper.ListDialogHelpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.PreferencesHelper;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsContentProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDoneProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDueProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDueProperty.Unit;
import de.azapps.mirakel.model.list.meta.SpecialListsFileProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsListProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsNameProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsPriorityProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsProgressProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsProgressProperty.OPERATION;
import de.azapps.mirakel.model.list.meta.SpecialListsReminderProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsSetProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsStringProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsStringProperty.Type;
import de.azapps.mirakel.model.list.meta.SpecialListsSubtaskProperty;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakel.settings.R;
import de.azapps.widgets.DueDialog;
import de.azapps.widgets.DueDialog.VALUE;

@SuppressLint("NewApi")
public class SpecialListSettings extends PreferencesHelper {
	protected SpecialList specialList;

	public SpecialListSettings(final SpecialListsSettingsActivity p,
			final SpecialList specialList) {
		super(p);
		this.specialList = specialList;
	}

	@SuppressLint("NewApi")
	public SpecialListSettings(final SpecialListsSettingsFragment p,
			final SpecialList s) {
		super(p);
		this.specialList = s;
	}

	protected String getFieldText(final String queryPart) {
		final Map<String, SpecialListsBaseProperty> where = this.specialList
				.getWhere();
		if (where.containsKey(queryPart)) {
			final SpecialListsBaseProperty prop = where.get(queryPart);
			return prop.getSummary(this.activity);
		}
		return this.activity.getString(R.string.empty);
	}

	@SuppressLint("NewApi")
	private LayoutInflater getLayoutInflater() {
		if (this.v4_0) {
			return ((SpecialListsSettingsFragment) this.ctx).getActivity()
					.getLayoutInflater();
		}
		return ((SpecialListsSettingsActivity) this.ctx).getLayoutInflater();
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private PreferenceScreen getPreferenceScreen() {
		if (this.v4_0) {
			return ((SpecialListsSettingsFragment) this.ctx)
					.getPreferenceScreen();
		}
		return ((SpecialListsSettingsActivity) this.ctx).getPreferenceScreen();
	}

	private View getView(final int id) {
		if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
			return getLayoutInflater().inflate(id, null);
		}
		return View.inflate(new ContextThemeWrapper(this.activity,
				R.style.Dialog), id, null);
	}

	public void setup() throws NoSuchListException {
		if (this.specialList == null) {
			throw new NoSuchListException();
		}
		final EditTextPreference name = (EditTextPreference) findPreference("special_list_name");
		name.setText(this.specialList.getName());
		name.setSummary(this.specialList.getName());
		name.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@SuppressLint("NewApi")
			@Override
			public boolean onPreferenceChange(final Preference preference,
					final Object newValue) {
				if (newValue != null && !((String) newValue).equals("")) {
					SpecialListSettings.this.specialList.setName(newValue
							.toString());
					SpecialListSettings.this.specialList.save();
					name.setSummary(SpecialListSettings.this.specialList
							.getName());
					if (MirakelCommonPreferences.isTablet()
							&& SpecialListSettings.this.v4_0) {
						((ListSettings) SpecialListSettings.this.ctx)
								.invalidateHeaders();
					}
				}
				return false;
			}
		});

		final CheckBoxPreference active = (CheckBoxPreference) findPreference("special_list_active");
		active.setChecked(this.specialList.isActive());
		active.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(final Preference preference,
					final Object newValue) {
				SpecialListSettings.this.specialList
						.setActive((Boolean) newValue);
				SpecialListSettings.this.specialList.save();
				return true;
			}
		});

		if (!MirakelCommonPreferences.isDebug()) {
			getPreferenceScreen().removePreference(
					findPreference("special_lists_where"));
		} else {
			findPreference("special_lists_where").setSummary(
					this.specialList.getWhereQueryForTasks());
		}

		final List<ListMirakel> lists = ListMirakel.all(false);

		final Preference sortBy = findPreference("special_default_sort");
		sortBy.setOnPreferenceChangeListener(null);
		sortBy.setSummary(this.activity.getResources().getStringArray(
				R.array.task_sorting_items)[this.specialList.getSortBy()]);
		sortBy.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(final Preference preference) {
				SpecialListSettings.this.specialList = (SpecialList) ListDialogHelpers
						.handleSortBy(SpecialListSettings.this.activity,
								SpecialListSettings.this.specialList, sortBy);
				return true;
			}
		});
		final Preference defList = findPreference("special_default_list");
		defList.setOnPreferenceChangeListener(null);
		String summary = "";
		if (this.specialList.getDefaultList() != null) {
			summary = this.specialList.getDefaultList().getName();
		}
		defList.setSummary(summary);
		defList.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(final Preference preference) {
				SpecialListSettings.this.specialList = ListDialogHelpers
						.handleDefaultList(SpecialListSettings.this.activity,
								SpecialListSettings.this.specialList, lists,
								defList);
				return true;
			}
		});
		final Preference defDate = findPreference("special_default_due");
		final int[] values = this.activity.getResources().getIntArray(
				R.array.special_list_def_date_picker_val);
		for (int j = 0; j < values.length; j++) {
			if (this.specialList.getDefaultDate() == null) {
				defDate.setSummary(this.activity.getResources().getStringArray(
						R.array.special_list_def_date_picker)[0]);
			} else if (values[j] == this.specialList.getDefaultDate()) {
				defDate.setSummary(this.activity.getResources().getStringArray(
						R.array.special_list_def_date_picker)[j]);
			}
		}
		defDate.setOnPreferenceChangeListener(null);
		defDate.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(final Preference preference) {
				SpecialListSettings.this.specialList = ListDialogHelpers
						.handleDefaultDate(SpecialListSettings.this.activity,
								SpecialListSettings.this.specialList, defDate);
				return true;
			}
		});

		// Where
		final Preference taskName = findPreference("special_where_name");
		taskName.setSummary(getFieldText("name"));
		final Preference list = findPreference("special_where_list");
		list.setSummary(getFieldText("list_id"));
		final Preference done = findPreference("special_where_done");
		done.setSummary(getFieldText("done"));
		final Preference content = findPreference("special_where_content");
		content.setSummary(getFieldText("content"));
		final Preference prio = findPreference("special_where_prio");
		prio.setSummary(getFieldText("priority"));
		final Preference due = findPreference("special_where_due");
		due.setSummary(getFieldText("due"));
		final Preference reminder = findPreference("special_where_reminder");
		reminder.setSummary(getFieldText("reminder"));
		final Preference progress = findPreference("special_where_progress");
		progress.setSummary(getFieldText(Task.PROGRESS));
		final Preference subtask = findPreference("special_where_subtask");
		subtask.setSummary(getFieldText(Task.SUBTASK_TABLE));
		final Preference file = findPreference("special_where_file");
		file.setSummary(getFieldText(FileMirakel.TABLE));

		done.setOnPreferenceChangeListener(null);
		done.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(final Preference preference) {
				final CharSequence[] SortingItems = {
						SpecialListSettings.this.activity
								.getString(R.string.nothing),
						SpecialListSettings.this.activity
								.getString(R.string.done),
						SpecialListSettings.this.activity
								.getString(R.string.undone) };
				int defVal = 0;
				final SpecialListsDoneProperty prop = (SpecialListsDoneProperty) SpecialListSettings.this.specialList
						.getWhere().get(Task.DONE);
				if (prop != null) {
					if (!prop.getDone()) {
						defVal = 2;
					} else {
						defVal = 1;
					}
				}
				new AlertDialog.Builder(SpecialListSettings.this.activity)
						.setTitle(
								SpecialListSettings.this.activity
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
													Task.DONE, done);
										} else {
											prop.setDone(item == 1);
											setNewWhere(prop, item == 1
													|| item == 2, Task.DONE,
													done);
										}
										dialog.dismiss(); // Ugly
									}

								}).show();
				return true;
			}
		});

		reminder.setOnPreferenceChangeListener(null);
		reminder.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(final Preference preference) {
				final CharSequence[] SortingItems = {
						SpecialListSettings.this.activity
								.getString(R.string.nothing),
						SpecialListSettings.this.activity
								.getString(R.string.reminder_set),
						SpecialListSettings.this.activity
								.getString(R.string.reminder_unset) };

				int defVal = 0;
				final SpecialListsReminderProperty prop = (SpecialListsReminderProperty) SpecialListSettings.this.specialList
						.getWhere().get(Task.REMINDER);
				if (prop != null) {
					if (!prop.isSet()) {
						defVal = 2;
					} else {
						defVal = 1;
					}
				}
				new AlertDialog.Builder(SpecialListSettings.this.activity)
						.setTitle(
								SpecialListSettings.this.activity
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
													Task.REMINDER, reminder);
										} else {
											prop.setIsSet(item == 1);
											setNewWhere(prop, item == 1
													|| item == 2,
													Task.REMINDER, reminder);
										}
										dialog.dismiss(); // Ugly
									}

								}).show();
				return false;
			}
		});

		list.setOnPreferenceChangeListener(null);
		list.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			private boolean[] mSelectedItems;

			@Override
			public boolean onPreferenceClick(final Preference preference) {
				final List<ListMirakel> lists = ListMirakel.all(true);
				int loc = 0;
				for (final ListMirakel list : lists) {
					if (list.getId() == SpecialListSettings.this.specialList
							.getId()) {
						lists.remove(loc);
						break;
					}
					loc++;
				}
				final CharSequence[] SortingItems = new String[lists.size() + 1];
				final int[] values = new int[lists.size() + 1];
				this.mSelectedItems = new boolean[SortingItems.length];
				final SpecialListsListProperty prop = (SpecialListsListProperty) SpecialListSettings.this.specialList
						.getWhere().get(Task.LIST_ID);
				values[0] = 0;
				this.mSelectedItems[0] = prop == null ? false : prop
						.isNegated();
				SortingItems[0] = SpecialListSettings.this.activity
						.getString(R.string.inverte);
				for (int i = 0; i < lists.size(); i++) {
					values[i + 1] = lists.get(i).getId();
					SortingItems[i + 1] = lists.get(i).getName();
					this.mSelectedItems[i + 1] = prop == null ? false : prop
							.getContent().contains(lists.get(i).getId());

				}
				new AlertDialog.Builder(SpecialListSettings.this.activity)
						.setTitle(
								SpecialListSettings.this.activity
										.getString(R.string.select_by))
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(
											final DialogInterface dialog,
											final int id) {
										setSetProperty(list, values, prop,
												mSelectedItems, Task.LIST_ID,
												false);
									}

								})
						.setNegativeButton(android.R.string.cancel, null)

						.setMultiChoiceItems(SortingItems, this.mSelectedItems,
								new OnMultiChoiceClickListener() {

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
		prio.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			private boolean[] mSelectedItems;

			@Override
			public boolean onPreferenceClick(final Preference preference) {
				final CharSequence[] SortingItems = new String[6];
				final int[] values = new int[SortingItems.length];
				this.mSelectedItems = new boolean[SortingItems.length];
				final SpecialListsPriorityProperty prop = (SpecialListsPriorityProperty) SpecialListSettings.this.specialList
						.getWhere().get(Task.PRIORITY);
				values[0] = -5;
				this.mSelectedItems[0] = prop == null ? false : prop
						.isNegated();
				SortingItems[0] = SpecialListSettings.this.activity
						.getString(R.string.inverte);
				for (int i = 1; i < SortingItems.length; i++) {
					SortingItems[i] = i - 3 + "";
					values[i] = i - 3;
					this.mSelectedItems[i] = prop == null ? false : prop
							.getContent().contains(i - 3);
				}
				new AlertDialog.Builder(SpecialListSettings.this.activity)
						.setTitle(
								SpecialListSettings.this.activity
										.getString(R.string.select_by))
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(
											final DialogInterface dialog,
											final int id) {
										setSetProperty(prio, values, prop,
												mSelectedItems, Task.PRIORITY,
												true);
									}
								})
						.setNegativeButton(android.R.string.cancel, null)

						.setMultiChoiceItems(SortingItems, this.mSelectedItems,
								new OnMultiChoiceClickListener() {

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
		content.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(final Preference preference) {

				final SpecialListsStringProperty prop = (SpecialListsStringProperty) SpecialListSettings.this.specialList
						.getWhere().get(Task.CONTENT);
				final View dialogView = getView(R.layout.content_name_dialog);

				final EditText search = (EditText) dialogView
						.findViewById(R.id.where_like);
				final CheckBox negated = (CheckBox) dialogView
						.findViewById(R.id.where_like_inverte);

				setUpStringProperty(prop, dialogView, search, negated);
				new AlertDialog.Builder(SpecialListSettings.this.activity)
						.setTitle(
								SpecialListSettings.this.activity
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
												true);
									}
								}).show();
				return false;
			}

		});

		taskName.setOnPreferenceChangeListener(null);
		taskName.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(final Preference preference) {
				final SpecialListsStringProperty prop = (SpecialListsStringProperty) SpecialListSettings.this.specialList
						.getWhere().get(DatabaseHelper.NAME);
				final View dialogView = getView(R.layout.content_name_dialog);

				final EditText search = (EditText) dialogView
						.findViewById(R.id.where_like);
				final CheckBox negated = (CheckBox) dialogView
						.findViewById(R.id.where_like_inverte);
				setUpStringProperty(prop, dialogView, search, negated);
				new AlertDialog.Builder(SpecialListSettings.this.activity)
						.setTitle(
								SpecialListSettings.this.activity
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
												false);
									}
								}).show();
				return false;
			}
		});

		due.setOnPreferenceChangeListener(null);
		due.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(final Preference preference) {
				final SpecialListsDueProperty prop = (SpecialListsDueProperty) SpecialListSettings.this.specialList
						.getWhere().get(Task.DUE);
				VALUE day = VALUE.DAY;
				if (prop != null) {
					switch (prop.getUnit()) {
					case DAY:
						day = VALUE.DAY;
						break;
					case MONTH:
						day = VALUE.MONTH;
						break;
					case YEAR:
						day = VALUE.YEAR;
						break;
					default:
						break;
					}
				}
				final DueDialog dueDialog = new DueDialog(
						SpecialListSettings.this.activity, false);
				dueDialog.setTitle(SpecialListSettings.this.activity
						.getString(R.string.select_by));
				dueDialog.setValue(prop == null ? 0 : prop.getLenght(), day);
				dueDialog.setNegativeButton(android.R.string.cancel, null);
				dueDialog.setNeutralButton(R.string.no_date,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(final DialogInterface dialog,
									final int which) {
								setNewWhere(prop, false, Task.DUE, due);
							}
						});
				dueDialog.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(final DialogInterface dialog,
									final int which) {

								final int val = dueDialog.getValue();
								Unit t;
								switch (dueDialog.getDayYear()) {
								case MONTH:
									t = Unit.MONTH;
									break;
								case YEAR:
									t = Unit.YEAR;
									break;
								case DAY:
									t = Unit.DAY;
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
											val), true, Task.DUE, due);
								} else {
									prop.setUnit(t);
									prop.setLenght(val);
									setNewWhere(prop, true, Task.DUE, due);
								}
							}
						});
				dueDialog.show();
				return false;
			}
		});
		if (v4_0) {
			progress.setOnPreferenceChangeListener(null);
			progress.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					final SpecialListsProgressProperty prop = (SpecialListsProgressProperty) specialList
							.getWhere().get(Task.PROGRESS);
					View v = activity.getLayoutInflater().inflate(
							R.layout.progress_dialog, null);
					final NumberPicker operationPicker = (NumberPicker) v
							.findViewById(R.id.progress_op);
					final NumberPicker valuePicker = (NumberPicker) v
							.findViewById(R.id.progress_value);

					String[] operations = { ">=", "=", "<=" };
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
					new AlertDialog.Builder(activity)
							.setView(v)
							.setPositiveButton(android.R.string.ok,
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											if (prop == null) {
												setNewWhere(
														new SpecialListsProgressProperty(
																valuePicker
																		.getValue(),
																OPERATION
																		.values()[operationPicker
																		.getValue()]),
														true, Task.PROGRESS,
														progress);
											} else {
												prop.setValue(valuePicker
														.getValue());
												prop.setOperation(OPERATION
														.values()[operationPicker
														.getValue()]);
												setNewWhere(prop, true,
														Task.PROGRESS, progress);
											}

										}
									})
							.setNegativeButton(R.string.remove,
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											setNewWhere(null, false,
													Task.PROGRESS, progress);

										}
									})
							.setTitle(
									SpecialListSettings.this.activity
											.getString(R.string.select_by))
							.show();

					return false;
				}

			});
		} else {
			// dont provide this on android 2.x
			removePreference("special_where_progress");
		}
		subtask.setOnPreferenceChangeListener(null);
		subtask.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				final SpecialListsSubtaskProperty prop = (SpecialListsSubtaskProperty) specialList
						.getWhere().get(Task.SUBTASK_TABLE);
				final boolean[] checked = new boolean[2];
				if (prop != null) {
					checked[0] = prop.isNegated();
					checked[1] = prop.isParent();
				}
				new AlertDialog.Builder(activity)
						.setTitle(R.string.select_by)
						.setMultiChoiceItems(
								R.array.special_lists_subtask_options, checked,
								new OnMultiChoiceClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which, boolean isChecked) {
										checked[which] = isChecked;

									}
								})
						.setNegativeButton(R.string.remove,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										setNewWhere(null, false,
												Task.SUBTASK_TABLE, subtask);

									}
								})
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										if (prop == null) {
											setNewWhere(
													new SpecialListsSubtaskProperty(
															checked[0],
															checked[1]), true,
													Task.SUBTASK_TABLE, subtask);
										} else {
											prop.setParent(checked[1]);
											prop.setNegated(checked[0]);
											setNewWhere(prop, true,
													Task.SUBTASK_TABLE, subtask);
										}

									}
								}).show();
				return false;
			}
		});

		file.setOnPreferenceChangeListener(null);
		file.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				final SpecialListsFileProperty prop = (SpecialListsFileProperty) specialList
						.getWhere().get(FileMirakel.TABLE);
				int checked = 0;
				if (prop != null) {
					if (prop.getDone()) {
						checked = 1;
					} else {
						checked = 2;
					}
				}

				new AlertDialog.Builder(activity)
						.setTitle(R.string.select_by)
						.setSingleChoiceItems(R.array.file_choice, checked,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										if (which == 0) {
											setNewWhere(null, false,
													FileMirakel.TABLE, file);
										} else {
											if (prop == null) {
												setNewWhere(
														new SpecialListsFileProperty(
																which == 1),
														true,
														FileMirakel.TABLE, file);
											} else {
												prop.setDone(which == 1);
												setNewWhere(prop, true,
														FileMirakel.TABLE, file);
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
			final boolean isContent) {
		Type t = Type.CONTAINS;
		final int id = ((RadioGroup) dialogView
				.findViewById(R.id.where_like_radio)).getCheckedRadioButtonId();
		if (id == R.id.where_like_begin) {
			t = Type.BEGIN;
		} else if (id == R.id.where_like_end) {
			t = Type.END;
		}
		final String searchString = search.getText().toString().trim();
		if (prop == null) {
			setNewWhere(
					isContent ? new SpecialListsContentProperty(
							negated.isChecked(), searchString, t.ordinal())
							: new SpecialListsNameProperty(negated.isChecked(),
									searchString, t.ordinal()),
					searchString.length() != 0, Task.CONTENT, pref);

		} else {
			prop.setType(t);
			prop.setSearchString(searchString);
			prop.setNegated(negated.isChecked());
			setNewWhere(prop, searchString.length() > 0, Task.CONTENT, pref);
		}
	}

	protected static void setUpStringProperty(
			final SpecialListsStringProperty prop, final View dialogView,
			final EditText search, final CheckBox negated) {
		((RadioButton) dialogView.findViewById(R.id.where_like_contain))
				.setChecked(prop == null ? true
						: prop.getType() == Type.CONTAINS);
		((RadioButton) dialogView.findViewById(R.id.where_like_begin))
				.setChecked(prop == null ? false : prop.getType() == Type.BEGIN);
		((RadioButton) dialogView.findViewById(R.id.where_like_end))
				.setChecked(prop == null ? false : prop.getType() == Type.END);
		negated.setChecked(prop == null ? false : prop.isNegated());
		search.setText(prop == null ? "" : prop.getSearchString());
	}

	protected void setSetProperty(final Preference list, final int[] values,
			final SpecialListsSetProperty prop, final boolean[] mSelectedItems,
			final String key, final boolean isPrio) {
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
			setNewWhere(isPrio ? new SpecialListsPriorityProperty(
					mSelectedItems[0], content) : new SpecialListsListProperty(
					mSelectedItems[0], content),
					!content.isEmpty() && content.size() > 0, key, list);
		} else {
			prop.setNegated(mSelectedItems[0]);
			prop.setContent(content);
			setNewWhere(prop, !content.isEmpty() && content.size() > 0, key,
					list);
		}
	}

	protected void setNewWhere(final SpecialListsBaseProperty prop,
			final boolean add, final String key, final Preference pref) {
		final Map<String, SpecialListsBaseProperty> where = this.specialList
				.getWhere();
		where.remove(key);
		if (add) {
			where.put(key, prop);
		}
		this.specialList.setWhere(where);
		this.specialList.save();
		pref.setSummary(getFieldText(key));
		if (MirakelCommonPreferences.isDebug()) {
			findPreference("special_lists_where").setSummary(
					this.specialList.getWhereQueryForTasks());
		}

	}

}
