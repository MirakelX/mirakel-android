package de.azapps.mirakelandroid.test;

import android.content.Context;

import com.jayway.android.robotium.solo.Solo;

public class TestUtils {
	/**
	 * Tests if all menu items are shown
	 * 
	 * @param message
	 * @param menu_items
	 */
	void menuTest(Solo solo, String message, String[] menu_items) {
		for (String item : menu_items) {
			solo.clickOnMenuItem(item);
			solo.goBack();
		}
	}

	/**
	 * Tests if all menu items are shown
	 * 
	 * @param message
	 * @param menu_items
	 */
	void menuTest(Context ctx, Solo solo, String message, int[] menu_items) {
		String string_items[] = new String[menu_items.length];
		for (int i = 0; i < menu_items.length; i++) {
			string_items[i] = ctx.getString(menu_items[i]);
		}
		menuTest(solo, message, string_items);
	}
}
