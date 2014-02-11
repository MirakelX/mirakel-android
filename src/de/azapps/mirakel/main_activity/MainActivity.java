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
package de.azapps.mirakel.main_activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.Vector;

import sheetrock.panda.changelog.ChangeLog;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
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
import de.azapps.mirakel.adapter.PagerAdapter;
import de.azapps.mirakel.helper.BuildHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.ListDialogHelpers;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.SharingHelper;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.helper.UndoHistory;
import de.azapps.mirakel.main_activity.list_fragment.ListFragment;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragment;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentV14;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentV8;
import de.azapps.mirakel.main_activity.tasks_fragment.TasksFragment;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
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
import de.azapps.mirakel.widget.MainWidgetProvider;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

/**
 * This is our main activity. Here happens nearly everything.
 * 
 * @author az
 */
public class MainActivity extends ActionBarActivity implements ViewPager.OnPageChangeListener {

	public static String		EXTRA_ID			= "de.azapps.mirakel.EXTRA_TASKID",
			SHOW_TASK = "de.azapps.mirakel.SHOW_TASK",
			SHOW_LIST = "de.azapps.mirakel.SHOW_LIST",
			SHOW_LISTS = "de.azapps.mirakel.SHOW_LISTS",
			SHOW_LIST_FROM_WIDGET = "de.azapps.mirakel.SHOW_LIST_FROM_WIDGET",
			ADD_TASK_FROM_WIDGET = "de.azapps.mirakel.ADD_TASK_FROM_WIDGET",
			SHOW_TASK_FROM_WIDGET = "de.azapps.mirakel.SHOW_TASK_FROM_WIDGET",
			TASK_ID = "de.azapp.mirakel.TASK_ID";
	private static boolean		isRTL;
	// Intent variables
	public static final int		LEFT_FRAGMENT		= 0, RIGHT_FRAGMENT = 1;
	public static final int		RESULT_SPEECH_NAME	= 1, RESULT_SPEECH = 3,
			RESULT_SETTINGS = 4, RESULT_ADD_FILE = 5, RESULT_CAMERA = 6,
			RESULT_ADD_PICTURE = 7;
	private static final String	TAG					= "MainActivity";
	// TODO We should do this somehow else
	public static boolean		updateTasksUUID		= false;

	private static int getTaskFragmentPosition() {
		if (MainActivity.isRTL || MirakelPreferences.isTablet())
			return MainActivity.LEFT_FRAGMENT;
		return MainActivity.RIGHT_FRAGMENT;
	}

	public static int getTasksFragmentPosition() {
		if (MainActivity.isRTL && !MirakelPreferences.isTablet())
			return MainActivity.RIGHT_FRAGMENT;
		return MainActivity.LEFT_FRAGMENT;
	}

	private boolean					closeOnBack		= false;
	private ListMirakel				currentList;

	protected int					currentPosition;
	// State variables
	private Task					currentTask;
	public boolean					darkTheme;
	private Uri						fileUri;

	private final Stack<Task>		goBackTo		= new Stack<Task>();
	// Foo variables (move them out of the MainActivity)
	private boolean					highlightSelected;
	// User interaction variables
	private boolean					isResumend;

	private boolean					isTablet;

	protected ListFragment			listFragment;
	private List<ListMirakel>		lists;
	private DrawerLayout			mDrawerLayout;
	private ActionBarDrawerToggle	mDrawerToggle;
	private Menu					menu;
	private PagerAdapter			mPagerAdapter;

	// Layout variables
	ViewPager						mViewPager;

	private String					newTaskContent, newTaskSubject;

	private View					oldClickedList	= null;

	private View					oldClickedTask	= null;

	private boolean					showNavDrawer	= false;

	private boolean					skipSwipe;

	private Intent					startIntent;
	protected TaskFragment			taskFragment;

