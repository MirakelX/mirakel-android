package com.sleepbot.datetimepicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.TextView;

/**
 * Fake Button class, used so TextViews can announce themselves as Buttons, for
 * accessibility.
 */
@SuppressLint("NewApi")
public class AccessibleTextView extends TextView {

	public AccessibleTextView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void onInitializeAccessibilityEvent(final AccessibilityEvent event) {
		if (Build.VERSION.SDK_INT >= 14) {
			super.onInitializeAccessibilityEvent(event);
			event.setClassName(Button.class.getName());
		}
	}

	@Override
	public void onInitializeAccessibilityNodeInfo(
			final AccessibilityNodeInfo info) {
		if (Build.VERSION.SDK_INT >= 14) {
			super.onInitializeAccessibilityNodeInfo(info);
			info.setClassName(Button.class.getName());
		}
	}
}
