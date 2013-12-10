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
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import sheetrock.panda.changelog.ChangeLog;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
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
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.ListDialogHelpers;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.SharingHelper;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.helper.UndoHistory;
import de.azapps.mirakel.main_activity.list_fragment.ListFragment;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragment;
import de.azapps.mirakel.main_activity.tasks_fragment.TasksFragment;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SearchList;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakel.static_activities.SettingsActivity;
import de.azapps.mirakel.static_activities.SplashScreenActivity;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.widget.MainWidgetProvider;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.FileUtils;

/**
 * This is our main activity. Here happens nearly everything.
 * 
 * @author az
 * 
 */
public class MainActivity extends ActionBarActivity implements
		ViewPager.OnPageChangeListener {

	// Layout variables
	ViewPager mViewPager;
	private PagerAdapter mPagerAdapter;
	protected ListFragment listFragment;
	protected TaskFragment taskFragment;
	private Menu menu;
	public boolean darkTheme;

	// State variables
	private Task currentTask;
	private ListMirakel currentList;
	private List<ListMirakel> lists;
	protected int				currentPosition;

	// Foo variables (move them out of the MainActivity)
	private boolean highlightSelected;
	private DrawerLayout mDrawerLayout;
	private Uri fileUri;
	// TODO We should do this somehow else
	public static boolean updateTasksUUID = false;

	// Intent variables
	public static final int		LEFT_FRAGMENT		= 0, RIGHT_FRAGMENT = 1;
	public static final int RESULT_SPEECH_NAME = 1, RESULT_SPEECH = 3,
			RESULT_SETTINGS = 4, RESULT_ADD_FILE = 5, RESULT_CAMERA = 6,
			RESULT_ADD_PICTURE = 7;
	public static String EXTRA_ID = "de.azapps.mirakel.EXTRA_TASKID",
			SHOW_TASK = "de.azapps.mirakel.SHOW_TASK",
			SHOW_LIST = "de.azapps.mirakel.SHOW_LIST",
			SHOW_LISTS = "de.azapps.mirakel.SHOW_LISTS",
			SHOW_LIST_FROM_WIDGET = "de.azapps.mirakel.SHOW_LIST_FROM_WIDGET",
			ADD_TASK_FROM_WIDGET = "de.azapps.mirakel.ADD_TASK_FROM_WIDGET",
			SHOW_TASK_FROM_WIDGET = "de.azapps.mirakel.SHOW_TASK_FROM_WIDGET",
			TASK_ID = "de.azapp.mirakel.TASK_ID";
	private Intent startIntent;

	private static final String TAG = "MainActivity";

	// User interaction variables
	private boolean isResumend;
	private boolean closeOnBack = false;
	private Stack<Task> goBackTo = new Stack<Task>();
	private boolean showNavDrawer = false;
	private boolean skipSwipe;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		darkTheme = MirakelPreferences.isDark();
		if (darkTheme)
			setTheme(R.style.AppBaseThemeDARK);
		super.onCreate(savedInstanceState);

		boolean isTablet = MirakelPreferences.isTablet();
		isRTL = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
				&& getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
		currentPosition = getTasksFragmentPosition();
		highlightSelected = MirakelPreferences.highlightSelected();
		if (!MirakelPreferences.containsHighlightSelected()) {
			SharedPreferences.Editor editor = MirakelPreferences.getEditor();
			editor.putBoolean("highlightSelected", isTablet);
			editor.commit();
		}
		if (!MirakelPreferences.containsStartupAllLists()) {
			SharedPreferences.Editor editor = MirakelPreferences.getEditor();
			editor.putBoolean("startupAllLists", false);
			editor.putString("startupList", "" + SpecialList.first().getId());
			editor.commit();
		}
		if (isTablet) {
			setContentView(R.layout.activity_main);
		} else {
			setContentView(R.layout.activity_main);
		}
		mPagerAdapter = null;
		// Show ChangeLog
		ChangeLog cl = new ChangeLog(this);
		if (cl.firstRun()) {
			cl.getLogDialog().show();
			showNavDrawer = true;
		}
		// currentList=preferences.getInt("s", defValue)
		skipSwipe = false;
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
		if (mViewPager.getCurrentItem() != currentPosition) {
			mViewPager.postDelayed(new Runnable() {

				@Override
				public void run() {
					mViewPager.setCurrentItem(currentPosition);

				}
			}, 10);
		}
	}

	public static int getTasksFragmentPosition() {
		if (isRTL) return RIGHT_FRAGMENT;
		return LEFT_FRAGMENT;
	}

	private int getTaskFragmentPosition() {
		if (isRTL) return LEFT_FRAGMENT;
		return RIGHT_FRAGMENT;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		switch (item.getItemId()) {
		case R.id.menu_delete:
			handleDestroyTask(currentTask);
			updateShare();
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
			getListFragment().editList(null);
			return true;
		case R.id.menu_sort_lists:
			boolean t = !item.isChecked();
			getListFragment().enableDrop(t);
			item.setChecked(t);
			return true;
		case R.id.menu_settings:
			Intent intent = new Intent(MainActivity.this,
					SettingsActivity.class);
			startActivityForResult(intent, RESULT_SETTINGS);
			break;
		case R.id.menu_sync_now:
			Bundle bundle = new Bundle();
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, true);
			// bundle.putBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE,true);
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
			ContentResolver.requestSync(null, Mirakel.AUTHORITY_TYP, bundle);
			break;
		case R.id.share_task:
			SharingHelper.share(this, getCurrentTask());
			break;
		case R.id.share_list:
			SharingHelper.share(this, getCurrentList());
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
			Intent killIntent = new Intent(getApplicationContext(),
					SplashScreenActivity.class);
			killIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			killIntent.setAction(SplashScreenActivity.EXIT);
			startActivity(killIntent);
			return false;
		case R.id.menu_undo:
			UndoHistory.undoLast(this);
			updateCurrentListAndTask();
				if (currentPosition == getTaskFragmentPosition())
				setCurrentTask(getCurrentTask());
			else {
				getListFragment().getAdapter().changeData(ListMirakel.all());
				getListFragment().getAdapter().notifyDataSetChanged();
				getTasksFragment().getAdapter().changeData(
						getCurrentList().tasks(), getCurrentList().getId());
				getTasksFragment().getAdapter().notifyDataSetChanged();
				if (!MirakelPreferences.isTablet()
							&& currentPosition == getTasksFragmentPosition())
					setCurrentList(getCurrentList());
			}
			ReminderAlarm.updateAlarms(this);
			break;
		case R.id.mark_as_subtask:
			TaskDialogHelpers.handleSubtask(this, currentTask, null, true);
			break;
		case R.id.menu_task_clone:
			try {
				Task newTask = currentTask.create();
				setCurrentTask(newTask, true);
				getListFragment().update();
				updatesForTask(newTask);
			} catch (NoSuchListException e) {
				Log.wtf(TAG, "List vanished on task cloning");
			}
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private void updateCurrentListAndTask() {
		if (currentTask == null && currentList == null)
			return;
		if (currentTask != null)
			currentTask = Task.get(currentTask.getId());
		else {
			if (currentList != null) {
				List<Task> currentTasks = currentList.tasks(MirakelPreferences
						.showDoneMain());
				if (currentTasks.size() == 0) {
					currentTask = Task.getDummy(getApplicationContext());
				} else {
					currentTask = currentTasks.get(0);
				}
			}
		}
		if (currentList != null) {
			currentList = ListMirakel.getList(currentList.getId());
		} else {
			currentList = currentTask.getList();
		}

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

	public void setSkipSwipe() {
		skipSwipe = true;
	}

	@Override
	public void onPageScrolled(int position, float positionOffset,
			int positionOffsetPixels) {
		if (getTasksFragment() != null
				&& getTasksFragment().getAdapter() != null
				&& MirakelPreferences.swipeBehavior() && !skipSwipe
				&& position == getTasksFragmentPosition()) {
			setCurrentTask(getTasksFragment().getAdapter().lastTouched(), false);
			skipSwipe = true;
		}
		if (positionOffset == 0.0f && position == getTasksFragmentPosition()) {
			skipSwipe = false;
		}
	}

	@Override
	public void onPageSelected(int position) {
		if (getTasksFragment() == null)
			return;
		getTasksFragment().closeActionMode();
		getTaskFragment().closeActionMode();
		if (MirakelPreferences.lockDrawerInTaskFragment()
				&& position == getTaskFragmentPosition()) {
			mDrawerLayout
					.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		} else {
			mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		}
		loadMenu(position);

	}

	@Override
	public boolean onCreateOptionsMenu(@SuppressWarnings("hiding") Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		updateLists();
		getMenuInflater().inflate(R.menu.main, menu);
		this.menu = menu;
		if (!showNavDrawer) {
			loadMenu(currentPosition, false, false);
		} else {
			showNavDrawer = false;
			loadMenu(-1, false, false);
		}
		return true;
	}

	public void loadMenu(int position) {
		loadMenu(position, true, false);
	}

	public void loadMenu(int position, boolean setPosition, boolean fromShare) {
		if (getTaskFragment() != null && getTaskFragment().getView() != null) {
			final InputMethodManager imm = (InputMethodManager) this
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getTaskFragment().getView()
					.getWindowToken(), 0);
		}
		if (menu == null)
			return;
		int newmenu;
		switch (position) {
		case -1:
			newmenu = R.menu.activity_list;
			getSupportActionBar().setTitle(getString(R.string.list_title));
			break;
			case RIGHT_FRAGMENT:
				newmenu = isRTL ? handleTasksFragmentMenu()
						: handleTaskFragmentMenu();
				break;
			case LEFT_FRAGMENT:
				newmenu = isRTL ? handleTaskFragmentMenu()
						: handleTasksFragmentMenu();
			break;
		default:
			Toast.makeText(getApplicationContext(), "Where are the dragons?",
					Toast.LENGTH_LONG).show();
			return;
		}
		if (setPosition)
			currentPosition = position;

		// Configure to use the desired menu
		if (newmenu == -1) return;
		menu.clear();
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(newmenu, menu);
		if (menu.findItem(R.id.menu_sync_now) != null)
			menu.findItem(R.id.menu_sync_now).setVisible(
					MirakelPreferences.useSync());
		if (menu.findItem(R.id.menu_kill_button) != null)
			menu.findItem(R.id.menu_kill_button).setVisible(
					MirakelPreferences.showKillButton());
		if (!fromShare)
			updateShare();

	}

	private int handleTaskFragmentMenu() {
		int newmenu;
		newmenu = R.menu.activity_task;
		getTaskFragment().update(currentTask);
		if (getSupportActionBar() != null && currentTask != null)
			getSupportActionBar().setTitle(currentTask.getName());
		return newmenu;
	}

	private int handleTasksFragmentMenu() {
		int newmenu;
		getListFragment().enableDrop(false);
		if (getTaskFragment() != null && getTaskFragment().adapter != null) {
			getTaskFragment().adapter.setEditContent(false);
		}
		if (currentList == null) return -1;
		if (!MirakelPreferences.isTablet())
			newmenu = R.menu.tasks;
		else
			newmenu = R.menu.tablet_right;
		getSupportActionBar().setTitle(currentList.getName());
		return newmenu;
	}

	public void updateShare() {
		if (menu != null && menu.findItem(R.id.share_list) != null
				&& currentList.countTasks() == 0) {
			menu.findItem(R.id.share_list).setVisible(false);
		} else if (currentPosition == getTasksFragmentPosition()
				&& menu != null
				&& menu.findItem(R.id.share_list) == null
				&& currentList.countTasks() > 0
				&& !mDrawerLayout.isDrawerOpen(Mirakel.GRAVITY_LEFT)) {
			loadMenu(getTasksFragmentPosition(), true, true);
		} else if (menu != null && menu.findItem(R.id.share_list) != null
				&& currentList.countTasks() > 0) {
			menu.findItem(R.id.share_list).setVisible(true);
		}
	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		boolean isOk = resultCode == RESULT_OK;
		Log.v(TAG, "Result:" + requestCode);
		switch (requestCode) {
		case RESULT_SPEECH_NAME:
			if (intent != null) {
				ArrayList<String> text = intent
						.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				((EditText) findViewById(R.id.edit_name)).setText(text.get(0));
			}
			break;
		case RESULT_SPEECH:
			if (intent != null) {
				ArrayList<String> text = intent
						.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				((EditText) getTasksFragment().getFragmentView().findViewById(
						R.id.tasks_new)).setText(text.get(0));
			}
			break;
		case RESULT_ADD_FILE:
			if (intent != null) {
				final String file_path = FileUtils.getPathFromUri(
						intent.getData(), this);
				if (FileMirakel.newFile(this, currentTask, file_path) == null) {
					Toast.makeText(this, getString(R.string.file_vanished),
							Toast.LENGTH_SHORT).show();
				} else {
					getTaskFragment().adapter.setData(currentTask);
				}
			}
			break;
		case RESULT_SETTINGS:
			getListFragment().update();
			highlightSelected = MirakelPreferences.highlightSelected();
			if (!highlightSelected
					&& (oldClickedList != null || oldClickedTask == null)) {
				clearAllHighlights();
			}
			if (darkTheme != MirakelPreferences.isDark()) {
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
			getTasksFragment().updateButtons();
			return;
		case RESULT_CAMERA:
		case RESULT_ADD_PICTURE:
			if (isOk) {
				Task task;
				if (requestCode == RESULT_ADD_PICTURE) {
					task = getCurrentTask();
				} else {
					task = Semantic.createTask(
							MirakelPreferences.getPhotoDefaultTitle(),
							currentList, false, this);
					safeSaveTask(task);
				}
				task.addFile(this, FileUtils.getPathFromUri(fileUri, this));
				setCurrentList(task.getList());
				setCurrentTask(task, true);
			}
			break;
			default:
				Log.w(TAG, "unknown activity result");
				break;
		}
	}

	@Override
	public void onBackPressed() {
		if (goBackTo.size() > 0 && currentPosition == getTaskFragmentPosition()) {
			Task goBack = goBackTo.pop();
			setCurrentList(goBack.getList(), null, false, false);
			setCurrentTask(goBack, false, false);
			return;
		}
		if (closeOnBack) {
			super.onBackPressed();
			return;
		}
		switch (mViewPager.getCurrentItem()) {
		/*
		 * case TASKS_FRAGMENT: mDrawerLayout.openDrawer(Gravity.LEFT); break;
		 */
			case LEFT_FRAGMENT:
				if (isRTL) {
					mViewPager.setCurrentItem(getTasksFragmentPosition());
					return;
				}
				break;
			case RIGHT_FRAGMENT:
				if (!isRTL) {
					mViewPager.setCurrentItem(getTasksFragmentPosition());
					return;
				}
				break;
		default:
				// Cannot be, do nothing
				break;
		}
		super.onBackPressed();
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

	@SuppressLint("NewApi")
	@Override
	protected void onPause() {
		if (getTasksFragment() != null)
			getTasksFragment().clearFocus();
		Intent intent = new Intent(this, MainWidgetProvider.class);
		intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		// Use an array and EXTRA_APPWIDGET_IDS instead of
		// AppWidgetManager.EXTRA_APPWIDGET_ID,
		// since it seems the onUpdate() is only fired on that:
		Context context = getApplicationContext();
		ComponentName name = new ComponentName(context,
				MainWidgetProvider.class);
		int widgets[] = AppWidgetManager.getInstance(context).getAppWidgetIds(
				name);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgets);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			for (int id : widgets) {
				AppWidgetManager.getInstance(this)
						.notifyAppWidgetViewDataChanged(id,
								R.id.widget_tasks_list);
			}
		}
		sendBroadcast(intent);
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
		getListFragment().setActivity(this);
		getTasksFragment().setActivity(this);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	// Fix Intent-behavior
	// default is not return new Intent by calling getIntent
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	private String newTaskContent, newTaskSubject;

	private void addFilesForTask(Task t, Intent intent) {
		String action = intent.getAction();
		String type = intent.getType();
		currentPosition = getTaskFragmentPosition();

		if (Intent.ACTION_SEND.equals(action) && type != null) {
			Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
			t.addFile(this, FileUtils.getPathFromUri(uri, this));
		} else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
			ArrayList<Uri> imageUris = intent
					.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			for (Uri uri : imageUris) {
				t.addFile(this, FileUtils.getPathFromUri(uri, this));
			}
		}

	}

	private void addTaskFromSharing(ListMirakel list) {
		if (newTaskSubject == null)
			return;
		Task task = Semantic.createTask(newTaskSubject, list, true, this);
		task.setContent(newTaskContent == null ? "" : newTaskContent);
		safeSaveTask(task);
		setCurrentTask(task);
		addFilesForTask(task, startIntent);
		setCurrentList(task.getList());
		setCurrentTask(task, true);
	}

	/**
	 * Initialize the ViewPager and setup the rest of the layout
	 */
	private void setupLayout() {
		closeOnBack = false;
		if (currentList == null)
			setCurrentList(SpecialList.firstSpecial());
		// Initialize ViewPager
		if (!isResumend && mPagerAdapter == null)
			intializeFragments();
		NotificationService.updateNotificationAndWidget(this);
		startIntent = getIntent();
		if (startIntent == null || startIntent.getAction() == null) {
			Log.d(TAG, "action null");
		} else if (startIntent.getAction().equals(SHOW_TASK)
				|| startIntent.getAction().equals(SHOW_TASK_FROM_WIDGET)) {
			Task task = TaskHelper.getTaskFromIntent(startIntent);
			if (task != null) {
				skipSwipe = true;
				setCurrentList(task.getList());
				setCurrentTask(task, true);
				mViewPager.setCurrentItem(getTaskFragmentPosition(), false);
			} else {
				Log.d(TAG, "task null");
			}
			if (startIntent.getAction().equals(SHOW_TASK_FROM_WIDGET))
				closeOnBack = true;
		} else if (startIntent.getAction().equals(Intent.ACTION_SEND)
				|| startIntent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
			closeOnBack = true;
			newTaskContent = startIntent.getStringExtra(Intent.EXTRA_TEXT);
			newTaskSubject = startIntent.getStringExtra(Intent.EXTRA_SUBJECT);

			// If from google now, the content is the subject…
			if (startIntent.getCategories() != null
					&& startIntent.getCategories().contains(
							"com.google.android.voicesearch.SELF_NOTE")) {
				if (!newTaskContent.equals("")) {
					newTaskSubject = newTaskContent;
					newTaskContent = "";
				}
			}

			if (!startIntent.getType().equals("text/plain")) {
				if (newTaskSubject == null) {
					newTaskSubject = MirakelPreferences.getImportFileTitle();
				}
			}
			ListMirakel listFromSharing = MirakelPreferences
					.getImportDefaultList(false);
			if (listFromSharing != null) {
				addTaskFromSharing(listFromSharing);
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
							public void onClick(DialogInterface dialog, int item) {
								addTaskFromSharing(ListMirakel.getList(list_ids
										.get(item)));
								dialog.dismiss();
							}
						});
				builder.create().show();
			}
		} else if (startIntent.getAction().equals(SHOW_LIST)
				|| startIntent.getAction().contains(SHOW_LIST_FROM_WIDGET)) {

			int listId;
			if (startIntent.getAction().equals(SHOW_LIST)) {
				listId = startIntent.getIntExtra(EXTRA_ID, 0);
			} else {
				listId = Integer.parseInt(startIntent.getAction().replace(
						SHOW_LIST_FROM_WIDGET, ""));
			}
			Log.wtf(TAG, "ListId: " + listId);
			ListMirakel list = ListMirakel.getList(listId);
			if (list == null)
				list = SpecialList.firstSpecial();
			setCurrentList(list);
			if (startIntent.getAction().contains(SHOW_LIST_FROM_WIDGET))
				closeOnBack = true;
		} else if (startIntent.getAction().equals(SHOW_LISTS)) {
			mDrawerLayout.openDrawer(Mirakel.GRAVITY_LEFT);
		} else if (startIntent.getAction().equals(Intent.ACTION_SEARCH)) {
			String query = startIntent.getStringExtra(SearchManager.QUERY);
			search(query);
		} else if (startIntent.getAction().contains(ADD_TASK_FROM_WIDGET)) {
			Log.d(TAG, "add");
			int listId = Integer.parseInt(startIntent.getAction().replace(
					ADD_TASK_FROM_WIDGET, ""));
			setCurrentList(ListMirakel.getList(listId));
			if (getTasksFragment() != null) {
				getTasksFragment().focusNew(true);
			} else {
				mViewPager.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (getTasksFragment() != null) {
							getTasksFragment().focusNew(true);
						} else {
							Log.wtf(TAG, "Tasksfragment null");
						}
					}
				}, 10);
			}

		} else {
			mViewPager.setCurrentItem(getTaskFragmentPosition());
		}
		if ((startIntent == null || startIntent.getAction() == null || (!startIntent
				.getAction().contains(ADD_TASK_FROM_WIDGET)))
				&& getTasksFragment() != null) {
			getTasksFragment().clearFocus();
		}
		setIntent(null);
	}

	private void safeSaveTask(Task task) {
		try {
			task.save();
		} catch (NoSuchListException e) {
			Toast.makeText(getApplicationContext(), R.string.list_vanished,
					Toast.LENGTH_LONG).show();
		}
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
		List<ListMirakel> l = new ArrayList<ListMirakel>();
		l.add(list);
		handleDestroyList(l);
	}

	public void handleDestroyList(@SuppressWarnings("hiding") final List<ListMirakel> lists) {
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
									getListFragment().update();
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
		if (tasks == null)
			return;
		final MainActivity main = this;
		// This must then be a bug in a ROM
		if (tasks.size() == 0 || tasks.get(0) == null)
			return;
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
									t.destroy();
								}
								setCurrentList(currentList);
								ReminderAlarm.updateAlarms(main);
								updateShare();
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
							safeSaveTask(t);
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
						 * getTasksFragment().updateList();
						 * getTasksFragment().update();
						 * 
						 * 3: Set the currentList to the old List
						 */
						if (currentPosition == getTaskFragmentPosition()) {
							Task task = tasks.get(0);
							if (task == null) {
								// What the hell?
								Log.wtf(TAG, "Task vanished");
							} else {
								setCurrentList(task.getList());
								setCurrentTask(task, true);
							}
						} else {
							setCurrentList(getCurrentList());
							getListFragment().update();
						}
						dialog.dismiss();
					}
				}).show();
	}

	private ActionBarDrawerToggle mDrawerToggle;
	private static boolean			isRTL;

	/**
	 * Initialize ViewPager
	 */
	@SuppressLint("NewApi")
	private void intializeFragments() {
		/*
		 * Setup NavigationDrawer
		 */
		if (ListFragment.getSingleton() != null)
			listFragment = ListFragment.getSingleton();
		// listFragment = new ListFragment();
		getListFragment().setActivity(this);
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
				getListFragment().closeActionMode();
			}

			public void onDrawerOpened(View drawerView) {
				loadMenu(-1, false, false);
			}
		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		if (showNavDrawer) {
			mDrawerLayout.openDrawer(Mirakel.GRAVITY_LEFT);
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		/*
		 * Setup other Fragments
		 */
		TasksFragment tasksFragment = new TasksFragment();
		tasksFragment.setActivity(this);
		fragments.add(tasksFragment);
		if (!MirakelPreferences.isTablet()) {
			fragments.add(new TaskFragment());
		}
		if (isRTL) {
			Fragment[] fragmentsLocal = new Fragment[fragments.size()];
			for (int i = 0; i < fragments.size(); i++) {
				fragmentsLocal[fragmentsLocal.length - 1 - i] = fragments
						.get(i);
			}
			this.mPagerAdapter = new PagerAdapter(
					super.getSupportFragmentManager(),
					Arrays.asList(fragmentsLocal));
		} else {
			this.mPagerAdapter = new PagerAdapter(
					super.getSupportFragmentManager(), fragments);
		}

		//
		this.mViewPager = (ViewPager) super.findViewById(R.id.viewpager);
		this.mViewPager.setAdapter(this.mPagerAdapter);
		this.mViewPager.setOnPageChangeListener(this);
		mViewPager.setOffscreenPageLimit(MirakelPreferences.isTablet() ? 1 : 2);

	}

	/**
	 * Return the currently showed tasks
	 * 
	 * @return
	 */
	public Task getCurrentTask() {
		return currentTask;
	}

	/**
	 * Set the current task and update the view
	 * 
	 * @param currentTask
	 */
	public void setCurrentTask(Task currentTask) {
		setCurrentTask(currentTask, false);
	}

	private View oldClickedTask = null;

	void highlightCurrentTask(@SuppressWarnings("hiding") Task currentTask, boolean multiselect) {
		if (getTaskFragment() == null
				|| getTasksFragment().getAdapter() == null
				|| currentTask == null)
			return;
		Log.v(TAG, currentTask.getName());
		View currentView = getTasksFragment().getAdapter().getViewForTask(
				currentTask);
		if (currentView == null) {
			currentView = getTasksFragment().getListView().getChildAt(0);
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

	public void setCurrentTask(Task currentTask, boolean switchFragment) {
		setCurrentTask(currentTask, switchFragment, true);
	}

	public void setCurrentTask(Task currentTask, boolean switchFragment,
			boolean resetGoBackTo) {

		this.currentTask = currentTask;
		if (resetGoBackTo)
			goBackTo.clear();

		highlightCurrentTask(currentTask, false);

		if (getTaskFragment() != null) {
			getTaskFragment().update(currentTask);
			boolean smooth = mViewPager.getCurrentItem() != getTaskFragmentPosition();
			if (!switchFragment)
				return;
			// Fix buggy behavior
			mViewPager.setCurrentItem(getTasksFragmentPosition(), false);
			mViewPager.setCurrentItem(getTaskFragmentPosition(), false);
			mViewPager.setCurrentItem(getTasksFragmentPosition(), false);
			mViewPager.setCurrentItem(getTaskFragmentPosition(), smooth);
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
			ListView view = (ListView) getTasksFragment().getFragmentView()
					.findViewById(R.id.tasks_list);
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
	public ListMirakel getCurrentList() {
		if (currentList == null)
			currentList = SpecialList.firstSpecialSafe(this);
		return currentList;
	}

	private View oldClickedList = null;

	/**
	 * Set the current list and update the views
	 * 
	 * @param currentList
	 * @param switchFragment
	 */
	public void setCurrentList(ListMirakel currentList, boolean switchFragment) {
		setCurrentList(currentList, null, switchFragment, true);
	}

	public void setCurrentList(ListMirakel currentList, View currentView) {
		setCurrentList(currentList, currentView, true, true);
	}

	public void setCurrentList(ListMirakel currentList) {
		setCurrentList(currentList, null, true, true);
	}

	public void setCurrentList(ListMirakel currentList, View currentView,
			boolean switchFragment, boolean resetGoBackTo) {
		if (currentList == null)
			return;
		if (resetGoBackTo)
			goBackTo.clear();
		this.currentList = currentList;
		if (mDrawerLayout != null)
			mDrawerLayout.closeDrawers();

		List<Task> currentTasks = currentList.tasks(MirakelPreferences
				.showDoneMain());
		if (currentTasks.size() == 0) {
			currentTask = Task.getDummy(getApplicationContext());
		} else {
			currentTask = currentTasks.get(0);
		}

		if (getTasksFragment() != null) {
			getTasksFragment().updateList();
			if (!MirakelPreferences.isTablet() && switchFragment)
				mViewPager.setCurrentItem(getTasksFragmentPosition());
		}
		if (currentView == null && listFragment != null
				&& getListFragment().getAdapter() != null)
			currentView = getListFragment().getAdapter().getViewForList(
					currentList);

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
		if (switchFragment)
			setCurrentTask(currentTask);
		if (currentPosition == getTasksFragmentPosition())
			getSupportActionBar().setTitle(currentList.getName());

	}

	/**
	 * Ugly Wrapper TODO make it more beautiful
	 * 
	 * @param task
	 */
	public void saveTask(Task task) {
		Log.v(TAG, "Saving task… (task:" + task.getId());
		safeSaveTask(task);
		updatesForTask(task);
	}

	/**
	 * Executes some View–Updates if a Task was changed
	 * 
	 * @param task
	 */
	public void updatesForTask(Task task) {
		if (currentTask != null && task.getId() == currentTask.getId()) {
			currentTask = task;
			getTaskFragment().update(task);
		}
		getTasksFragment().updateList(false);
		getListFragment().update();
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

	public TasksFragment getTasksFragment() {
		if (mPagerAdapter == null) {
			Log.i(TAG, "pageadapter null");
			return null;
		}
		Fragment f = this.getSupportFragmentManager().findFragmentByTag(
				getFragmentTag(getTasksFragmentPosition()));
		return (TasksFragment) f;
	}

	public TaskFragment getTaskFragment() {
		if (mPagerAdapter == null)
			return null;
		if (MirakelPreferences.isTablet())
			return taskFragment;
		Fragment f = this.getSupportFragmentManager().findFragmentByTag(
				getFragmentTag(getTaskFragmentPosition()));
		return (TaskFragment) f;
	}

	public void setTaskFragment(TaskFragment tf) {
		taskFragment = tf;

	}

	private String getFragmentTag(int pos) {
		return "android:switcher:" + R.id.viewpager + ":" + pos;
	}

	private void search(String query) {
		setCurrentList(new SearchList(this, query));
		mViewPager.setCurrentItem(getTasksFragmentPosition());
	}

	public void showMessageFromSync() {
		CharSequence messageFromSync = SyncAdapter.getLastMessage();
		if (messageFromSync != null) {
			Toast.makeText(getApplicationContext(), messageFromSync,
					Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Set the Task, to which we switch, if the user press the back-button. It
	 * is reseted, if one of the setCurrent*-functions on called
	 * 
	 * @param t
	 */
	public void setGoBackTo(Task t) {
		goBackTo.push(t);
	}

	public void setFileUri(Uri file) {
		fileUri = file;
	}

	public int getCurrentPosition() {
		return currentPosition;
	}
}
