package de.azapps.tools.taptest;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import de.azapps.mirakel.helper.BuildHelper;

public class TapTestLayout extends DrawerLayout {
	private static GestureDetectorCompat	mDetector;

	public TapTestLayout(Context context) {
		super(context);
	}

	public static void setGestureDetector(GestureDetectorCompat mDetector) {
		TapTestLayout.mDetector = mDetector;
	}

	public TapTestLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public TapTestLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (BuildHelper.TAPTEST && mDetector != null)
			mDetector.onTouchEvent(event);
		return false; // Do not prevent the rest of the layout from being touched
	}

}
