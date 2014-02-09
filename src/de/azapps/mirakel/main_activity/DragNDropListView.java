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
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.tools.Log;

public class DragNDropListView extends ListView {

	public interface DragListener {
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
		 * Called when a drag starts.
		 * 
		 * @param itemView
		 *            - the view of the item to be dragged i.e. the drag view
		 */
		void onStartDrag(View itemView);

		/**
		 * Called when a drag stops. Any changes in onStartDrag need to be
		 * undone here so that the view can be used in the list again.
		 * 
		 * @param itemView
		 *            - the view of the item to be dragged i.e. the drag view
		 */
		void onStopDrag(View itemView);
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
	public interface RemoveListener {

		/**
		 * Called when an item is to be removed
		 * 
		 * @param which
		 *            - indicates which item to remove.
		 */
		void onRemove(int which);
	}

	private static final String TAG = "DragNDropListView";
	protected boolean			allowRemove;
	private boolean enableDrag;

	protected boolean			isSpecial;
	DragListener mDragListener;

	protected boolean			mDragMode;
	int mDragPointOffset; // Used to adjust drag view location
	ImageView mDragView;

	DropListener mDropListener;

	int mEndPosition;

	GestureDetector mGestureDetector;

	RemoveListener mRemoveListener;
	int mStartPosition;

	protected boolean			resetEndDrop;

	protected int				startX;

	protected int				startY;
	private int					xOffset;

	public DragNDropListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.resetEndDrop = true;
	}

	public void allowRemove(boolean allow) {
		this.allowRemove = allow;
	}

	// move the drag view
	private void drag(int x, int y) {
		if (this.mDragView != null) {
			WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) this.mDragView
					.getLayoutParams();
			layoutParams.x = this.allowRemove ? x : this.xOffset;
			layoutParams.y = y - this.mDragPointOffset;
			WindowManager mWindowManager = (WindowManager) getContext()
					.getSystemService(Context.WINDOW_SERVICE);
			mWindowManager.updateViewLayout(this.mDragView, layoutParams);
			if (y > 8 / 9 * getHeight()
					&& getChildCount() > getLastVisiblePosition()) {
				smoothScrollToPosition(pointToPosition(x, y) + 1);
			} else if (y < 3 / 9 * getHeight()) {
				smoothScrollToPosition(pointToPosition(x, y) - 1);
			}
			if (this.mDragListener != null) {
				this.mDragListener.onDrag(x, y, this);
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();
		if (action == MotionEvent.ACTION_DOWN && (x < getWidth() / 3
				|| this.allowRemove)) {// width<~imagewidth
			this.mDragMode = true;
		}
		Log.w(TAG, "x: " + x + "   Y: " + y);
		if (!this.mDragMode || !this.enableDrag) return super.onTouchEvent(ev);

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				if (!this.resetEndDrop) {
					break;
				}
				this.mStartPosition = pointToPosition(x, y);
				if (this.mStartPosition != INVALID_POSITION) {

					int mItemPosition = this.mStartPosition
							- getFirstVisiblePosition();
					if (mItemPosition < SpecialList.getSpecialListCount()
							- getFirstVisiblePosition()) {
						this.isSpecial = true;
						// break;
					} else {
						this.isSpecial = false;
					}
					Log.d(TAG, "" + mItemPosition);
					this.mDragPointOffset = y
							- getChildAt(mItemPosition).getTop();
					this.mDragPointOffset -= (int) ev.getRawY() - y;
					startDrag(mItemPosition, y);
					drag(0, y);
				}
				this.startX = x;
				this.startY = y;
				break;
			case MotionEvent.ACTION_MOVE:
				if (this.allowRemove) {
					drag(x, y);
				} else {
					drag(0, y);
				}
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
			default:

				this.mDragMode = false;
				postDelayed(new Runnable() {

					@Override
					public void run() {
						if (DragNDropListView.this.mDragMode == false) {
							DragNDropListView.this.mEndPosition = pointToPosition(
									x, y);
							if (DragNDropListView.this.mStartPosition >= DragNDropListView.this.mEndPosition
									&& y - DragNDropListView.this.startY > 0) {
								DragNDropListView.this.mEndPosition=getLastVisiblePosition();
							}
							stopDrag(DragNDropListView.this.mStartPosition
									- getFirstVisiblePosition());
							if (DragNDropListView.this.mEndPosition < SpecialList.getSpecialListCount()
									&& !DragNDropListView.this.isSpecial) {
								DragNDropListView.this.mEndPosition = SpecialList.getSpecialListCount();
								Log.d(TAG, "not special");
							} else if (DragNDropListView.this.mEndPosition > SpecialList
									.getSpecialListCount()
									&& DragNDropListView.this.isSpecial) {
								DragNDropListView.this.mEndPosition = SpecialList
										.getSpecialListCount() - 1;
								Log.d(TAG, "special");
							}
							if (DragNDropListView.this.mDropListener != null
									&& DragNDropListView.this.mStartPosition != INVALID_POSITION
									&& DragNDropListView.this.mEndPosition != INVALID_POSITION
									&& Math.abs(x
											- DragNDropListView.this.startX) < getWidth() / 3) {
								DragNDropListView.this.mDropListener.onDrop(
										DragNDropListView.this.mStartPosition,
										DragNDropListView.this.mEndPosition);
							} else if (DragNDropListView.this.mRemoveListener != null
									&& DragNDropListView.this.mStartPosition != INVALID_POSITION
									&& DragNDropListView.this.allowRemove
									&& Math.abs(x
											- DragNDropListView.this.startX) > 2 * getWidth() / 3) {
								DragNDropListView.this.mRemoveListener
								.onRemove(DragNDropListView.this.mStartPosition);
							}
							DragNDropListView.this.resetEndDrop = true;
						}

					}
				}, 100);
				break;
		}
		return true;
	}

	public void setDragListener(DragListener l) {
		this.mDragListener = l;
	}

	public void setDropListener(DropListener l) {
		this.mDropListener = l;
	}

	public void setEnableDrag(boolean enableDrag) {
		this.enableDrag = enableDrag;
		this.resetEndDrop = true;
		this.xOffset = -1 * getWidth() / 2 + getWidth() / 6;
	}

	public void setRemoveListener(RemoveListener l) {
		this.mRemoveListener = l;
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
		if (this.mDragListener != null) {
			this.mDragListener.onStartDrag(item);
		}

		// Create a copy of the drawing cache so that it does not get recycled
		// by the framework when the list tries to clean up memory
		Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());

		WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
		mWindowParams.gravity = Gravity.TOP;
		mWindowParams.x = -this.xOffset;
		mWindowParams.y = y - this.mDragPointOffset;

		mWindowParams.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
		mWindowParams.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
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
		this.mDragView = v;
	}

	// destroy drag view
	protected void stopDrag(int itemIndex) {
		if (this.mDragView != null) {
			if (this.mDragListener != null) {
				this.mDragListener.onStopDrag(getChildAt(itemIndex));
			}
			this.mDragView.setVisibility(GONE);
			WindowManager wm = (WindowManager) getContext().getSystemService(
					Context.WINDOW_SERVICE);
			wm.removeView(this.mDragView);
			this.mDragView.setImageDrawable(null);
			this.mDragView = null;
		}
	}

}
