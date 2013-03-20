package de.azapps.mirakel;

import java.io.IOException;
import java.net.URISyntaxException;
import android.os.AsyncTask;


public class Network extends AsyncTask<String, String, String> {
	//private String TAG="GetData";
	protected DataCommand after_ex;
	protected DownloadCommand download;

	public Network(DataCommand after, DownloadCommand down) {
		this.after_ex=after;
		this.download=down;
	}

	@Override
	protected String doInBackground(String... urls) {

		// params comes from the execute() call: params[0] is the url.
		try {
			return download.downloadUrl(urls[0]);
		} catch (IOException e) {
			return "Unable to retrieve web page. URL may be invalid.";
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	protected void onPostExecute(String result) {
		//Log.e(TAG,"Get done "+result);
		after_ex.after_exec(result);
	}
}
