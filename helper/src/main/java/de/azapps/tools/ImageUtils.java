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

package de.azapps.tools;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;

public class ImageUtils {

    /*
    * Scaling down the image
    * "Source: http://www.androiddevelopersolution.com/2012/09/bitmap-how-to-scale-down-image-for.html"
    */
    @Nullable
    public static Bitmap getScaleImage(final @NonNull Context ctx, final @NonNull Uri path,
                                       final float boundBoxInDp, boolean useLongerSide) throws IOException {
        final Bitmap bitmap = BitmapFactory.decodeStream(FileUtils.getStreamFromUri(ctx, path));
        if (bitmap == null) {
            return null;
        }
        // Get current dimensions
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        // Determine how much to scale: the dimension requiring
        // less scaling is.
        // closer to the its side. This way the image always
        // stays inside your.
        // bounding box AND either x/y axis touches it.
        final float xScale = boundBoxInDp / width;
        final float yScale = boundBoxInDp / height;
        final float scale;
        if (useLongerSide) {
            scale = (xScale >= yScale) ? xScale : yScale;
        } else {
            scale = (xScale <= yScale) ? xScale : yScale;
        }
        // Create a matrix for the scaling and add the scaling data
        final Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postConcat(getRotation(path, ctx));
        // Create a new bitmap and convert it to a format understood
        // by the
        // ImageView
        // Apply the scaled bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, width,
                                   height, matrix, false);
    }

    public static Bitmap getSquaredImage(final @NonNull Context ctx, final @NonNull Uri path,
                                         final float boundBoxInDp) throws IOException {
        Bitmap scaledBitmap = getScaleImage(ctx, path, boundBoxInDp, true);
        if (scaledBitmap == null) {
            return null;
        }
        Bitmap newBitmap = Bitmap.createBitmap((int) boundBoxInDp, (int) boundBoxInDp,
                                               scaledBitmap.getConfig());
        Canvas canvas = new Canvas(newBitmap);
        final int top, left;
        if (scaledBitmap.getWidth() > scaledBitmap.getHeight()) {
            top = 0;
            left = (int) (scaledBitmap.getWidth() - boundBoxInDp) / 2;
        } else {
            left = 0;
            top = (int) (scaledBitmap.getHeight() - boundBoxInDp) / 2;

        }
        Rect srcRect = new Rect(left, top, (int) (left + boundBoxInDp), (int) (top + boundBoxInDp));
        canvas.drawBitmap(scaledBitmap, srcRect, new Rect(0, 0, (int) boundBoxInDp, (int) boundBoxInDp),
                          null);
        return newBitmap;
    }

    private static int getOrientation( final Uri photoUri, final Context ctx) throws IOException {
        if (photoUri.toString().startsWith("file://")) {
            final ExifInterface exif = new ExifInterface(photoUri.getPath());
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } else {
            final Cursor cursor = ctx.getContentResolver().query(photoUri,
                                  new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

            final int ret;
            if ((cursor == null) || (cursor.getCount() != 1)) {

                ret = ExifInterface.ORIENTATION_UNDEFINED;
            } else {
                cursor.moveToFirst();
                ret = cursor.getInt(0);
            }
            if (cursor != null) {
                cursor.close();
            }
            return ret;
        }
    }

    private static Matrix getRotation(final Uri photoUri, final Context ctx) throws IOException {
        final Matrix matrix = new Matrix();
        final int orientation = getOrientation(photoUri, ctx);
        Log.d("EXIF", "Exif: " + orientation);
        switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
            matrix.postRotate(90.0F);
            break;
        case ExifInterface.ORIENTATION_ROTATE_180:
            matrix.postRotate(180.0F);
            break;
        case ExifInterface.ORIENTATION_ROTATE_270:
            matrix.postRotate(270.0F);
            break;
        default://ignore other cases
            break;
        }
        return matrix;
    }
}
