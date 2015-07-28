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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakelandroid.R;


public class TagSpan extends ReplacementSpan {

    private final String text;

    private final Paint paintBackground;
    private final Paint paintText;
    private final float textLength;
    private final Context context;
    private int padding;

    public TagSpan(final Tag tag, final Context context) {
        this.context = context;

        this.paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.paintBackground.setColor(tag.getBackgroundColor());


        this.text = tag.getName();
        this.paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.paintText.setTextSize(context.getResources().getDimension(
                                       R.dimen.text_size_small));
        this.paintText.setColor((tag.isDarkText()) ? ThemeManager.getColor(R.attr.colorTextBlack) :
                                ThemeManager.getColor(R.attr.colorTextWhite));
        padding = Math.round(scale(10.0F));

        textLength = this.paintText.measureText(text);
    }

    private float scale(final float number) {
        return context.getResources().getDisplayMetrics().density * number;
    }

    @Override
    public int getSize(final Paint paint, final CharSequence text, final int start, final int end,
                       final Paint.FontMetricsInt fm) {
        return Math.round(textLength + (2 * padding));
    }



    @Override
    public void draw(final Canvas canvas, final CharSequence text, final int start, final int end,
                     final float x, final int top, final int y, final int bottom, final Paint paint) {
        // do not add padding to the bottom because it will be cut of on old devices
        final RectF rect = new RectF(x, top, x + textLength + (2 * padding), bottom);
        canvas.drawRoundRect(rect, scale(2.0F), scale(2.0F), paintBackground);
        // we will move the text 2dp upwards. This looks a tiny bit weird when editing but in 90%
        // of the time it looks better
        canvas.drawText(this.text, x + padding, y - scale(2.0F), paintText);
    }

}
