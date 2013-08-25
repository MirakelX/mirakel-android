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
package de.azapps.mirakel.main_activity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.PagerAdapter;
import de.azapps.mirakel.helper.ChangeLog;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.ListDialogHelpers;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SearchList;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakel.static_activities.CreditsActivity;
import de.azapps.mirakel.static_activities.SettingsActivity;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakelandroid.R;

/**
 * @see "https://thepseudocoder.wordpress.com/2011/10/13/android-tabs-viewpager-swipe-able-tabs-ftw/"
 * @author az
 * 
 */
public class MainActivity extends ActionBarActivity implements
		ViewPager.OnPageChangeListener {

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	private PagerAdapter mPagerAdapter;
	protected ListFragment listFragment;
	protected TasksFragment tasksFragment;
	protected TasksFragment tasksFragment_l;
	protected TasksFragment tasksFragment_r;
	protected TaskFragment taskFragment;
	private Menu menu;
	private Task currentTask;
	private ListMirakel currentList;
	private List<ListMirakel> lists;
	private AlertDialog taskMoveDialog;
	protected boolean isTablet;

	protected static final int LIST_FRAGMENT = 0, TASKS_FRAGMENT = 1,
			TASK_FRAGMENT = 2;
	protected static final int RESULT_SPEECH_NAME = 1, RESULT_SPEECH = 3,
			RESULT_SETTINGS = 4;
	private static final String TAG = "MainActivity";

	public static String EXTRA_ID = "de.azapps.mirakel.EXTRA_TASKID",
			SHOW_TASK = "de.azapps.mirakel.SHOW_TASK",
			TASK_DONE = "de.azapps.mirakel.TASK_DONE",
			TASK_LATER = "de.azapps.mirakel.TASK_LATER",
			SHOW_LIST = "de.azapps.mirakel.SHOW_LIST",
			SHOW_LISTS = "de.azapps.mirakel.SHOW_LISTS",
			SHOW_LIST_FROM_WIDGET = "de.azapps.mirakel.SHOW_LIST_FROM_WIDGET",
			ADD_TASK_FROM_WIDGET = "de.azapps.mirakel.ADD_TASK_FROM_WIDGET",
			SHOW_TASK_FROM_WIDGET = "de.azapps.mirakel.SHOW_TASK_FROM_WIDGET";
	public SharedPreferences preferences;

	protected int currentPosition = TASKS_FRAGMENT;
	private Parcelable tasksState, listState;
	private boolean darkTheme;
	private boolean isResumend;
	private Intent startIntent;

	public static boolean updateTasksUUID = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		darkTheme = preferences.getBoolean("DarkTheme", false);
		if (darkTheme)
			setTheme(R.style.AppBaseThemeDARK);
		isTablet = getResources().getBoolean(R.bool.isTablet);
		if (!preferences.contains("highlightSelected")) {
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("highlightSelected", isTablet);
			editor.commit();
		}
		if (!preferences.contains("startupAllLists")) {
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("startupAllLists", false);
			editor.putString("startupList", "" + SpecialList.first().getId());
			editor.commit();
		}
		setContentView(R.layout.activity_main);
		mPagerAdapter = null;
		// Show ChangeLog
		ChangeLog cl = new ChangeLog(this);
		if (cl.firstRun()) {
			cl.getLogDialog().show();
		}
		// currentList=preferences.getInt("s", defValue)
		setupLayout();
		isResumend = false;

		if (MainActivity.updateTasksUUID) {
			List<Task> tasks = Task.all();
			for (Task t : tasks) {
				t.setUUID(java.util.UUID.randomUUID().toString());
				try {
					t.save();
				} catch (NoSuchListException e) {
					Log.wtf(TAG, "WTF? No such list");
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		updateLists();
		getMenuInflater().inflate(R.menu.main, menu);
		this.menu = menu;
		onPageSelected(TASKS_FRAGMENT);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_delete:
			handleDestroyTask(currentTask);
			return true;
		case R.id.menu_move:
			handleMoveTask(currentTask);
			return true;

		case R.id.list_delete:
			handleDestroyList(currentList);
			return true;
		case R.id.task_sorting:

			currentList = ListDialogHelpers.handleSortBy(this, currentList,
					new Helpers.ExecInterface() {

						@Override
						public void exec() {
							if (isTablet) {
								tasksFragment_l.updateList();
								tasksFragment_r.updateList();
							} else
								tasksFragment.updateList();
							listFragment.update();
						}
					}, null);
			return true;
		case R.id.menu_new_list:
			listFragment.editList(null);
			return true;
		case R.id.menu_sort_lists:
			boolean t = !item.isChecked();
			listFragment.enable_drop(t);
			item.setChecked(t);
			return true;
		case R.id.menu_settings_list:
		case R.id.menu_settings_task:
		case R.id.menu_settings_tasks:
			Intent intent = new Intent(MainActivity.this,
					SettingsActivity.class);
			startActivityForResult(intent, RESULT_SETTINGS);
			break;
		case R.id.menu_sync_now_list:
		case R.id.menu_sync_now_task:
		case R.id.menu_sync_now_tasks:
			Bundle bundle = new Bundle();
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, true);
			// bundle.putBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE,true);
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
			ContentResolver.requestSync(null, Mirakel.AUTHORITY_TYP, bundle);
			break;
		case R.id.menu_credits_list:
		case R.id.menu_credits_task:
		case R.id.menu_credits_tasks:
			Intent creditsIntent = new Intent(MainActivity.this,
					CreditsActivity.class);
			startActivity(creditsIntent);
			break;
		case R.id.share_task:
			Helpers.share(this, getCurrentTask());
			break;
		case R.id.share_list:
			Helpers.share(this, getCurrentList());
			break;
		case R.id.search:
			onSearchRequested();
			break;
		case R.id.menu_contact_list:
		case R.id.menu_contact_task:
		case R.id.menu_contact_tasks:
			Helpers.contact(getApplicationContext());
			break;
		case R.id.menu_kill_button:
			// Only Close
			NotificationService.stop(this);
			if (startService(new Intent(MainActivity.this,
					NotificationService.class)) != null) {
				stopService(new Intent(MainActivity.this,
						NotificationService.class));
			}
			finish();
		case R.id.menu_undo:
			Helpers.undoLast(this);
			currentList=ListMirakel.getList(currentList.getId());
			currentTask=Task.get(currentTask.getId());
			if(currentPosition==TASK_FRAGMENT)
				setCurrentTask(getCurrentTask());
			else{
				listFragment.getAdapter().changeData(ListMirakel.all());
				listFragment.getAdapter().notifyDataSetChanged();
				if(isTablet){
					tasksFragment_l.getAdapter().changeData(getCurrentList().tasks(), getCurrentList().getId());
					tasksFragment_l.getAdapter().notifyDataSetChanged();
					tasksFragment_r.getAdapter().changeData(getCurrentList().tasks(), getCurrentList().getId());
					tasksFragment_r.getAdapter().notifyDataSetChanged();
				}else{
					tasksFragment.getAdapter().changeData(getCurrentList().tasks(), getCurrentList().getId());
					tasksFragment.getAdapter().notifyDataSetChanged();
					if(currentPosition==TASKS_FRAGMENT)
						setCurrentList(getCurrentList());
				}
			}
			ReminderAlarm.updateAlarms(this);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// outState.putString("tab", mTabHost.getCurrentTabTag()); // save the
		// tab
		// selected
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onPageScrolled(int position, float positionOffset,
			int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		loadMenu(position);

	}

	public void loadMenu(int position) {
		if (taskFragment != null && taskFragment.getView() != null) {
			final InputMethodManager imm = (InputMethodManager) this
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(
					taskFragment.getView().getWindowToken(), 0);
		}
		if (menu == null)
			return;
		int newmenu;
		if (currentPosition == TASKS_FRAGMENT) {
			tasksState = tasksFragment.getState();
		} else if (currentPosition == LIST_FRAGMENT) {
			listState = listFragment.getState();
		}
		switch (position) {
		case LIST_FRAGMENT:
			if (!isTablet)
				newmenu = R.menu.activity_list;
			else
				newmenu = R.menu.tablet_left;
			getSupportActionBar().setTitle(getString(R.string.list_title));
			if (listState != null)
				listFragment.setState(listState);
			break;
		case TASKS_FRAGMENT:
			listFragment.enable_drop(false);
			if (!isTablet)
				newmenu = R.menu.tasks;
			else
				newmenu = R.menu.tablet_right;
			if (currentList == null)
				return;
			getSupportActionBar().setTitle(currentList.getName());
			if (tasksState != null && currentPosition != LIST_FRAGMENT)
				tasksFragment.setState(tasksState);
			break;
		case TASK_FRAGMENT:
			newmenu = R.menu.activity_task;
			taskFragment.update();
			getSupportActionBar().setTitle(currentTask.getName());
			break;
		default:
			Toast.makeText(getApplicationContext(), "Where are the dragons?",
					Toast.LENGTH_LONG).show();
			return;
		}
		currentPosition = position;

		// Configure to use the desired menu

		menu.clear();
		MenuInflater inflater = getMenuInflater();

		inflater.inflate(newmenu, menu);

		if (preferences.getBoolean("syncUse", false) == false) {
			MenuItem mitem;
			mitem = menu.findItem(R.id.menu_sync_now_list);
			if (mitem == null)
				mitem = menu.findItem(R.id.menu_sync_now_task);
			if (mitem == null)
				mitem = menu.findItem(R.id.menu_sync_now_tasks);
			mitem.setVisible(false);

		}
		menu.findItem(R.id.menu_kill_button).setVisible(preferences.getBoolean("KillButton", false));

	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RESULT_SETTINGS) {
			listFragment.update();
			if (!preferences.getBoolean("highlightSelected", isTablet)
					&& (oldClickedList != null || oldClickedTask == null)) {
				clearAllHighlights();
			}
			if (darkTheme != preferences.getBoolean("DarkTheme", false)) {
				finish();
				if (startIntent == null) {
					startIntent = new Intent(MainActivity.this,
							MainActivity.class);
					startIntent.setAction(MainActivity.SHOW_LISTS);
					Log.wtf(TAG, "startIntent is null by switching theme");

				}
				startActivity(startIntent);
			}
			loadMenu(mViewPager.getCurrentItem());
			return;
		}
		if (resultCode == RESULT_OK && null != data) {
			ArrayList<String> text = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			switch (requestCode) {
			case RESULT_SPEECH_NAME:
				((EditText) findViewById(R.id.edit_name)).setText(text.get(0));
				break;
			case RESULT_SPEECH:
				if (resultCode == RESULT_OK && null != data) {
					((EditText) tasksFragment.view.findViewById(R.id.tasks_new))
							.setText(text.get(0));
				}
				break;
			}
		}
	}

	@Override
	public void onBackPressed() {
		switch (mViewPager.getCurrentItem()) {
		case TASKS_FRAGMENT:
			mViewPager.setCurrentItem(LIST_FRAGMENT);
			break;
		case TASK_FRAGMENT:
			mViewPager.setCurrentItem(TASKS_FRAGMENT);
			break;
		default:
			super.onBackPressed();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (isResumend)
			setupLayout();
		isResumend = true;
		showMessageFromSync();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		taskFragment.setActivity(this);
		listFragment.setActivity(this);
		tasksFragment.setActivity(this);
	}

	// Fix Intent-behavior
	// default is not return new Intent by calling getIntent
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		Log.d(TAG, "New Indent");
	}

	/**
	 * Initialize the ViewPager and setup the rest of the layout
	 */
	private void setupLayout() {
		if (currentList == null)
			setCurrentList(SpecialList.firstSpecial());
		// Initialize ViewPager
		if (!isResumend)
			intializeViewPager();
		NotificationService.updateNotificationAndWidget(this);
		startIntent = getIntent();
		if (startIntent == null || startIntent.getAction() == null) {

		} else if (startIntent.getAction().equals(SHOW_TASK)) {
			Task task = Helpers.getTaskFromIntent(startIntent);
			if (task != null) {
				Log.d(TAG, "TaskID: " + task.getId());
				currentList = task.getList();
				setCurrentTask(task);
			} else {
				Log.d(TAG, "task null");
			}
		} else if (startIntent.getAction().equals(Intent.ACTION_SEND)
				&& startIntent.getType().equals("text/plain")) {
			final String content = startIntent
					.getStringExtra(Intent.EXTRA_TEXT);
			final String subject = startIntent
					.getStringExtra(Intent.EXTRA_SUBJECT);
			if (content != null || subject != null) {
				int id = getCurrentList().getId();
				if (preferences.getBoolean("importDefaultList", false)) {
					id = preferences.getInt("defaultImportList", id);
					Task task = Task
							.newTask(subject == null ? "" : subject, id);
					task.setContent(content == null ? "" : content);
					safeSafeTask(task);
					setCurrentTask(task);
					tasksFragment.updateList(true);
					listFragment.update();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(R.string.import_to);
					List<CharSequence> items = new ArrayList<CharSequence>();
					final List<Integer> list_ids = new ArrayList<Integer>();
					int currentItem = 0;
					for (ListMirakel list : ListMirakel.all()) {
						if (list.getId() > 0) {
							items.add(list.getName());
							list_ids.add(list.getId());
						}
					}
					builder.setSingleChoiceItems(
							items.toArray(new CharSequence[items.size()]),
							currentItem, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int item) {
									Task task = Task.newTask(
											subject == null ? "" : subject,
											list_ids.get(item));
									task.setContent(content == null ? ""
											: content);
									safeSafeTask(task);
									setCurrentTask(task);
									tasksFragment.updateList(true);
									listFragment.update();
									dialog.dismiss();
								}
							});
					builder.create().show();
				}
			}
		} else if (startIntent.getAction().equals(TASK_DONE)
				|| startIntent.getAction().equals(TASK_LATER)) {
			handleReminder(startIntent);
		} else if (startIntent.getAction().equals(SHOW_LIST)
				|| startIntent.getAction().equals(SHOW_LIST_FROM_WIDGET)) {

			int listId = startIntent.getIntExtra(EXTRA_ID, 0);
			ListMirakel list = ListMirakel.getList(listId);
			if (list == null)
				list = SpecialList.firstSpecial();
			Log.d(TAG, list.getName() + " " + listId);
			setCurrentList(list);
		} else if (startIntent.getAction().equals(SHOW_LISTS)) {
			mViewPager.setCurrentItem(LIST_FRAGMENT);
		} else if (startIntent.getAction().equals(Intent.ACTION_SEARCH)) {
			String query = startIntent.getStringExtra(SearchManager.QUERY);
			search(query);
		} else if (startIntent.getAction().equals(ADD_TASK_FROM_WIDGET)) {
			int listId = startIntent.getIntExtra(EXTRA_ID, 0);
			setCurrentList(ListMirakel.getList(listId));
			tasksFragment.focusNew();

		} else {
			mViewPager.setCurrentItem(TASKS_FRAGMENT);
		}
		setIntent(null);
	}

	private void safeSafeTask(Task task) {
		try {
			task.save();
		} catch (NoSuchListException e) {
			Toast.makeText(getApplicationContext(), R.string.list_vanished,
					Toast.LENGTH_LONG).show();
		}
	}

	private void handleReminder(Intent intent) {
		Task task = Helpers.getTaskFromIntent(intent);
		if (task == null)
			return;
		if (intent.getAction() == TASK_DONE) {
			task.setDone(true);
			safeSafeTask(task);
			Toast.makeText(this,
					getString(R.string.reminder_notification_done_confirm),
					Toast.LENGTH_LONG).show();
		} else if (intent.getAction() == TASK_LATER) {
			GregorianCalendar reminder = new GregorianCalendar();
			int addMinutes = preferences.getInt("alarm_later", 15);
			reminder.add(Calendar.MINUTE, addMinutes);
			task.setReminder(reminder);
			safeSafeTask(task);
			Toast.makeText(
					this,
					getString(R.string.reminder_notification_later_confirm,
							addMinutes), Toast.LENGTH_LONG).show();
		}
		ReminderAlarm.updateAlarms(this);
		listFragment.update();
		setCurrentList(task.getList());
		setCurrentTask(task);
	}

	/**
	 * Update the internal List of Lists (e.g. for the Move Task dialog)
	 */
	public void updateLists() {
		this.lists = ListMirakel.all(false);
	}

	/**
	 * Handle the actions after clicking on a destroy-list button
	 * 
	 * @param list
	 */
	public void handleDestroyList(final ListMirakel list) {
		new AlertDialog.Builder(this)
				.setTitle(list.getName())
				.setMessage(this.getString(R.string.list_delete_content))
				.setPositiveButton(this.getString(R.string.Yes),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								list.destroy();
								listFragment.update();
								if (getCurrentList().getId() == list.getId()) {
									setCurrentList(SpecialList.firstSpecial());
								}
							}
						})
				.setNegativeButton(this.getString(R.string.no),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								// do nothing
							}
						}).show();
	}

	/**
	 * Handle the actions after clicking on a destroy-task button
	 * 
	 * @param task
	 */
	public void handleDestroyTask(final Task task) {
		final MainActivity main=this;
		new AlertDialog.Builder(this)
				.setTitle(task.getName())
				.setMessage(this.getString(R.string.task_delete_content))
				.setPositiveButton(this.getString(R.string.Yes),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								task.delete();
								setCurrentList(currentList);
								ReminderAlarm.updateAlarms(main);
							}
						})
				.setNegativeButton(this.getString(R.string.no),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								// do nothing
							}
						}).show();
	}

	/**
	 * Handle the actions after clicking on a move task button
	 * 
	 * @param task
	 */
	public void handleMoveTask(final Task task) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_move);
		List<CharSequence> items = new ArrayList<CharSequence>();
		final List<Integer> list_ids = new ArrayList<Integer>();
		int currentItem = 0, i = 0;
		for (ListMirakel list : lists) {
			if (list.getId() > 0) {
				items.add(list.getName());
				if (task.getList().getId() == list.getId()) {
					currentItem = i;
				}
				list_ids.add(list.getId());
				++i;
			}
		}

		builder.setSingleChoiceItems(
				items.toArray(new CharSequence[items.size()]), currentItem,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						task.setList(ListMirakel.getList(list_ids.get(item)));
						safeSafeTask(task);
						/*
						 * There are 3 possibilities how to handle the post-move
						 * of a task:
						 * 
						 * 1: update the currentList to the List, the task was
						 * 
						 * moved to setCurrentList(task.getList());
						 * 
						 * 2: update the tasksView but not update the taskView:
						 * 
						 * tasksFragment.updateList(); tasksFragment.update();
						 * 
						 * 3: Set the currentList to the old List
						 */
						setCurrentList(getCurrentList());

						listFragment.update();
						taskMoveDialog.dismiss();
					}
				});

		taskMoveDialog = builder.create();
		taskMoveDialog.show();
	}

	/**
	 * Initialize ViewPager
	 */
	private void intializeViewPager() {
		List<Fragment> fragments = new Vector<Fragment>();
		listFragment = new ListFragment();
		listFragment.setActivity(this);
		fragments.add(listFragment);
		tasksFragment_r = new TasksFragment();
		tasksFragment = tasksFragment_r;
		tasksFragment.setActivity(this);
		fragments.add(tasksFragment);
		if (!isTablet) {
			taskFragment = new TaskFragment();
			taskFragment.setActivity(this);
			fragments.add(taskFragment);
		}
		this.mPagerAdapter = new PagerAdapter(
				super.getSupportFragmentManager(), fragments);
		//
		this.mViewPager = (ViewPager) super.findViewById(R.id.viewpager);
		this.mViewPager.setAdapter(this.mPagerAdapter);
		this.mViewPager.setOnPageChangeListener(this);
		mViewPager.setOffscreenPageLimit(isTablet ? 1 : 2);

	}

	/**
	 * Return the currently showed tasks
	 * 
	 * @return
	 */
	Task getCurrentTask() {
		return currentTask;
	}

	/**
	 * Set the current task and update the view
	 * 
	 * @param currentTask
	 */
	void setCurrentTask(Task currentTask) {
		setCurrentTask(currentTask, null);
	}

	private View oldClickedTask = null;

	void setCurrentTask(Task currentTask, View currentView) {
		this.currentTask = currentTask;

		if (currentView != null
				&& preferences.getBoolean("highlightSelected", isTablet)) {
			if (oldClickedTask != null) {
				oldClickedTask.setSelected(false);
				oldClickedTask.setBackgroundColor(0x00000000);
			}
			if (isTablet) {
				try {
					ListView leftView = (ListView) tasksFragment_l.getView()
							.findViewById(R.id.tasks_list);
					ListView rightView = (ListView) tasksFragment_r.getView()
							.findViewById(R.id.tasks_list);
					int pressed_color = getResources().getColor(
							R.color.pressed_color);
					int pos_l = leftView.getPositionForView(currentView);
					int pos_r = rightView.getPositionForView(currentView);
					clearHighlighted();

					if (pos_l != -1) {
						leftView.getChildAt(pos_l).setBackgroundColor(
								pressed_color);
						rightView.getChildAt(pos_l).setBackgroundColor(
								pressed_color);
					} else if (pos_r != -1) {
						leftView.getChildAt(pos_r).setBackgroundColor(
								pressed_color);
						rightView.getChildAt(pos_r).setBackgroundColor(
								pressed_color);
					} else {
						Log.wtf(TAG, "View not found");
					}

				} catch (Exception e) {
					Log.wtf(TAG, "Listview not found");
				}
			}
			currentView.setBackgroundColor(getResources().getColor(
					R.color.pressed_color));
			oldClickedTask = currentView;
		}

		if (taskFragment != null) {
			boolean smooth = mViewPager.getCurrentItem() != TASK_FRAGMENT;
			taskFragment.update();
			// Fix buggy behavior
			mViewPager.setCurrentItem(LIST_FRAGMENT, false);
			mViewPager.setCurrentItem(TASK_FRAGMENT, false);
			mViewPager.setCurrentItem(LIST_FRAGMENT, false);
			mViewPager.setCurrentItem(TASK_FRAGMENT, smooth);
		}
	}

	private void clearAllHighlights() {
		if (oldClickedList != null) {
			oldClickedList.setSelected(false);
			oldClickedList.setBackgroundColor(0x00000000);
		}
		if (oldClickedTask != null) {
			oldClickedTask.setSelected(false);
			oldClickedTask.setBackgroundColor(0x00000000);
		}
		clearHighlighted();
	}

	private void clearHighlighted() {
		try {
			ListView leftView = (ListView) tasksFragment_l.getView()
					.findViewById(R.id.tasks_list);
			ListView rightView = (ListView) tasksFragment_r.getView()
					.findViewById(R.id.tasks_list);
			int pos_old_l = (leftView).getPositionForView(oldClickedTask);
			int pos_old_r = rightView.getPositionForView(oldClickedTask);
			if (pos_old_l != -1) {
				(leftView).getChildAt(pos_old_l).setBackgroundColor(0x00000000);
				rightView.getChildAt(pos_old_l).setBackgroundColor(0x00000000);
			} else if (pos_old_r != -1) {
				(leftView).getChildAt(pos_old_r).setBackgroundColor(0x00000000);
				rightView.getChildAt(pos_old_r).setBackgroundColor(0x00000000);
			} else {
				Log.wtf(TAG, "View not found");
			}
		} catch (Exception e) {
			Log.wtf(TAG, "Listview not found");
		}
	}

	/**
	 * Return the currently showed List
	 * 
	 * @return
	 */
	ListMirakel getCurrentList() {
		if (currentList == null)
			currentList = SpecialList.firstSpecial();
		return currentList;
	}

	/**
	 * Set the current list and update the views
	 * 
	 * @param currentList
	 */
	void setCurrentList(ListMirakel currentList) {
		setCurrentList(currentList, null);
	}

	private View oldClickedList = null;

	void setCurrentList(ListMirakel currentList, View currentView) {
		if (currentList == null)
			return;
		this.currentList = currentList;
		if (tasksFragment != null) {

			if (!isTablet) {
				tasksFragment.updateList();
				mViewPager.setCurrentItem(TASKS_FRAGMENT);
			} else {
				if (tasksFragment_l != null)
					tasksFragment_l.updateList();
				if (tasksFragment_r != null)
					tasksFragment_r.updateList();
			}
		}
		if (currentView != null
				&& preferences.getBoolean("highlightSelected", isTablet)) {
			clearHighlighted();
			if (oldClickedList != null) {
				oldClickedList.setSelected(false);
				oldClickedList.setBackgroundColor(0x00000000);
			}
			currentView.setBackgroundColor(getResources().getColor(
					R.color.pressed_color));
			oldClickedList = currentView;
		}
		List<Task> currentTasks = currentList.tasks(preferences.getBoolean(
				"showDone", true));
		if (currentTasks.size() == 0) {
			currentTask = Task.getDummy(getApplicationContext());
		} else {
			currentTask = currentTasks.get(0);
		}
		if (taskFragment != null) {
			taskFragment.update();
		}
		if (currentPosition == TASKS_FRAGMENT)
			getSupportActionBar().setTitle(currentList.getName());

	}

	/**
	 * Ugly Wrapper TODO make it more beautiful
	 * 
	 * @param task
	 */
	void saveTask(Task task) {
		Log.v(TAG, "Saving task… (task:" + task.getId() + " – current:"
				+ currentTask.getId());
		safeSafeTask(task);
		updatesForTask(task);
	}

	/**
	 * Executes some View–Updates if a Task was changed
	 * 
	 * @param task
	 */
	void updatesForTask(Task task) {
		if (task.getId() == currentTask.getId()) {
			currentTask = task;
			taskFragment.update();
		}
		if (isTablet) {
			tasksFragment_l.updateList(false);
			tasksFragment_l.update(false);
			tasksFragment_r.updateList(false);
			tasksFragment_r.update(false);
		} else {
			tasksFragment.updateList(false);
			tasksFragment.update(false);
		}
		listFragment.update();
		NotificationService.updateNotificationAndWidget(this);

	}

	/**
	 * Returns the ListFragment
	 * 
	 * @return
	 */
	public ListFragment getListFragment() {
		return listFragment;
	}

	private void search(String query) {
		setCurrentList(new SearchList(this, query));
		mViewPager.setCurrentItem(TASKS_FRAGMENT);
	}

	public void setTaskFragment(TaskFragment taskFragment) {
		this.taskFragment = taskFragment;
	}

	protected void showMessageFromSync() {
		CharSequence messageFromSync = SyncAdapter.getLastMessage();
		if (messageFromSync != null) {
			Toast.makeText(getApplicationContext(), messageFromSync,
					Toast.LENGTH_SHORT).show();
		}
	}
}
