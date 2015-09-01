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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.Cursor2List;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.model.query_builder.CursorWrapper;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.FileUtils;
import de.azapps.tools.ImageUtils;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;

public class FileMirakel extends FileBase {

    private static final CursorWrapper.CursorConverter<List<FileMirakel>> LIST_FROM_CURSOR = new
    Cursor2List<>(FileMirakel.class);
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


    public FileMirakel(final CursorGetter c) {
        super(c.getInt(ID), c.getString(NAME), Task.get(c.getLong(TASK)).orNull(),
              Uri.parse(c.getString(PATH)));
    }

    private FileMirakel(final @NonNull CursorGetter c, final @NonNull Task t) {
        super(c.getInt(ID), c.getString(NAME), t, Uri.parse(c.getString(PATH)));
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
        return new MirakelQueryBuilder(context).and(TASK, Operation.EQ, task).select(ID,
                NAME,
        PATH).query(URI).doWithCursor(new Cursor2List<>(new CursorWrapper.CursorConverter<FileMirakel>() {
            @Override
            public FileMirakel convert(@NonNull final CursorGetter getter) {
                return new FileMirakel(getter, task);
            }
        }));
    }

    @NonNull
    public static Optional<FileMirakel> newFile(final Context ctx, final Task task,
            final Uri uri) {
        if (uri == null) {
            return absent();
        }
        final String name = FileUtils.getNameFromUri(ctx, uri);
        final FileMirakel newFile = FileMirakel.newFile(task, name, uri);
        try {
            final Bitmap bitmap = ImageUtils.getSquaredImage(ctx, uri,
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
        return of(newFile);
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
        final FileMirakel m = new FileMirakel(0L, name, task, uri);
        return m.create();
    }

    FileMirakel(final long id, final String name, final Task task, final Uri uri) {
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
    public Optional<Bitmap> getPreview(@NonNull final Context context) {
        final File osFile = new File(fileCacheDir, getId() + ".png");
        if (osFile.exists()) {
            return fromNullable(BitmapFactory.decodeFile(osFile.getAbsolutePath()));
        } else {
            return absent();
        }
    }

    // Parcelable stuff


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(this.task, 0);
        dest.writeParcelable(this.fileUri, 0);
        dest.writeLong(this.getId());
        dest.writeString(this.getName());
    }

    private FileMirakel(final Parcel in) {
        super();
        this.task = in.readParcelable(Task.class.getClassLoader());
        this.fileUri = in.readParcelable(Uri.class.getClassLoader());
        this.setId(in.readLong());
        this.setName(in.readString());
    }

    public static final Parcelable.Creator<FileMirakel> CREATOR = new
    Parcelable.Creator<FileMirakel>() {
        public FileMirakel createFromParcel(final Parcel source) {
            return new FileMirakel(source);
        }
        public FileMirakel[] newArray(final int size) {
            return new FileMirakel[size];
        }
    };
}
