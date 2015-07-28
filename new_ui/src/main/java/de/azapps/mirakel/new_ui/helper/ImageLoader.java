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
package de.azapps.mirakel.new_ui.helper;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.azapps.material_elements.drawable.RoundedBitmapDrawable;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.material_elements.utils.ViewHelper;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.tools.FileUtils;

public class ImageLoader extends AsyncTask<FileMirakel, Void, List<Drawable>> {
    private final List<ImageView> imageViews;
    private final Context context;

    public ImageLoader(final @NonNull List<ImageView> views, final @NonNull Context ctx) {
        imageViews = views;
        context = ctx;
    }

    private Drawable makePreviewDrawable(final FileMirakel file,
                                         final Optional<Bitmap> bitmapOptional) {
        final Bitmap bitmap;
        final float borderWidth = context.getResources().getDimension(
                                      de.azapps.mirakel.model.R.dimen.file_preview_corner_radius);
        if (bitmapOptional.isPresent()) {
            bitmap = bitmapOptional.get();
        } else {
            final int drawableId;
            if (FileUtils.isAudio(file.getFileUri())) {
                drawableId = de.azapps.mirakel.model.R.drawable.ic_music_note_white_48dp;
            } else if (FileUtils.isVideo(file.getFileUri())) {
                drawableId = de.azapps.mirakel.model.R.drawable.ic_camcorder_white_48dp;
            } else {
                drawableId = de.azapps.mirakel.model.R.drawable.ic_description_white_48dp;
            }
            final Bitmap src = BitmapFactory.decodeResource(context.getResources(), drawableId);
            final ColorFilter filter = new PorterDuffColorFilter(ThemeManager.getColor(
                        de.azapps.mirakel.model.R.attr.colorPreviewBorder), PorterDuff.Mode.MULTIPLY);
            bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight() + ViewHelper.dpToPx(context,
                                         25) - (int) borderWidth, src.getConfig());
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setColorFilter(filter);
            Rect rect = new Rect(0, 0, src.getWidth(), src.getHeight());
            canvas.drawBitmap(src, rect, rect, paint);
        }

        final float size = context.getResources().getDimension(
                               de.azapps.mirakel.model.R.dimen.file_preview_size);
        final float paddingVertical = (float) ((size - bitmap.getHeight()) / 2.0);
        final float paddingHorizontal = (float) ((size - bitmap.getWidth()) / 2.0);
        final RectF targetRect = new RectF(paddingHorizontal, paddingVertical, size - paddingHorizontal,
                                           size - paddingVertical);
        final Bitmap dest = Bitmap.createBitmap((int) size, (int) size, bitmap.getConfig());
        final Canvas canvas = new Canvas(dest);

        // Center bitmap
        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawBitmap(bitmap, null, targetRect, null);

        final RectF fullRect = new RectF(borderWidth / 2, borderWidth / 2, size - borderWidth / 2,
                                         size - borderWidth / 2);
        // Draw border
        final Paint borderPaint = new Paint();
        borderPaint.setColor(ThemeManager.getColor(de.azapps.mirakel.model.R.attr.colorPreviewBorder));
        borderPaint.setAntiAlias(true);
        borderPaint.setStrokeWidth(borderWidth);
        borderPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(fullRect, borderWidth, borderWidth, borderPaint);

        // Draw corner
        return new RoundedBitmapDrawable(dest,
                                         context.getResources().getDimension(de.azapps.mirakel.model.R.dimen.file_preview_corner_radius), 0);
    }

    @Override
    protected List<Drawable> doInBackground(final FileMirakel... params) {
        return new ArrayList<>(Collections2.transform(Arrays.asList(params),
        new Function<FileMirakel, Drawable>() {
            @Override
            public Drawable apply(final FileMirakel file) {
                return makePreviewDrawable(file, file.getPreview(context));
            }
        }));
    }

    @Override
    protected void onPostExecute(final List<Drawable> result) {
        if (!isCancelled()) {
            for (int i = 0; (i < imageViews.size()) && (i < result.size()); i++) {
                imageViews.get(i).setImageDrawable(result.get(i));
            }
        }

    }
}
