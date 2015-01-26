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
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.common.base.Optional;

import de.azapps.mirakel.settings.R;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;


public class SwipeLinearLayout extends LinearLayout  {
    public final static int SWIPEABLE_VIEW = R.id.remove_handle;

    private static final int SWIPE_THRESHOLD_VELOCITY = 1200;

    private int selectedChildIndex = 0;
    @Nullable
    private Thread resetThread = null;
    @Nullable
    private View currentTouchView = null;
    private float xDown;
    private float yDown;
    @NonNull
    private Optional<OnItemRemoveListener> listener = absent();
    @NonNull
    private final VelocityTracker velocityTracker = VelocityTracker.obtain();
    private int screenW;
    private RelativeLayout.LayoutParams leaveBehindParams;
    private double childWidth;
    private boolean isRemoving = false;
    private boolean moved = false;


    @Override
    public boolean onInterceptTouchEvent(final @NonNull MotionEvent ev) {
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
                currentTouchView = null;
            }
            break;
        default:
            break;
        }
        return false;
    }

    private int getDistanceTo(final @NonNull MotionEvent ev) {

        final double x = (double) (ev.getX() - xDown);
        final double y = (double) (ev.getY() - yDown);
        return (int) Math.sqrt((x * x) + (y * y));
    }

    boolean processTouchDown(final @NonNull MotionEvent ev) {
        final int x = (int) ev.getX();
        velocityTracker.clear();
        if (stopReset() && isSwipeable(currentTouchView)) {
            updateParams(currentTouchView, x);
            return false;
        }
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
        if ((currentTouchView == null) || !(currentTouchView instanceof LinearLayout) ) {
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
        if ((v instanceof LinearLayout) && (((LinearLayout) v).getChildCount() > 1)) {
            if (((LinearLayout) v).getChildAt(1).getTag(SWIPEABLE_VIEW) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(final @NonNull MotionEvent event) {
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

    private boolean handleEndTouchEvent(final @NonNull MotionEvent event, final int x) {
        if (isSwipeable(currentTouchView) &&
            !(ViewConfiguration.getTapTimeout() < event.getEventTime() - event.getDownTime() ||
              ViewConfiguration.get(getContext()).getScaledTouchSlop() < getDistanceTo(event))) {
            updateParams(currentTouchView, 0);
            currentTouchView.onTouchEvent(event);
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

    private boolean handleMove(final @NonNull MotionEvent event, final int x) {
        moved = true;
        getParent().requestDisallowInterceptTouchEvent(true);
        stopReset();
        updateParams(currentTouchView, (int) (x - xDown));
        if ((currentTouchView != null) && listener.isPresent() && (getDistanceTo(event) > (screenW / 2)) &&
            (Math.abs(velocityTracker.getXVelocity()) > SWIPE_THRESHOLD_VELOCITY)) {
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
                        listener.get().onRemove(0, selectedChildIndex);
                        removeView(currentTouchView);
                        getParent().requestDisallowInterceptTouchEvent(false);
                        currentTouchView = null;
                        isRemoving = false;
                    }
                }
            }));
            return true;
        }
        return false;
    }

    private boolean isInMotion(final @NonNull MotionEvent event, final int x) {
        return ((Math.abs(x - xDown) > 2 * Math.abs(event.getY() - yDown) &&
                 Math.abs(x - xDown) > screenW / 10) && (isSwipeable(currentTouchView) &&
                         (ViewConfiguration.getTapTimeout() < (event.getEventTime() - event.getDownTime())))) &&
               (ViewConfiguration.get(getContext()).getScaledTouchSlop() < getDistanceTo(event));
    }

    private void animateView(final int start, final long duration,
                             final @NonNull TimeInterpolator interpolator,
                             final @NonNull ValueAnimator.AnimatorUpdateListener onUpdate,
                             final @NonNull Animator.AnimatorListener animatorListener) {
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
    private void updateParams(final @Nullable View view, final int value) {
        if ((view == null) || !(view instanceof LinearLayout &&
                                ((LinearLayout) view).getChildCount() > 2)) {
            return;
        }
        final int pos = (value < 0) ? 2 : 0;
        final int otherPos = (value < 0) ? 0 : 2;

        final LinearLayout.LayoutParams cparams = (LinearLayout.LayoutParams) ((
                    LinearLayout) view).getChildAt(1).getLayoutParams();
        cparams.width = (int) Math.max((screenW - Math.abs(value)), childWidth);

        int marginMultiplier = 1;
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) && (value <= 0)) {
            cparams.leftMargin = value;
            marginMultiplier = 2;
        } else {
            if ((getLayoutDirection() == LAYOUT_DIRECTION_LTR) && (value <= 0)) {
                cparams.leftMargin = value;
                marginMultiplier = 2;
            } else if ((getLayoutDirection() == LAYOUT_DIRECTION_RTL) && (value >= 0)) {
                cparams.rightMargin = -1 * value;
                marginMultiplier = 2;
            }
        }
        ((LinearLayout) view).getChildAt(1).setLayoutParams(cparams);

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

    public void setOnItemRemovedListener(final @Nullable OnItemRemoveListener onItemRemovedListener) {
        this.listener = fromNullable(onItemRemovedListener);
    }


    public interface OnItemRemoveListener {
        void onRemove(int position, int index);
    }

    public SwipeLinearLayout(final Context context) {
        this(context, null);
    }



    public SwipeLinearLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        requestDisallowInterceptTouchEvent(true);
        setWillNotDraw(true);
        leaveBehindParams = new RelativeLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        leaveBehindParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    }


    @Override
    protected boolean addViewInLayout(@NonNull final View child, final int index,
                                      final ViewGroup.LayoutParams params, final boolean preventRequestLayout) {
        addView(setupChildView(child));
        return true;
    }

    private View setupChildView(final View child) {
        if (!isSwipeable(child)) {
            return child;
        }
        final LinearLayout wrapper = new LinearLayout(getContext());
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.addView(getLeaveBehindView(false));
        childWidth = Math.max(child.getMeasuredWidthAndState(), childWidth);
        wrapper.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
        child.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                              ViewGroup.LayoutParams.WRAP_CONTENT));
        wrapper.addView(child);
        wrapper.addView(getLeaveBehindView(true));
        wrapper.setTag(SWIPEABLE_VIEW, child.getTag(SWIPEABLE_VIEW));
        return wrapper;
    }

    private View getLeaveBehindView(final boolean left) {

        final ImageView leaveBehind = new ImageView(getContext());
        leaveBehind.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT));
        leaveBehind.setImageResource(R.drawable.ic_delete_24px);
        leaveBehind.setColorFilter(Color.DKGRAY);
        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        leaveBehind.setLayoutParams(params);
        leaveBehind.setBackgroundColor(Color.TRANSPARENT);

        final RelativeLayout leaveBehindWrapper = new RelativeLayout(getContext());
        leaveBehindWrapper.addView(leaveBehind);
        leaveBehindWrapper.setBackgroundResource(left ? R.drawable.leave_behind_background_right :
                R.drawable.leave_behind_background_left);
        leaveBehindWrapper.setLayoutParams(leaveBehindParams);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            leaveBehindWrapper.setElevation(-15.0F);
        }
        return leaveBehindWrapper;
    }

    @Override
    public void addView(@NonNull final View child, final int index,
                        final ViewGroup.LayoutParams params) {
        super.addView(setupChildView(child), index, params);
    }

    private interface AnimationEnd {
        void onAnimationEnd(final @NonNull Animator animation);
    }

    private static class EndMoveAnimatorListener implements Animator.AnimatorListener {

        private final AnimationEnd onEnd;

        EndMoveAnimatorListener(final AnimationEnd onEnd) {
            this.onEnd = onEnd;
        }

        @Override
        public void onAnimationStart(final Animator animation) {

        }

        @Override
        public void onAnimationEnd(final Animator animation) {
            onEnd.onAnimationEnd(animation);
        }

        @Override
        public void onAnimationCancel(final Animator animation) {

        }

        @Override
        public void onAnimationRepeat(final Animator animation) {

        }
    }
}
