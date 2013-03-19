package de.azapps.mirakel;

import android.view.MotionEvent;
import android.view.View;

public interface SwipeCommand {
	void runCommand(View v, MotionEvent event);

}
