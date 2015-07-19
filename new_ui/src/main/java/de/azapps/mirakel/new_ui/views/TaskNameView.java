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

package de.azapps.mirakel.new_ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.TextView;

import de.azapps.material_elements.utils.ViewHelper;
import de.azapps.mirakelandroid.R;

public class TaskNameView extends TextView {

    private boolean isStrikeThrough;
    private final Paint stroke = new Paint();
    private final Rect bounds = new Rect();
    private final int strokeSize;
    private final int strokeMargin;
    private final boolean isRTL;

    public TaskNameView(final Context ctx, final AttributeSet attrs) {
        super(ctx, attrs);
        stroke.setColor(getTextColors().getDefaultColor());
        isRTL = ViewHelper.isRTL(ctx);
        final TypedArray a = ctx.getTheme().obtainStyledAttributes(attrs, R.styleable.TaskName, 0, 0);
        try {
            strokeSize = (int)a.getDimension(R.styleable.TaskName_strokeSize, ViewHelper.dpToPx(ctx, 2.0F));
            strokeMargin = (int)a.getDimension(R.styleable.TaskName_strokeMargin, ViewHelper.dpToPx(ctx, 5.0F));
            isStrikeThrough = a.getBoolean(R.styleable.TaskName_isStroked, false);
        } finally {
            a.recycle();
        }
    }

    public boolean isStrikeThrough() {
        return isStrikeThrough;
    }

    public void setStrikeThrough(final boolean newStrikeThrough) {
        isStrikeThrough = newStrikeThrough;
    }

    @Override
    public void onDraw(@NonNull final Canvas canvas) {
        super.onDraw(canvas);
        if (isStrikeThrough) {
            final Layout layout = getLayout();
            if (layout != null) {
                final int lineCount = layout.getLineCount();
                for (int i = 0; i < lineCount; i++) {
                    layout.getLineBounds(i, bounds);
                    final float middle = (bounds.top + bounds.bottom) / 2.0F + (i == 0 ? strokeSize : 0);
                    bounds.top = (int) (middle);
                    if (isRTL) {
                        bounds.left = (int) (getWidth() - strokeMargin - layout.getLineWidth(i));
                        bounds.right = getWidth();
                    } else {
                        bounds.left = -1 * strokeMargin;
                        bounds.right = (int) (strokeMargin + layout.getLineWidth(i));
                    }
                    bounds.bottom = (int) (middle + strokeSize);
                    canvas.drawRect(bounds, stroke);

                }
            }
        }
    }
}
