package de.azapps.mirakelandroid.test.main_activity;

import android.test.ActivityInstrumentationTestCase2;

import com.jayway.android.robotium.solo.Solo;

import de.azapps.mirakel.main_activity.MainActivity;

public class TestMainActivity extends
		ActivityInstrumentationTestCase2<MainActivity> {
	Solo solo;

	public TestMainActivity() {
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


	public void testMainActivity() {
	}
}
