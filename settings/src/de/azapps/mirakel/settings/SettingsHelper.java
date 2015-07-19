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

package de.azapps.mirakel.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.helper.export_import.AnyDoImport;
import de.azapps.mirakel.helper.export_import.ExportImport;
import de.azapps.mirakel.helper.export_import.WunderlistImport;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class SettingsHelper {

    private static final String TAG = "SettingsHelper";

    public static boolean handleActivityResult(final int requestCode,
            final int resultCode, final @NonNull Intent data, final @NonNull Context ctx) {
        final FileInputStream stream;
        switch (requestCode) {
        case SettingsActivity.FILE_IMPORT_DB:
            if (resultCode != Activity.RESULT_OK) {
                return false;
            }
            final Uri uri = data.getData();
            if (uri == null
                || !"db".equals(FileUtils.getFileExtension(uri))) {
                ErrorReporter.report(ErrorType.FILE_NOT_MIRAKEL_DB);
            }
            try {
                stream = FileUtils.getStreamFromUri(ctx, uri);
            } catch (final FileNotFoundException ignored) {
                ErrorReporter.report(ErrorType.FILE_NOT_MIRAKEL_DB);
                break;
            }
            new AlertDialogWrapper.Builder(ctx)
            .setTitle(R.string.import_sure)
            .setMessage(
                ctx.getString(R.string.import_sure_summary,
                              FileUtils.getNameFromUri(ctx, uri)))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.yes,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(
                    final DialogInterface dialog,
                    final int which) {
                    ExportImport.importDB(ctx, stream);
                }
            }).create().show();
            break;
        case SettingsActivity.FILE_ASTRID:
        case SettingsActivity.FILE_ANY_DO:
        case SettingsActivity.FILE_WUNDERLIST:
            if (resultCode != Activity.RESULT_OK) {
                return true;
            }
            try {
                stream = FileUtils.getStreamFromUri(ctx, data.getData());
            } catch (final FileNotFoundException e) {
                ErrorReporter.report(ErrorType.FILE_NOT_FOUND);
                break;
            }
            // Do the import in a background-task
            new AsyncTask<String, Void, Boolean>() {
                ProgressDialog dialog;

                @Override
                protected Boolean doInBackground(final String... params) {
                    switch (requestCode) {
                    case SettingsActivity.FILE_ASTRID:
                        return ExportImport.importAstrid(ctx, stream,
                                                         FileUtils.getMimeType(data.getData()));
                    case SettingsActivity.FILE_ANY_DO:
                        try {
                            return AnyDoImport.exec(ctx, stream);
                        } catch (final DefinitionsHelper.NoSuchListException e) {
                            ErrorReporter
                            .report(ErrorType.LIST_VANISHED);
                            Log.wtf(TAG, "list vanished", e);
                            return true;
                        }
                    case SettingsActivity.FILE_WUNDERLIST:
                        return WunderlistImport.exec(stream);
                    default:
                        return false;
                    }
                }

                @Override
                protected void onPostExecute(final Boolean result) {
                    this.dialog.dismiss();
                    if (!result) {
                        ErrorReporter.report(ErrorType.ASTRID_ERROR);
                    } else {
                        Toast.makeText(ctx, R.string.astrid_success, Toast.LENGTH_SHORT)
                        .show();
                        // ugly but simple
                        Helpers.restartApp(ctx);
                    }
                }

                @Override
                protected void onPreExecute() {
                    this.dialog = ProgressDialog
                                  .show(ctx, ctx.getString(R.string.importing), ctx.getString(R.string.wait), true);
                }
            } .execute("");
            break;
        case SettingsActivity.DONATE:
            if (resultCode != Activity.RESULT_OK) {
                return false;
            }
            if (!MirakelCommonPreferences.isTablet()) {
                return true;
            }
            break;
        default:
            break;
        }
        return false;
    }
}
