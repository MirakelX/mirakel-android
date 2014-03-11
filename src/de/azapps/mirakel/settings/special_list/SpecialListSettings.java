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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.helper.ListDialogHelpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.PreferencesHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakel.settings.R;
import de.azapps.widgets.DueDialog;
import de.azapps.widgets.DueDialog.VALUE;

@SuppressLint("NewApi")
public class SpecialListSettings extends PreferencesHelper implements
		OnPreferenceChangeListener {
	protected SpecialList specialList;

	public SpecialListSettings(SpecialListsSettingsActivity p,
			SpecialList specialList) {
		super(p);
		this.specialList = specialList;
	}

	@SuppressLint("NewApi")
	public SpecialListSettings(SpecialListsSettingsFragment p, SpecialList s) {
		super(p);
		this.specialList = s;
	}

	protected String getFieldText(String queryPart) {
		String[] whereParts = this.specialList.getWhereQueryForTasks(false)
				.split("and");
		String returnString = "";
		for (String s : whereParts) {
			if (s.contains(queryPart)) {
				if (queryPart.equals("done"))
					return s.contains("=1") ? this.activity
							.getString(R.string.done) : this.activity
							.getString(R.string.undone);
				if (queryPart.equals("due")) {
					if (!s.contains("not null")) {
						Pattern pattern = Pattern.compile("[\"'].*?[\"']");
						Matcher matcher = pattern.matcher(s);
						int i = 0;
						while (matcher.find()) {
							if (++i > 1) {
								s = matcher.group().replaceAll("[\"']", "");
								break;
							}
						}
						if (s.contains("localtime"))
							return this.activity.getString(R.string.today);

						int day = 0;
						if (s.contains("year")) {
							s = s.replace(
									s.contains("years") ? "years" : "year", "")
									.trim();
							day = 2;
						} else if (s.contains("month")) {
							s = s.replace(
									s.contains("months") ? "months" : "month",
									"").trim();
							day = 1;
						} else {
							s = s.replace(s.contains("days") ? "days" : "day",
									"").trim();
						}
						return s.trim()
								+ " "
								+ this.activity
										.getResources()
										.getStringArray(
												s.contains("1") ? R.array.due_day_year_values
														: R.array.due_day_year_values_plural)[day];
					}
				}
				if (queryPart.equals("reminder"))
					return s.contains("not") ? this.activity
							.getString(R.string.reminder_set) : this.activity
							.getString(R.string.reminder_unset);
				if (queryPart.equals("list_id")) {
					returnString = this.specialList.getWhereQueryForTasks(false)
							.contains("not list_id") ? this.activity
							.getString(R.string.not_in) + " " : "";
					String[] p = s
							.replace(
									(returnString.trim().length() == 0 ? ""
											: "not ") + "list_id in(", "")
							.replace(")", "").split(",");
					for (int i = 0; i < p.length; i++) {
						returnString += (i == 0 ? "" : ", ")
								+ ListMirakel.getList(
										Integer.parseInt(p[i].trim()))
										.getName();
					}
					return returnString;
				}
				if (queryPart.equals("priority")) {
					returnString = this.specialList.getWhereQueryForTasks(false)
							.contains("not priority") ? this.activity
							.getString(R.string.not_in) + " " : "";
					return returnString
							+ s.replace(
									(returnString.trim().length() == 0 ? ""
											: "not ") + "priority in (", "")
									.replace(")", "").replace(",", ", ");
				}
				if (queryPart.equals("content") || queryPart.equals("name")) {
					if (s.contains("not")) {
						s = s.replace("not", "").trim();
						returnString += this.activity.getString(R.string.not)
								+ " ";
					}
					s = s.replace(queryPart + " like", "").trim();
					if (s.replaceAll("[\"'%]", "").trim().length() == 0)
						return this.activity.getString(R.string.empty);
					if (s.matches("[\"'].%['\"]")) {
						returnString += this.activity
								.getString(R.string.where_like_begin_text)
								+ " " + s.replaceAll("[\"'%]", "");
					} else if (s.matches("[\"']%.['\"]")) {
						returnString += this.activity
								.getString(R.string.where_like_end_text)
								+ " "
								+ s.replaceAll("[\"'%]", "");
					} else {
						returnString += this.activity
								.getString(R.string.where_like_contain_text)
								+ " " + s.replaceAll("[\"'%]", "");
					}
				} else {
					returnString += s + " ";
				}
			}
		}
		return returnString.equals("") ? this.activity
				.getString(R.string.empty) : returnString;
	}

	@SuppressLint("NewApi")
	private LayoutInflater getLayoutInflater() {
		if (this.v4_0)
			return ((SpecialListsSettingsFragment) this.ctx).getActivity()
					.getLayoutInflater();
		return ((SpecialListsSettingsActivity) this.ctx).getLayoutInflater();
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private PreferenceScreen getPreferenceScreen() {
		if (this.v4_0)
			return ((SpecialListsSettingsFragment) this.ctx)
					.getPreferenceScreen();
		return ((SpecialListsSettingsActivity) this.ctx).getPreferenceScreen();
	}

	private View getView(int id) {
		if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB)
			return getLayoutInflater().inflate(id, null);
		return View.inflate(new ContextThemeWrapper(this.activity,
				R.style.Dialog), id, null);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// Nothing
		return false;
	}

	public void setup() throws NoSuchListException {
		if (this.specialList == null)
			throw new NoSuchListException();
		final EditTextPreference name = (EditTextPreference) findPreference("special_list_name");
		name.setText(this.specialList.getName());
		name.setSummary(this.specialList.getName());
		name.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@SuppressLint("NewApi")
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
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

		CheckBoxPreference active = (CheckBoxPreference) findPreference("special_list_active");
		active.setChecked(this.specialList.isActive());
		active.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
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
					this.specialList.getWhereQueryForTasks(false));
		}

		final List<ListMirakel> lists = ListMirakel.all(false);

		final Preference sortBy = findPreference("special_default_sort");
		sortBy.setOnPreferenceChangeListener(this);
		sortBy.setSummary(this.activity.getResources().getStringArray(
				R.array.task_sorting_items)[this.specialList.getSortBy()]);
		sortBy.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				SpecialListSettings.this.specialList = (SpecialList) ListDialogHelpers
						.handleSortBy(SpecialListSettings.this.activity,
								SpecialListSettings.this.specialList, sortBy);
				return true;
			}
		});
		final Preference defList = findPreference("special_default_list");
		defList.setOnPreferenceChangeListener(this);
		String summary = "";
		if (this.specialList.getDefaultList() != null) {
			summary = this.specialList.getDefaultList().getName();
		}
		defList.setSummary(summary);
		defList.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				SpecialListSettings.this.specialList = ListDialogHelpers
						.handleDefaultList(SpecialListSettings.this.activity,
								SpecialListSettings.this.specialList, lists,
								defList);
				return true;
			}
		});
		final Preference defDate = findPreference("special_default_due");
		int[] values = this.activity.getResources().getIntArray(
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
		defDate.setOnPreferenceChangeListener(this);
		defDate.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
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

		done.setOnPreferenceChangeListener(this);
		done.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				final CharSequence[] SortingItems = {
						SpecialListSettings.this.activity
								.getString(R.string.nothing),
						SpecialListSettings.this.activity
								.getString(R.string.done),
						SpecialListSettings.this.activity
								.getString(R.string.undone) };
				int defVal = 0;
				if (SpecialListSettings.this.specialList.getWhereQueryForTasks(false)
						.contains("done=0")) {
					defVal = 2;
				} else if (SpecialListSettings.this.specialList.getWhereQueryForTasks(
						false).contains("done=1")) {
					defVal = 1;
				}
				new AlertDialog.Builder(SpecialListSettings.this.activity)
						.setTitle(
								SpecialListSettings.this.activity
										.getString(R.string.select_by))
						.setSingleChoiceItems(SortingItems, defVal,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int item) {
										String newWhere = "";
										switch (item) {
										case 1:
											newWhere = "done=1";
											done.setSummary(R.string.done);
											break;
										case 2:
											newWhere = "done=0";
											done.setSummary(SpecialListSettings.this.activity
													.getString(R.string.undone));
											break;
										default:
											done.setSummary(SpecialListSettings.this.activity
													.getString(R.string.empty));
											break;
										}
										updateWhere("done", newWhere);
										dialog.dismiss(); // Ugly
									}

								}).show();
				return true;
			}
		});

		reminder.setOnPreferenceChangeListener(this);
		reminder.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				final CharSequence[] SortingItems = {
						SpecialListSettings.this.activity
								.getString(R.string.nothing),
						SpecialListSettings.this.activity
								.getString(R.string.reminder_set),
						SpecialListSettings.this.activity
								.getString(R.string.reminder_unset) };

				int defVal = 0;
				if (SpecialListSettings.this.specialList.getWhereQueryForTasks(false)
						.contains("not")) {
					defVal = 1;
				} else if (SpecialListSettings.this.specialList.getWhereQueryForTasks(
						false).contains("reminder")) {
					defVal = 2;
				}
				new AlertDialog.Builder(SpecialListSettings.this.activity)
						.setTitle(
								SpecialListSettings.this.activity
										.getString(R.string.select_by))
						.setSingleChoiceItems(SortingItems, defVal,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int item) {
										String newWhere = "";
										switch (item) {
										case 1:
											newWhere = "reminder is not null";
											reminder.setSummary(R.string.reminder_set);
											break;
										case 2:
											newWhere = "reminder is null";
											reminder.setSummary(SpecialListSettings.this.activity
													.getString(R.string.reminder_unset));
											break;
										default:
											reminder.setSummary(SpecialListSettings.this.activity
													.getString(R.string.empty));
											break;
										}
										updateWhere("reminder", newWhere);
										dialog.dismiss();
									}

								}).show();
				return false;
			}
		});

		list.setOnPreferenceChangeListener(this);
		list.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			private boolean[] mSelectedItems;

			@Override
			public boolean onPreferenceClick(Preference preference) {
				List<ListMirakel> lists = ListMirakel.all(true);
				int loc = 0;
				for (ListMirakel list : lists) {
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
				values[0] = 0;
				this.mSelectedItems[0] = SpecialListSettings.this.specialList
						.getWhereQueryForTasks(false).contains("not list_id");
				SortingItems[0] = SpecialListSettings.this.activity
						.getString(R.string.inverte);
				for (int i = 0; i < lists.size(); i++) {
					values[i + 1] = lists.get(i).getId();
					SortingItems[i + 1] = lists.get(i).getName();
					this.mSelectedItems[i + 1] = false;
					this.mSelectedItems[i + 1] = (SpecialListSettings.this.specialList
							.getWhereQueryForTasks(false).contains(
									"," + lists.get(i).getId()) || SpecialListSettings.this.specialList
							.getWhereQueryForTasks(false).contains(
									"(" + lists.get(i).getId()))
							&& (SpecialListSettings.this.specialList
									.getWhereQueryForTasks(false).contains(
											lists.get(i).getId() + ",") || SpecialListSettings.this.specialList
									.getWhereQueryForTasks(false).contains(
											lists.get(i).getId() + ")"));
				}
				new AlertDialog.Builder(SpecialListSettings.this.activity)
						.setTitle(
								SpecialListSettings.this.activity
										.getString(R.string.select_by))
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										String newWhere = (mSelectedItems[0] ? "not "
												: "")
												+ "list_id in(";
										String text = mSelectedItems[0] ? SpecialListSettings.this.activity
												.getString(R.string.not_in)
												+ " " : "";
										boolean first = true;
										for (int i = 1; i < mSelectedItems.length; i++) {
											if (mSelectedItems[i]) {
												text += (first ? "" : ", ")
														+ SortingItems[i];
												newWhere += (first ? "" : ",")
														+ values[i];
												first = false;
											}
										}
										updateWhere("list_id", first ? ""
												: newWhere + ")");
										list.setSummary(first ? SpecialListSettings.this.activity
												.getString(R.string.empty)
												: text);
									}
								})
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										// User cancelled the dialog
									}
								})

						.setMultiChoiceItems(SortingItems, this.mSelectedItems,
								new OnMultiChoiceClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which, boolean isChecked) {
										mSelectedItems[which] = isChecked;
									}

								}).show();

				return false;
			}
		});

		prio.setOnPreferenceChangeListener(this);
		prio.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			private boolean[] mSelectedItems;

			@Override
			public boolean onPreferenceClick(Preference preference) {
				final CharSequence[] SortingItems = new String[6];
				final int[] values = new int[SortingItems.length];
				this.mSelectedItems = new boolean[SortingItems.length];
				values[0] = -5;
				this.mSelectedItems[0] = SpecialListSettings.this.specialList
						.getWhereQueryForTasks(false).contains("not priority");
				SortingItems[0] = SpecialListSettings.this.activity
						.getString(R.string.inverte);
				for (int i = 1; i < SortingItems.length; i++) {
					SortingItems[i] = i - 3 + "";
					values[i] = i - 3;
					this.mSelectedItems[i] = false;
				}
				String[] p = SpecialListSettings.this.specialList
						.getWhereQueryForTasks(false).split("and");
				for (String s : p) {
					if (s.contains("priority")) {
						String[] r = s
								.replace(
										(!this.mSelectedItems[0] ? "" : "not ")
												+ "priority in (", "")
								.replace(")", "").trim().split(",");
						for (String t : r) {
							try {
								switch (Integer.parseInt(t)) {
								case -2:
									this.mSelectedItems[1] = true;
									break;
								case -1:
									this.mSelectedItems[2] = true;
									break;
								case 0:
									this.mSelectedItems[3] = true;
									break;
								case 1:
									this.mSelectedItems[4] = true;
									break;
								case 2:
									this.mSelectedItems[5] = true;
									break;
								}
							} catch (NumberFormatException e) {
							}
						}
						break;
					}
				}
				new AlertDialog.Builder(SpecialListSettings.this.activity)
						.setTitle(
								SpecialListSettings.this.activity
										.getString(R.string.select_by))
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										String newWhere = (mSelectedItems[0] ? "not "
												: "")
												+ "priority in (";
										String text = mSelectedItems[0] ? SpecialListSettings.this.activity
												.getString(R.string.not_in)
												+ " " : "";
										boolean first = true;
										for (int i = 1; i < mSelectedItems.length; i++) {
											if (mSelectedItems[i]) {
												text += (first ? "" : ", ")
														+ values[i];
												newWhere += (first ? "" : ",")
														+ values[i];
												first = false;
											}
										}
										updateWhere("priority", first ? ""
												: newWhere + ")");
										prio.setSummary(first ? SpecialListSettings.this.activity
												.getString(R.string.empty)
												: text);
									}
								})
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										// User cancelled the dialog
									}
								})

						.setMultiChoiceItems(SortingItems, this.mSelectedItems,
								new OnMultiChoiceClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which, boolean isChecked) {
										mSelectedItems[which] = isChecked;
									}

								}).show();
				return false;
			}
		});

		content.setOnPreferenceChangeListener(this);
		content.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				final View dialogView = getView(R.layout.content_name_dialog);
				String[] p = SpecialListSettings.this.specialList
						.getWhereQueryForTasks(false).split("and");
				((RadioButton) dialogView.findViewById(R.id.where_like_contain))
						.setChecked(true);
				for (String s : p) {
					if (s.contains("content")) {
						if (s.contains("not")) {
							s = s.replace("not", "").trim();
							((CheckBox) dialogView
									.findViewById(R.id.where_like_inverte))
									.setChecked(true);
						}
						s = s.replace("content like", "").replace("'", "")
								.trim();
						if (s.charAt(0) == '%') {
							if (s.charAt(s.length() - 1) == '%') {
								((EditText) dialogView
										.findViewById(R.id.where_like))
										.setText(s.replace("%", ""));
								((RadioButton) dialogView
										.findViewById(R.id.where_like_contain))
										.setChecked(true);
							} else {
								((EditText) dialogView
										.findViewById(R.id.where_like))
										.setText(s.replace("%", ""));
								((RadioButton) dialogView
										.findViewById(R.id.where_like_begin))
										.setChecked(true);
							}
						} else if (s.charAt(s.length() - 1) == '%') {
							((EditText) dialogView
									.findViewById(R.id.where_like)).setText(s
									.replace("%", ""));
							((RadioButton) dialogView
									.findViewById(R.id.where_like_end))
									.setChecked(true);
						}
						break;
					}
				}
				new AlertDialog.Builder(SpecialListSettings.this.activity)
						.setTitle(
								SpecialListSettings.this.activity
										.getString(R.string.select_by))
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {

									}
								})
						.setView(dialogView)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										String t = ((EditText) dialogView
												.findViewById(R.id.where_like))
												.getText().toString();
										if (t.trim().length() == 0) {
											updateWhere("content", "");
											content.setSummary(SpecialListSettings.this.activity
													.getString(R.string.empty));
											return;
										}
										String newWhere = (((CheckBox) dialogView
												.findViewById(R.id.where_like_inverte))
												.isChecked() ? "not " : "")
												+ "content like ";
										String text = ((CheckBox) dialogView
												.findViewById(R.id.where_like_inverte))
												.isChecked() ? SpecialListSettings.this.activity
												.getString(R.string.not) + " "
												: "";
										int id = ((RadioGroup) dialogView
												.findViewById(R.id.where_like_radio))
												.getCheckedRadioButtonId();
										if (id == R.id.where_like_begin) {
											newWhere += "'" + t + "%'";
											text += SpecialListSettings.this.activity
													.getString(R.string.where_like_begin_text)
													+ " " + t;
										} else if (id == R.id.where_like_end) {
											newWhere += "'%" + t + "'";
											text += SpecialListSettings.this.activity
													.getString(R.string.where_like_end_text)
													+ " " + t;
										} else {
											newWhere += "'%" + t + "%'";
											text += SpecialListSettings.this.activity
													.getString(R.string.where_like_contain_text)
													+ " " + t;
										}
										updateWhere("content", newWhere);
										content.setSummary(text);
									}
								}).show();
				return false;
			}
		});

		taskName.setOnPreferenceChangeListener(this);
		taskName.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				final View dialogView = getView(R.layout.content_name_dialog);
				String[] p = SpecialListSettings.this.specialList
						.getWhereQueryForTasks(false).split("and");
				((RadioButton) dialogView.findViewById(R.id.where_like_contain))
						.setChecked(true);
				for (String s : p) {
					if (s.contains("name")) {
						if (s.contains("not")) {
							s = s.replace("not", "").trim();
							((CheckBox) dialogView
									.findViewById(R.id.where_like_inverte))
									.setChecked(true);
						}
						s = s.replace("name like", "").replace("'", "").trim();
						if (s.charAt(0) == '%') {
							if (s.charAt(s.length() - 1) == '%') {
								((EditText) dialogView
										.findViewById(R.id.where_like))
										.setText(s.replace("%", ""));
								((RadioButton) dialogView
										.findViewById(R.id.where_like_contain))
										.setChecked(true);
							} else {
								((EditText) dialogView
										.findViewById(R.id.where_like))
										.setText(s.replace("%", ""));
								((RadioButton) dialogView
										.findViewById(R.id.where_like_begin))
										.setChecked(true);
							}
						} else if (s.charAt(s.length() - 1) == '%') {
							((EditText) dialogView
									.findViewById(R.id.where_like)).setText(s
									.replace("%", ""));
							((RadioButton) dialogView
									.findViewById(R.id.where_like_end))
									.setChecked(true);
						}
						break;
					}
				}
				new AlertDialog.Builder(SpecialListSettings.this.activity)
						.setTitle(
								SpecialListSettings.this.activity
										.getString(R.string.select_by))
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {

									}
								})
						.setView(dialogView)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										String t = ((EditText) dialogView
												.findViewById(R.id.where_like))
												.getText().toString();
										if (t.trim().length() == 0) {
											updateWhere("name", "");
											taskName.setSummary(SpecialListSettings.this.activity
													.getString(R.string.empty));
											return;
										}
										String newWhere = (((CheckBox) dialogView
												.findViewById(R.id.where_like_inverte))
												.isChecked() ? "not " : "")
												+ "name like ";
										String text = ((CheckBox) dialogView
												.findViewById(R.id.where_like_inverte))
												.isChecked() ? SpecialListSettings.this.activity
												.getString(R.string.not) + " "
												: "";
										int id = ((RadioGroup) dialogView
												.findViewById(R.id.where_like_radio))
												.getCheckedRadioButtonId();
										if (id == R.id.where_like_begin) {

											newWhere += "'" + t + "%'";
											text += SpecialListSettings.this.activity
													.getString(R.string.where_like_begin_text)
													+ " " + t;
										} else if (id == R.id.where_like_end) {
											newWhere += "'%" + t + "'";
											text += SpecialListSettings.this.activity
													.getString(R.string.where_like_end_text)
													+ " " + t;
										} else {

											newWhere += "'%" + t + "%'";
											text += SpecialListSettings.this.activity
													.getString(R.string.where_like_contain_text)
													+ " " + t;

										}
										updateWhere("name", newWhere);
										taskName.setSummary(text);
									}
								}).show();
				return false;
			}
		});

		due.setOnPreferenceChangeListener(this);
		due.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				String[] p = SpecialListSettings.this.specialList
						.getWhereQueryForTasks(false).split("and");
				VALUE day = VALUE.DAY;
				int val = 0;
				for (String r : p) {
					if (r.contains("date(due)")) {
						Pattern pattern = Pattern.compile("[\"'].*?[\"']");
						Matcher matcher = pattern.matcher(r);
						int i = 0;
						while (matcher.find()) {
							if (++i > 1) {
								r = matcher.group().replaceAll("[\"']", "");
								break;
							}
						}
						if (!r.contains("localtime")) {
							if (r.contains("year")) {
								r = r.replace(
										r.contains("years") ? "years" : "year",
										"").trim();
								day = VALUE.YEAR;
							} else if (r.contains("month")) {
								r = r.replace(
										r.contains("months") ? "months"
												: "month", "").trim();
								day = VALUE.MONTH;
							} else {
								r = r.replace(
										r.contains("days") ? "days" : "day", "")
										.trim();
							}
							try {
								val = Integer.parseInt(r.replace("+", ""));
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}

						}

					}
				}
				final DueDialog dueDialog = new DueDialog(
						SpecialListSettings.this.activity, false);
				dueDialog.setTitle(SpecialListSettings.this.activity
						.getString(R.string.select_by));
				dueDialog.setValue(val, day);
				dueDialog.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
							}
						});
				dueDialog.setNeutralButton(R.string.no_date,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								updateWhere("due", "");
								due.setSummary(SpecialListSettings.this.activity
										.getString(R.string.empty));
							}
						});
				dueDialog.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								int val = dueDialog.getValue();
								String newWhere = "";
								if (val == 0) {
									newWhere = "date(due)<=date(\"now\",\"localtime\")";
									due.setSummary(SpecialListSettings.this.activity
											.getString(R.string.today));
								} else {
									String mod = "";
									VALUE day = dueDialog.getDayYear();
									String summary = val + " ";
									switch (day) {
									case MONTH:
										mod = val == 1 || val == -1 ? "month"
												: "months";
										summary += SpecialListSettings.this.activity
												.getResources()
												.getQuantityString(
														R.plurals.due_month,
														val);
										break;
									case YEAR:
										mod = val == 1 || val == -1 ? "year"
												: "years";
										summary += SpecialListSettings.this.activity
												.getResources()
												.getQuantityString(
														R.plurals.due_year, val);
										break;
									case DAY:
										mod = val == 1 || val == -1 ? "day"
												: "days";
										summary += SpecialListSettings.this.activity
												.getResources()
												.getQuantityString(
														R.plurals.due_day, val);
										break;
									default:
										// The other things aren't
										// shown in
										// the dialog so we haven't to care
										// about them
										break;
									}

									due.setSummary(summary);
									newWhere = "date(due)<=date(\"now\",\""
											+ val + " " + mod
											+ "\",\"localtime\")";
								}
								updateWhere("due", newWhere);
							}
						});
				dueDialog.show();
				return false;
			}
		});

	}

	void updateWhere(String attr, String newWhere) {
		if (this.specialList.getWhereQueryForTasks(false).contains(attr)) {
			String[] parts = this.specialList.getWhereQueryForTasks(false).split("and");
			String n = "";
			boolean first = true;
			for (int i = 0; i < parts.length; i++) {
				if (parts[i].contains(attr)
						&& (!parts[i].contains("not null")
								|| newWhere.trim().length() == 0 || attr != "due")) {
					parts[i] = newWhere;
					if (newWhere.trim().length() == 0) {
						continue;
					}
				}
				n += (first ? "" : " and ") + parts[i].trim();
				first = false;
			}
			this.specialList.setWhereQuery(n);
		} else if (this.specialList.getWhereQueryForTasks(false).trim().length() == 0
				&& !newWhere.trim().equals("")) {
			this.specialList
					.setWhereQuery((attr.equals("due") ? "due is not null and "
							: "") + newWhere);
		} else if (newWhere != "") {
			this.specialList
					.setWhereQuery((attr.equals("due") ? "due is not null and "
							: "")
							+ this.specialList.getWhereQueryForTasks(false)
							+ " and " + newWhere);
		}
		this.specialList.save();
		if (MirakelCommonPreferences.isDebug()) {
			findPreference("special_lists_where").setSummary(
					this.specialList.getWhereQueryForTasks(false));
		}
	}
}
