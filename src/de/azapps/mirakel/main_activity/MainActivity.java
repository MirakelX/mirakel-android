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
import java.util.List;
import java.util.Vector;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.PagerAdapter;
import de.azapps.mirakel.R;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.helper.ListDialogHelpers;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakel.static_activities.CreditsActivity;
import de.azapps.mirakel.static_activities.SettingsActivity;

/**
 * @see "https://thepseudocoder.wordpress.com/2011/10/13/android-tabs-viewpager-swipe-able-tabs-ftw/"
 * @author az
 * 
 */
public class MainActivity extends FragmentActivity implements
		ViewPager.OnPageChangeListener {

	/**
	 * /** The {@link ViewPager} that will host the section contents.
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

	private static final int LIST_FRAGMENT = 0, TASKS_FRAGMENT = 1,
			TASK_FRAGMENT = 2;
	protected static final int RESULT_SPEECH_NAME = 1,
			RESULT_SPEECH_CONTENT = 2, RESULT_SPEECH = 3;
	private static final String TAG = "MainActivity";

	public static String EXTRA_ID = "de.azapps.mirakel.EXTRA_TASKID";
	public static String SHOW_TASK = "de.azapps.mirakel.SHOW_TASK";
	public static String SHOW_LIST = "de.azapps.mirakel.SHOW_LIST";
	public static String SHOW_LISTS = "de.azapps.mirakel.SHOW_LISTS";
	public static String SHOW_LIST_FROM_WIDGET = "de.azapps.mirakel.SHOW_LIST_FROM_WIDGET";
	private SharedPreferences preferences;

	private int currentPosition = 1;
	private Parcelable tasksState, listState;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (!preferences.contains("startupAllLists")) {

			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("startupAllLists", false);
			editor.putString("startupList", "" + SpecialList.first().getId());
			editor.commit();
		}
		setContentView(R.layout.activity_main);
		setupLayout();
		//currentList=preferences.getInt("s", defValue)
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
							//tasksFragment.updateList();
							tasksFragment.update();
							listFragment.update();
						}
					});
			return true;
		case R.id.menu_new_list:
			ListMirakel list = ListMirakel.newList(this
					.getString(R.string.list_menu_new_list));
			listFragment.update();
			listFragment.editList(list);
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
			startActivity(intent);
			break;
		case R.id.menu_sync_now_list:
		case R.id.menu_sync_now_task:
		case R.id.menu_sync_now_tasks:
			Bundle bundle = new Bundle();
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, true);
			//bundle.putBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE,true);
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
		case 0:
			newmenu = R.menu.activity_list;
			this.setTitle(getString(R.string.list_title));
			if (listState != null)
				listFragment.setState(listState);
			break;
		case 1:
			listFragment.enable_drop(false);
			newmenu = R.menu.tasks;
			if(currentList==null)
				return;
			this.setTitle(currentList.getName());
			if (tasksState != null && currentPosition != LIST_FRAGMENT)
				tasksFragment.setState(tasksState);
			break;
		case 2:
			newmenu = R.menu.activity_task;
			taskFragment.update();
			this.setTitle(currentTask.getName());
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
	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && null != data) {
			ArrayList<String> text = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			switch (requestCode) {
			case RESULT_SPEECH_CONTENT:
				((EditText) findViewById(R.id.edit_content)).setText(text
						.get(0));
				break;
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
		NotificationService.updateNotificationAndWidget(this);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		taskFragment.setActivity(this);
		listFragment.setActivity(this);
		tasksFragment.setActivity(this);
	}

	/**
	 * Initialize the ViewPager and setup the rest of the layout
	 */
	private void setupLayout() {
		setCurrentList(SpecialList.first());

		// Initialize ViewPager
		this.intializeViewPager();
		NotificationService.updateNotificationAndWidget(this);
		Intent intent = getIntent();
		if (intent.getAction() == SHOW_TASK) {
			int taskId = intent.getIntExtra(EXTRA_ID, 0);
			if (taskId != 0) {
				Task task = Task.get(taskId);
				currentList = task.getList();
				setCurrentTask(task);
				return;
			}
		} else if (intent.getAction() == SHOW_LIST
				|| intent.getAction() == SHOW_LIST_FROM_WIDGET) {

			int listId = intent.getIntExtra(EXTRA_ID, 0);
			ListMirakel list = ListMirakel.getList(listId);
			setCurrentList(list);
			return;
		} else if (intent.getAction() == SHOW_LISTS) {
			mViewPager.setCurrentItem(LIST_FRAGMENT);
		} else {
			mViewPager.setCurrentItem(TASKS_FRAGMENT);
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
		new AlertDialog.Builder(this)
				.setTitle(task.getName())
				.setMessage(this.getString(R.string.task_delete_content))
				.setPositiveButton(this.getString(R.string.Yes),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								task.delete();
								setCurrentList(currentList);
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
						task.save();
						tasksFragment.update();
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
		tasksFragment = new TasksFragment();
		tasksFragment.setActivity(this);
		fragments.add(tasksFragment);
		taskFragment = new TaskFragment();
		taskFragment.setActivity(this);
		fragments.add(taskFragment);
		this.mPagerAdapter = new PagerAdapter(
				super.getSupportFragmentManager(), fragments);
		//
		this.mViewPager = (ViewPager) super.findViewById(R.id.viewpager);
		this.mViewPager.setAdapter(this.mPagerAdapter);
		this.mViewPager.setOnPageChangeListener(this);
		mViewPager.setOffscreenPageLimit(2);

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
		this.currentTask = currentTask;
		if (taskFragment != null) {
			taskFragment.update();
			mViewPager.setCurrentItem(TASK_FRAGMENT);
		}
	}

	/**
	 * Return the currently showed List
	 * 
	 * @return
	 */
	ListMirakel getCurrentList() {
		return currentList;
	}

	/**
	 * Set the current list and update the views
	 * 
	 * @param currentList
	 */
	void setCurrentList(ListMirakel currentList) {
		this.currentList = currentList;
		if (tasksFragment != null) {
			tasksFragment.updateList();
			tasksFragment.update();
			mViewPager.setCurrentItem(TASKS_FRAGMENT);
		}
		if(currentList==null)
			return;
		List<Task> currentTasks = currentList.tasks();
		if (currentTasks.size() == 0) {
			currentTask = new Task(getApplicationContext().getString(
					R.string.task_empty));
		} else {
			currentTask = currentTasks.get(0);
		}
		if (taskFragment != null) {
			taskFragment.update();
		}
		if (currentPosition == TASKS_FRAGMENT)
			this.setTitle(currentList.getName());

	}

	/**
	 * Ugly Wrapper TODO make it more beautiful
	 * 
	 * @param task
	 */
	void saveTask(Task task) {
		Log.v(TAG, "Saving task… (task:" + task.getId() + " – current:"
				+ currentTask.getId());
		task.save();
		updatesForTask(task);
	}

	/**
	 * Executes some View–Updates if a Task was changed
	 * @param task
	 */
	void updatesForTask(Task task) {
		if (task.getId() == currentTask.getId()) {
			currentTask = task;
			taskFragment.update();
		}
		tasksFragment.update(false);
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

}
