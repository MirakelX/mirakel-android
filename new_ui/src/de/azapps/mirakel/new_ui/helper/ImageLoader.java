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
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.azapps.mirakel.model.file.FileMirakel;

public class ImageLoader extends AsyncTask<FileMirakel, Void, List<Drawable>> {
    private final List<ImageView> imageViews;
    private final Context context;

    public ImageLoader(final @NonNull List<ImageView> views, final @NonNull Context ctx) {
        imageViews = views;
        context = ctx;
    }

    @Override
    protected List<Drawable> doInBackground(final FileMirakel... params) {
        return new ArrayList<>(Collections2.transform(Arrays.asList(params),
        new Function<FileMirakel, Drawable>() {
            @Override
            public Drawable apply(final FileMirakel input) {
                return input.getPreview(context).orNull();
            }
        }));
    }

    @Override
    protected void onPostExecute(final List<Drawable> result) {
        if (!isCancelled() ) {
            for (int i = 0; (i < imageViews.size()) && (i < result.size()); i++) {
                imageViews.get(i).setImageDrawable(result.get(i));
            }
        }

    }
}
