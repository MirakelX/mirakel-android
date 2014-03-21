package de.azapps.mirakelandroid.test;

import android.content.Context;

import com.robotium.solo.Solo;

public class TestUtils {
	/**
	 * Tests if all menu items are shown
	 * 
	 * @param message
	 * @param menu_items
	 */
	void menuTest(final Solo solo, final String message,
			final String[] menu_items) {
		for (final String item : menu_items) {
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
	void menuTest(final Context ctx, final Solo solo, final String message,
			final int[] menu_items) {
		final String string_items[] = new String[menu_items.length];
		for (int i = 0; i < menu_items.length; i++) {
			string_items[i] = ctx.getString(menu_items[i]);
		}
		menuTest(solo, message, string_items);
	}
}
