/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *  Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.new_ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

import de.azapps.mirakel.new_ui.R;

public class TaskNameView extends TextView {

    private boolean isStrikeThrough;
    private final Paint stroke = new Paint();
    private final Rect textSize = new Rect();
    private final int strokeSize;
    private final int strokeMargin;

    public TaskNameView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        stroke.setColor(getTextColors().getDefaultColor());
        TypedArray a = ctx.getTheme().obtainStyledAttributes(attrs, R.styleable.TaskName, 0, 0);
        try {
            strokeSize = (int)a.getDimension(R.styleable.TaskName_strokeSize, ViewHelper.dpToPx(2, ctx));
            strokeMargin = (int)a.getDimension(R.styleable.TaskName_strokeMargin, ViewHelper.dpToPx(5, ctx));
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
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isStrikeThrough) {
            final String text = getText().toString();
            getPaint().getTextBounds("H", 0, 1, textSize);
            final int top = textSize.top;
            final int bottom = textSize.bottom;
            canvas.translate(0, (bottom - top));
            getPaint().getTextBounds(text, 0, text.length(), textSize);
            textSize.top = 0;
            textSize.bottom = strokeSize;
            textSize.left -= strokeMargin;
            textSize.right += strokeMargin;
            canvas.drawRect(textSize, stroke);
        }
    }
}