	private void addFilesForTask(final Task t, final Intent intent) {
		final String action = intent.getAction();
		final String type = intent.getType();
		this.currentPosition = getTaskFragmentPosition();

		if (Intent.ACTION_SEND.equals(action) && type != null) {
			final Uri uri = (Uri) intent
					.getParcelableExtra(Intent.EXTRA_STREAM);
			t.addFile(this, FileUtils.getPathFromUri(uri, this));
		} else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
			final ArrayList<Uri> imageUris = intent
					.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			for (final Uri uri : imageUris) {
				t.addFile(this, FileUtils.getPathFromUri(uri, this));
			}
		}

	}

	private void addTaskFromSharing(final ListMirakel list) {
		if (this.newTaskSubject == null) return;
		final Task task = Semantic.createTask(this.newTaskSubject, list, true,
				this);
		task.setContent(this.newTaskContent == null ? "" : this.newTaskContent);
		task.safeSave();
		setCurrentTask(task);
		addFilesForTask(task, this.startIntent);
		setCurrentList(task.getList());
		setCurrentTask(task, true);
	}

	private void clearAllHighlights() {
		if (this.oldClickedList != null) {
			this.oldClickedList.setSelected(false);
			this.oldClickedList.setBackgroundColor(0x00000000);
		}
		if (this.oldClickedTask != null) {
			this.oldClickedTask.setSelected(false);
			this.oldClickedTask.setBackgroundColor(0x00000000);
		}
		clearHighlighted();
	}

	private void clearHighlighted() {
		if (this.oldClickedTask == null) return;
		try {
			final ListView view = (ListView) getTasksFragment()
					.getFragmentView().findViewById(R.id.tasks_list);
			final int pos_old = view.getPositionForView(this.oldClickedTask);
			if (pos_old != -1) {
				view.getChildAt(pos_old).setBackgroundColor(0x00000000);
			} else {
				Log.wtf(MainActivity.TAG, "View not found");
			}
		} catch (final Exception e) {
			Log.wtf(MainActivity.TAG, "Listview not found");
			Log.e(MainActivity.TAG, Log.getStackTraceString(e));
		}
	}

	private void draw() {
		setContentView(R.layout.activity_main);
		this.mPagerAdapter = null;
		// Show ChangeLog
		final ChangeLog cl = new ChangeLog(this);
		if (cl.firstRun()) {
			cl.getLogDialog().show();
			this.showNavDrawer = true;
		}
		// currentList=preferences.getInt("s", defValue)
		this.skipSwipe = false;
		setupLayout();
		this.isResumend = false;
	}

	private void forceRebuildLayout(final boolean tabletLocal) {
		this.isTablet = tabletLocal;
		this.mPagerAdapter = null;
		this.isResumend = false;
		this.skipSwipe = true;
		setupLayout();
		this.skipSwipe = true;
		getTaskFragment().update(MainActivity.this.currentTask);
		loadMenu(this.currentPosition, false, false);
	}

	/**
	 * Return the currently showed List
	 * 
	 * @return
	 */
	public ListMirakel getCurrentList() {
		if (this.currentList == null) {
			this.currentList = SpecialList.firstSpecialSafe(this);
		}
		return this.currentList;
	}

	public int getCurrentPosition() {
		return this.currentPosition;
	}

	/**
	 * Return the currently showed tasks
	 * 
	 * @return
	 */
	public Task getCurrentTask() {
		this.currentTask = this.currentList.getFirstTask();
		if (this.currentTask == null) {
			this.currentTask = Task.getDummy(getApplicationContext());
		}

		return this.currentTask;
	}

	/**
	 * Returns the ListFragment
	 * 
	 * @return
	 */
	public ListFragment getListFragment() {
		return this.listFragment;
	}

	public TaskFragment getTaskFragment() {
		if (this.mPagerAdapter == null) return null;
		if (MirakelPreferences.isTablet()) return this.taskFragment;
		final Fragment f = this.mPagerAdapter
				.getItem(getTaskFragmentPosition());
		try {
			return (TaskFragment) f;
		} catch (ClassCastException e) {
			Log.wtf(TAG, "cannot cast fragment");
			forceRebuildLayout(MirakelPreferences.isTablet());
			return getTaskFragment();
		}
	}

	public TasksFragment getTasksFragment() {
		if (this.mPagerAdapter == null) {
			Log.i(MainActivity.TAG, "pageadapter null");
			return null;
		}
		final Fragment f = this.mPagerAdapter.getItem(MainActivity
				.getTasksFragmentPosition());
		return (TasksFragment) f;
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
							@Override
							public void onClick(final DialogInterface dialog, final int which) {
								for (final ListMirakel list : lists) {
									list.destroy();
									if (getCurrentList().getId() == list
											.getId()) {
										setCurrentList(SpecialList
												.firstSpecial());
									}
								}
								if (getListFragment() != null
										&& getListFragment().getAdapter() != null) {
									getListFragment().getAdapter().changeData(
											ListMirakel.all());
									getListFragment().getAdapter()
											.notifyDataSetChanged();
								}
							}
						})
				.setNegativeButton(this.getString(android.R.string.no), null)
				.show();
	}

	/**
	 * Handle the actions after clicking on a destroy-list button
	 * 
	 * @param list
	 */
	public void handleDestroyList(final ListMirakel list) {
		final List<ListMirakel> l = new ArrayList<ListMirakel>();
		l.add(list);
		handleDestroyList(l);
	}

	public void handleDestroyTask(final List<Task> tasks) {
		if (tasks == null) return;
		final MainActivity main = this;
		// This must then be a bug in a ROM
		if (tasks.size() == 0 || tasks.get(0) == null) return;
		String names = tasks.get(0).getName();
		for (int i = 1; i < tasks.size(); i++) {
			names += ", " + tasks.get(i).getName();
		}
		new AlertDialog.Builder(this)
				.setTitle(
						getResources().getQuantityString(R.plurals.task_delete,
								tasks.size()))
				.setMessage(this.getString(R.string.task_delete_content, names))
				.setPositiveButton(this.getString(android.R.string.yes),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog, final int which) {
								for (final Task t : tasks) {
									t.destroy();
								}
								setCurrentList(MainActivity.this.currentList);
								ReminderAlarm.updateAlarms(main);
								updateShare();
							}
						})
				.setNegativeButton(this.getString(android.R.string.no), null)
				.show();
		getTasksFragment().updateList(false);
	}

	/**
	 * Handle the actions after clicking on a destroy-task button
	 * 
	 * @param task
	 */
	public void handleDestroyTask(final Task task) {
		final List<Task> t = new ArrayList<Task>();
		t.add(task);
		handleDestroyTask(t);
	}

	public void handleMoveTask(final List<Task> tasks) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_move);
		final List<CharSequence> items = new ArrayList<CharSequence>();
		final List<Integer> list_ids = new ArrayList<Integer>();
		int currentItem = 0, i = 0;
		for (final ListMirakel list : this.lists) {
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
					@Override
					public void onClick(final DialogInterface dialog, final int item) {
						for (final Task t : tasks) {
							t.setList(ListMirakel.getList(list_ids.get(item)),
									true);
							t.safeSave();
						}
						/*
						 * There are 3 possibilities how to handle the post-move of a task: 1:
						 * update the currentList to the List, the task was moved to
						 * setCurrentList(task.getList()); 2: update the tasksView but not update
						 * the taskView: getTasksFragment().updateList();
						 * getTasksFragment().update(); 3: Set the currentList to the old List
						 */
						if (MainActivity.this.currentPosition == getTaskFragmentPosition()) {
							final Task task = tasks.get(0);
							if (task == null) {
								// What the hell?
								Log.wtf(MainActivity.TAG, "Task vanished");
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

	/**
	 * Handle the actions after clicking on a move task button
	 * 
	 * @param tasks
	 */
	public void handleMoveTask(final Task task) {
		final List<Task> t = new ArrayList<Task>();
		t.add(task);
		handleMoveTask(t);
	}

	private int handleTaskFragmentMenu() {
		if (getSupportActionBar() != null && this.currentTask != null
				&& this.mViewPager != null) {
			this.mViewPager.post(new Runnable() {
				@Override
				public void run() {
					getTaskFragment().update(MainActivity.this.currentTask);
					if (MainActivity.this.currentTask == null) {
						MainActivity.this.currentTask = Task
								.getDummy(MainActivity.this);
					}
					getSupportActionBar().setTitle(
							MainActivity.this.currentTask.getName());
				}
			});

		}
		return R.menu.activity_task;
	}

	private int handleTasksFragmentMenu() {
		int newmenu;
		getListFragment().enableDrop(false);
		// if (getTaskFragment() != null && getTaskFragment().adapter !=
		// null&&this.mViewPager!=null) {
		// this.mViewPager.post(new Runnable() {
		//
		// @Override
		// public void run() {
		// getTaskFragment().adapter.setEditContent(false);
		//
		// }
		// });
		//
		// }
		if (this.currentList == null) return -1;
		if (!MirakelPreferences.isTablet()) {
			newmenu = R.menu.tasks;
		} else {
			newmenu = R.menu.tablet_right;
		}
		if (this.mViewPager != null) {
			this.mViewPager.post(new Runnable() {
				@Override
				public void run() {
					getSupportActionBar().setTitle(
							MainActivity.this.currentList.getName());
				}
			});
		}

		return newmenu;
	}

	void highlightCurrentTask(final Task currentTask, final boolean multiselect) {
		if (getTaskFragment() == null || getTasksFragment() == null
				|| getTasksFragment().getAdapter() == null
				|| currentTask == null) return;
		Log.v(MainActivity.TAG, currentTask.getName());
		View currentView = getTasksFragment().getAdapter().getViewForTask(
				currentTask);
		if (currentView == null) {
			currentView = getTasksFragment().getListView().getChildAt(0);
		}

		if (currentView != null && this.highlightSelected && !multiselect) {
			if (this.oldClickedTask != null) {
				this.oldClickedTask.setSelected(false);
				this.oldClickedTask.setBackgroundColor(0x00000000);
			}
			currentView.setBackgroundColor(getResources().getColor(
					this.darkTheme ? R.color.highlighted_text_holo_dark
							: R.color.highlighted_text_holo_light));
			this.oldClickedTask = currentView;
		}
	}

	/**
	 * Initialize ViewPager
	 */
	@SuppressLint("NewApi")
	private void intializeFragments() {
		/*
		 * Setup NavigationDrawer
		 */
		if (ListFragment.getSingleton() != null) {
			this.listFragment = ListFragment.getSingleton();
		}
		// listFragment = new ListFragment();
		getListFragment().setActivity(this);
		// fragments.add(listFragment);

		final List<Fragment> fragments = new Vector<Fragment>();
		this.mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		this.mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		this.mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
		R.string.list_title, /* "open drawer" description */
		R.string.list_title /* "close drawer" description */
		) {
			@Override
			public void onDrawerClosed(final View view) {
				loadMenu(MainActivity.this.currentPosition);
				if (getListFragment() != null) {
					getListFragment().closeNavDrawer();
				}

			}

			@Override
			public void onDrawerOpened(final View drawerView) {
				loadMenu(-1, false, false);
				MainActivity.this.listFragment.refresh();
			}
		};

		// Set the drawer toggle as the DrawerListener
		this.mDrawerLayout.setDrawerListener(this.mDrawerToggle);
		if (this.showNavDrawer) {
			this.mDrawerLayout.openDrawer(Mirakel.GRAVITY_LEFT);
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		/*
		 * Setup other Fragments
		 */
		final TasksFragment tasksFragment = new TasksFragment();
		tasksFragment.setActivity(this);
		fragments.add(tasksFragment);
		if (!MirakelPreferences.isTablet()) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				fragments.add(new TaskFragmentV8());
			} else {
				fragments.add(new TaskFragmentV14());
			}
		}
		if (MainActivity.isRTL && !MirakelPreferences.isTablet()) {
			final Fragment[] fragmentsLocal = new Fragment[fragments.size()];
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
		if (this.mViewPager == null) {
			this.mViewPager = (ViewPager) super.findViewById(R.id.viewpager);
		}
		if (this.mViewPager == null) {
			Log.wtf(MainActivity.TAG, "viewpager null");
			return;
		}
		this.mViewPager.setOffscreenPageLimit(MirakelPreferences.isTablet() ? 1
				: 2);
		this.mViewPager.setAdapter(this.mPagerAdapter);
		this.mViewPager.setOnPageChangeListener(this);
		this.mViewPager.setOffscreenPageLimit(MirakelPreferences.isTablet() ? 1
				: 2);

	}

	public void loadMenu(final int position) {
		loadMenu(position, true, false);
	}

	public void loadMenu(int position, boolean setPosition, final boolean fromShare) {
		if (getTaskFragment() != null && getTaskFragment().getView() != null) {
			final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getTaskFragment().getView()
					.getWindowToken(), 0);
		}
		if (this.menu == null) return;
		final int newmenu;
		if (this.isTablet && position != -1) {
			newmenu = R.menu.tablet_right;
		} else {
			switch (position) {
				case -1:
					newmenu = R.menu.activity_list;
					getSupportActionBar().setTitle(
							getString(R.string.list_title));
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
					Toast.makeText(getApplicationContext(),
							"Where are the dragons?", Toast.LENGTH_LONG).show();
					return;
			}
		}
		if (setPosition) {
			this.currentPosition = position;
		}

		// Configure to use the desired menu
		if (newmenu == -1) return;
		if (this.mViewPager != null) {
			this.mViewPager.post(new Runnable() {

				@Override
				public void run() {
					MainActivity.this.menu.clear();
					MenuInflater inflater = getMenuInflater();
					inflater.inflate(newmenu, MainActivity.this.menu);
					if (MainActivity.this.menu.findItem(R.id.menu_sync_now) != null) {
						MainActivity.this.menu.findItem(R.id.menu_sync_now)
								.setVisible(MirakelPreferences.useSync());
					}
					if (MainActivity.this.menu.findItem(R.id.menu_kill_button) != null) {
						MainActivity.this.menu
								.findItem(R.id.menu_kill_button)
								.setVisible(MirakelPreferences.showKillButton());
					}
					if (MainActivity.this.menu.findItem(R.id.menu_contact) != null) {
						MainActivity.this.menu.findItem(R.id.menu_contact)
								.setVisible(BuildHelper.isBeta());
					}

					if (!fromShare) {
						updateShare();
					}

				}
			});
		}

	}

	public void lockDrawer() {
		this.mDrawerLayout
				.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
		final boolean isOk = resultCode == Activity.RESULT_OK;
		Log.v(MainActivity.TAG, "Result:" + requestCode);
		switch (requestCode) {
			case RESULT_SPEECH_NAME:
				if (intent != null) {
					final ArrayList<String> text = intent
							.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
					((EditText) findViewById(R.id.edit_name)).setText(text
							.get(0));
				}
				break;
			case RESULT_SPEECH:
				if (intent != null) {
					final ArrayList<String> text = intent
							.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
					((EditText) getTasksFragment().getFragmentView()
							.findViewById(R.id.tasks_new)).setText(text.get(0));
				}
				break;
			case RESULT_ADD_FILE:
				if (intent != null) {
					Log.d(TAG, "taskname " + this.currentTask.getName());
					final String file_path = FileUtils.getPathFromUri(
							intent.getData(), this);
					if (FileMirakel.newFile(this, this.currentTask, file_path) == null) {
						Toast.makeText(this, getString(R.string.file_vanished),
								Toast.LENGTH_SHORT).show();
					} else {
						getTaskFragment().update(this.currentTask);
					}
				}
				break;
			case RESULT_SETTINGS:
				getListFragment().update();
				getTaskFragment().updateLayout();
				this.highlightSelected = MirakelPreferences.highlightSelected();
				if (!this.highlightSelected
						&& (this.oldClickedList != null || this.oldClickedTask == null)) {
					clearAllHighlights();
				}
				if (this.darkTheme != MirakelPreferences.isDark()) {
					finish();
					if (this.startIntent == null) {
						this.startIntent = new Intent(MainActivity.this,
								MainActivity.class);
						this.startIntent.setAction(MainActivity.SHOW_LISTS);
						Log.wtf(MainActivity.TAG,
								"startIntent is null by switching theme");

					}
					startActivity(this.startIntent);
				}
				if (this.isTablet != MirakelPreferences.isTablet()) {
					forceRebuildLayout(MirakelPreferences.isTablet());
				} else {
					loadMenu(this.mViewPager.getCurrentItem());
				}
				if (getTasksFragment() != null) {
					getTasksFragment().updateButtons();
				}
				if (getTaskFragment() != null) {
					getTaskFragment().update(this.currentTask);
				}
				return;
			case RESULT_CAMERA:
			case RESULT_ADD_PICTURE:
				if (isOk) {
					Task task;
					if (requestCode == MainActivity.RESULT_ADD_PICTURE) {
						task = this.currentTask;
					} else {
						task = Semantic.createTask(
								MirakelPreferences.getPhotoDefaultTitle(),
								this.currentList, false, this);
						task.safeSave();
					}
					task.addFile(this,
							FileUtils.getPathFromUri(this.fileUri, this));
					getTaskFragment().update(task);
				}
				break;
			default:
				Log.w(MainActivity.TAG, "unknown activity result");
				break;
		}
	}

	@Override
	public void onBackPressed() {
		// getTaskFragment().cancelEditing();
		if (this.goBackTo.size() > 0
				&& this.currentPosition == getTaskFragmentPosition()) {
			final Task goBack = this.goBackTo.pop();
			setCurrentList(goBack.getList(), null, false, false);
			setCurrentTask(goBack, false, false);
			return;
		}
		if (this.closeOnBack) {
			super.onBackPressed();
			return;
		}
		switch (this.mViewPager.getCurrentItem()) {
		/*
		 * case TASKS_FRAGMENT: mDrawerLayout.openDrawer(Gravity.LEFT); break;
		 */
			case LEFT_FRAGMENT:
				if (MainActivity.isRTL) {
					this.mViewPager.setCurrentItem(MainActivity
							.getTasksFragmentPosition());
					return;
				}
				break;
			case RIGHT_FRAGMENT:
				if (!MainActivity.isRTL) {
					this.mViewPager.setCurrentItem(MainActivity
							.getTasksFragmentPosition());
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
	public void onConfigurationChanged(final Configuration newConfig) {
		Locale.setDefault(Helpers.getLocal(this));
		super.onConfigurationChanged(newConfig);
		final boolean tabletLocal = MirakelPreferences.isTablet();
		if (tabletLocal != this.isTablet) {
			forceRebuildLayout(tabletLocal);
		} else {
			getListFragment().setActivity(this);
			getTasksFragment().setActivity(this);
			this.mDrawerToggle.onConfigurationChanged(newConfig);
		}
	}

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		this.darkTheme = MirakelPreferences.isDark();
		if (this.darkTheme) {
			setTheme(R.style.AppBaseThemeDARK);
		}
		Locale.setDefault(Helpers.getLocal(this));
		super.onCreate(savedInstanceState);

		// Set Alarms

		new Thread(new Runnable() {
			@Override
			public void run() {
				ReminderAlarm.updateAlarms(getApplicationContext());
				if (!MirakelPreferences.containsHighlightSelected()) {
					final SharedPreferences.Editor editor = MirakelPreferences
							.getEditor();
					editor.putBoolean("highlightSelected",
							MainActivity.this.isTablet);
					editor.commit();
				}

				if (!MirakelPreferences.containsStartupAllLists()) {
					final SharedPreferences.Editor editor = MirakelPreferences
							.getEditor();
					editor.putBoolean("startupAllLists", false);
					editor.putString("startupList", ""
							+ ListMirakel.first().getId());
					editor.commit();
				}
				// We should remove this in the future, nobody uses such old versions (hopefully)
				if (MainActivity.updateTasksUUID) {
					final List<Task> tasks = Task.all();
					for (final Task t : tasks) {
						t.setUUID(java.util.UUID.randomUUID().toString());
						t.safeSave();
					}
				}

				registerReceiver(new MainActivityBroadcastReceiver(
						MainActivity.this), new IntentFilter(
						Mirakel.SYNC_FINISHED));
			}
		}).run();
		this.isTablet = MirakelPreferences.isTablet();
		MainActivity.isRTL = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
				&& getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
		this.currentPosition = MainActivity.getTasksFragmentPosition();
		this.highlightSelected = MirakelPreferences.highlightSelected();

		draw();

		if (this.mViewPager.getCurrentItem() != this.currentPosition) {
			this.mViewPager.postDelayed(new Runnable() {

				@Override
				public void run() {
					MainActivity.this.mViewPager
							.setCurrentItem(MainActivity.this.currentPosition);

				}
			}, 10);
		}

		setCurrentTask(this.currentTask, false);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		updateLists();
		getMenuInflater().inflate(R.menu.main, menu);
		this.menu = menu;
		if (!this.showNavDrawer) {
			loadMenu(this.currentPosition, false, false);
		} else {
			this.showNavDrawer = false;
			loadMenu(-1, false, false);
		}
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	// Fix Intent-behavior
	// default is not return new Intent by calling getIntent
	@Override
	protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (this.mDrawerToggle.onOptionsItemSelected(item)) return true;
		switch (item.getItemId()) {
			case R.id.menu_delete:
				handleDestroyTask(this.currentTask);
				updateShare();
				return true;
			case R.id.menu_move:
				handleMoveTask(this.currentTask);
				return true;
			case R.id.list_delete:
				handleDestroyList(this.currentList);
				return true;
			case R.id.task_sorting:
				this.currentList = ListDialogHelpers.handleSortBy(this,
						this.currentList, new Helpers.ExecInterface() {

							@Override
							public void exec() {
								setCurrentList(MainActivity.this.currentList);
							}
						}, null);
				return true;
			case R.id.menu_new_list:
				getListFragment().editList(null);
				return true;
			case R.id.menu_sort_lists:
				final boolean t = !item.isChecked();
				getListFragment().enableDrop(t);
				item.setChecked(t);
				return true;
			case R.id.menu_settings:
				final Intent intent = new Intent(MainActivity.this,
						SettingsActivity.class);
				startActivityForResult(intent, MainActivity.RESULT_SETTINGS);
				break;
			case R.id.menu_contact:
				Helpers.contact(this);
				break;
			case R.id.menu_sync_now:
				final Bundle bundle = new Bundle();
				bundle.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY,
						true);
				// bundle.putBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE,true);
				bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
				bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);

				new Thread(new Runnable() {
					@SuppressLint("InlinedApi")
					@Override
					public void run() {
						List<AccountMirakel> accounts = AccountMirakel
								.getEnabled(true);
						for (AccountMirakel a : accounts) {
							// davdroid accounts should be there only from API>=14...
							ContentResolver.requestSync(
									a.getAndroidAccount(),
									a.getType() == ACCOUNT_TYPES.TASKWARRIOR ? Mirakel.AUTHORITY_TYP
											: CalendarContract.AUTHORITY,
									bundle);
						}

					}
				}).start();
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
				final Intent killIntent = new Intent(getApplicationContext(),
						SplashScreenActivity.class);
				killIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				killIntent.setAction(SplashScreenActivity.EXIT);
				startActivity(killIntent);
				finish();
				return false;
			case R.id.menu_undo:
				UndoHistory.undoLast();
				updateCurrentListAndTask();
				if (this.currentPosition == getTaskFragmentPosition()) {
					setCurrentTask(this.currentTask);
				} else {
					getListFragment().getAdapter()
							.changeData(ListMirakel.all());
					getListFragment().getAdapter().notifyDataSetChanged();
					getTasksFragment().getAdapter().changeData(
							getCurrentList().tasks(), getCurrentList().getId());
					getTasksFragment().getAdapter().notifyDataSetChanged();
					if (!MirakelPreferences.isTablet()
							&& this.currentPosition == MainActivity
									.getTasksFragmentPosition()) {
						setCurrentList(getCurrentList());
					}
				}
				ReminderAlarm.updateAlarms(this);
				break;
			case R.id.mark_as_subtask:
				TaskDialogHelpers.handleSubtask(this, this.currentTask, null,
						true);
				break;
			case R.id.menu_task_clone:
				try {
					final Task newTask = this.currentTask.create();
					setCurrentTask(newTask, true);
					getListFragment().update();
					updatesForTask(newTask);
				} catch (final NoSuchListException e) {
					Log.wtf(MainActivity.TAG, "List vanished on task cloning");
				}
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
		if (getTaskFragment() != null
				&& getTasksFragment().getAdapter() != null
				&& MirakelPreferences.swipeBehavior() && !this.skipSwipe) {
			this.skipSwipe = true;
			setCurrentTask(getTasksFragment().getAdapter().lastTouched(), false);
		}
	}

	@Override
	public void onPageScrollStateChanged(final int state) {
		if (this.mViewPager.getCurrentItem() == getTaskFragmentPosition()) {
			this.skipSwipe = false;
		}
	}

	@Override
	public void onPageSelected(final int position) {
		if (getTasksFragment() == null) return;
		getTasksFragment().closeActionMode();
		getTaskFragment().closeActionMode();
		if (MirakelPreferences.lockDrawerInTaskFragment()
				&& position == getTaskFragmentPosition()) {
			this.mDrawerLayout
					.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		} else {
			this.mDrawerLayout
					.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		}
		loadMenu(position);

	}

	@SuppressLint("NewApi")
	@Override
	protected void onPause() {
		if (getTasksFragment() != null) {
			getTasksFragment().clearFocus();
		}
		final Intent intent = new Intent(this, MainWidgetProvider.class);
		intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		// Use an array and EXTRA_APPWIDGET_IDS instead of
		// AppWidgetManager.EXTRA_APPWIDGET_ID,
		// since it seems the onUpdate() is only fired on that:
		final Context context = getApplicationContext();
		final ComponentName name = new ComponentName(context,
				MainWidgetProvider.class);
		final int widgets[] = AppWidgetManager.getInstance(context)
				.getAppWidgetIds(name);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgets);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			for (final int id : widgets) {
				AppWidgetManager.getInstance(this)
						.notifyAppWidgetViewDataChanged(id,
								R.id.widget_tasks_list);
			}
		}
		sendBroadcast(intent);
		TaskDialogHelpers.stopRecording();
		super.onPause();
	}

	@Override
	protected void onPostCreate(final Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		this.mDrawerToggle.syncState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (this.isResumend) {
			setupLayout();
		}
		this.isResumend = true;
		// showMessageFromSync();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		// outState.putString("tab", mTabHost.getCurrentTabTag()); // save the
		// tab
		// selected
		super.onSaveInstanceState(outState);
	}

	/**
	 * Ugly Wrapper TODO make it more beautiful
	 * 
	 * @param task
	 */
	public void saveTask(final Task task) {
		Log.v(MainActivity.TAG, "Saving task… (task:" + task.getId());
		task.safeSave();
		updatesForTask(task);
	}

	private void search(final String query) {
		setCurrentList(new SearchList(this, query));
		this.mViewPager.setCurrentItem(MainActivity.getTasksFragmentPosition());
	}

	public void setCurrentList(final ListMirakel currentList) {
		setCurrentList(currentList, null, true, true);
	}

	/**
	 * Set the current list and update the views
	 * 
	 * @param currentList
	 * @param switchFragment
	 */
	public void setCurrentList(final ListMirakel currentList, final boolean switchFragment) {
		setCurrentList(currentList, null, switchFragment, true);
	}

	public void setCurrentList(final ListMirakel currentList, final View currentView) {
		setCurrentList(currentList, currentView, true, true);
	}

	public void setCurrentList(final ListMirakel currentList, View currentView, final boolean switchFragment, final boolean resetGoBackTo) {
		if (currentList == null) return;
		if (resetGoBackTo) {
			this.goBackTo.clear();
		}
		this.currentList = currentList;
		if (this.mDrawerLayout != null) {
			this.mDrawerLayout.closeDrawers();
		}

		this.currentTask = currentList.getFirstTask();
		if (this.currentTask == null) {
			this.currentTask = Task.getDummy(getApplicationContext());
		}

		if (getTasksFragment() != null) {
			getTasksFragment().updateList(true);
			if (!MirakelPreferences.isTablet() && switchFragment) {
				this.mViewPager.setCurrentItem(MainActivity
						.getTasksFragmentPosition());
			}
		}
		if (currentView == null && this.listFragment != null
				&& getListFragment().getAdapter() != null) {
			currentView = getListFragment().getAdapter().getViewForList(
					currentList);
		}

		if (currentView != null && this.highlightSelected) {
			clearHighlighted();
			if (this.oldClickedList != null) {
				this.oldClickedList.setSelected(false);
				this.oldClickedList.setBackgroundColor(0x00000000);
			}
			currentView.setBackgroundColor(getResources().getColor(
					R.color.pressed_color));
			this.oldClickedList = currentView;
		}
		if (switchFragment) {
			setCurrentTask(this.currentTask);
		}
		if (this.currentPosition == getTasksFragmentPosition()) {
			getSupportActionBar().setTitle(currentList.getName());
		}

	}

	/**
	 * Set the current task and update the view
	 * 
	 * @param currentTask
	 */
	public void setCurrentTask(final Task currentTask) {
		setCurrentTask(currentTask, false);
	}

	public void setCurrentTask(final Task currentTask, final boolean switchFragment) {
		setCurrentTask(currentTask, switchFragment, true);
	}

	public void setCurrentTask(final Task currentTask, final boolean switchFragment, final boolean resetGoBackTo) {
		if (currentTask == null) return;
		this.currentTask = currentTask;
		if (resetGoBackTo) {
			this.goBackTo.clear();
		}

		highlightCurrentTask(currentTask, false);

		if (getTaskFragment() != null) {
			getTaskFragment().update(currentTask);
			final boolean smooth = this.mViewPager.getCurrentItem() != getTaskFragmentPosition();
			if (!switchFragment) return;
			// Fix buggy behavior
			this.mViewPager.setCurrentItem(
					MainActivity.getTasksFragmentPosition(), false);
			this.mViewPager.setCurrentItem(getTaskFragmentPosition(), false);
			this.mViewPager.setCurrentItem(
					MainActivity.getTasksFragmentPosition(), false);
			this.mViewPager.setCurrentItem(getTaskFragmentPosition(), smooth);
		}
	}

	public void setFileUri(final Uri file) {
		this.fileUri = file;
	}

	/**
	 * Set the Task, to which we switch, if the user press the back-button. It is reseted, if one of
	 * the setCurrent*-functions on called
	 * 
	 * @param t
	 */
	public void setGoBackTo(final Task t) {
		this.goBackTo.push(t);
	}

	public void setSkipSwipe() {
		this.skipSwipe = true;
	}

	public void setTaskFragment(final TaskFragment tf) {
		this.taskFragment = tf;
		if (this.taskFragment != null && this.currentTask != null) {
			this.taskFragment.update(this.currentTask);
		}

	}

	/**
	 * Initialize the ViewPager and setup the rest of the layout
	 */
	private void setupLayout() {
		this.closeOnBack = false;
		if (this.currentList == null) {
			setCurrentList(SpecialList.firstSpecial());
		}
		// Initialize ViewPager
		if (!this.isResumend && this.mPagerAdapter == null) {
			intializeFragments();
		}
		NotificationService.updateNotificationAndWidget(this);
		this.startIntent = getIntent();
		if (this.startIntent == null || this.startIntent.getAction() == null) {
			Log.d(MainActivity.TAG, "action null");
		} else if (this.startIntent.getAction().equals(MainActivity.SHOW_TASK)
				|| this.startIntent.getAction().equals(
						MainActivity.SHOW_TASK_FROM_WIDGET)) {
			final Task task = TaskHelper.getTaskFromIntent(this.startIntent);
			if (task != null) {
				this.skipSwipe = true;
				this.currentList = task.getList();

				// setCurrentList(task.getList());
				this.mViewPager.postDelayed(new Runnable() {

					@Override
					public void run() {
						setCurrentTask(task, true);
						MainActivity.this.mViewPager.setCurrentItem(
								getTaskFragmentPosition(), false);

					}
				}, 1);
				new Thread(new Runnable() {

					@Override
					public void run() {
						MainActivity.this.mViewPager.setCurrentItem(
								getTaskFragmentPosition(), false);

					}
				}).start();

			} else {
				Log.d(MainActivity.TAG, "task null");
			}
			if (this.startIntent.getAction().equals(
					MainActivity.SHOW_TASK_FROM_WIDGET)) {
				this.closeOnBack = true;
			}
		} else if (this.startIntent.getAction().equals(Intent.ACTION_SEND)
				|| this.startIntent.getAction().equals(
						Intent.ACTION_SEND_MULTIPLE)) {
			this.closeOnBack = true;
			this.newTaskContent = this.startIntent
					.getStringExtra(Intent.EXTRA_TEXT);
			this.newTaskSubject = this.startIntent
					.getStringExtra(Intent.EXTRA_SUBJECT);

			// If from google now, the content is the subject…
			if (this.startIntent.getCategories() != null
					&& this.startIntent.getCategories().contains(
							"com.google.android.voicesearch.SELF_NOTE")) {
				if (!this.newTaskContent.equals("")) {
					this.newTaskSubject = this.newTaskContent;
					this.newTaskContent = "";
				}
			}

			if (!this.startIntent.getType().equals("text/plain")) {
				if (this.newTaskSubject == null) {
					this.newTaskSubject = MirakelPreferences
							.getImportFileTitle();
				}
			}
			final ListMirakel listFromSharing = MirakelPreferences
					.getImportDefaultList(false);
			if (listFromSharing != null) {
				addTaskFromSharing(listFromSharing);
			} else {
				final AlertDialog.Builder builder = new AlertDialog.Builder(
						this);
				builder.setTitle(R.string.import_to);
				final List<CharSequence> items = new ArrayList<CharSequence>();
				final List<Integer> list_ids = new ArrayList<Integer>();
				final int currentItem = 0;
				for (final ListMirakel list : ListMirakel.all()) {
					if (list.getId() > 0) {
						items.add(list.getName());
						list_ids.add(list.getId());
					}
				}
				builder.setSingleChoiceItems(
						items.toArray(new CharSequence[items.size()]),
						currentItem, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog, final int item) {
								addTaskFromSharing(ListMirakel.getList(list_ids
										.get(item)));
								dialog.dismiss();
							}
						});
				builder.create().show();
			}
		} else if (this.startIntent.getAction().equals(MainActivity.SHOW_LIST)
				|| this.startIntent.getAction().contains(
						MainActivity.SHOW_LIST_FROM_WIDGET)) {

			int listId;
			if (this.startIntent.getAction().equals(MainActivity.SHOW_LIST)) {
				listId = this.startIntent.getIntExtra(MainActivity.EXTRA_ID, 0);
			} else {
				listId = Integer.parseInt(this.startIntent.getAction().replace(
						MainActivity.SHOW_LIST_FROM_WIDGET, ""));
			}
			Log.v(MainActivity.TAG, "ListId: " + listId);
			ListMirakel list = ListMirakel.getList(listId);
			if (list == null) {
				list = SpecialList.firstSpecial();
			}
			setCurrentList(list);
			if (this.startIntent.getAction().contains(
					MainActivity.SHOW_LIST_FROM_WIDGET)) {
				this.closeOnBack = true;
			}
		} else if (this.startIntent.getAction().equals(MainActivity.SHOW_LISTS)) {
			this.mDrawerLayout.openDrawer(Mirakel.GRAVITY_LEFT);
		} else if (this.startIntent.getAction().equals(Intent.ACTION_SEARCH)) {
			final String query = this.startIntent
					.getStringExtra(SearchManager.QUERY);
			search(query);
		} else if (this.startIntent.getAction().contains(
				MainActivity.ADD_TASK_FROM_WIDGET)) {
			final int listId = Integer.parseInt(this.startIntent.getAction()
					.replace(MainActivity.ADD_TASK_FROM_WIDGET, ""));
			setCurrentList(ListMirakel.getList(listId));
			if (getTasksFragment() != null && getTasksFragment().isReady()) {
				getTasksFragment().focusNew(true);
			} else {
				this.mViewPager.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (getTasksFragment() != null) {
							getTasksFragment().focusNew(true);
						} else {
							Log.wtf(MainActivity.TAG, "Tasksfragment null");
						}
					}
				}, 10);
			}

		} else {
			this.mViewPager.setCurrentItem(getTaskFragmentPosition());
		}
		if ((this.startIntent == null || this.startIntent.getAction() == null || !this.startIntent
				.getAction().contains(MainActivity.ADD_TASK_FROM_WIDGET))
				&& getTasksFragment() != null) {
			getTasksFragment().clearFocus();
		}
		setIntent(null);
		if (this.currentList == null) {
			setCurrentList(SpecialList.firstSpecial());
		}
	}

	public void unlockDrawer() {
		this.mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
	}

	private void updateCurrentListAndTask() {
		if (this.currentTask == null && this.currentList == null) return;
		if (this.currentTask != null) {
			this.currentTask = Task.get(this.currentTask.getId());
		} else {
			if (this.currentList != null) {
				final List<Task> currentTasks = this.currentList
						.tasks(MirakelPreferences.showDoneMain());
				if (currentTasks.size() == 0) {
					this.currentTask = Task.getDummy(getApplicationContext());
				} else {
					this.currentTask = currentTasks.get(0);
				}
			}
		}
		if (this.currentList != null) {
			this.currentList = ListMirakel.getList(this.currentList.getId());
		} else {
			this.currentList = this.currentTask.getList();
		}

	}

	/**
	 * Update the internal List of Lists (e.g. for the Move Task dialog)
	 */
	public void updateLists() {
		this.lists = ListMirakel.all(false);
	}

	/**
	 * Executes some View–Updates if a Task was changed
	 * 
	 * @param task
	 */
	public void updatesForTask(final Task task) {
		if (this.currentTask != null
				&& task.getId() == this.currentTask.getId()) {
			this.currentTask = task;
			getTaskFragment().update(task);
		}
		getTasksFragment().updateList(false);
		getListFragment().update();
		NotificationService.updateNotificationAndWidget(this);

	}

	public void updateShare() {
		if (this.menu != null) {
			final MenuItem share_list = this.menu.findItem(R.id.share_list);
			if (share_list != null && this.mViewPager != null) {
				this.mViewPager.post(new Runnable() {

					@Override
					public void run() {
						if (MainActivity.this.currentList.countTasks() == 0) {
							share_list.setVisible(false);
						} else if (MainActivity.this.currentList.countTasks() > 0) {
							share_list.setVisible(true);
						}

					}
				});

			} else if (this.currentPosition == MainActivity
					.getTasksFragmentPosition()
					&& share_list == null
					&& this.currentList != null
					&& this.currentList.countTasks() > 0
					&& !this.mDrawerLayout.isDrawerOpen(Mirakel.GRAVITY_LEFT)) {
				loadMenu(MainActivity.getTasksFragmentPosition(), true, true);

			}
		}

	}

	public void updateUI() {
		getTasksFragment().updateList(false);
		// This is very buggy
		// getTaskFragment().updateLayout();
	}

}
