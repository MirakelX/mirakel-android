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
package de.azapps.mirakel.sync.taskwarrior;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.sync.R;
import de.azapps.mirakel.sync.taskwarrior.services.SyncAdapter;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class TaskWarriorSetupActivity extends Activity {
    private final static String TAG = "TaskWarriorSetupActivity";
    private static final Pattern DELIMITER = Pattern.compile(":");

    private class DownloadTask extends AsyncTask<URL, Integer, Integer> {
        private final static String TAG = "DownloadTask";
        private final Exec pre, progress, post;

        public DownloadTask(final Exec pre, final Exec progress, final Exec post) {
            this.pre = pre;
            this.progress = progress;
            this.post = post;
        }

        @Override
        protected Integer doInBackground(final URL... sUrl) {
            final URL url = sUrl[0];
            try {
                final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.connect();
                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return RESULT_ERROR;
                }
                setupTaskWarrior(connection.getInputStream(), false);
            } catch (final ProtocolException e) {
                Log.e(TAG, "Could not download config", e);
                return RESULT_ERROR;
            } catch (final IOException e) {
                Log.e(TAG, "Could not download config", e);
                return RESULT_ERROR;
            }
            return RESULT_SUCCESS;
        }

        @Override
        protected void onPostExecute(final Integer result) {
            this.post.execute(result);
        }

        @Override
        protected void onPreExecute() {
            this.pre.execute(null);
        }

        @Override
        protected void onProgressUpdate(final Integer... progresses) {
            super.onProgressUpdate(progresses);
            this.progress.execute(progresses[0]);
        }

    }

    private interface Exec {
        void execute(final Integer status);
    }

    private static final Integer RESULT_ERROR = 0;
    protected static final Integer RESULT_SUCCESS = 1;

    private static final int CONFIG_QR = 0, CONFIG_TASKWARRIOR = 1;

    @NonNull
    private AccountManager mAccountManager;

    private ProgressDialog progressDialog;

    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
        case CONFIG_QR:
            final String inputUrl = data.getStringExtra("SCAN_RESULT");
            setupTaskwarriorFromURL(inputUrl);
            break;
        case CONFIG_TASKWARRIOR:
            handleFileIntent(data);
            break;
        default:
            break;
        }
    }

    private void handleFileIntent(final Intent data) {
        try {
            final InputStream stream = FileUtils.getStreamFromUri(this, data.getData());
            setupTaskWarrior(stream, true);
        } catch (final FileNotFoundException e) {
            Log.e(TAG, "File not found", e);
            ErrorReporter.report(ErrorType.TASKWARRIOR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mAccountManager = AccountManager.get(this);
        final Intent intent = getIntent();
        if ((intent != null) && Intent.ACTION_VIEW.equals(intent.getAction()) &&
            (intent.getData() != null)) {
            handleFileIntent(intent);
        }
        ThemeManager.setTheme(this);
        setContentView(R.layout.activity_sync_taskwarrior);
        final Button scanQR = (Button) findViewById(R.id.sync_taskwarrior_scan_qr);
        scanQR.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                try {
                    final Intent intent = new Intent(
                        "com.google.zxing.client.android.SCAN");
                    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                    startActivityForResult(intent,
                                           TaskWarriorSetupActivity.CONFIG_QR);
                } catch (final RuntimeException ignored) {
                    new AlertDialogWrapper.Builder(TaskWarriorSetupActivity.this)
                    .setTitle(R.string.no_barcode_app)
                    .setMessage(R.string.no_barcode_app_message)
                    .setPositiveButton(R.string.no_barcode_app_install,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            final DialogInterface dialog,
                            final int which) {
                            final Uri marketUri = Uri
                                                  .parse("market://details?id=com.google.zxing.client.android");
                            final Intent marketIntent = new Intent(
                                Intent.ACTION_VIEW,
                                marketUri);
                            startActivity(marketIntent);
                        }
                    }).show();
                }
            }
        });
        final Button selectConfigFile = (Button) findViewById(R.id.sync_taskwarrior_select_file);
        selectConfigFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                Helpers.showFileChooser(
                    TaskWarriorSetupActivity.CONFIG_TASKWARRIOR,
                    getString(R.string.select_config), TaskWarriorSetupActivity.this);
            }
        });
        final Button register = (Button) findViewById(R.id.sync_taskwarrior_register);
        register.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
                                                        .parse("http://mirakel.azapps.de/users/sign_up"));
                startActivity(browserIntent);
            }
        });
        final Button howto = (Button) findViewById(R.id.sync_taskwarrior_how_to);
        howto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
                                                        .parse("http://mirakel.azapps.de/taskwarrior.html"));
                startActivity(browserIntent);
            }
        });
    }

    enum PARSE_STATE {
        ONE_LINER, MULTI
    }

    private Map<String, String> parseTWFile(final InputStream stream)
    throws IOException {
        final BufferedReader r = new BufferedReader(new InputStreamReader(
                    stream));
        final Map<String, String> values = new HashMap<>(7);
        PARSE_STATE state = PARSE_STATE.ONE_LINER;
        String line;
        String tkey = null;
        StringBuilder tvalue = new StringBuilder();
        // General parsing
        while ((line = r.readLine()) != null) {
            if ("ca.cert".equals(tkey) && line.contains(":")) {
                values.put(tkey, tvalue.toString());
                state = PARSE_STATE.ONE_LINER;
            }
            switch (state) {
            case ONE_LINER:
                final String[] splitted = DELIMITER.split(line, 2);
                if (splitted.length != 2) {
                    continue;
                }
                final String key = splitted[0].trim().toLowerCase();
                final String value = splitted[1].trim();
                if (value.isEmpty()) {
                    tkey = key;
                    tvalue = new StringBuilder();
                    state = PARSE_STATE.MULTI;
                } else {
                    values.put(key, value);
                }
                break;
            case MULTI:
                // Yeah, there is an \n at the end of the file
                tvalue.append(line).append('\n');
                if (line.startsWith("-----END") && !"ca.cert".equals(tkey)) {
                    values.put(tkey, tvalue.toString());
                    state = PARSE_STATE.ONE_LINER;
                }
                break;
            }
        }
        if (state == PARSE_STATE.MULTI) {
            values.put(tkey, tvalue.toString());
        }
        return values;
    }

    private static class ParseException extends Exception {
        private static final long serialVersionUID = -5931298406798881507L;

        public ParseException(final String message) {
            super(message);
        }
    }

    private void setupTaskWarrior(final InputStream stream,
                                  final boolean showToast) {
        try {
            setupTaskWarrior(stream);
            Toast.makeText(this,
                           getString(R.string.sync_taskwarrior_setup_success),
                           Toast.LENGTH_LONG).show();
            finish();
        } catch (final IOException e) {
            Log.e(TAG, "IOException", e);
            ErrorReporter.report(ErrorType.TASKWARRIOR_FILE_NOT_FOUND);
        } catch (final ParseException e) {
            Log.e(TAG, "ParseException", e);
            if (showToast) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupTaskWarrior(final InputStream stream)
    throws ParseException, IOException {
        final Map<String, String> values;
        try {
            values = parseTWFile(stream);
        } catch (final IOException e) {
            Log.e(TAG, "IOException", e);
            throw new ParseException(getString(R.string.config_404));
        }
        final String[] neededValues = { "username", "org", "user key",
                                        "server", "client.cert", "client.key", "ca.cert"
                                      };
        for (final String key : neededValues) {
            if (!values.containsKey(key)) {
                throw new ParseException(getString(R.string.config_error_empty,
                                                   key));
            }
        }
        final Bundle b = new Bundle();
        b.putString(SyncAdapter.BUNDLE_SERVER_TYPE, TaskWarriorSync.TYPE);
        final Account account = new Account(values.get("username"),
                                            AccountMirakel.ACCOUNT_TYPE_MIRAKEL);
        b.putString(SyncAdapter.BUNDLE_ORG, values.get("org"));
        b.putString(SyncAdapter.BUNDLE_SERVER_URL, values.get("server"));
        b.putString(DefinitionsHelper.BUNDLE_CERT, values.get("ca.cert"));
        b.putString(DefinitionsHelper.BUNDLE_CERT_CLIENT,
                    values.get("client.cert"));
        this.mAccountManager.addAccountExplicitly(account,
                values.get("client.key") + "\n:" + values.get("user key"), b);
    }

    public void setupTaskwarriorFromURL(String inputUrl) {
        this.progressDialog = new ProgressDialog(this);
        this.progressDialog
        .setMessage(getString(R.string.sync_taskwarrior_configuring));
        this.progressDialog.setIndeterminate(true);
        this.progressDialog.show();
        if (!inputUrl.startsWith("http")) {
            inputUrl = "http://" + inputUrl;
        }
        try {
            final URL url = new URL(inputUrl);
            final DownloadTask dlTask = new DownloadTask(new Exec() {
                @Override
                public void execute(final Integer status) {
                }
            }, new Exec() {
                @Override
                public void execute(final Integer status) {
                }
            }, new Exec() {
                @Override
                public void execute(final Integer result) {
                    if (!RESULT_SUCCESS.equals(result)) {
                        ErrorReporter
                        .report(ErrorType.TASKWARRIOR_COULD_NOT_DOWNLOAD);
                    } else {
                        Toast.makeText(
                            TaskWarriorSetupActivity.this,
                            getString(R.string.sync_taskwarrior_setup_success),
                            Toast.LENGTH_LONG).show();
                    }
                    TaskWarriorSetupActivity.this.progressDialog.dismiss();
                    finish();
                }
            });
            dlTask.execute(url);
        } catch (final MalformedURLException ignore) {
            this.progressDialog.dismiss();
            ErrorReporter.report(ErrorType.TASKWARRIOR_URL_NOT_FOUND);
            this.progressDialog.dismiss();
        }
    }

}
