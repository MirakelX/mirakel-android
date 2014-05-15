package de.azapps.mirakelandroid.test;

import android.test.AndroidTestCase;

public class MirakelTestCase extends AndroidTestCase {
	@Override
	protected void setUp() throws Exception {
		TestHelper.init(getContext());
	}

	@Override
	protected void tearDown() throws Exception {
		// TODO Auto-generated method stub
		super.tearDown();
		TestHelper.terminate();
	}

}
