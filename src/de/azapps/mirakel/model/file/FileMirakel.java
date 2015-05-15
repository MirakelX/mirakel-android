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

package de.azapps.mirakel.model.file;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.azapps.material_elements.drawable.RoundedBitmapDrawable;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.FileUtils;
import de.azapps.tools.ImageUtils;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;

public class FileMirakel extends FileBase {

    public static final String[] allColumns = { ID, NAME, TASK, PATH };
    public static final String cacheDirPath = FileUtils.getMirakelDir()
            + "image_cache";
    public static final File fileCacheDir = new File(cacheDirPath);
    public static final String TABLE = "files";
    private static final String TAG = "FileMirakel";
    public static final Uri URI = MirakelInternalContentProvider.FILE_URI;

    // private static final String TAG = "FileMirakel";

    @Override
    protected Uri getUri() {
        return URI;
    }

    /**
     * Get all Tasks
     *
     * @return
     */
    public static List<FileMirakel> all() {
        return new MirakelQueryBuilder(context).getList(FileMirakel.class);
    }

    public static List<FileMirakel> cursorToFileList(final Cursor c) {
        List<FileMirakel> ret = new ArrayList<>();
        if (c.moveToFirst()) {
            do {
                ret.add(new FileMirakel(c));
            } while (c.moveToNext());
        }
        c.close();
        return ret;
    }

    public FileMirakel(final Cursor c) {
        super(c.getInt(c.getColumnIndex(ID)), c.getString(c
                .getColumnIndex(NAME)), Task.get(c.getInt(c
                        .getColumnIndex(TASK))).orNull(), Uri.parse(c.getString(c
                                .getColumnIndex(PATH))));
    }

    private FileMirakel(final @NonNull Cursor c, final @NonNull Task t) {
        super(c.getInt(c.getColumnIndex(ID)), c.getString(c
                .getColumnIndex(NAME)), t, Uri.parse(c.getString(c
                        .getColumnIndex(PATH))));
    }

    // Static Methods

    public static void destroyForTask(final Task t) {
        final List<FileMirakel> files = getForTask(t);
        MirakelInternalContentProvider
        .withTransaction(new MirakelInternalContentProvider.DBTransaction() {
            @Override
            public void exec() {
                for (final FileMirakel file : files) {
                    final File destFile = new File(
                        FileMirakel.fileCacheDir, file.getId()
                        + ".png");
                    if (destFile.exists()) {
                        destFile.delete();
                    }
                    file.destroy();
                }
            }
        });
    }

    /**
     * Get a Task by id
     *
     * @param id
     * @return
     */
    @NonNull
    public static Optional<FileMirakel> get(final long id) {
        return new MirakelQueryBuilder(context).get(FileMirakel.class, id);
    }

    @NonNull
    public static List<FileMirakel> getForTask(final Task task) {
        final Cursor c = new MirakelQueryBuilder(context).and(TASK, Operation.EQ, task).select(ID, NAME,
                PATH).query(URI);
        final List<FileMirakel> ret = new ArrayList<>(c.getCount());
        if (c.moveToFirst()) {
            do {
                ret.add(new FileMirakel(c, task));
            } while (c.moveToNext());
        }
        c.close();
        return ret;
    }

    @NonNull
    public static FileMirakel newFile(final Context ctx, final Task task,
                                      final Uri uri) {
        if (uri == null) {
            return null;
        }
        final String name = FileUtils.getNameFromUri(ctx, uri);
        final FileMirakel newFile = FileMirakel.newFile(task, name, uri);
        try {
            final Bitmap bitmap = ImageUtils.getScaleImage(uri, ctx,
                                  ctx.getResources().getDimension(R.dimen.file_preview_size));

            if (bitmap != null) {
                // create directory if not exists
                boolean success = true;
                if (!FileMirakel.fileCacheDir.exists()) {
                    success = FileMirakel.fileCacheDir.mkdirs();
                }
                if (success) {
                    final File destFile = new File(FileMirakel.fileCacheDir,
                                                   newFile.getId() + ".png");
                    final FileOutputStream out = new FileOutputStream(destFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 42, out);
                    out.close();
                }
            }
        } catch (final IOException e) {
            Log.wtf(TAG, "failed to scale image", e);
        }
        return newFile;
    }

    /**
     * Create a new File
     *
     * @param task
     * @param name
     * @param uri
     * @return new File
     */
    @NonNull
    public static FileMirakel newFile(final Task task, final String name,
                                      final Uri uri) {
        final FileMirakel m = new FileMirakel(0, name, task, uri);
        return m.create();
    }

    FileMirakel(final int id, final String name, final Task task, final Uri uri) {
        super(id, name, task, uri);
    }

    @NonNull
    public FileMirakel create() {
        final ContentValues values = getContentValues();
        values.remove("_id");
        final long insertId = insert(URI, values);
        return FileMirakel.get(insertId).get();
    }

    @Override
    public void destroy() {
        super.destroy();
        new File(fileCacheDir, getId() + ".png").delete();
    }

    @NonNull
    public Optional<Drawable> getPreview(@NonNull final Context context) {
        final File osFile = new File(fileCacheDir, getId() + ".png");
        if (osFile.exists()) {
            final Bitmap bpm = BitmapFactory.decodeFile(osFile.getAbsolutePath());
            return of((Drawable) new RoundedBitmapDrawable(bpm,
                      context.getResources().getDimension(R.dimen.file_preview_corner_radius), 0));
        } else {
            final int drawableId;
            if (FileUtils.isAudio(fileUri)) {
                drawableId = R.drawable.ic_play_circle_fill_big;
            } else {
                drawableId = R.drawable.ic_description_big;
            }
            final Drawable drawable = context.getResources().getDrawable(drawableId);
            if (drawable != null) {
                drawable.setColorFilter(ThemeManager.getColor(R.attr.colorTextGrey), PorterDuff.Mode.SRC_IN);
            }
            return fromNullable(drawable);
        }
    }

    // Parcelable stuff


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.task, 0);
        dest.writeParcelable(this.fileUri, 0);
        dest.writeLong(this.getId());
        dest.writeString(this.getName());
    }

    private FileMirakel(Parcel in) {
        super();
        this.task = in.readParcelable(Task.class.getClassLoader());
        this.fileUri = in.readParcelable(Uri.class.getClassLoader());
        this.setId(in.readLong());
        this.setName(in.readString());
    }

    public static final Parcelable.Creator<FileMirakel> CREATOR = new
    Parcelable.Creator<FileMirakel>() {
        public FileMirakel createFromParcel(Parcel source) {
            return new FileMirakel(source);
        }
        public FileMirakel[] newArray(int size) {
            return new FileMirakel[size];
        }
    };
}
