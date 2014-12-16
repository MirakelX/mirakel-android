package android.support.v7.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    private final Drawable mDivider;
    private boolean mShowFirstDivider = false;
    private boolean mShowLastDivider = false;


    public DividerItemDecoration(final Context context, final AttributeSet attrs) {
        final TypedArray a = context
                             .obtainStyledAttributes(attrs, new int[] {android.R.attr.listDivider});
        mDivider = a.getDrawable(0);
        a.recycle();
    }

    public DividerItemDecoration(final Context context, final AttributeSet attrs,
                                 final boolean showFirstDivider,
                                 final boolean showLastDivider) {
        this(context, attrs);
        mShowFirstDivider = showFirstDivider;
        mShowLastDivider = showLastDivider;
    }

    public DividerItemDecoration(final Drawable divider) {
        mDivider = divider;
    }

    public DividerItemDecoration(final Drawable divider, final boolean showFirstDivider,
                                 final boolean showLastDivider) {
        this(divider);
        mShowFirstDivider = showFirstDivider;
        mShowLastDivider = showLastDivider;
    }

    @Override
    public void onDrawOver(final Canvas c, final RecyclerView parent, final RecyclerView.State state) {
        if (mDivider == null) {
            super.onDrawOver(c, parent, state);
            return;
        }

        // Initialization needed to avoid compiler warning
        int left = 0;
        int right = 0;
        int top = 0;
        int bottom = 0;
        final int size;
        final int orientation = getOrientation(parent);
        final int childCount = parent.getChildCount();

        if (orientation == LinearLayoutManager.VERTICAL) {
            size = mDivider.getIntrinsicHeight();
            left = parent.getPaddingLeft();
            right = parent.getWidth() - parent.getPaddingRight();
        } else { //horizontal
            size = mDivider.getIntrinsicWidth();
            top = parent.getPaddingTop();
            bottom = parent.getHeight() - parent.getPaddingBottom();
        }

        for (int i = mShowFirstDivider ? 0 : 1; i < ((mShowLastDivider &&
                (childCount > 0)) ? (childCount + 1) : childCount); i++) {
            final View child = parent.getChildAt((i < childCount) ? i : (childCount - 1));
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            if (orientation == LinearLayoutManager.VERTICAL) {
                top = i == childCount ? (child.getBottom() + params.bottomMargin) : (child.getTop() -
                        params.topMargin);
                bottom = top + size;
            } else { //horizontal
                left = i == childCount ? (child.getRight() + params.rightMargin) : (child.getLeft() -
                        params.leftMargin);
                right = left + size;
            }
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }

    private static int getOrientation(final RecyclerView parent) {
        if (parent.getLayoutManager() instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) parent.getLayoutManager()).getOrientation();
        } else {
            throw new IllegalStateException(
                "DividerItemDecoration can only be used with a LinearLayoutManager.");
        }
    }
}