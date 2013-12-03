package de.azapps.mirakelandroid.test.main_activity;

import android.test.ActivityInstrumentationTestCase2;

import com.jayway.android.robotium.solo.Solo;

import de.azapps.mirakel.main_activity.MainActivity;

public class TestListFragment extends
		ActivityInstrumentationTestCase2<MainActivity> {
	Solo solo;

	public TestListFragment() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		solo = new Solo(getInstrumentation(), getActivity());
	}

	@Override
	protected void tearDown() throws Exception {
		solo.finishOpenedActivities();
	}

	public void testClick() {

	}

	public void testAdd() {

	}

	public void testDelete() {

	}

	public void testMove() {

	}

	public void testShareList() {

	}

	public void testEditList() {

	}

	public void testChangeAccount() {

	}

}
