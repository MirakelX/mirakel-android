/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.main_activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.list.SpecialList;

public class DragNDropListView extends ListView {

	private static final String TAG = "DragNDropListView";

	boolean mDragMode;
	private boolean enableDrag;

	int mStartPosition;
	int mEndPosition;
	int mDragPointOffset; // Used to adjust drag view location

	ImageView mDragView;
	GestureDetector mGestureDetector;

	DropListener mDropListener;
	RemoveListener mRemoveListener;
	DragListener mDragListener;

	private boolean isSpecial;

	public void setEnableDrag(boolean enableDrag) {
		this.enableDrag = enableDrag;
	}

	public DragNDropListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setDropListener(DropListener l) {
		mDropListener = l;
	}

	public void setRemoveListener(RemoveListener l) {
		mRemoveListener = l;
	}

	public void setDragListener(DragListener l) {
		mDragListener = l;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();
		if (action == MotionEvent.ACTION_DOWN && x < this.getWidth() / 3) {// width<~imagewidth
			mDragMode = true;
		}

		if (!mDragMode || !enableDrag) {
			return super.onTouchEvent(ev);
		}

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mStartPosition = pointToPosition(x, y);
			if (mStartPosition != INVALID_POSITION) {

				int mItemPosition = mStartPosition - getFirstVisiblePosition();
				if (mItemPosition < SpecialList.getSpecialListCount()
						- getFirstVisiblePosition()) {
					isSpecial = true;
					// break;
				} else {
					isSpecial = false;
				}
				Log.d(TAG, "" + mItemPosition);
				mDragPointOffset = y - getChildAt(mItemPosition).getTop();
				mDragPointOffset -= ((int) ev.getRawY()) - y;
				startDrag(mItemPosition, y);
				drag(0, y);
			}
			break;
		case MotionEvent.ACTION_MOVE:
			drag(0, y);
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
		default:
			mDragMode = false;
			mEndPosition = pointToPosition(x, y);
			stopDrag(mStartPosition - getFirstVisiblePosition());
			if (mEndPosition < SpecialList.getSpecialListCount() && !isSpecial) {
				mEndPosition = SpecialList.getSpecialListCount();
				Log.d(TAG, "not special");
			} else if (mEndPosition > SpecialList.getSpecialListCount()
					&& isSpecial) {
				mEndPosition = SpecialList.getSpecialListCount() - 1;
				Log.d(TAG, "special");
			}
			if (mDropListener != null && mStartPosition != INVALID_POSITION
					&& mEndPosition != INVALID_POSITION)
				mDropListener.onDrop(mStartPosition, mEndPosition);
			break;
		}
		return true;
	}

	// move the drag view
	private void drag(int x, int y) {
		if (mDragView != null) {
			WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mDragView
					.getLayoutParams();
			layoutParams.x = x;
			layoutParams.y = y - mDragPointOffset;
			WindowManager mWindowManager = (WindowManager) getContext()
					.getSystemService(Context.WINDOW_SERVICE);
			mWindowManager.updateViewLayout(mDragView, layoutParams);
			if (y > (8 / 9) * getHeight()
					&& getChildCount() > getLastVisiblePosition()) {
				smoothScrollToPosition(pointToPosition(x, y) + 1);
			} else if (y < (3 / 9) * getHeight()) {
				smoothScrollToPosition(pointToPosition(x, y) - 1);
			}
			if (mDragListener != null)
				mDragListener.onDrag(x, y, this);
		}
	}

	// enable the drag view for dragging
	private void startDrag(int itemIndex, int y) {
		stopDrag(itemIndex);

		View item = getChildAt(itemIndex);
		if (item == null)
			return;
		item.setDrawingCacheEnabled(true);
		item.destroyDrawingCache();
		item.buildDrawingCache();
		if (mDragListener != null)
			mDragListener.onStartDrag(item);

		// Create a copy of the drawing cache so that it does not get recycled
		// by the framework when the list tries to clean up memory
		Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());

		WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
		mWindowParams.gravity = Gravity.TOP;
		mWindowParams.x = 0;
		mWindowParams.y = y - mDragPointOffset;

		mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
		mWindowParams.format = PixelFormat.TRANSLUCENT;
		mWindowParams.windowAnimations = 0;

		Context context = getContext();
		ImageView v = new ImageView(context);
		v.setImageBitmap(bitmap);

		WindowManager mWindowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		mWindowManager.addView(v, mWindowParams);
		mDragView = v;
	}

	// destroy drag view
	private void stopDrag(int itemIndex) {
		if (mDragView != null) {
			if (mDragListener != null)
				mDragListener.onStopDrag(getChildAt(itemIndex));
			mDragView.setVisibility(GONE);
			WindowManager wm = (WindowManager) getContext().getSystemService(
					Context.WINDOW_SERVICE);
			wm.removeView(mDragView);
			mDragView.setImageDrawable(null);
			mDragView = null;
		}
	}

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

	public interface DragListener {
		/**
		 * Called when a drag starts.
		 * 
		 * @param itemView
		 *            - the view of the item to be dragged i.e. the drag view
		 */
		void onStartDrag(View itemView);

		/**
		 * Called when a drag is to be performed.
		 * 
		 * @param x
		 *            - horizontal coordinate of MotionEvent.
		 * @param y
		 *            - verital coordinate of MotionEvent.
		 * @param listView
		 *            - the listView
		 */
		void onDrag(int x, int y, ListView listView);

		/**
		 * Called when a drag stops. Any changes in onStartDrag need to be
		 * undone here so that the view can be used in the list again.
		 * 
		 * @param itemView
		 *            - the view of the item to be dragged i.e. the drag view
		 */
		void onStopDrag(View itemView);
	}

	public interface RemoveListener {

		/**
		 * Called when an item is to be removed
		 * 
		 * @param which
		 *            - indicates which item to remove.
		 */
		void onRemove(int which);
	}

}
