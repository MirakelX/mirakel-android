package de.azapps.material_elements.utils;

import android.animation.Animator;
import android.content.Context;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import de.azapps.material_elements.R;

public class AnimationHelper {
    private static final int IS_MOVED = R.id.moved;

    public static void moveViewUp(final Context context, final View view, final int height) {
        Object tag = view.getTag(IS_MOVED);
        if ((tag == null) || !(Boolean) tag) {
            view.animate()
            .translationY(-height)
            .setDuration(context.getResources().getInteger(R.integer.anim_snackbar_duration))
            .setListener(new AnimationEventListener() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setTag(IS_MOVED, true);
                }
            })
            .setInterpolator(new DecelerateInterpolator(1.5F)).start();

        }

    }
    public static void moveViewDown(final Context context, final View view, final int height) {
        view.animate()
        .translationY(0)
        .setDuration(context.getResources().getInteger(R.integer.anim_snackbar_duration))
        .setListener(new AnimationEventListener() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                view.setTag(IS_MOVED, false);
            }
        })
        .setInterpolator(new AccelerateInterpolator(1.5F)).start();
    }

    public static void slideIn(final Context context, final View view) {
        Object tag = view.getTag(IS_MOVED);
        if ((tag == null) || !(Boolean) tag) {
            view.setVisibility(View.VISIBLE);
            view.animate()
            .setDuration(context.getResources().getInteger(R.integer.anim_snackbar_duration))
            .alpha(1)
            .translationY(0)
            .setListener(new AnimationEventListener() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setTag(IS_MOVED, true);
                }
            })
            .setInterpolator(new DecelerateInterpolator(1.5F))
            .start();
        }
    }

    public static void slideOut(final Context context, final View view) {
        view.setVisibility(View.VISIBLE);
        view.animate()
        .setDuration(context.getResources().getInteger(R.integer.anim_snackbar_duration))
        .alpha(0)
        .translationY(view.getHeight())
        .setListener(new AnimationEventListener() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                super.onAnimationEnd(animation);
                view.setTag(IS_MOVED, false);
                view.setVisibility(View.GONE);
            }
        })
        .setInterpolator(new AccelerateInterpolator(1.5F))
        .start();
    }
}
