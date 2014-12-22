package de.azapps.mirakel.settings;
/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
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

import android.content.Context;
import android.gesture.GestureOverlayView;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.common.base.Optional;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;


public class SwipeLinearLayout extends GestureOverlayView  {
    public final static int SWIPEABLE_VIEW = R.id.remove_handle;

    private static final int SWIPE_THRESHOLD_VELOCITY = 1200;

    private int xDelta;
    private int selectedChildIndex = 0;
    private int startMargin;
    @Nullable
    private Thread resetThread = null;
    @Nullable
    private View currentTouchView = null;
    @NonNull
    private LinearLayout mContainer;
    private float xDown;
    private float yDown;
    @NonNull
    private Optional<OnItemRemoveListener> listener = absent();
    @NonNull
    private final VelocityTracker velocityTracker = VelocityTracker.obtain();
    private int screenW;


    @Override
    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (processTouchDown(ev)) {
                velocityTracker.addMovement(ev);
                return true;
            }
            return false;
        default:
            break;
        }
        return false;
    }

    private int getDistanceTo(MotionEvent ev) {

        final double x = (double) (ev.getX() - xDown);
        final double y = (double) (ev.getY() - yDown);
        return (int) Math.sqrt((x * x) + (y * y));
    }

    boolean processTouchDown(final @NonNull MotionEvent ev) {
        final int x = (int) ev.getRawX();
        velocityTracker.clear();
        if (stopReset()) {
            updateParams(currentTouchView, x - xDelta);
            return false;
        }
        currentTouchView = null;
        for (int i = 0; i < mContainer.getChildCount(); i++) {
            final View child = mContainer.getChildAt(i);
            final Rect outRect = new Rect(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
            if (outRect.contains((int) ev.getX(), (int) ev.getY())) {
                currentTouchView = child;
                selectedChildIndex = i;
                break;
            }
        }
        if ((currentTouchView == null) || !(currentTouchView instanceof RelativeLayout) ) {
            currentTouchView = null;
            return false;
        }
        final MarginLayoutParams initialParams;
        try {
            initialParams = (MarginLayoutParams) currentTouchView.getLayoutParams();
        } catch (final ClassCastException e) {
            currentTouchView = null;
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            xDelta = x - initialParams.leftMargin;
            startMargin = initialParams.leftMargin;
        } else {
            startMargin = initialParams.getMarginStart();
            xDelta = x - initialParams.getMarginStart();
        }
        xDown = ev.getX();
        yDown = ev.getY();
        return isSwipeable(currentTouchView).isPresent();
    }

    @NonNull
    private static Optional<OnClickListener> isSwipeable(@Nullable final View v) {
        if (v == null) {
            return absent();
        }
        final Object tag;
        if (v instanceof RelativeLayout) {
            tag = ((RelativeLayout) v).getChildAt(0).getTag(SWIPEABLE_VIEW);
        } else {
            tag = v.getTag(SWIPEABLE_VIEW);
        }
        if (tag instanceof OnClickListener) {
            return of((OnClickListener)tag);
        }
        return absent();
    }

    @Override
    public boolean onTouchEvent(@NonNull final MotionEvent event) {
        if (super.onTouchEvent(event)) {
            return true;
        }
        velocityTracker.addMovement(event);
        velocityTracker.computeCurrentVelocity(1000);
        final int x = (int) event.getRawX();
        switch (MotionEvent.obtain(event).getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            break;
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
            final Optional<OnClickListener> click = isSwipeable(currentTouchView);
            if (click.isPresent() &&
                !(ViewConfiguration.getTapTimeout() < event.getEventTime() - event.getDownTime() ||
                  ViewConfiguration.get(getContext()).getScaledTouchSlop() < getDistanceTo(event))) {
                currentTouchView.setOnClickListener(click.get());
                currentTouchView.performClick();
                currentTouchView = null;
                return true;
            }
            synchronized (this) {
                resetThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(100L);
                        } catch (final InterruptedException ignored) {
                            return;
                        }
                        synchronized (SwipeLinearLayout.this) {
                            if ((resetThread != null) && !resetThread.isInterrupted() && (currentTouchView != null)) {
                                currentTouchView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateParams(currentTouchView, startMargin);
                                        currentTouchView = null;
                                        getParent().requestDisallowInterceptTouchEvent(false);
                                    }
                                });
                                resetThread = null;
                            }
                        }
                    }
                });
                resetThread.start();
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if ((isSwipeable(currentTouchView).isPresent() &&
                 (ViewConfiguration.getTapTimeout() < (event.getEventTime() - event.getDownTime()))) ||
                (ViewConfiguration.get(getContext()).getScaledTouchSlop() < getDistanceTo(event))) {

                getParent().requestDisallowInterceptTouchEvent(true);
                stopReset();
                updateParams(currentTouchView, x - xDelta);

                if ((currentTouchView != null) && listener.isPresent() && (getDistanceTo(event) > (screenW / 2)) &&
                    (Math.abs(velocityTracker.getXVelocity()) > SWIPE_THRESHOLD_VELOCITY)) {
                    listener.get().onRemove(selectedChildIndex);
                    mContainer.removeView(currentTouchView);
                    currentTouchView = null;
                    return false;
                }
            }

            break;
        default:
            return false;
        }
        return true;
    }

    @Override
    public void onSizeChanged (final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenW = w;
    }

    private synchronized boolean stopReset() {
        if (resetThread != null) {
            resetThread.interrupt();
            resetThread = null;
        }
        return currentTouchView != null;
    }

    private static void updateParams(final @Nullable View view, final int value) {
        if (view == null) {
            return;
        }
        final MarginLayoutParams layoutParams = (MarginLayoutParams) view.getLayoutParams();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            layoutParams.leftMargin = value;
        } else {
            layoutParams.setMarginStart(value);
        }
        view.setLayoutParams(layoutParams);
        view.invalidate();
    }

    public void setOnItemRemovedListener(final @Nullable OnItemRemoveListener onItemRemovedListener) {
        this.listener = fromNullable(onItemRemovedListener);
    }


    public interface OnItemRemoveListener {
        void onRemove(int index);
    }

    public SwipeLinearLayout(final Context context) {
        this(context, null);
    }

    public SwipeLinearLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        requestDisallowInterceptTouchEvent(true);
        mContainer = new LinearLayout(getContext());
        mContainer.setOrientation(LinearLayout.VERTICAL);
        addView(mContainer);
        setWillNotDraw(true);
    }


    @Override
    protected boolean addViewInLayout(@NonNull final View child, final int index,
                                      final ViewGroup.LayoutParams params, final boolean preventRequestLayout) {
        mContainer.addView(setupChildView(child));
        return true;
    }

    private static View setupChildView(final View view) {
        if (!isSwipeable(view).isPresent()) {
            return view;
        }
        View child = view;
        if (!(view instanceof RelativeLayout)) {
            final  RelativeLayout wrapper = new RelativeLayout(view.getContext());

            wrapper.requestDisallowInterceptTouchEvent(true);
            wrapper.addView(view);
            child = wrapper;
        }
        //c.setOnTouchListener(null);
        //c.setOnClickListener(null);
        return child;
    }

    @Override
    public void addView(@NonNull final View child, final int index,
                        final ViewGroup.LayoutParams params) {
        if (mContainer.equals(child)) {
            super.addView(child, index, params);
            return;
        }
        mContainer.addView(setupChildView(child), index);
    }
}
