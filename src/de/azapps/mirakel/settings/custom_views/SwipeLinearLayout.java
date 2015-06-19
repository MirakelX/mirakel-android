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

package de.azapps.mirakel.settings.custom_views;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;

import com.google.common.base.Optional;

import java.util.Timer;
import java.util.TimerTask;

import de.azapps.mirakel.settings.R;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;


public class SwipeLinearLayout extends LinearLayout  {
    public static final int SWIPEABLE_VIEW = R.id.remove_handle;

    private static final int SWIPE_THRESHOLD_VELOCITY = 1200;

    private int selectedChildIndex = 0;
    @Nullable
    private Thread resetThread = null;
    @Nullable
    private View currentTouchView = null;
    private float xDown;
    private float yDown;
    @NonNull
    private Optional<OnItemRemoveListener> removeListener = absent();
    @NonNull
    private Optional<OnUndoListener> undoListener = absent();

    @NonNull
    private final VelocityTracker velocityTracker = VelocityTracker.obtain();
    private int screenW;
    private RelativeLayout.LayoutParams leaveBehindParams;
    private double childWidth;
    private boolean isRemoving = false;
    private boolean moved = false;
    private boolean undo = false;

    private final boolean isRTL;

    public SwipeLinearLayout(final Context context) {
        this(context, null);
    }



