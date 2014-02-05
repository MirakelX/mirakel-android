package de.azapps.tools.taptest;

import android.view.GestureDetector;
import android.view.MotionEvent;

public class TapGestureDetector extends GestureDetector.SimpleOnGestureListener {
	private TapTest taptest;

	public TapGestureDetector(TapTest taptest) {
		this.taptest = taptest;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		taptest.write("solo.clickOnScreen(" + e.getX() + "f, " + e.getY() + "f);");
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		taptest.write("solo.drag(" + e1.getX() + "f, " + e2.getX() + "f, "
				+ e1.getY() + "f, " + e2.getY() + "f, 10);");
		return true;
	}

}
