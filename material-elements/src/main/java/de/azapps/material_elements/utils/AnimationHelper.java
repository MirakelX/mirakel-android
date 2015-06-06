package de.azapps.material_elements.utils;

import android.animation.Animator;
import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;

import de.azapps.material_elements.R;

public class AnimationHelper {
    private static final int IS_MOVED = R.id.moved;

    public static void moveViewUp(final Context context, final View view, final int height) {
        Object tag = view.getTag(IS_MOVED);
        if ((tag == null) || !(Boolean) tag) {
            view.animate()
            .translationYBy(-height)
            .setDuration(context.getResources().getInteger(R.integer.anim_snackbar_duration))
            .setListener(new AnimationEventListener() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setTag(IS_MOVED, true);
                }
            })
            .setInterpolator(new DecelerateInterpolator()).start();

        }

    }
    public static void moveViewDown(final Context context, final View view, final int height) {
        view.animate()
        .translationYBy(height)
        .setDuration(context.getResources().getInteger(R.integer.anim_snackbar_duration))
        .setListener(new AnimationEventListener() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                view.setTag(IS_MOVED, false);
            }
        })
        .setInterpolator(new DecelerateInterpolator()).start();
    }

    public static void slideIn(final Context context, final View view) {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in);
        animation.setInterpolator(context, R.interpolator.decelerate_cubic);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(animation);
    }

    public static void slideOut(final Context context, final View view) {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_out);
        animation.setInterpolator(context, R.interpolator.accelerate_cubic);
        view.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}
