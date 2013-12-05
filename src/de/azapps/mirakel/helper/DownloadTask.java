package de.azapps.mirakel.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Pair;

public class DownloadTask extends AsyncTask<Pair<URL, File>, Integer, Integer> {
	private final static String TAG = "DownloadTask";

	public static final Integer RESULT_ERROR = 0, RESULT_SUCCESS = 1;
	private Exec pre, progress, post;

	public DownloadTask(Exec pre, Exec progress, Exec post) {
		this.pre = pre;
		this.progress = progress;
		this.post = post;
	}

	@Override
	protected Integer doInBackground(Pair<URL, File>... sUrl) {
		URL url = sUrl[0].first;
		File destFile = sUrl[0].second;

		InputStream input = null;
		OutputStream output = null;
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.connect();

			// expect HTTP 200 OK, so we don't mistakenly save error report
			// instead of the file
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				return RESULT_ERROR;

			// this will be useful to display download percentage
			// might be -1: server did not report the length
			int fileLength = connection.getContentLength();

			// download the file
			input = connection.getInputStream();
			output = new FileOutputStream(destFile);

			byte data[] = new byte[4096];
			long total = 0;
			int count;
			while ((count = input.read(data)) != -1) {
				// allow canceling with back button
				if (isCancelled()) {
					input.close();
					return null;
				}
				total += count;
				// publishing the progress....
				if (fileLength > 0) // only if total length is known
					publishProgress((int) (total * 100 / fileLength));
				output.write(data, 0, count);
			}
		} catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
			return RESULT_ERROR;
		} finally {
			try {
				if (output != null)
					output.close();
				if (input != null)
					input.close();
			} catch (IOException ignored) {
			}

			if (connection != null)
				connection.disconnect();
		}

		return RESULT_SUCCESS;
	}

	@Override
	protected void onPreExecute() {
		pre.execute(null);
	}

	@Override
	protected void onPostExecute(Integer result) {
		post.execute(result);
	}

	@Override
	protected void onProgressUpdate(Integer... progresses) {
		super.onProgressUpdate(progresses);
		progress.execute(progresses[0]);
	}

	public interface Exec {
		void execute(Integer status);
	}
}
