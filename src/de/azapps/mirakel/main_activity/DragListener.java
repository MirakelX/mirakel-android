package de.azapps.mirakel.main_activity;

import android.view.View;
import android.widget.ListView;

public interface DragListener {
	/**
	 * Called when a drag starts.
	 * @param itemView - the view of the item to be dragged i.e. the drag view
	 */
	void onStartDrag(View itemView);
	
	/**
	 * Called when a drag is to be performed.
	 * @param x - horizontal coordinate of MotionEvent.
	 * @param y - verital coordinate of MotionEvent.
	 * @param listView - the listView
	 */
	void onDrag(int x, int y, ListView listView);
	
	/**
	 * Called when a drag stops.
	 * Any changes in onStartDrag need to be undone here 
	 * so that the view can be used in the list again.
	 * @param itemView - the view of the item to be dragged i.e. the drag view
	 */
	void onStopDrag(View itemView);
}