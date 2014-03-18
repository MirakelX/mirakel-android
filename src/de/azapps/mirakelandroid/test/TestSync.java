package de.azapps.mirakelandroid.test;

import java.util.Random;

import android.database.Cursor;
import android.test.ActivityInstrumentationTestCase2;

import com.jayway.android.robotium.solo.Solo;

import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakelandroid.R;

public class TestSync extends ActivityInstrumentationTestCase2<MainActivity> {
	Solo solo;

	public TestSync() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		solo = new Solo(getInstrumentation(), getActivity());
		solo.clickInList(0);
	}

	@Override
	protected void tearDown() throws Exception {
		solo.finishOpenedActivities();
	}
	
	public void testTWSync(){
		Random randomGenerator = new Random();
		while(true){
			solo.drag(0, 100, 50, 100, 100);
			solo.clickInList(5);//hardcoded list
			DatabaseHelper openHelper = new DatabaseHelper(getActivity());
			Cursor c=openHelper.getReadableDatabase().rawQuery("select count(*) from tasks left join lists on lists._id=tasks.list_id where lists.account_id=6;", null);
			int taskCount=0;
			if(c.getCount()!=0){
				c.moveToFirst();
				taskCount=c.getInt(0);
			}
			c.close();
			//change something to trigger sync
			solo.clickInList(0);
			solo.clickOnView(solo.getView(R.id.task_prio));
			solo.clickInList(randomGenerator.nextInt(6));
			solo.clickOnMenuItem(getActivity().getString(R.string.action_sync_now));
			
			solo.sleep(5000);
			
			
			c=openHelper.getReadableDatabase().rawQuery("select count(*) from tasks left join lists on lists._id=tasks.list_id where lists.account_id=6;", null);
			if(c.getCount()!=0){
				c.moveToFirst();
				assertTrue(c.getInt(0)==taskCount);
			}else{
				assertTrue(false);
			}
			c.close();
			
			openHelper.close();
			
		}
		
	}

}
