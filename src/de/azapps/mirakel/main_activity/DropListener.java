package de.azapps.mirakel.main_activity;

public interface DropListener {

	/**
	 * Called when an item is to be dropped.
	 * 
	 * @param from
	 *            - index item started at.
	 * @param to
	 *            - index to place item at.
	 */
	void onDrop(int from, int to);
}