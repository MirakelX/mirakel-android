/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
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
import android.content.Context;
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
import de.azapps.mirakel.helper.DueDialog;
import de.azapps.mirakel.helper.ListDialogHelpers;
import de.azapps.mirakel.helper.DueDialog.VALUE;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.R;

public class SpecialListSettings implements OnPreferenceChangeListener {
	private SpecialList specialList;
	private boolean v4_0;
	private Object settings;
	private Context ctx;

	@SuppressLint("NewApi")
	public SpecialListSettings(SpecialListsSettingsFragment p, SpecialList s) {
		specialList = s;
		v4_0 = true;
		settings = p;
		ctx = p.getActivity();
	}

	public SpecialListSettings(SpecialListsSettingsActivity p,
			SpecialList specialList) {
		ctx = p;
		settings = p;
		v4_0 = false;
		this.specialList = specialList;
	}

	public void setup() {
		final EditTextPreference name = (EditTextPreference) findPreference("special_list_name");
		name.setText(specialList.getName());
		name.setSummary(specialList.getName());
		name.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				if (newValue != null && !((String) newValue).equals("")) {
					specialList.setName(newValue.toString());
					specialList.save();

					name.setSummary(specialList.getName());
				}
				return false;
			}
		});

		CheckBoxPreference active = (CheckBoxPreference) findPreference("special_list_active");
		active.setChecked(specialList.isActive());
		active.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				specialList.setActive((Boolean) newValue);
				specialList.save();
				return true;
			}
		});

		if (!BuildConfig.DEBUG) {
			getPreferenceScreen().removePreference(
					findPreference("special_lists_where"));
		} else {
			findPreference("special_lists_where").setSummary(
					specialList.getWhereQuery(false));
		}

		final List<ListMirakel> lists = ListMirakel.all(false);

		final Preference sortBy = findPreference("special_default_sort");
		sortBy.setOnPreferenceChangeListener(this);
		sortBy.setSummary(ctx.getResources().getStringArray(
				R.array.task_sorting_items)[specialList.getSortBy()]);
		sortBy.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				specialList = (SpecialList) ListDialogHelpers.handleSortBy(ctx,
						specialList, sortBy);
				return true;
			}
		});
		final Preference defList = findPreference("special_default_list");
		defList.setOnPreferenceChangeListener(this);
		defList.setSummary(specialList.getDefaultList().getName());
		defList.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				specialList = ListDialogHelpers.handleDefaultList(ctx,
						specialList, lists, defList);
				return true;
			}
		});
		final Preference defDate = findPreference("special_default_due");
		int[] values = ctx.getResources().getIntArray(
				R.array.special_list_def_date_picker_val);
		for (int j = 0; j < values.length; j++) {
			if (specialList.getDefaultDate() == null)
				defDate.setSummary(ctx.getResources().getStringArray(
						R.array.special_list_def_date_picker)[0]);
			else if (values[j] == specialList.getDefaultDate()) {
				defDate.setSummary(ctx.getResources().getStringArray(
						R.array.special_list_def_date_picker)[j]);
			}
		}
		defDate.setOnPreferenceChangeListener(this);
		defDate.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				specialList = ListDialogHelpers.handleDefaultDate(ctx,
						specialList, defDate);
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
						ctx.getString(R.string.nothing),
						ctx.getString(R.string.done),
						ctx.getString(R.string.undone) };
				int defVal = 0;
				if (specialList.getWhereQuery(false).contains("done=0"))
					defVal = 2;
				else if (specialList.getWhereQuery(false).contains("done=1"))
					defVal = 1;
				new AlertDialog.Builder(ctx)
						.setTitle(ctx.getString(R.string.select_by))
						.setSingleChoiceItems(SortingItems, defVal,
								new DialogInterface.OnClickListener() {
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
											done.setSummary(ctx
													.getString(R.string.undone));
											break;
										default:
											done.setSummary(ctx
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
						ctx.getString(R.string.nothing),
						ctx.getString(R.string.reminder_set),
						ctx.getString(R.string.reminder_unset) };

				int defVal = 0;
				if (specialList.getWhereQuery(false).contains("not"))
					defVal = 1;
				else if (specialList.getWhereQuery(false).contains("reminder"))
					defVal = 2;
				new AlertDialog.Builder(ctx)
						.setTitle(ctx.getString(R.string.select_by))
						.setSingleChoiceItems(SortingItems, defVal,
								new DialogInterface.OnClickListener() {
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
											reminder.setSummary(ctx
													.getString(R.string.reminder_unset));
											break;
										default:
											reminder.setSummary(ctx
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
					if (list.getId() == specialList.getId()) {
						lists.remove(loc);
						break;
					}
					loc++;
				}
				final CharSequence[] SortingItems = new String[lists.size() + 1];
				final int[] values = new int[lists.size() + 1];
				mSelectedItems = new boolean[SortingItems.length];
				values[0] = 0;
				mSelectedItems[0] = specialList.getWhereQuery(false).contains(
						"not list_id");
				SortingItems[0] = ctx.getString(R.string.inverte);
				for (int i = 0; i < lists.size(); i++) {
					values[i + 1] = lists.get(i).getId();
					SortingItems[i + 1] = lists.get(i).getName();
					mSelectedItems[i + 1] = false;
					mSelectedItems[i + 1] = (specialList.getWhereQuery(false)
							.contains("," + lists.get(i).getId()) || specialList
							.getWhereQuery(false).contains(
									"(" + lists.get(i).getId()))
							&& (specialList.getWhereQuery(false).contains(
									lists.get(i).getId() + ",") || specialList
									.getWhereQuery(false).contains(
											lists.get(i).getId() + ")"));
				}
				new AlertDialog.Builder(ctx)
						.setTitle(ctx.getString(R.string.select_by))
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										String newWhere = (mSelectedItems[0] ? "not "
												: "")
												+ "list_id in(";
										String text = (mSelectedItems[0] ? ctx
												.getString(R.string.not_in)
												+ " " : "");
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
										updateWhere("list_id", (first ? ""
												: newWhere + ")"));
										list.setSummary(first ? ctx
												.getString(R.string.empty)
												: text);
									}
								})
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										// User cancelled the dialog
									}
								})

						.setMultiChoiceItems(SortingItems, mSelectedItems,
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
				mSelectedItems = new boolean[SortingItems.length];
				values[0] = -5;
				mSelectedItems[0] = specialList.getWhereQuery(false).contains(
						"not priority");
				SortingItems[0] = ctx.getString(R.string.inverte);
				for (int i = 1; i < SortingItems.length; i++) {
					SortingItems[i] = (i - 3) + "";
					values[i] = (i - 3);
					mSelectedItems[i] = false;
				}
				String[] p = specialList.getWhereQuery(false).split("and");
				for (String s : p) {
					if (s.contains("priority")) {
						String[] r = s
								.replace(
										(!mSelectedItems[0] ? "" : "not ")
												+ "priority in (", "")
								.replace(")", "").trim().split(",");
						for (String t : r) {
							try {
								switch (Integer.parseInt(t)) {
								case -2:
									mSelectedItems[1] = true;
									break;
								case -1:
									mSelectedItems[2] = true;
									break;
								case 0:
									mSelectedItems[3] = true;
									break;
								case 1:
									mSelectedItems[4] = true;
									break;
								case 2:
									mSelectedItems[5] = true;
									break;
								}
							} catch (NumberFormatException e) {
							}
						}
						break;
					}
				}
				new AlertDialog.Builder(ctx)
						.setTitle(ctx.getString(R.string.select_by))
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										String newWhere = (mSelectedItems[0] ? "not "
												: "")
												+ "priority in (";
										String text = (mSelectedItems[0] ? ctx
												.getString(R.string.not_in)
												+ " " : "");
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
										updateWhere("priority", (first ? ""
												: newWhere + ")"));
										prio.setSummary(first ? ctx
												.getString(R.string.empty)
												: text);
									}
								})
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										// User cancelled the dialog
									}
								})

						.setMultiChoiceItems(SortingItems, mSelectedItems,
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
				String[] p = specialList.getWhereQuery(false).split("and");
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
				new AlertDialog.Builder(ctx)
						.setTitle(ctx.getString(R.string.select_by))
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
											content.setSummary(ctx
													.getString(R.string.empty));
											return;
										}
										String newWhere = (((CheckBox) dialogView
												.findViewById(R.id.where_like_inverte))
												.isChecked() ? "not " : "")
												+ "content like ";
										String text = (((CheckBox) dialogView
												.findViewById(R.id.where_like_inverte))
												.isChecked() ? ctx
												.getString(R.string.not) + " "
												: "");
										switch (((RadioGroup) dialogView
												.findViewById(R.id.where_like_radio))
												.getCheckedRadioButtonId()) {
										case R.id.where_like_begin:
											newWhere += "'" + t + "%'";
											text += ctx
													.getString(R.string.where_like_begin_text)
													+ " " + t;
											break;
										case R.id.where_like_end:
											newWhere += "'%" + t + "'";
											text += ctx
													.getString(R.string.where_like_end_text)
													+ " " + t;
											break;
										default:
											newWhere += "'%" + t + "%'";
											text += ctx
													.getString(R.string.where_like_contain_text)
													+ " " + t;
											break;
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
				String[] p = specialList.getWhereQuery(false).split("and");
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
				new AlertDialog.Builder(ctx)
						.setTitle(ctx.getString(R.string.select_by))
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
											taskName.setSummary(ctx
													.getString(R.string.empty));
											return;
										}
										String newWhere = (((CheckBox) dialogView
												.findViewById(R.id.where_like_inverte))
												.isChecked() ? "not " : "")
												+ "name like ";
										String text = (((CheckBox) dialogView
												.findViewById(R.id.where_like_inverte))
												.isChecked() ? ctx
												.getString(R.string.not) + " "
												: "");
										switch (((RadioGroup) dialogView
												.findViewById(R.id.where_like_radio))
												.getCheckedRadioButtonId()) {
										case R.id.where_like_begin:
											newWhere += "'" + t + "%'";
											text += ctx
													.getString(R.string.where_like_begin_text)
													+ " " + t;
											break;
										case R.id.where_like_end:
											newWhere += "'%" + t + "'";
											text += ctx
													.getString(R.string.where_like_end_text)
													+ " " + t;
											break;
										default:
											newWhere += "'%" + t + "%'";
											text += ctx
													.getString(R.string.where_like_contain_text)
													+ " " + t;
											break;
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

				String[] p = specialList.getWhereQuery(false).split("and");
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
										(r.contains("years") ? "years" : "year"),
										"").trim();
								day = VALUE.YEAR;
							} else if (r.contains("month")) {
								r = r.replace(
										(r.contains("months") ? "months"
												: "month"), "").trim();
								day = VALUE.MONTH;
							} else {
								r = r.replace(
										(r.contains("days") ? "days" : "day"),
										"").trim();
							}
							try {
								val = Integer.parseInt(r.replace("+", ""));
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}

						}

					}
				}
				final DueDialog dueDialog = new DueDialog(ctx);
				dueDialog.setTitle(ctx.getString(R.string.select_by));
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
								due.setSummary(ctx.getString(R.string.empty));
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
									due.setSummary(ctx
											.getString(R.string.today));
								} else {
									String mod = "";
									VALUE day = dueDialog.getDayYear();
									String summary = val + " ";
									switch (day) {
									case MONTH:
										mod = (val == 1 || val == -1 ? "month"
												: "months");
										summary += ctx.getResources()
												.getQuantityString(
														R.plurals.due_month,
														val);
										break;
									case YEAR:
										mod = (val == 1 || val == -1 ? "year"
												: "years");
										summary += ctx
												.getResources()
												.getQuantityString(
														R.plurals.due_year, val);
										break;
									case DAY:
										mod = (val == 1 || val == -1 ? "day"
												: "days");
										summary += ctx.getResources()
												.getQuantityString(
														R.plurals.due_day, val);
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

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private PreferenceScreen getPreferenceScreen() {
		if (v4_0) {
			return ((SpecialListsSettingsFragment) settings)
					.getPreferenceScreen();
		} else {
			return ((SpecialListsSettingsActivity) settings)
					.getPreferenceScreen();
		}
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private Preference findPreference(String key) {
		if (v4_0) {
			return ((SpecialListsSettingsFragment) settings)
					.findPreference(key);
		} else {
			return ((SpecialListsSettingsActivity) settings)
					.findPreference(key);
		}
	}

	private void updateWhere(String attr, String newWhere) {
		if (specialList.getWhereQuery(false).contains(attr)) {
			String[] parts = specialList.getWhereQuery(false).split("and");
			String n = "";
			boolean first = true;
			for (int i = 0; i < parts.length; i++) {
				if ((parts[i].contains(attr))
						&& (!parts[i].contains("not null")
								|| newWhere.trim().length() == 0 || attr != "due")) {
					parts[i] = newWhere;
					if (newWhere.trim().length() == 0)
						continue;
				}
				n += (first ? "" : " and ") + parts[i].trim();
				first = false;
			}
			specialList.setWhereQuery(n);
		} else if (specialList.getWhereQuery(false).trim().length() == 0
				&& !newWhere.trim().equals("")) {
			specialList
					.setWhereQuery((attr.equals("due") ? "due is not null and "
							: "") + newWhere);
		} else if (newWhere != "") {
			specialList
					.setWhereQuery((attr.equals("due") ? "due is not null and "
							: "")
							+ specialList.getWhereQuery(false)
							+ " and "
							+ newWhere);
		}
		specialList.save();
		if (BuildConfig.DEBUG) {
			findPreference("special_lists_where").setSummary(
					specialList.getWhereQuery(false));
		}
	}

	private View getView(int id) {
		if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB)
			return getLayoutInflater().inflate(id, null);
		return View.inflate(new ContextThemeWrapper(ctx, R.style.Dialog), id,
				null);
	}

	@SuppressLint("NewApi")
	private LayoutInflater getLayoutInflater() {
		if (v4_0) {
			return ((SpecialListsSettingsFragment) settings).getActivity()
					.getLayoutInflater();
		} else {
			return ((SpecialListsSettingsActivity) settings)
					.getLayoutInflater();
		}
	}

	protected String getFieldText(String queryPart) {
		String[] whereParts = specialList.getWhereQuery(false).split("and");
		String returnString = "";
		for (String s : whereParts) {
			if (s.contains(queryPart)) {
				if (queryPart.equals("done")) {
					return s.contains("=1") ? ctx.getString(R.string.done)
							: ctx.getString(R.string.undone);
				}
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
						if (s.contains("localtime")) {
							return ctx.getString(R.string.today);
						}

						int day = 0;
						if (s.contains("year")) {
							s = s.replace(
									(s.contains("years") ? "years" : "year"),
									"").trim();
							day = 2;
						} else if (s.contains("month")) {
							s = s.replace(
									(s.contains("months") ? "months" : "month"),
									"").trim();
							day = 1;
						} else {
							s = s.replace(
									(s.contains("days") ? "days" : "day"), "")
									.trim();
						}
						return s.trim()
								+ " "
								+ ctx.getResources()
										.getStringArray(
												(s.contains("1") ? R.array.due_day_year_values
														: R.array.due_day_year_values_plural))[day];
					}
				}
				if (queryPart.equals("reminder")) {
					return s.contains("not") ? ctx
							.getString(R.string.reminder_set) : ctx
							.getString(R.string.reminder_unset);
				}
				if (queryPart.equals("list_id")) {
					returnString = (specialList.getWhereQuery(false).contains(
							"not list_id") ? ctx.getString(R.string.not_in)
							+ " " : "");
					String[] p = s
							.replace(
									(returnString.trim().length() == 0 ? ""
											: "not ") + "list_id in(", "")
							.replace(")", "").split(",");
					for (int i = 0; i < p.length; i++) {
						returnString += (i == 0 ? "" : ", ")
								+ (ListMirakel.getList(Integer.parseInt(p[i]
										.trim())).getName());
					}
					return returnString;
				}
				if (queryPart.equals("priority")) {
					returnString = (specialList.getWhereQuery(false).contains(
							"not priority") ? ctx.getString(R.string.not_in)
							+ " " : "");
					return returnString
							+ s.replace(
									(returnString.trim().length() == 0 ? ""
											: "not ") + "priority in (", "")
									.replace(")", "").replace(",", ", ");
				}
				if (queryPart.equals("content") || queryPart.equals("name")) {
					if (s.contains("not")) {
						s = s.replace("not", "").trim();
						returnString += ctx.getString(R.string.not) + " ";
					}
					s = s.replace(queryPart + " like", "").trim();
					if (s.replaceAll("[\"'%]", "").trim().length() == 0)
						return ctx.getString(R.string.empty);
					if (s.matches("[\"'].%['\"]"))
						returnString += ctx
								.getString(R.string.where_like_begin_text)
								+ " " + s.replaceAll("[\"'%]", "");
					else if (s.matches("[\"']%.['\"]"))
						returnString += ctx
								.getString(R.string.where_like_end_text)
								+ " "
								+ s.replaceAll("[\"'%]", "");
					else
						returnString += ctx
								.getString(R.string.where_like_contain_text)
								+ " " + s.replaceAll("[\"'%]", "");
				} else {
					returnString += s + " ";
				}
			}
		}
		return returnString.equals("") ? ctx.getString(R.string.empty)
				: returnString;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// Nothing
		return false;
	}
}
