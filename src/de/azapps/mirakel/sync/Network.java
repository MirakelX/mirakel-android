/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.sync;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpReport;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.Log;

public class Network extends AsyncTask<String, Integer, String> {

	public enum HttpMode {
		GET, POST, PUT, DELETE, REPORT;
	}

	private static String				TAG			= "MirakelNetwork";
	private static final Integer		NoNetwork	= 1;
	private static final Integer		NoHTTPS		= 2;

	protected DataDownloadCommand		commands;
	protected List<BasicNameValuePair>	headerData;
	protected HttpMode					mode;
	protected Context					context;
	protected String					token;
	protected ACCOUNT_TYPES				syncTyp;
	private String						content;
	private String						username;
	private String						password;

	public Network(DataDownloadCommand commands, HttpMode mode, List<BasicNameValuePair> data, Context context, String Token) {
		this.commands = commands;
		this.mode = mode;
		this.headerData = data;
		this.context = context;
		this.token = Token;
		this.syncTyp = ACCOUNT_TYPES.CALDAV;
	}

	public Network(DataDownloadCommand commands, HttpMode mode, String content, Context ctx, String username, String password) {
		this.commands = commands;
		this.mode = mode;
		this.content = content;
		this.context = ctx;
		this.username = username;
		this.password = password;
	}

	public static String getToken(String json) {
		if (json.indexOf("{\"token\":\"") != -1) {
			try {
				return json.substring(10, 30);
			} catch (IndexOutOfBoundsException e) {
				Log.d(TAG, "Unkown responsformat");
				return null;
			}
		} else return null;
	}

	@Override
	protected String doInBackground(String... urls) {
		try {
			return downloadUrl(urls[0]);
		} catch (IOException e) {
			Log.e(TAG, "Unable to retrieve web page. URL may be invalid.");
		} catch (URISyntaxException e) {
			Log.e(TAG, "Invalid UrlSyntax");
		}
		Integer[] t = { NoNetwork };
		publishProgress(t);

		return "";
	}

	@Override
	protected void onProgressUpdate(Integer... integers) {
		if (integers[0] == NoNetwork) {
			Toast.makeText(context, context.getString(R.string.NoNetwork),
					Toast.LENGTH_LONG).show();
		} else if (integers[0] == NoHTTPS) {
			Toast.makeText(context, context.getString(R.string.no_https),
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onPostExecute(String result) {
		Log.d(TAG, "No Response");
		commands.after_exec(result);
	}
	
	private String downloadUrl(String myurl) throws IOException, URISyntaxException {
		if (token != null) {
			myurl += "?authentication_key=" + token;
		}
		if (myurl.indexOf("https") == -1) {
			Integer[] t = { NoHTTPS };
			publishProgress(t);
		}

		/*
		 * String authorizationString = null;
		 * if (syncTyp == ACCOUNT_TYPES.CALDAV) {
		 * authorizationString = "Basic "
		 * + Base64.encodeToString(
		 * (username + ":" + password).getBytes(),
		 * Base64.NO_WRAP);
		 * }
		 */

		CredentialsProvider credentials = new BasicCredentialsProvider();
		credentials.setCredentials(new AuthScope(new URI(myurl).getHost(), -1),
				new UsernamePasswordCredentials(username, password));

		HttpParams params = new BasicHttpParams();
		params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION,
				HttpVersion.HTTP_1_1);
		HttpConnectionParams.setTcpNoDelay(params, true);
		HttpClient httpClient;
		/*
		 * if(syncTyp == ACCOUNT_TYPES.MIRAKEL)
		 * httpClient = sslClient(client);
		 * else {
		 */
		DefaultHttpClient tmpHttpClient = new DefaultHttpClient(params);
		tmpHttpClient.setCredentialsProvider(credentials);

		httpClient = tmpHttpClient;
		// }
		httpClient.getParams().setParameter("http.protocol.content-charset",
				HTTP.UTF_8);

		HttpResponse response;
		try {
			switch (mode) {
				case GET:
					Log.v(TAG, "GET " + myurl);
					HttpGet get = new HttpGet();
					get.setURI(new URI(myurl));
					response = httpClient.execute(get);
					break;
				case PUT:
					Log.v(TAG, "PUT " + myurl);
					HttpPut put = new HttpPut();
					if (syncTyp == ACCOUNT_TYPES.CALDAV) {
						put.addHeader(HTTP.CONTENT_TYPE,
								"text/calendar; charset=utf-8");
					}
					put.setURI(new URI(myurl));
					put.setEntity(new StringEntity(content, HTTP.UTF_8));
					Log.v(TAG, content);

					response = httpClient.execute(put);
					break;
				case POST:
					Log.v(TAG, "POST " + myurl);
					HttpPost post = new HttpPost();
					post.setURI(new URI(myurl));
					post.setEntity(new UrlEncodedFormEntity(headerData,
							HTTP.UTF_8));
					response = httpClient.execute(post);
					break;
				case DELETE:
					Log.v(TAG, "DELETE " + myurl);
					HttpDelete delete = new HttpDelete();
					delete.setURI(new URI(myurl));
					response = httpClient.execute(delete);
					break;
				case REPORT:
					Log.v(TAG, "REPORT " + myurl);
					HttpReport report = new HttpReport();
					report.setURI(new URI(myurl));
					Log.d(TAG, content);
					report.setEntity(new StringEntity(content, HTTP.UTF_8));
					response = httpClient.execute(report);
					break;
				default:
					Log.wtf("HTTP-MODE", "Unknown Http-Mode");
					return null;
			}
		} catch (Exception e) {
			Log.e(TAG, "No Networkconnection available");
			Log.w(TAG, Log.getStackTraceString(e));
			return "";
		}
		Log.v(TAG, "Http-Status: " + response.getStatusLine().getStatusCode());
		if (response.getEntity() == null) return "";
		String r = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);
		Log.d(TAG, r);
		return r;
	}
}
