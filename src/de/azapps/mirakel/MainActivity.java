package de.azapps.mirakel;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.Toast;

/**
 * @see https 
 *      ://thepseudocoder.wordpress.com/2011/10/13/android-tabs-viewpager-swipe
 *      -able-tabs-ftw/
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
	TasksDataSource taskDataSource;
	ListsDataSource listDataSource;
	Task currentTask;
	List_mirakle currentList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		taskDataSource=new TasksDataSource(this);
		taskDataSource.open();
		listDataSource=new ListsDataSource(this);
		listDataSource.open();
		
		currentList=listDataSource.getList(0);
		currentTask=taskDataSource.getTasks(0).get(0);

		if (savedInstanceState != null) {
			// mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}
		// Intialise ViewPager
		this.intialiseViewPager();
		mViewPager.setCurrentItem(1);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		this.menu = menu;
		onPageSelected(1);
		return true;
	}
	private List<List_mirakle> lists;
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_delete:
			new AlertDialog.Builder(this)
					.setTitle(this.getString(R.string.task_delete_title))
					.setMessage(this.getString(R.string.task_delete_content))
					.setPositiveButton(this.getString(R.string.Yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									taskDataSource.deleteTask(currentTask);
									mViewPager.setCurrentItem(1);
								}
							})
					.setNegativeButton(this.getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// do nothing
								}
							}).show();
			return true;
		case android.R.id.home:
			finish();
			return true;
		case R.id.menu_move:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(
					R.string.dialog_move);
/*
			builder.setPositiveButton(this.getString(R.string.Yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

						}
					});
			builder.setNegativeButton(this.getString(R.string.no),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// do nothing
						}
					});*/
			ListsDataSource listds=new ListsDataSource(this);
			listds.open();
			lists=listds.getAllLists();
			listds.close();
			List<CharSequence> items=new ArrayList<CharSequence>();
			final List<Integer> list_ids=new ArrayList<Integer>();
			for(List_mirakle list:lists) {
				if(list.getId()>0){
					items.add(list.getName());
					list_ids.add(list.getId());
				}
			}
			
			builder.setItems(items.toArray(new CharSequence[items.size()]), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					currentTask.setListId(list_ids.get(item));
					taskDataSource.saveTask(currentTask);
				}
			});

			AlertDialog dialog = builder.create();
			dialog.show();
			return true;

		default:
			return super.onOptionsItemSelected(item);

		}
	}


	/**
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
	 */
	protected void onSaveInstanceState(Bundle outState) {
		// outState.putString("tab", mTabHost.getCurrentTabTag()); // save the
		// tab
		// selected
		super.onSaveInstanceState(outState);
	}

	/**
	 * Initialise ViewPager
	 */
	private void intialiseViewPager() {

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

	@Override
	public void onPageScrolled(int position, float positionOffset,
			int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		if(menu==null)
			return;
		int newmenu;
		switch (position) {
		case 0:
			newmenu = R.menu.activity_list;
			break;
		case 1:
			newmenu = R.menu.tasks;
			break;
		case 2:
			newmenu = R.menu.activity_task;
			break;
		default:
			Toast.makeText(getApplicationContext(), "Where are the dragons?",
					Toast.LENGTH_LONG).show();
			return;
		}

		// Configure to use the desired menu

		menu.clear();
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(newmenu, menu);
	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}
	
	
}