    public SwipeLinearLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        requestDisallowInterceptTouchEvent(true);
        setWillNotDraw(true);
        leaveBehindParams = new RelativeLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        leaveBehindParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        isRTL = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) &&
                (context.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
    }


    @Override
    public boolean onInterceptTouchEvent(@NonNull final MotionEvent ev) {
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (processTouchDown(ev)) {
                moved = false;
                velocityTracker.addMovement(ev);
            }
            return false;
        case MotionEvent.ACTION_MOVE:
            if (isInMotion(ev, (int) ev.getX())) {
                return !handleMove(ev, (int) ev.getX());
            }
            break;
        case MotionEvent.ACTION_UP:
            if (currentTouchView != null) {
                if (undo) {
                    ((Button)currentTouchView.findViewById(R.id.undo)).performClick();
                }
                currentTouchView = null;
            }
            break;
        default:
            break;
        }
        return false;
    }

    private int getDistanceTo(@NonNull final MotionEvent ev) {

        final double x = (double) (ev.getX() - xDown);
        final double y = (double) (ev.getY() - yDown);
        return (int) Math.sqrt((x * x) + (y * y));
    }

    boolean processTouchDown(@NonNull final MotionEvent ev) {
        velocityTracker.clear();
        currentTouchView = null;
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            final Rect outRect = new Rect(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
            if (outRect.contains((int) ev.getX(), (int) ev.getY())) {
                currentTouchView = child;
                selectedChildIndex = i;
                break;
            }
        }
        if (!(currentTouchView instanceof ViewSwitcher)) {
            currentTouchView = null;
            return false;
        }
        xDown = ev.getX();
        yDown = ev.getY();
        return isSwipeable(currentTouchView);
    }

    private static boolean isSwipeable(@Nullable final View v) {
        if (v == null) {
            return false;
        }
        if (v.getTag(SWIPEABLE_VIEW) != null) {
            return true;
        }
        return (v instanceof LinearLayout) && (((LinearLayout) v).getChildCount() > 1) &&
               (((LinearLayout) v).getChildAt(1).getTag(SWIPEABLE_VIEW) != null);
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
            if (handleEndTouchEvent(event, x)) {
                return false;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (isInMotion(event, x) && handleMove(event, x)) {
                return false;
            }
            break;
        default:
            return false;
        }
        return true;
    }

    private boolean handleEndTouchEvent(@NonNull final MotionEvent event, final int x) {
        if ((undo && currentTouchView != null) || (isSwipeable(currentTouchView) &&
                !((ViewConfiguration.getTapTimeout() < (event.getEventTime() - event.getDownTime())) ||
                  (ViewConfiguration.get(getContext()).getScaledTouchSlop() < getDistanceTo(event))))) {
            updateParams(currentTouchView, 0);
            if (undo && !moved) {
                ((Button) currentTouchView.findViewById(R.id.undo)).performClick();
            } else {
                currentTouchView.onTouchEvent(event);
            }
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
                        if (moved && (resetThread != null) && !resetThread.isInterrupted() && (currentTouchView != null) &&
                            !isRemoving) {
                            currentTouchView.post(new Runnable() {
                                @Override
                                public void run() {
                                    animateView((int) (x - xDown), 300L, new DecelerateInterpolator(),
                                    new ValueAnimator.AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(final ValueAnimator animation) {
                                            updateParams(currentTouchView, (Integer) animation.getAnimatedValue());
                                        }
                                    }, new EndMoveAnimatorListener(new AnimationEnd() {
                                        @Override
                                        public void onAnimationEnd(@NonNull final Animator animation) {
                                            currentTouchView = null;
                                            if (getParent() != null) {
                                                getParent().requestDisallowInterceptTouchEvent(false);
                                            }
                                        }
                                    }));



                                }
                            });
                            resetThread = null;
                        }
                    }
                }
            });
            resetThread.start();
        }
        return false;
    }

    private boolean handleMove(@NonNull final MotionEvent event, final int x) {
        if (getParent() == null) {
            return false;
        }
        moved = true;
        getParent().requestDisallowInterceptTouchEvent(true);
        stopReset();
        updateParams(currentTouchView, (int) (x - xDown));
        if ((currentTouchView != null)  && (getDistanceTo(event) > (screenW / 2)) &&
            (Math.abs(velocityTracker.getXVelocity()) > SWIPE_THRESHOLD_VELOCITY)) {
            if (removeListener.isPresent()) {
                isRemoving = true;
                animateView(currentTouchView.getMeasuredHeight(), 300L, new LinearInterpolator(),
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(final ValueAnimator animation) {
                        if (currentTouchView != null) {
                            final ViewGroup.LayoutParams params = currentTouchView.getLayoutParams();
                            params.height = (int) animation.getAnimatedValue();
                            currentTouchView.setLayoutParams(params);
                        }
                    }
                }, new EndMoveAnimatorListener(new AnimationEnd() {
                    @Override
                    public void onAnimationEnd(@NonNull final Animator animation) {
                        if (currentTouchView != null) {
                            removeListener.get().onRemove(0, selectedChildIndex);
                            removeView(currentTouchView);
                            getParent().requestDisallowInterceptTouchEvent(false);
                            currentTouchView = null;
                            isRemoving = false;
                        }
                    }
                }));
            } else if (undoListener.isPresent() && (currentTouchView instanceof ViewSwitcher)) {
                final ViewSwitcher switcher = (ViewSwitcher) currentTouchView;
                undo = true;
                final int index = selectedChildIndex;
                final Timer timer = new Timer();
                switcher.findViewById(R.id.undo).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        timer.cancel();
                        timer.purge();
                        switcher.setDisplayedChild(0);
                        updateParams(switcher, 0);
                        undo = false;
                    }
                });
                switcher.setDisplayedChild(1);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                removeView(switcher);
                                if (currentTouchView == switcher) {
                                    currentTouchView = null;
                                    isRemoving = false;
                                }
                                undoListener.get().onRemove(index);
                                switcher.setDisplayedChild(0);
                                undo = false;
                            }
                        });
                    }
                }, 3000L);
            }
            return true;
        }
        return false;
    }

    private boolean isInMotion(@NonNull final MotionEvent event, final int x) {
        if (!isSwipeable(currentTouchView)) {
            return false;
        }
        final float dx = Math.abs(x - xDown);
        final float dy = Math.abs(event.getY() - yDown);
        if (dx < (4.0F * dy)) {
            return false;
        }
        if (dx < (screenW / 10.0F)) {
            return false;
        }
        if (ViewConfiguration.getTapTimeout() > Math.abs(event.getEventTime() - event.getDownTime())) {
            return false;
        }
        if (ViewConfiguration.get(getContext()).getScaledTouchSlop() > getDistanceTo(event)) {
            return false;
        }
        return true;
    }

    private static void animateView(final int start, final long duration,
                                    @NonNull final TimeInterpolator interpolator,
                                    @NonNull final ValueAnimator.AnimatorUpdateListener onUpdate,
                                    @NonNull final Animator.AnimatorListener animatorListener) {
        final ValueAnimator animator = new ValueAnimator();
        animator.setIntValues(start, 0);
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(onUpdate);
        animator.addListener(animatorListener);
        animator.start();
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

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void updateParams(@Nullable final View v, final int value) {
        if (v == null) {
            return;
        }
        View view = v.findViewById(R.id.swipe_child_wrapper);
        if ((view == null) || !((view instanceof LinearLayout) &&
                                (((LinearLayout) view).getChildCount() > 2))) {
            return;
        }
        final int pos;
        final int otherPos;
        if (isRTL) {
            pos = (value > 0) ? 2 : 0;
            otherPos = (value > 0) ? 0 : 2;
        } else {
            pos = (value < 0) ? 2 : 0;
            otherPos = (value < 0) ? 0 : 2;
        }

        final LinearLayout.LayoutParams cparams = (LinearLayout.LayoutParams) ((
                    LinearLayout) view).getChildAt(1).getLayoutParams();

        cparams.width = (int) Math.max(screenW - Math.abs(value), childWidth);
        if (isRTL) {
            cparams.setMarginStart(-1 * value);
        } else {
            cparams.leftMargin = value;
        }
        ((LinearLayout) view).getChildAt(1).setLayoutParams(cparams);

        int marginMultiplier = 1;
        if ((!isRTL && (value <= 0)) || (isRTL && (value >= 0))) {
            marginMultiplier = 2;
        }


        final LinearLayout.LayoutParams lparams = (LinearLayout.LayoutParams) ((
                    LinearLayout) view).getChildAt(pos).getLayoutParams();
        lparams.width = Math.abs(value) * marginMultiplier;
        ((LinearLayout) view).getChildAt(pos).setLayoutParams(lparams);

        final LinearLayout.LayoutParams rparams = (LinearLayout.LayoutParams) ((
                    LinearLayout) view).getChildAt(otherPos).getLayoutParams();
        rparams.width = 0;
        ((LinearLayout) view).getChildAt(otherPos).setLayoutParams(rparams);
        view.invalidate();
    }

    public void setOnItemRemovedListener(@Nullable final OnItemRemoveListener onItemRemovedListener) {
        this.removeListener = fromNullable(onItemRemovedListener);
    }

    public void setOnUndoListener(@Nullable final OnUndoListener onItemRemoved) {
        this.undoListener = fromNullable(onItemRemoved);
    }

    public interface OnItemRemoveListener {
        void onRemove(int position, int index);
    }
    public interface OnUndoListener {
        void onRemove(int index);
    }

    @Override
    public void addView(@NonNull final View child, final int index) {
        super.addView(setupChildView(child), index);
    }

    @Override
    protected boolean addViewInLayout(@NonNull final View child, final int index,
                                      final ViewGroup.LayoutParams params, final boolean preventRequestLayout) {
        return super.addViewInLayout(setupChildView(child), index, params, preventRequestLayout);
    }

    private View setupChildView(final View child) {
        final View wrapper = LayoutInflater.from(getContext()).inflate(R.layout.leave_behind_wrapper, null);
        final FrameLayout container = (FrameLayout) wrapper.findViewById(R.id.leave_behind_center);
        setRipple(child, container);
        container.addView(child);
        if (isSwipeable(child)) {
            wrapper.setTag(SWIPEABLE_VIEW, child.getTag(SWIPEABLE_VIEW));
        }
        return wrapper;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setRipple(@NonNull final  View child, @NonNull final FrameLayout container) {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) && child.isClickable()) {
            container.setForeground(getContext().getDrawable(R.drawable.ripple));
            container.setClickable(true);
            child.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(final View v, final MotionEvent event) {
                    final float x = event.getX() + v.getLeft();
                    final float y = event.getY() + v.getTop();
                    container.drawableHotspotChanged(x, y);

                    switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        container.setPressed(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        container.setPressed(false);
                        break;
                    }

                    // Pass all events through to the host view.
                    return false;
                }
            });
        }
    }


    @Override
    public void addView(@NonNull final View child, final int index,
                        final ViewGroup.LayoutParams params) {
        super.addView(setupChildView(child), index, params);
    }

    private interface AnimationEnd {
        void onAnimationEnd(@NonNull final Animator animation);
    }

    private static class EndMoveAnimatorListener implements Animator.AnimatorListener {

        private final AnimationEnd onEnd;

        EndMoveAnimatorListener(final AnimationEnd onEnd) {
            this.onEnd = onEnd;
        }

        @Override
        public void onAnimationStart(final Animator animation) {
            //nothing
        }

        @Override
        public void onAnimationEnd(final Animator animation) {
            onEnd.onAnimationEnd(animation);
        }

        @Override
        public void onAnimationCancel(final Animator animation) {
            //nothing
        }

        @Override
        public void onAnimationRepeat(final Animator animation) {
            //nothing
        }
    }
}
