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
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SearchList;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakel.services.NotificationService;
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
	protected TaskFragment taskFragment;
	private Menu menu;
	private Task currentTask;
	private ListMirakel currentList;
	private List<ListMirakel> lists;
	private AlertDialog taskMoveDialog;
	protected boolean isTablet;
	private int baseList;
	private boolean highlightSelected;
	private DrawerLayout mDrawerLayout;

	protected static final int TASKS_FRAGMENT = 0, TASK_FRAGMENT = 1;
	protected static final int RESULT_SPEECH_NAME = 1, RESULT_SPEECH = 3,
			RESULT_SETTINGS = 4, RESULT_ADD_FILE = 5;
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
	public boolean darkTheme;
	private boolean isResumend;
	private Intent startIntent;

	public static boolean updateTasksUUID = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		darkTheme = preferences.getBoolean("DarkTheme", false);
		highlightSelected = preferences.getBoolean("highlightSelected",
				isTablet);
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
		/*
		 * final List<ListMirakel> values = ListMirakel.all(); ListAdapter
		 * adapter = new ListAdapter(this, R.layout.lists_row, values, false);
		 * ListView list_drawer=(ListView) findViewById(R.id.list_drawer);
		 * list_drawer.setAdapter(adapter);
		 */
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
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
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
							setCurrentList(currentList);
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
		case R.id.share_task:
			Helpers.share(this, getCurrentTask());
			break;
		case R.id.share_list:
			Helpers.share(this, getCurrentList());
			break;
		case R.id.search:
			onSearchRequested();
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
			currentList = ListMirakel.getList(currentList.getId());
			currentTask = Task.get(currentTask.getId());
			if (currentPosition == TASK_FRAGMENT)
				setCurrentTask(getCurrentTask());
			else {
				listFragment.getAdapter().changeData(ListMirakel.all());
				listFragment.getAdapter().notifyDataSetChanged();
				tasksFragment.getAdapter().changeData(getCurrentList().tasks(),
						getCurrentList().getId());
				tasksFragment.getAdapter().notifyDataSetChanged();
				if (!isTablet && currentPosition == TASKS_FRAGMENT)
					setCurrentList(getCurrentList());
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
		loadMenu(position, true);
	}

	public void loadMenu(int position, boolean setPosition) {
		if (taskFragment != null && taskFragment.getView() != null) {
			final InputMethodManager imm = (InputMethodManager) this
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(
					taskFragment.getView().getWindowToken(), 0);
		}
		if (menu == null)
			return;
		int newmenu;
		switch (position) {
		case -1:
			newmenu = R.menu.activity_list;
			getSupportActionBar().setTitle(getString(R.string.list_title));
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
			break;
		case TASK_FRAGMENT:
			newmenu = R.menu.activity_task;
			taskFragment.update(currentTask);
			getSupportActionBar().setTitle(currentTask.getName());
			break;
		default:
			Toast.makeText(getApplicationContext(), "Where are the dragons?",
					Toast.LENGTH_LONG).show();
			return;
		}
		if (setPosition)
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
		menu.findItem(R.id.menu_kill_button).setVisible(
				preferences.getBoolean("KillButton", false));

	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(TAG, "Result:" + requestCode);
		if (requestCode == RESULT_SETTINGS) {
			listFragment.update();
			highlightSelected = preferences.getBoolean("highlightSelected",
					isTablet);
			if (!highlightSelected
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
			case RESULT_ADD_FILE:
				final String file_path = Helpers.getPathFromUri(data.getData(),
						this);
				if (FileMirakel.newFile(currentTask, file_path) == null) {
					Toast.makeText(this, getString(R.string.file_vanished),
							Toast.LENGTH_SHORT).show();
				}else{
					taskFragment.adapter.setData(currentTask);
				}

				break;
			}
		}
	}

	@Override
	public void onBackPressed() {
		switch (mViewPager.getCurrentItem()) {
		case TASKS_FRAGMENT:
			mDrawerLayout.openDrawer(Gravity.LEFT);
			break;
		case TASK_FRAGMENT:
			mViewPager.setCurrentItem(TASKS_FRAGMENT);
			break;
		default:
			super.onBackPressed();
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
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
		listFragment.setActivity(this);
		tasksFragment.setActivity(this);
		mDrawerToggle.onConfigurationChanged(newConfig);
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
			intializeFragments();
		NotificationService.updateNotificationAndWidget(this);
		startIntent = getIntent();
		if (startIntent == null || startIntent.getAction() == null) {

		} else if (startIntent.getAction().equals(SHOW_TASK)) {
			Task task = Helpers.getTaskFromIntent(startIntent);
			if (task != null) {
				Log.d(TAG, "TaskID: " + task.getId());
				setCurrentList(task.getList());
				setCurrentTask(task, true);
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
			mDrawerLayout.openDrawer(Gravity.LEFT);
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

	}

	public void handleDestroyList(final List<ListMirakel> lists) {
		String names = lists.get(0).getName();
		for (int i = 1; i < lists.size(); i++) {
			names += ", " + lists.get(i).getName();
		}
		new AlertDialog.Builder(this)
				.setTitle(
						getResources().getQuantityString(R.plurals.list_delete,
								lists.size()))
				.setMessage(this.getString(R.string.list_delete_content, names))
				.setPositiveButton(this.getString(android.R.string.yes),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								for (ListMirakel list : lists) {
									list.destroy();
									listFragment.update();
									if (getCurrentList().getId() == list
											.getId()) {
										setCurrentList(SpecialList
												.firstSpecial());
									}
								}
							}
						})
				.setNegativeButton(this.getString(android.R.string.no),
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
		List<Task> t = new ArrayList<Task>();
		t.add(task);
		handleDestroyTask(t);
	}

	public void handleDestroyTask(final List<Task> tasks) {

		final MainActivity main = this;
		String names = tasks.get(0).getName();
		for (int i = 1; i < tasks.size(); i++) {
			names += ", " + tasks.get(i).getName();
		}
		new AlertDialog.Builder(this)
				.setTitle(
						this.getResources().getQuantityString(
								R.plurals.task_delete, tasks.size()))
				.setMessage(this.getString(R.string.task_delete_content, names))
				.setPositiveButton(this.getString(android.R.string.yes),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								for (Task t : tasks) {
									t.delete();
								}
								setCurrentList(currentList);
								ReminderAlarm.updateAlarms(main);
							}
						})
				.setNegativeButton(this.getString(android.R.string.no),
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
	 * @param tasks
	 */
	public void handleMoveTask(final Task task) {
		List<Task> t = new ArrayList<Task>();
		t.add(task);
		handleMoveTask(t);
	}

	public void handleMoveTask(final List<Task> tasks) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_move);
		List<CharSequence> items = new ArrayList<CharSequence>();
		final List<Integer> list_ids = new ArrayList<Integer>();
		int currentItem = 0, i = 0;
		for (ListMirakel list : lists) {
			if (list.getId() > 0) {
				items.add(list.getName());
				if (tasks.get(0).getList().getId() == list.getId()
						&& tasks.size() == 1) {
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
						for (Task t : tasks) {
							t.setList(ListMirakel.getList(list_ids.get(item)));
							safeSafeTask(t);
						}
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

	private ActionBarDrawerToggle mDrawerToggle;

	/**
	 * Initialize ViewPager
	 */
	private void intializeFragments() {
		/*
		 * Setup NavigationDrawer
		 */
		if (ListFragment.getSingleton() != null)
			listFragment = ListFragment.getSingleton();
		// listFragment = new ListFragment();
		listFragment.setActivity(this);
		// fragments.add(listFragment);

		List<Fragment> fragments = new Vector<Fragment>();
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
		R.string.list_title, /* "open drawer" description */
		R.string.list_title /* "close drawer" description */
		) {
			public void onDrawerClosed(View view) {
				loadMenu(currentPosition);
			}

			public void onDrawerOpened(View drawerView) {
				loadMenu(-1, false);
			}
		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		/*
		 * Setup other Fragments
		 */
		tasksFragment = new TasksFragment();
		tasksFragment.setActivity(this);
		fragments.add(tasksFragment);
		if (!isTablet) {
			taskFragment = new TaskFragment();
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
		setCurrentTask(currentTask, false);
	}

	private View oldClickedTask = null;

	void highlightCurrentTask(Task currentTask) {
	}

	void highlightCurrentTask(Task currentTask, boolean multiselect) {
		if (taskFragment == null || tasksFragment.getAdapter() == null
				|| currentTask == null)
			return;
		Log.v(TAG, currentTask.getName());
		View currentView = tasksFragment.getAdapter().getViewForTask(
				currentTask);
		if (currentView == null) {
			currentView = tasksFragment.getListView().getChildAt(0);
			Log.v(TAG, "current view is null");
		}

		if (currentView != null && highlightSelected && !multiselect) {
			if (oldClickedTask != null) {
				oldClickedTask.setSelected(false);
				oldClickedTask.setBackgroundColor(0x00000000);
			}
			currentView.setBackgroundColor(getResources().getColor(
					darkTheme ? R.color.highlighted_text_holo_dark
							: R.color.highlighted_text_holo_light));
			oldClickedTask = currentView;
		}
	}

	void setCurrentTask(Task currentTask, boolean switchFragment) {
		this.currentTask = currentTask;

		highlightCurrentTask(currentTask);

		if (taskFragment != null) {
			taskFragment.update(currentTask);
			boolean smooth = mViewPager.getCurrentItem() != TASK_FRAGMENT;
			if (!switchFragment)
				return;
			// Fix buggy behavior
			mViewPager.setCurrentItem(TASKS_FRAGMENT, false);
			mViewPager.setCurrentItem(TASK_FRAGMENT, false);
			mViewPager.setCurrentItem(TASKS_FRAGMENT, false);
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
		if (oldClickedTask == null)
			return;
		try {
			ListView view = (ListView) tasksFragment.getView().findViewById(
					R.id.tasks_list);
			int pos_old = (view).getPositionForView(oldClickedTask);
			if (pos_old != -1) {
				(view).getChildAt(pos_old).setBackgroundColor(0x00000000);
			} else {
				Log.wtf(TAG, "View not found");
			}
		} catch (Exception e) {
			Log.wtf(TAG, "Listview not found");
			Log.e(TAG, Log.getStackTraceString(e));
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

	private View oldClickedList = null;

	/**
	 * Set the current list and update the views
	 * 
	 * @param currentList
	 */
	void setCurrentList(ListMirakel currentList) {
		setCurrentList(currentList, null);
	}

	void setCurrentList(ListMirakel currentList, View currentView) {
		if (currentList == null)
			return;
		this.currentList = currentList;
		if (mDrawerLayout != null)
			mDrawerLayout.closeDrawers();

		List<Task> currentTasks = currentList.tasks(preferences.getBoolean(
				"showDone", true));
		if (currentTasks.size() == 0) {
			currentTask = Task.getDummy(getApplicationContext());
		} else {
			currentTask = currentTasks.get(0);
		}

		if (tasksFragment != null) {
			tasksFragment.updateList();
			if (!isTablet)
				mViewPager.setCurrentItem(TASKS_FRAGMENT);
		}
		if (currentView == null && listFragment != null
				&& listFragment.getAdapter() != null)
			currentView = listFragment.getAdapter().getViewForList(currentList);

		if (currentView != null && highlightSelected) {
			clearHighlighted();
			if (oldClickedList != null) {
				oldClickedList.setSelected(false);
				oldClickedList.setBackgroundColor(0x00000000);
			}
			currentView.setBackgroundColor(getResources().getColor(
					R.color.pressed_color));
			oldClickedList = currentView;
		}
		setCurrentTask(currentTask);
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
			taskFragment.update(task);
		}
		tasksFragment.updateList(false);
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
		baseList = getCurrentList().getId();
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

	public int getBaseList() {
		return baseList;
	}

	public SharedPreferences getPreferences() {
		return preferences;
	}
}
