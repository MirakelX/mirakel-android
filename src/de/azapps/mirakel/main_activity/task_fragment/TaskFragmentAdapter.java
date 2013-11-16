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
package de.azapps.mirakel.main_activity.task_fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewSwitcher;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.adapter.MirakelArrayAdapter;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakelandroid.R;

public class TaskFragmentAdapter extends
		MirakelArrayAdapter<Pair<Integer, Integer>> {// TYPE,INDEX
	private static final String TAG = "TaskFragmentAdapter";
	private Task task;
	protected boolean mIgnoreTimeSet;
	private LayoutInflater inflater;
	private TaskFragmentAdapter adapter;
	private List<Task> subtasks;
	private List<FileMirakel> files;
	private boolean editContent;
	protected Handler mHandler;
	protected ActionMode mActionMode;
	private static final int SUBTITLE_SUBTASKS = 0, SUBTITLE_FILES = 1;
	private static final Integer inactive_color = android.R.color.darker_gray;
	private View.OnClickListener cameraButtonClick = null;

	public class TYPE {
		final static int HEADER = 0;
		final static int FILE = 1;
		final static int DUE = 2;
		final static int REMINDER = 3;
		final static int CONTENT = 4;
		final static int SUBTITLE = 5;
		final static int SUBTASK = 6;
	}

	public TaskFragmentAdapter(Context c) {
		// Do not call!!
		super(c, 0, new ArrayList<Pair<Integer, Integer>>());
	}

	public TaskFragmentAdapter(Context context, int textViewResourceId, Task t) {
		super(context, textViewResourceId, generateData(t));
		this.task = t;
		if (task == null)
			task = Task.getDummy(context);
		subtasks = task.getSubtasks();
		files = task.getFiles();
		this.inflater = ((Activity) context).getLayoutInflater();
		this.adapter = this;
		editContent = false;

	}

	@Override
	public int getViewTypeCount() {
		return 7;
	}

	@Override
	public int getItemViewType(int position) {
		return data.get(position).first;
	}

	public void setEditContent(boolean edit) {
		editContent = edit;
		notifyDataSetChanged();
		if (mActionMode != null && edit == false) {
			mActionMode.finish();
		}
	}

	public void closeActionMode() {
		if (mActionMode != null) {
			mActionMode.finish();
		}

	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = new View(context);
		if (position >= data.size()) {
			Log.w(TAG, "position > data");
			return row;
		}
		int width;
		Display display = ((Activity) context).getWindowManager()
				.getDefaultDisplay();
		;
		try {
			Point size = new Point();
			display.getRealSize(size);
			width = size.x;
		} catch (NoSuchMethodError e) {
			// API<17
			width = display.getWidth();
		}
		switch (getItemViewType(position)) {
		case TYPE.DUE:
			row = setupDue(parent, convertView, width);
			break;
		case TYPE.FILE:
			row = setupFile(parent, files.get(data.get(position).second),
					convertView, position);
			break;
		case TYPE.HEADER:
			row = setupHeader(parent, convertView);
			break;
		case TYPE.REMINDER:
			if (width < 600)
				row = setupReminder(parent, convertView);
			break;
		case TYPE.SUBTASK:
			row = setupSubtask(parent, convertView,
					subtasks.get(data.get(position).second), position);
			break;
		case TYPE.SUBTITLE:
			String title = null;
			OnClickListener action = null;
			boolean pencilButton = false;
			boolean cameraButton = false;
			switch (data.get(position).second) {
			case SUBTITLE_SUBTASKS:
				title = context.getString(R.string.add_subtasks);
				action = new OnClickListener() {
					@Override
					public void onClick(View v) {
						TaskDialogHelpers.handleSubtask(context, task, adapter,
								false);
					}
				};
				break;
			case SUBTITLE_FILES:
				cameraButton = true;
				title = context.getString(R.string.add_files);
				action = new OnClickListener() {
					@Override
					public void onClick(View v) {
						Helpers.showFileChooser(MainActivity.RESULT_ADD_FILE,
								context.getString(R.string.file_select),
								(Activity) context);
					}
				};
				break;

			default:
				Log.w(TAG, "unknown subtitle");
				break;
			}
			row = setupSubtitle(parent, title, pencilButton, cameraButton,
					action, convertView);
			break;
		case TYPE.CONTENT:
			row = setupContent(parent, convertView);
			break;

		}

		return row;
	}

	private void saveContent() {
		if (!editContent) {// End Edit, save content
			if (mActionMode != null) {
				mActionMode.finish();
			}
			EditText txt = (EditText) ((MainActivity) context)
					.findViewById(R.id.task_content_edit);
			if (txt != null) {
				((InputMethodManager) context
						.getSystemService(Context.INPUT_METHOD_SERVICE))
						.showSoftInput(txt,
								InputMethodManager.HIDE_IMPLICIT_ONLY);
				if (!txt.getText().toString().trim().equals(task.getContent())) {
					task.setContent(txt.getText().toString().trim());
					try {
						task.save();
					} catch (NoSuchListException e) {
						Log.w(TAG, "List did vanish");
					}
				} else {
					Log.d(TAG, "content equal");
				}
			} else {
				Log.d(TAG, "edit_content not found");
			}
		}
	}

	static class TaskHolder {
		CheckBox taskRowDone;
		LinearLayout taskRowDoneWrapper;
		TextView taskRowName;
		TextView taskRowPriority;
		TextView taskRowDue, taskRowList;
		ImageView taskRowHasContent;
	}

	private View setupSubtask(ViewGroup parent, View convertView, Task task,
			int position) {
		final View row = (convertView == null || convertView.getId() != R.id.tasks_row) ? inflater
				.inflate(R.layout.tasks_row, parent, false) : convertView;
		final TaskHolder holder;

		if (convertView == null) {
			// Initialize the View
			holder = new TaskHolder();
			holder.taskRowDone = (CheckBox) row
					.findViewById(R.id.tasks_row_done);
			holder.taskRowDoneWrapper = (LinearLayout) row
					.findViewById(R.id.tasks_row_done_wrapper);
			holder.taskRowName = (TextView) row
					.findViewById(R.id.tasks_row_name);
			holder.taskRowPriority = (TextView) row
					.findViewById(R.id.tasks_row_priority);
			holder.taskRowDue = (TextView) row.findViewById(R.id.tasks_row_due);
			holder.taskRowHasContent = (ImageView) row
					.findViewById(R.id.tasks_row_has_content);
			holder.taskRowList = (TextView) row
					.findViewById(R.id.tasks_row_list_name);

			holder.taskRowDoneWrapper.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Task task = (Task) v.getTag();
					task.toggleDone();
					((MainActivity) context).saveTask(task);
					ReminderAlarm.updateAlarms(context);
					holder.taskRowDone.setChecked(task.isDone());
					updateName(task, row, holder);
				}
			});

			holder.taskRowPriority.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					final Task task = (Task) v.getTag();
					TaskDialogHelpers.handlePriority(context, task,
							new ExecInterface() {
								@Override
								public void exec() {
									TaskFragmentAdapter.this.task = Task
											.get(TaskFragmentAdapter.this.task
													.getId());
									adapter.notifyDataSetChanged();
								}
							});

				}
			});

			row.setTag(holder);
		} else {
			holder = (TaskHolder) row.getTag();
		}
		if (task == null)
			return row;

		// Done
		holder.taskRowDone.setChecked(task.isDone());
		holder.taskRowDone.setTag(task);
		holder.taskRowDoneWrapper.setTag(task);
		holder.taskRowDone.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Task task = (Task) v.getTag();
				task.toggleDone();
				((MainActivity) context).saveTask(task);
				ReminderAlarm.updateAlarms(context);
				holder.taskRowDone.setChecked(task.isDone());
				updateName(task, row, holder);
			}
		});
		if (task.getContent().length() != 0) {
			holder.taskRowHasContent.setVisibility(View.VISIBLE);
		} else {
			holder.taskRowHasContent.setVisibility(View.INVISIBLE);
		}
		if (task != null && task.getList() != null) {
			holder.taskRowList.setVisibility(View.VISIBLE);
			holder.taskRowList.setText(task.getList().getName());
		} else {
			holder.taskRowList.setVisibility(View.GONE);
		}

		// Name
		holder.taskRowName.setText(task.getName());

		updateName(task, row, holder);

		// Priority
		holder.taskRowPriority.setText("" + task.getPriority());

		GradientDrawable bg = (GradientDrawable) holder.taskRowPriority
				.getBackground();
		bg.setColor(TaskHelper.getPrioColor(task.getPriority(), context));
		holder.taskRowPriority.setTag(task);

		// Due
		if (task.getDue() != null) {
			holder.taskRowDue.setVisibility(View.VISIBLE);
			holder.taskRowDue
					.setText(DateTimeHelper.formatDate(context, task.getDue()));
			holder.taskRowDue.setTextColor(row.getResources().getColor(
					TaskHelper.getTaskDueColor(task.getDue(), task.isDone())));
		} else {
			holder.taskRowDue.setVisibility(View.GONE);
		}
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (selected.get(position)) {
			row.setBackgroundColor(context.getResources().getColor(
					darkTheme ? R.color.highlighted_text_holo_dark
							: R.color.highlighted_text_holo_light));
		} else if (settings.getBoolean("colorize_tasks", true)) {
			if (settings.getBoolean("colorize_subtasks", true)) {
				int w = row.getWidth() == 0 ? parent.getWidth() : row
						.getWidth();
				Helpers.setListColorBackground(task.getList(), row, darkTheme,
						w);
			} else {
				row.setBackgroundColor(context.getResources().getColor(
						android.R.color.transparent));
			}
		} else {
			row.setBackgroundColor(context.getResources().getColor(
					android.R.color.transparent));
		}
		return row;
	}

	private void updateName(Task task, View row, final TaskHolder holder) {
		if (task.isDone()) {
			holder.taskRowName.setTextColor(row.getResources().getColor(
					R.color.Grey));
		} else {
			holder.taskRowName.setTextColor(row.getResources().getColor(
					darkTheme ? android.R.color.primary_text_dark
							: android.R.color.primary_text_light));
		}
	}

	static class FileHolder {
		ImageView fileImage;
		TextView fileName;
		TextView filePath;
	}

	private View setupFile(ViewGroup parent, final FileMirakel file,
			View convertView, int position) {
		final View row = convertView == null ? inflater.inflate(
				R.layout.files_row, parent, false) : convertView;
		final FileHolder holder;
		if (convertView == null) {
			holder = new FileHolder();
			holder.fileImage = (ImageView) row.findViewById(R.id.file_image);
			holder.fileName = (TextView) row.findViewById(R.id.file_name);
			holder.filePath = (TextView) row.findViewById(R.id.file_path);
			row.setTag(holder);
		} else {
			holder = (FileHolder) row.getTag();
		}
		new Thread(new Runnable() {
			public void run() {
				final Bitmap preview = file.getPreview();
				if (preview != null) {
					holder.fileImage.post(new Runnable() {
						@Override
						public void run() {
							holder.fileImage.setImageBitmap(preview);

						}
					});
					Log.e(TAG, file.getPath());
				} else {

					holder.fileImage.post(new Runnable() {
						@Override
						public void run() {
							LayoutParams params = (LayoutParams) holder.fileImage
									.getLayoutParams();
							params.height = 0;
							holder.fileImage.setLayoutParams(params);

						}
					});
				}
			}
		}).start();
		holder.fileName.setText(file.getName());
		holder.filePath.setText(file.getPath());
		if (!file.getFile().exists()) {
			holder.filePath.setText(R.string.file_vanished);
		} else {
			holder.filePath.setText(file.getPath());
		}
		markSelected(position, row);

		return row;
	}

	private void markSelected(int position, final View row) {
		if (selected.get(position)) {
			row.setBackgroundColor(context.getResources().getColor(
					darkTheme ? R.color.highlighted_text_holo_dark
							: R.color.highlighted_text_holo_light));
		} else {
			row.setBackgroundColor(context.getResources().getColor(
					android.R.color.transparent));
		}
	}

	static class SubtitleHolder {
		TextView title;
		ImageButton button;
		ImageButton cameraButton;
		View divider;
	}

	private View setupSubtitle(ViewGroup parent, String title,
			boolean pencilButton, boolean cameraButton, OnClickListener action,
			View convertView) {
		if (title == null || action == null)
			return new View(context);
		final View subtitle = convertView == null ? inflater.inflate(
				R.layout.task_subtitle, parent, false) : convertView;

		final SubtitleHolder holder;
		if (convertView == null) {
			holder = new SubtitleHolder();
			holder.title = ((TextView) subtitle
					.findViewById(R.id.task_subtitle));
			holder.button = ((ImageButton) subtitle
					.findViewById(R.id.task_subtitle_button));
			holder.cameraButton = ((ImageButton) subtitle
					.findViewById(R.id.task_subtitle_camera_button));
			holder.divider = subtitle.findViewById(R.id.item_separator);
			subtitle.setTag(holder);
		} else {
			holder = (SubtitleHolder) subtitle.getTag();
		}
		holder.title.setText(title);
		holder.button.setOnClickListener(action);

		if (cameraButton) {

			holder.cameraButton.setVisibility(View.VISIBLE);
			if (cameraButtonClick != null)
				holder.cameraButton.setOnClickListener(cameraButtonClick);
		} else {
			holder.cameraButton.setVisibility(View.INVISIBLE);
		}
		if (pencilButton) {
			if (editContent) {
				holder.button.setImageDrawable(context.getResources()
						.getDrawable(android.R.drawable.ic_menu_save));
			} else {
				holder.button.setImageDrawable(context.getResources()
						.getDrawable(android.R.drawable.ic_menu_edit));
			}
		} else {
			holder.button.setImageDrawable(context.getResources().getDrawable(
					android.R.drawable.ic_menu_add));
		}
		holder.divider.setBackgroundColor(context.getResources().getColor(
				inactive_color));
		holder.title.setTextColor(context.getResources().getColor(
				inactive_color));
		return subtitle;
	}

	static class ContentHolder {
		TextView taskContent;
		EditText taskContentEdit;
		ViewSwitcher taskContentSwitcher;
		ImageButton editContent;
		View divider;
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.save, menu);
			return true;
		}

		// Called each time the action mode is shown. Always called after
		// onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false; // Return false if nothing is done
		}

		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.save:
				editContent = !editContent;
				saveContent();
				notifyDataSetChanged();
				break;
			case R.id.cancel:
				editContent = !editContent;
				notifyDataSetChanged();
				mode.finish();
			}
			return true;
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			if (editContent
					&& Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {// on
																				// 2.x
																				// you
																				// cannot
																				// get
																				// done
																				// button
				editContent = !editContent;
				saveContent();
				notifyDataSetChanged();
			}
			mActionMode = null;
		}

	};
	private String taskEditText;
	protected int cursorPos;

	@SuppressLint("NewApi")
	private View setupContent(ViewGroup parent, View convertView) {
		final View content = convertView == null ? inflater.inflate(
				R.layout.task_content, parent, false) : convertView;
		final ContentHolder holder;
		if (convertView == null) {
			holder = new ContentHolder();
			holder.taskContent = (TextView) content
					.findViewById(R.id.task_content);
			holder.taskContentSwitcher = (ViewSwitcher) content
					.findViewById(R.id.switcher_content);
			holder.taskContentEdit = (EditText) content
					.findViewById(R.id.task_content_edit);
			holder.taskContentEdit
					.setOnFocusChangeListener(new OnFocusChangeListener() {

						@Override
						public void onFocusChange(View v, boolean hasFocus) {
							if (hasFocus) {
								((MainActivity) context)
										.getWindow()
										.setSoftInputMode(
												WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
							} else {
								((MainActivity) context)
										.getWindow()
										.setSoftInputMode(
												WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
							}

						}
					});
			holder.taskContentEdit.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {

				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					if (editContent) {
						cursorPos = holder.taskContentEdit.getSelectionEnd();
					}

				}

				@Override
				public void afterTextChanged(Editable s) {
					taskEditText = holder.taskContentEdit.getText().toString();

				}
			});
			holder.editContent = (ImageButton) content
					.findViewById(R.id.edit_content);
			holder.editContent.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					editContent = !editContent;
					saveContent();
					notifyDataSetChanged();
				}
			});
			holder.divider = content.findViewById(R.id.item_separator);
			content.setTag(holder);
		} else {
			holder = (ContentHolder) content.getTag();
		}
		while (holder.taskContentSwitcher.getCurrentView().getId() != (editContent ? R.id.task_content_edit
				: R.id.task_content)) {
			holder.taskContentSwitcher.showNext();
		}
		if (editContent) {
			holder.editContent.setImageDrawable(context.getResources()
					.getDrawable(android.R.drawable.ic_menu_save));
			holder.divider.setBackgroundColor(context.getResources().getColor(
					inactive_color));
			editContent = false;// do not record Textchanges
			holder.taskContentEdit.setText(taskEditText);
			holder.taskContentEdit.setSelection(cursorPos == 0
					|| cursorPos > taskEditText.length() ? taskEditText
					.length() : cursorPos);
			Linkify.addLinks(holder.taskContentEdit, Linkify.WEB_URLS);
			holder.taskContentEdit.requestFocus();
			InputMethodManager imm = ((InputMethodManager) context
					.getSystemService(Context.INPUT_METHOD_SERVICE));
			imm.showSoftInput(holder.taskContentEdit,
					InputMethodManager.SHOW_IMPLICIT);
			holder.taskContentEdit.setSelected(true);
			if (mActionMode == null) {
				mActionMode = ((ActionBarActivity) context)
						.startSupportActionMode(mActionModeCallback);
				View doneButton;
				if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
					int doneButtonId = Resources.getSystem().getIdentifier(
							"action_mode_close_button", "id", "android");
					doneButton = ((ActionBarActivity) context)
							.findViewById(doneButtonId);
					if (doneButton != null) {
						doneButton
								.setOnClickListener(new View.OnClickListener() {

									@Override
									public void onClick(View v) {
										saveContent();
										editContent = false;
										notifyDataSetChanged();
										if (mActionMode != null) {
											mActionMode.finish();
										}
									}
								});
					}
				}
			}
			editContent = true;
		} else {
			// Task content
			holder.editContent.setImageDrawable(context.getResources()
					.getDrawable(android.R.drawable.ic_menu_edit));
			if (task.getContent().length() > 0) {
				holder.taskContent.setText(task.getContent());
				taskEditText = task.getContent();
				cursorPos = taskEditText.length();
				Linkify.addLinks(holder.taskContent, Linkify.WEB_URLS);
				holder.divider.setBackgroundColor(context.getResources()
						.getColor(inactive_color));
				holder.taskContent.setTextColor(context.getResources()
						.getColor(
								darkTheme ? android.R.color.white
										: android.R.color.black));
			} else {
				holder.taskContent.setText(R.string.add_content);
				holder.divider.setBackgroundColor(context.getResources()
						.getColor(inactive_color));
				holder.taskContent.setTextColor(context.getResources()
						.getColor(inactive_color));
				taskEditText = "";
				cursorPos = 0;
			}
			InputMethodManager imm = (InputMethodManager) context
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(
					holder.taskContentEdit.getWindowToken(), 0);
		}
		return content;
	}

	static class ReminderHolder {
		TextView taskReminder;
	}

	private View setupReminder(ViewGroup parent, View convertView) {
		final View reminder = convertView == null
				|| convertView.getId() != R.id.task_reminder ? inflater
				.inflate(R.layout.task_reminder, parent, false) : convertView;
		setupReminderView(convertView, reminder);
		return reminder;
	}

	private void setupReminderView(View convertView, final View reminder) {
		if (reminder == null) {
			Log.wtf(TAG, "reminder=null");
			return;
		}
		final ReminderHolder holder;
		if (convertView == null || convertView.getTag() == null) {
			holder = new ReminderHolder();
			holder.taskReminder = (TextView) reminder
					.findViewById(R.id.task_reminder);
			holder.taskReminder.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					TaskDialogHelpers.handleReminder((Activity) context, task,
							new ExecInterface() {

								@Override
								public void exec() {
									notifyDataSetChanged();
									ReminderAlarm.updateAlarms(context);

								}
							});
				}
			});
			reminder.setTag(holder);
		} else {
			holder = (ReminderHolder) reminder.getTag();
		}
		// Task Reminder
		Drawable reminder_img = context.getResources().getDrawable(
				android.R.drawable.ic_menu_recent_history);
		reminder_img.setBounds(0, 1, 42, 42);
		holder.taskReminder.setCompoundDrawables(reminder_img, null,
				getRecurringDrawable(task.getRecurringReminder()), null);
		if (task.getReminder() == null) {
			holder.taskReminder
					.setText(context.getString(R.string.no_reminder));
			holder.taskReminder.setTextColor(context.getResources().getColor(
					inactive_color));
		} else {
			holder.taskReminder.setText(DateTimeHelper.formatDate(task.getReminder(),
					context.getString(R.string.humanDateTimeFormat)));
			holder.taskReminder.setTextColor(context.getResources().getColor(
					inactive_color));
		}
	}

	private Drawable getRecurringDrawable(Recurring recurring) {
		if (recurring != null) {
			Drawable r = context.getResources().getDrawable(
					android.R.drawable.ic_menu_rotate);
			r.setBounds(0, 1, 42, 42);
			return r;
		}
		return null;
	}

	static class DueHolder {
		TextView taskDue;
	}

	private View setupDue(ViewGroup parent, View convertView, int width) {
		if (width < 600) {
			final View due = (convertView == null || convertView.getId() != R.id.task_due) ? inflater
					.inflate(R.layout.task_due, parent, false) : convertView;
			setupDueView(convertView, due);
			return due;
		} else {
			View due_reminder = (convertView == null || convertView.getId() != R.id.wrapper_reminder_due) ? inflater
					.inflate(R.layout.due_reminder_row, parent, false)
					: convertView;
			View due = due_reminder.findViewById(R.id.wrapper_due);
			View reminder = due_reminder.findViewById(R.id.wrapper_reminder);
			setupDueView(due, due);
			setupReminderView(reminder, reminder);
			return due_reminder;
		}
	}

	private void setupDueView(View convertView, final View due) {
		if (due == null) {
			Log.wtf(TAG, "due=null");
			return;
		}
		final DueHolder holder;
		if (convertView == null || convertView.getTag() == null) {
			holder = new DueHolder();
			holder.taskDue = (TextView) due.findViewById(R.id.task_due);
			holder.taskDue.setOnClickListener(new View.OnClickListener() {

				@TargetApi(Build.VERSION_CODES.HONEYCOMB)
				@Override
				public void onClick(View v) {
					mIgnoreTimeSet = false;
					final Calendar due = (task.getDue() == null ? new GregorianCalendar()
							: task.getDue());
					final View content = ((Activity) context)
							.getLayoutInflater().inflate(
									R.layout.datepicker_dialog, null);
					final DatePicker dp = (DatePicker) content
							.findViewById(R.id.dialog_datePicker);
					Spinner recurrence = (Spinner) content
							.findViewById(R.id.add_reccuring);
					TaskDialogHelpers.handleRecurrence(context, task,
							recurrence, true);

					final SimpleDateFormat dueFormater = new SimpleDateFormat(
							"E, dd. MMMM yyyy", Locale.getDefault());
					final AlertDialog dialog = new AlertDialog.Builder(context)
							.setTitle(dueFormater.format(due.getTime()))
							.setPositiveButton(android.R.string.ok,
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											if (which == DialogInterface.BUTTON_POSITIVE) {
												if (mIgnoreTimeSet)
													return;
												task.setDue(new GregorianCalendar(
														dp.getYear(), dp
																.getMonth(),
														dp.getDayOfMonth()));
												((MainActivity) context)
														.saveTask(task);
												holder.taskDue
														.setText(new SimpleDateFormat(
																context.getString(R.string.dateFormat),
																Locale.getDefault())
																.format(task
																		.getDue()
																		.getTime()));

											}
										}
									})
							.setNegativeButton(R.string.no_date,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog1,
												int which) {
											if (which == DialogInterface.BUTTON_NEGATIVE) {
												mIgnoreTimeSet = true;
												Log.v(TAG, "cancel");
												task.setDue(null);
												((MainActivity) context)
														.saveTask(task);
												holder.taskDue
														.setText(R.string.no_date);
											}
										}
									}).setView(content).show();
					dp.init(due.get(Calendar.YEAR), due.get(Calendar.MONTH),
							due.get(Calendar.DAY_OF_MONTH),
							new OnDateChangedListener() {

								@Override
								public void onDateChanged(DatePicker view,
										int year, int monthOfYear,
										int dayOfMonth) {
									due.set(year, monthOfYear, dayOfMonth);
									dialog.setTitle(dueFormater.format(due
											.getTime()));
								}
							});
				}
			});
			due.setTag(holder);
		} else {
			holder = (DueHolder) due.getTag();
		}
		// Task due
		Drawable dueImg = context.getResources().getDrawable(
				android.R.drawable.ic_menu_today);
		dueImg.setBounds(0, 1, 42, 42);
		holder.taskDue.setCompoundDrawables(dueImg, null,
				getRecurringDrawable(task.getRecurring()), null);
		if (task.getDue() == null) {
			holder.taskDue.setText(context.getString(R.string.no_date));
			holder.taskDue.setTextColor(context.getResources().getColor(
					inactive_color));
		} else {
			holder.taskDue.setText(DateTimeHelper.formatDate(context, task.getDue()));
			holder.taskDue.setTextColor(context.getResources().getColor(
					TaskHelper.getTaskDueColor(task.getDue(), task.isDone())));
		}
	}

	static class HeaderHolder {
		TextView taskName;
		CheckBox taskDone;
		TextView taskPrio;
		ViewSwitcher switcher;
		EditText txt;
	}

	private View setupHeader(ViewGroup parent, View convertView) {
		// Task Name
		final View header = convertView == null ? inflater.inflate(
				R.layout.task_head_line, null, false) : convertView;
		final HeaderHolder holder;
		if (convertView == null) {
			holder = new HeaderHolder();
			holder.taskName = (TextView) header.findViewById(R.id.task_name);
			holder.taskDone = (CheckBox) header.findViewById(R.id.task_done);
			holder.taskPrio = (TextView) header.findViewById(R.id.task_prio);
			holder.switcher = (ViewSwitcher) header
					.findViewById(R.id.switch_name);
			holder.txt = (EditText) header.findViewById(R.id.edit_name);
			holder.taskName.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (((MainActivity) context).isTablet) {
						((EditText) ((MainActivity) context)
								.findViewById(R.id.tasks_new))
								.setOnFocusChangeListener(null);
					}
					holder.switcher.showNext(); // or switcher.showPrevious();
					CharSequence name = holder.taskName.getText();
					holder.txt.setText(name);
					holder.txt
							.setOnFocusChangeListener(new OnFocusChangeListener() {

								@Override
								public void onFocusChange(View v,
										boolean hasFocus) {
									InputMethodManager imm = (InputMethodManager) context
											.getSystemService(Context.INPUT_METHOD_SERVICE);
									if (hasFocus) {
										imm.showSoftInput(
												holder.txt,
												InputMethodManager.SHOW_IMPLICIT);
									} else {
										imm.showSoftInput(
												holder.txt,
												InputMethodManager.HIDE_IMPLICIT_ONLY);
									}

								}
							});
					holder.txt.requestFocus();
					holder.txt
							.setOnEditorActionListener(new OnEditorActionListener() {
								public boolean onEditorAction(TextView v,
										int actionId, KeyEvent event) {
									if (actionId == EditorInfo.IME_ACTION_DONE) {
										EditText txt = (EditText) header
												.findViewById(R.id.edit_name);
										InputMethodManager imm = (InputMethodManager) context
												.getSystemService(Context.INPUT_METHOD_SERVICE);
										task.setName(txt.getText().toString());
										((MainActivity) context).saveTask(task);
										holder.taskName.setText(task.getName());
										txt.setOnFocusChangeListener(null);
										imm.hideSoftInputFromWindow(
												txt.getWindowToken(), 0);
										holder.switcher.showPrevious();

										return true;
									}
									return false;
								}

							});
					holder.txt.setSelection(name.length());
				}
			});
			holder.taskPrio.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					TaskDialogHelpers.handlePriority(context, task,
							new ExecInterface() {
								@Override
								public void exec() {
									((MainActivity) context)
											.updatesForTask(task);
									setPrio(holder.taskPrio, task);

								}
							});
				}
			});
			header.setTag(holder);
		} else {
			holder = (HeaderHolder) header.getTag();
			holder.taskDone.setOnCheckedChangeListener(null);
		}

		String tname = task.getName();
		holder.taskName.setText(tname == null ? "" : tname);
		if (((MainActivity) context).isTablet)
			holder.taskName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);

		if (holder.switcher.getCurrentView().getId() == R.id.edit_name
				&& !task.getName().equals(holder.txt.getText().toString())) {
			holder.switcher.showPrevious();
		}

		// Task done
		holder.taskDone.setChecked(task.isDone());
		holder.taskDone
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						task.setDone(isChecked);
						((MainActivity) context).saveTask(task);
						ReminderAlarm.updateAlarms(context);
						((MainActivity) context).getListFragment().update();
					}
				});

		// Task priority
		setPrio(holder.taskPrio, task);
		return header;
	}

	protected void setPrio(TextView Task_prio, Task task) {
		Task_prio.setText("" + task.getPriority());

		GradientDrawable bg = (GradientDrawable) Task_prio.getBackground();
		bg.setColor(TaskHelper.getPrioColor(task.getPriority(), context));

	}

	public void setData(Task t) {
		if (t == null) {
			Log.wtf(TAG, "task null");
			return;
		}
		List<Pair<Integer, Integer>> generateData = generateData(t);
		super.changeData(generateData);
		task = t;
		subtasks = task.getSubtasks();
		files = task.getFiles();
		notifyDataSetInvalidated();
	}

	public List<Pair<Integer, Integer>> getData() {
		return data;
	}

	public Task getTask() {
		return task;
	}

	private static List<Pair<Integer, Integer>> generateData(Task task) {
		List<Pair<Integer, Integer>> data = new ArrayList<Pair<Integer, Integer>>();
		data.add(new Pair<Integer, Integer>(TYPE.HEADER, 0));
		data.add(new Pair<Integer, Integer>(TYPE.DUE, 0));
		data.add(new Pair<Integer, Integer>(TYPE.REMINDER, 0));
		data.add(new Pair<Integer, Integer>(TYPE.CONTENT, 0));
		data.add(new Pair<Integer, Integer>(TYPE.SUBTITLE, SUBTITLE_SUBTASKS));
		int subtaskCount = task == null ? 0 : task.getSubtaskCount();
		for (int i = 0; i < subtaskCount; i++) {
			data.add(new Pair<Integer, Integer>(TYPE.SUBTASK, i));
		}
		data.add(new Pair<Integer, Integer>(TYPE.SUBTITLE, SUBTITLE_FILES));
		int fileCount = FileMirakel.getFileCount(task);
		for (int i = 0; i < fileCount; i++)
			data.add(new Pair<Integer, Integer>(TYPE.FILE, i));
		return data;
	}

	public void setcameraButtonClick(OnClickListener cameraButtonClick) {
		this.cameraButtonClick = cameraButtonClick;
	}

}
