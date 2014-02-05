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
		taptest.write("solo.clickOnScreen(" + e.getX() + ", " + e.getY() + ");");
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		taptest.write("solo.drag(" + e1.getX() + ", " + e2.getX() + ", "
				+ e1.getY() + ", " + e2.getY() + ", 10);");
		return true;
	}

}
