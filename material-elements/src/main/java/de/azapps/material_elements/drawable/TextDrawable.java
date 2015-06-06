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

package de.azapps.material_elements.drawable;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;


public class TextDrawable extends Drawable {

    private final Paint textPaint;
    private String text;
    private final int color;
    private int height;
    private int width;
    private final int fontSize;
    private final boolean upperCase;
    private int displacment;
    private int topPadding=0;
    private Drawable background;
    private float left;


    private TextDrawable(final Builder builder) {
        super();

        // shape properties
        height = builder.height;
        width = builder.width;
        upperCase=builder.toUpperCase;
        displacment=builder.displacment;

        background=builder.backGround;

        // text and color
        setNewText(builder.text);
        color = builder.color;

        // text paint settings
        fontSize = builder.fontSize;
        textPaint = new Paint();
        textPaint.setColor(builder.textColor);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(builder.isBold);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(builder.font);
        textPaint.setTextAlign(Paint.Align.CENTER);


        setBubbleColor();
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
    }

    private void setBubbleColor() {
        if(background instanceof ShapeDrawable){
            Paint paint = ((ShapeDrawable) background).getPaint();
            paint.setColor(color);
        }else{
            background.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }


    public void setNewText(final String text){
        this.text = upperCase ? text.toUpperCase() : text;
    }

    public void setHeight(final int height){
        this.height=height;
    }

    public void setWidth(final int width){
        this.width=width;
    }

    public void setDisplacment(final int displacment){
        this.displacment=displacment;
    }

    @Override
    public void draw(final Canvas canvas) {
        final int count = canvas.save();
        final Rect r = getBounds();
        canvas.translate(left+left,r.top+topPadding);
        background.setBounds(r);
        background.draw(canvas);

        canvas.translate(r.left, r.top+displacment);

        // draw text
        final int width = (this.width < 0) ? r.width() : this.width;
        final int height = (this.height < 0) ? r.height() : this.height;
        final int fontSize = (this.fontSize < 0) ? (Math.min(width, height) / 2) : this.fontSize;
        textPaint.setTextSize(fontSize);
        canvas.drawText(text, width / 2, (height / 2) - ((textPaint.descent() + textPaint.ascent()) / 2), textPaint);

        canvas.restoreToCount(count);

    }


    @Override
    public void setAlpha(final int alpha) {
        textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(final ColorFilter cf) {
        textPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    public static IShapeBuilder builder() {
        return new Builder();
    }

    public void setTopPadding(int topPadding) {
        this.topPadding = topPadding;
    }

    public int getTopPadding() {
        return topPadding;
    }



    public void setLeft(float left) {
        this.left = left;
    }

    public float getLeft() {
        return left;
    }

    public static class Builder implements IConfigBuilder, IShapeBuilder, IBuilder {

        private String text;

        private int color;

        private int width;

        private int height;

        private Typeface font;

        private Drawable backGround;

        public int textColor;

        private int fontSize;

        private boolean isBold;

        private boolean toUpperCase;

        public int displacment;

        private Builder() {
            text = "";
            color = Color.GRAY;
            textColor = Color.WHITE;
            width = -1;
            height = -1;
            backGround = new ShapeDrawable(new RectShape());
            font = Typeface.create("sans-serif-light", Typeface.NORMAL);
            fontSize = -1;
            isBold = false;
            toUpperCase = false;
            displacment=0;
        }

        @Override
        public IConfigBuilder width(final int width) {
            this.width = width;
            return this;
        }

        @Override
        public IConfigBuilder height(final int height) {
            this.height = height;
            return this;
        }

        @Override
        public IConfigBuilder textColor(final int color) {
            this.textColor = color;
            return this;
        }


        @Override
        public IConfigBuilder useFont(final Typeface font) {
            this.font = font;
            return this;
        }

        @Override
        public IConfigBuilder fontSize(final int size) {
            this.fontSize = size;
            return this;
        }

        @Override
        public IConfigBuilder bold() {
            this.isBold = true;
            return this;
        }

        @Override
        public IConfigBuilder toUpperCase() {
            this.toUpperCase = true;
            return this;
        }

        @Override
        public IConfigBuilder displace(final int displacment) {
            this.displacment=displacment;
            return this;
        }

        @Override
        public IConfigBuilder beginConfig() {
            return this;
        }

        @Override
        public TextDrawable buildWithBackground(final String text, final int color, final Drawable background) {
            backGround=background;
            return build(text,color);
        }

        @Override
        public IShapeBuilder endConfig() {
            return this;
        }


        @Override
        public TextDrawable build(final String text, final int color) {
            this.color = color;
            this.text = text;
            return new TextDrawable(this);
        }
    }

    public interface IConfigBuilder {
        public IConfigBuilder width(final int width);

        public IConfigBuilder height(final int height);

        public IConfigBuilder textColor(final int color);

        public IConfigBuilder useFont(final Typeface font);

        public IConfigBuilder fontSize(final int size);

        public IConfigBuilder bold();

        public IConfigBuilder toUpperCase();

        public IConfigBuilder displace(final int displacment);

        public IShapeBuilder endConfig();
    }

    public static interface IBuilder {

        public TextDrawable build(final String text, final int color);
    }

    public static interface IShapeBuilder {

        public IConfigBuilder beginConfig();

        public TextDrawable buildWithBackground(final String text, final int color, final Drawable background);
    }
}


