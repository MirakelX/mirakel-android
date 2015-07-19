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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.MimeTypeMap;

import com.google.common.base.Optional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;

public class FileUtils {
    private static final String TAG = "FileUtils";
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_AUDIO = 3;
    public static final String ERROR_NO_MEDIA_DIR = "noMediaStorageDir";
    private static String MIRAKEL_DIR;

    public static FileInputStream getStreamFromUri(final Context ctx,
            final Uri uri) throws FileNotFoundException {
        try {
            return new ParcelFileDescriptor.AutoCloseInputStream(ctx
                    .getContentResolver().openFileDescriptor(uri, "r"));
        } catch (final SecurityException e) {
            Log.wtf(TAG, "no permission to read uri " + uri);
            // Log.w(TAG, Log.getStackTraceString(e));
        }
        return null;
    }

    /**
     * Copy File
     *
     * @param src
     * @param dst
     * @throws IOException
     */
    public static void copyFile(final File src, final File dst)
    throws IOException {
        if (!src.canRead() || !dst.canWrite()) {
            Log.e(TAG, "cannot copy file");
            return;
        }
        copyByStream(new FileInputStream(src), new FileOutputStream(dst));
    }

    public static void copyByStream(final FileInputStream src,
                                    final FileOutputStream dst) throws IOException {
        final FileChannel inChannel = src.getChannel();
        final FileChannel outChannel = dst.getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    /**
     * Unzip a File and Copy it to a location
     *
     * @param fin
     * @param location
     */
    public static void unzip(final FileInputStream fin, final File location)
    throws FileNotFoundException, IOException {
        final ZipInputStream zin = new ZipInputStream(fin);
        ZipEntry ze = null;
        while ((ze = zin.getNextEntry()) != null) {
            if (ze.isDirectory()) {
                dirChecker(location, ze.getName());
            } else {
                final FileOutputStream fout = new FileOutputStream(new File(
                            location, ze.getName()));
                for (int c = zin.read(); c != -1; c = zin.read()) {
                    fout.write(c);
                }
                zin.closeEntry();
                fout.close();
            }
        }
        zin.close();
    }

    private static void dirChecker(final File location, final String dir) {
        final File f = new File(location, dir);
        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }

    public static void safeWriteToFile(final File f, final String s) {
        try {
            writeToFile(f, s);
        } catch (final IOException e) {
            Log.e(TAG, "cannot write to file", e);
        }
    }

    public static void writeToFile(final File f, final String s)
    throws IOException {
        if (f.exists()) {
            f.delete();
        }
        final BufferedWriter out = new BufferedWriter(new FileWriter(f));
        out.write(s);
        out.close();
    }

    public static String getMimeType(final Uri uri) {
        final String extension = getFileExtension(uri);
        final MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getMimeTypeFromExtension(extension);
    }

    public static String getNameFromUri(final Context ctx, final @NonNull Uri uri) {
        if ("file".equals(uri.getScheme())) {
            return new File(uri.getPath()).getName();
        } else {
            final ContentResolver cr = ctx.getContentResolver();
            String ret = "";
            Cursor metaCursor = null;
            try {
                metaCursor = cr.query(uri,
                                      new String[] {MediaStore.MediaColumns.DISPLAY_NAME},
                                      null, null, null);
            } catch (final SecurityException e) {
                Log.wtf(TAG, "no permission to read uri " + uri, e);
                ErrorReporter.report(ErrorType.FILE_NO_PERMISSION);
                return "";
            }
            if (metaCursor != null) {
                try {
                    if (metaCursor.moveToFirst()) {
                        ret = metaCursor.getString(0);
                    }
                } finally {
                    metaCursor.close();
                }
            }
            return ret;
        }
    }

    public static String getFileExtension(final Uri uri) {
        String m = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (m == null) {
            m = "";
        }
        return m;
    }

    /** Create a file Uri for saving an image or video */
    public static Uri getOutputMediaFileUri(final int type) throws IOException {
        final File file = FileUtils.getOutputMediaFile(type);
        if (file == null) {
            return null;
        }
        return Uri.fromFile(file);
    }

    /**
     * Create a File for saving an image or video
     *
     * @param type
     * @return
     */
    public static File getOutputMediaFile(final int type) throws IOException {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        final String state = Environment.getExternalStorageState();
        if ((state == null) || (!state.equals(Environment.MEDIA_MOUNTED)
                                && !state.equals(Environment.MEDIA_MOUNTED_READ_ONLY))) {
            throw new IOException("External Storage Dir not mounted");
        }
        final File mediaStorageDir = FileUtils.getMediaStorageDir(type);
        if (mediaStorageDir == null) {
            throw new IOException(ERROR_NO_MEDIA_DIR);
        }
        // Create a media file name
        final String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss",
                Locale.getDefault()).format(new Date());
        final File mediaFile;
        switch (type) {
        case MEDIA_TYPE_IMAGE:
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                                 + "IMG_" + timeStamp + ".jpg");
            break;
        case MEDIA_TYPE_VIDEO:
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                                 + "VID_" + timeStamp + ".mp4");
            break;
        case MEDIA_TYPE_AUDIO:
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                                 + "AUD_" + timeStamp + ".wav");
            break;
        default:
            return null;
        }
        return mediaFile;
    }

    /**
     * Returns the directory on the SD-Card
     *
     * @return
     */
    public static File getMediaStorageDir(final int type) {
        final File mediaStorageDir;
        switch (type) {
        case MEDIA_TYPE_AUDIO:
            mediaStorageDir = new File(
                Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "Mirakel");
            break;
        case MEDIA_TYPE_VIDEO:
            mediaStorageDir = new File(
                Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "Mirakel");
            break;
        case MEDIA_TYPE_IMAGE:
            mediaStorageDir = new File(
                Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Mirakel");
            break;
        default:
            mediaStorageDir = new File(getExportDir(), "files");
            break;
        }
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            return null;
        }
        return mediaStorageDir;
    }

    /**
     * Returns the Mirakel dir in /data/data/
     *
     * @return
     */
    public static String getMirakelDir() {
        if (DefinitionsHelper.APK_NAME == null) {
            DefinitionsHelper.APK_NAME = "de.azapps.mirakelandroid";
        }
        if (MIRAKEL_DIR == null) {
            MIRAKEL_DIR = Environment.getDataDirectory() + "/data/"
                          + DefinitionsHelper.APK_NAME + '/';
        }
        return MIRAKEL_DIR;
    }

    public static File getExportDir() {
        final File dir = new File(Environment.getExternalStorageDirectory(),
                                  "mirakel");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getLogDir() {
        final File dir = new File(getExportDir(), "logs");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static String readFile(final File f) throws IOException {
        int len;
        final char[] chr = new char[4096];
        final StringBuilder buffer = new StringBuilder();
        final FileReader reader = new FileReader(f);
        try {
            while ((len = reader.read(chr)) > 0) {
                buffer.append(chr, 0, len);
            }
        } finally {
            reader.close();
        }
        final String s = buffer.toString();
        Log.w(TAG, s);
        if (s == null) {
            Log.wtf(TAG, "file is empty");
        }
        return s;
    }

    public static boolean isAudio(final Uri uri) {
        return checkMimeBaseType(uri, "audio");
    }
    public static boolean isVideo(final Uri uri) {
        return checkMimeBaseType(uri, "video");
    }

    public static boolean isImage(final Uri uri) {
        return checkMimeBaseType(uri, "image");
    }

    private static boolean checkMimeBaseType(final Uri uri, final String type) {
        final String mimeType = getMimeType(uri);
        return (mimeType != null) && mimeType.startsWith(type);
    }

    @NonNull
    public static Optional<Uri> parsePath(@Nullable final String path) {
        final Optional<Uri> uri;
        if (path == null) {
            uri = Optional.absent();
        } else {
            uri = Optional.fromNullable(Uri.parse(path));
        }
        return uri;
    }
}
