/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.mobeta.android.dslv;

import android.graphics.Point;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;

/**
 * Class that starts and stops item drags on a {@link DragSortListView} based on
 * touch gestures. This class also inherits from {@link SimpleFloatViewManager},
 * which provides basic float View creation.
 *
 * An instance of this class is meant to be passed to the methods
 * {@link DragSortListView#setTouchListener()} and
 * {@link DragSortListView#setFloatViewManager()} of your
 * {@link DragSortListView} instance.
 */
public class DragSortController extends SimpleFloatViewManager implements
    View.OnTouchListener, GestureDetector.OnGestureListener {

    /**
     * Drag init mode enum.
     */
    public static final int ON_DOWN = 0;
    public static final int ON_DRAG = 1;
    public static final int ON_LONG_PRESS = 2;

    private int mDragInitMode = ON_DOWN;

    private boolean mSortEnabled = true;

    /**
     * Remove mode enum.
     */
    public static final int CLICK_REMOVE = 0;
    public static final int FLING_REMOVE = 1;

    /**
     * The current remove mode.
     */
    private int mRemoveMode;

    private boolean mRemoveEnabled = false;
    private boolean mIsRemoving = false;

    private final GestureDetector mDetector;

    private final GestureDetector mFlingRemoveDetector;

    private final int mTouchSlop;

    public static final int MISS = -1;

    private int mHitPos = MISS;
    private int mFlingHitPos = MISS;

    private int mClickRemoveHitPos = MISS;

    private final int[] mTempLoc = new int[2];

    private int mItemX;
    private int mItemY;

    private int mCurrX;
    private int mCurrY;

    private boolean mDragging = false;

    private static final float mFlingSpeed = 500f;

    private int mDragHandleId;

    private int mClickRemoveId;

    private int mFlingHandleId;
    private boolean mCanDrag;

    private final DragSortListView mDslv;
    private int mPositionX;

    /**
     * Calls {@link #DragSortController(DragSortListView, int)} with a 0 drag
     * handle id, FLING_RIGHT_REMOVE remove mode, and ON_DOWN drag init. By
     * default, sorting is enabled, and removal is disabled.
     *
     * @param dslv
     *            The DSLV instance
     */
    public DragSortController(final DragSortListView dslv) {
        this(dslv, 0, ON_DOWN, FLING_REMOVE);
    }

    public DragSortController(final DragSortListView dslv,
                              final int dragHandleId, final int dragInitMode, final int removeMode) {
        this(dslv, dragHandleId, dragInitMode, removeMode, 0);
    }

    public DragSortController(final DragSortListView dslv,
                              final int dragHandleId, final int dragInitMode,
                              final int removeMode, final int clickRemoveId) {
        this(dslv, dragHandleId, dragInitMode, removeMode, clickRemoveId, 0);
    }

    /**
     * By default, sorting is enabled, and removal is disabled.
     *
     * @param dslv
     *            The DSLV instance
     * @param dragHandleId
     *            The resource id of the View that represents the drag handle in
     *            a list item.
     */
    public DragSortController(final DragSortListView dslv,
                              final int dragHandleId, final int dragInitMode,
                              final int removeMode, final int clickRemoveId,
                              final int flingHandleId) {
        super(dslv);
        this.mDslv = dslv;
        this.mDetector = new GestureDetector(dslv.getContext(), this);
        this.mFlingRemoveDetector = new GestureDetector(dslv.getContext(),
                this.mFlingRemoveListener);
        this.mFlingRemoveDetector.setIsLongpressEnabled(false);
        this.mTouchSlop = ViewConfiguration.get(dslv.getContext())
                          .getScaledTouchSlop();
        this.mDragHandleId = dragHandleId;
        this.mClickRemoveId = clickRemoveId;
        this.mFlingHandleId = flingHandleId;
        setRemoveMode(removeMode);
        setDragInitMode(dragInitMode);
    }

    public int getDragInitMode() {
        return this.mDragInitMode;
    }

    /**
     * Set how a drag is initiated. Needs to be one of {@link ON_DOWN},
     * {@link ON_DRAG}, or {@link ON_LONG_PRESS}.
     *
     * @param mode
     *            The drag init mode.
     */
    public void setDragInitMode(final int mode) {
        this.mDragInitMode = mode;
    }

    /**
     * Enable/Disable list item sorting. Disabling is useful if only item
     * removal is desired. Prevents drags in the vertical direction.
     *
     * @param enabled
     *            Set <code>true</code> to enable list item sorting.
     */
    public void setSortEnabled(final boolean enabled) {
        this.mSortEnabled = enabled;
    }

    public boolean isSortEnabled() {
        return this.mSortEnabled;
    }

    /**
     * One of {@link CLICK_REMOVE}, {@link FLING_RIGHT_REMOVE},
     * {@link FLING_LEFT_REMOVE}, {@link SLIDE_RIGHT_REMOVE}, or
     * {@link SLIDE_LEFT_REMOVE}.
     */
    public void setRemoveMode(final int mode) {
        this.mRemoveMode = mode;
    }

    public int getRemoveMode() {
        return this.mRemoveMode;
    }

    /**
     * Enable/Disable item removal without affecting remove mode.
     */
    public void setRemoveEnabled(final boolean enabled) {
        this.mRemoveEnabled = enabled;
    }

    public boolean isRemoveEnabled() {
        return this.mRemoveEnabled;
    }

    /**
     * Set the resource id for the View that represents the drag handle in a
     * list item.
     *
     * @param id
     *            An android resource id.
     */
    public void setDragHandleId(final int id) {
        this.mDragHandleId = id;
    }

    /**
     * Set the resource id for the View that represents the fling handle in a
     * list item.
     *
     * @param id
     *            An android resource id.
     */
    public void setFlingHandleId(final int id) {
        this.mFlingHandleId = id;
    }

    /**
     * Set the resource id for the View that represents click removal button.
     *
     * @param id
     *            An android resource id.
     */
    public void setClickRemoveId(final int id) {
        this.mClickRemoveId = id;
    }

    /**
     * Sets flags to restrict certain motions of the floating View based on
     * DragSortController settings (such as remove mode). Starts the drag on the
     * DragSortListView.
     *
     * @param position
     *            The list item position (includes headers).
     * @param deltaX
     *            Touch x-coord minus left edge of floating View.
     * @param deltaY
     *            Touch y-coord minus top edge of floating View.
     *
     * @return True if drag started, false otherwise.
     */
    public boolean startDrag(final int position, final int deltaX,
                             final int deltaY) {
        int dragFlags = 0;
        if (this.mSortEnabled && !this.mIsRemoving) {
            dragFlags |= DragSortListView.DRAG_POS_Y
                         | DragSortListView.DRAG_NEG_Y;
        }
        if (this.mRemoveEnabled && this.mIsRemoving) {
            dragFlags |= DragSortListView.DRAG_POS_X;
            dragFlags |= DragSortListView.DRAG_NEG_X;
        }
        this.mDragging = this.mDslv.startDrag(
                             position - this.mDslv.getHeaderViewsCount(), dragFlags, deltaX,
                             deltaY);
        return this.mDragging;
    }

    @Override
    public boolean onTouch(final View v, final MotionEvent ev) {
        if (!this.mDslv.isDragEnabled() || this.mDslv.listViewIntercepted()) {
            return false;
        }
        this.mDetector.onTouchEvent(ev);
        if (this.mRemoveEnabled && this.mDragging
            && this.mRemoveMode == FLING_REMOVE) {
            this.mFlingRemoveDetector.onTouchEvent(ev);
        }
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            this.mCurrX = (int) ev.getX();
            this.mCurrY = (int) ev.getY();
            break;
        case MotionEvent.ACTION_UP:
            if (this.mRemoveEnabled && this.mIsRemoving) {
                final int x = this.mPositionX >= 0 ? this.mPositionX
                              : -this.mPositionX;
                final int removePoint = this.mDslv.getWidth() / 2;
                if (x > removePoint) {
                    this.mDslv.stopDragWithVelocity(true, 0);
                }
            }
        case MotionEvent.ACTION_CANCEL:
            this.mIsRemoving = false;
            this.mDragging = false;
            break;
        }
        return false;
    }

    /**
     * Overrides to provide fading when slide removal is enabled.
     */
    @Override
    public void onDragFloatView(final View floatView, final Point position,
                                final Point touch) {
        if (this.mRemoveEnabled && this.mIsRemoving) {
            this.mPositionX = position.x;
        }
    }

    /**
     * Get the position to start dragging based on the ACTION_DOWN MotionEvent.
     * This function simply calls {@link #dragHandleHitPosition(MotionEvent)}.
     * Override to change drag handle behavior; this function is called
     * internally when an ACTION_DOWN event is detected.
     *
     * @param ev
     *            The ACTION_DOWN MotionEvent.
     *
     * @return The list position to drag if a drag-init gesture is detected;
     *         MISS if unsuccessful.
     */
    public int startDragPosition(final MotionEvent ev) {
        return dragHandleHitPosition(ev);
    }

    public int startFlingPosition(final MotionEvent ev) {
        return this.mRemoveMode == FLING_REMOVE ? flingHandleHitPosition(ev)
               : MISS;
    }

    /**
     * Checks for the touch of an item's drag handle (specified by
     * {@link #setDragHandleId(int)}), and returns that item's position if a
     * drag handle touch was detected.
     *
     * @param ev
     *            The ACTION_DOWN MotionEvent.
     *
     * @return The list position of the item whose drag handle was touched; MISS
     *         if unsuccessful.
     */
    public int dragHandleHitPosition(final MotionEvent ev) {
        return viewIdHitPosition(ev, this.mDragHandleId);
    }

    public int flingHandleHitPosition(final MotionEvent ev) {
        return viewIdHitPosition(ev, this.mFlingHandleId);
    }

    public int viewIdHitPosition(final MotionEvent ev, final int id) {
        final int x = (int) ev.getX();
        final int y = (int) ev.getY();
        final int touchPos = this.mDslv.pointToPosition(x, y); // includes
        // headers/footers
        final int numHeaders = this.mDslv.getHeaderViewsCount();
        final int numFooters = this.mDslv.getFooterViewsCount();
        final int count = this.mDslv.getCount();
        // Log.d("mobeta", "touch down on position " + itemnum);
        // We're only interested if the touch was on an
        // item that's not a header or footer.
        if (touchPos != AdapterView.INVALID_POSITION && touchPos >= numHeaders
            && touchPos < count - numFooters) {
            final View item = this.mDslv.getChildAt(touchPos
                                                    - this.mDslv.getFirstVisiblePosition());
            final int rawX = (int) ev.getRawX();
            final int rawY = (int) ev.getRawY();
            final View dragBox = id == 0 ? item : (View) item.findViewById(id);
            if (dragBox != null) {
                dragBox.getLocationOnScreen(this.mTempLoc);
                if (rawX > this.mTempLoc[0] && rawY > this.mTempLoc[1]
                    && rawX < this.mTempLoc[0] + dragBox.getWidth()
                    && rawY < this.mTempLoc[1] + dragBox.getHeight()) {
                    this.mItemX = item.getLeft();
                    this.mItemY = item.getTop();
                    return touchPos;
                }
            }
        }
        return MISS;
    }

    @Override
    public boolean onDown(final MotionEvent ev) {
        if (this.mRemoveEnabled && this.mRemoveMode == CLICK_REMOVE) {
            this.mClickRemoveHitPos = viewIdHitPosition(ev, this.mClickRemoveId);
        }
        this.mHitPos = startDragPosition(ev);
        if (this.mHitPos != MISS && this.mDragInitMode == ON_DOWN) {
            startDrag(this.mHitPos, (int) ev.getX() - this.mItemX,
                      (int) ev.getY() - this.mItemY);
        }
        this.mIsRemoving = false;
        this.mCanDrag = true;
        this.mPositionX = 0;
        this.mFlingHitPos = startFlingPosition(ev);
        return true;
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        final int x1 = (int) e1.getX();
        final int y1 = (int) e1.getY();
        final int x2 = (int) e2.getX();
        final int y2 = (int) e2.getY();
        final int deltaX = x2 - this.mItemX;
        final int deltaY = y2 - this.mItemY;
        if (this.mCanDrag && !this.mDragging
            && (this.mHitPos != MISS || this.mFlingHitPos != MISS)) {
            if (this.mHitPos != MISS) {
                if (this.mDragInitMode == ON_DRAG
                    && Math.abs(y2 - y1) > this.mTouchSlop
                    && this.mSortEnabled) {
                    startDrag(this.mHitPos, deltaX, deltaY);
                } else if (this.mDragInitMode != ON_DOWN
                           && Math.abs(x2 - x1) > this.mTouchSlop
                           && this.mRemoveEnabled) {
                    this.mIsRemoving = true;
                    startDrag(this.mFlingHitPos, deltaX, deltaY);
                }
            } else if (this.mFlingHitPos != MISS) {
                if (Math.abs(x2 - x1) > this.mTouchSlop && this.mRemoveEnabled) {
                    this.mIsRemoving = true;
                    startDrag(this.mFlingHitPos, deltaX, deltaY);
                } else if (Math.abs(y2 - y1) > this.mTouchSlop) {
                    this.mCanDrag = false; // if started to scroll the list then
                    // don't allow sorting nor
                    // fling-removing
                }
            }
        }
        // return whatever
        return false;
    }

    @Override
    public void onLongPress(final MotionEvent e) {
        // Log.d("mobeta", "lift listener long pressed");
        if (this.mHitPos != MISS && this.mDragInitMode == ON_LONG_PRESS) {
            this.mDslv
            .performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            startDrag(this.mHitPos, this.mCurrX - this.mItemX, this.mCurrY
                      - this.mItemY);
        }
    }

    // complete the OnGestureListener interface
    @Override
    public final boolean onFling(final MotionEvent e1, final MotionEvent e2,
                                 final float velocityX, final float velocityY) {
        return false;
    }

    // complete the OnGestureListener interface
    @Override
    public boolean onSingleTapUp(final MotionEvent ev) {
        if (this.mRemoveEnabled && this.mRemoveMode == CLICK_REMOVE) {
            if (this.mClickRemoveHitPos != MISS) {
                this.mDslv.removeItem(this.mClickRemoveHitPos
                                      - this.mDslv.getHeaderViewsCount());
            }
        }
        return true;
    }

    // complete the OnGestureListener interface
    @Override
    public void onShowPress(final MotionEvent ev) {
        // do nothing
    }

    private final GestureDetector.OnGestureListener mFlingRemoveListener = new
    GestureDetector.SimpleOnGestureListener() {
        @Override
        public final boolean onFling(final MotionEvent e1,
                                     final MotionEvent e2, final float velocityX,
                                     final float velocityY) {
            // Log.d("mobeta", "on fling remove called");
            if (DragSortController.this.mRemoveEnabled
                && DragSortController.this.mIsRemoving) {
                final int w = DragSortController.this.mDslv.getWidth();
                final int minPos = w / 5;
                if (velocityX > DragSortController.mFlingSpeed) {
                    if (DragSortController.this.mPositionX > -minPos) {
                        DragSortController.this.mDslv.stopDragWithVelocity(
                            true, velocityX);
                    }
                } else if (velocityX < -DragSortController.mFlingSpeed) {
                    if (DragSortController.this.mPositionX < minPos) {
                        DragSortController.this.mDslv.stopDragWithVelocity(
                            true, velocityX);
                    }
                }
                DragSortController.this.mIsRemoving = false;
            }
            return false;
        }
    };

}
