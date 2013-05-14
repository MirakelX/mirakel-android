/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
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
import android.util.Log;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;

public class Network extends AsyncTask<String, Integer, String> {
	public static final class SYNC_STATE {
		public final static short NOTHING = 0;
		public final static short DELETE = -1;
		public final static short ADD = 1;
		public final static short NEED_SYNC = 2;
		public final static short IS_SYNCED = 3;
	}

	private String TAG = "GetData";
	protected DataDownloadCommand commands;
	protected List<BasicNameValuePair> HeaderData;
	protected int Mode;
	protected Context context;
	protected String Token;

	private static final Integer NoNetwork = 1;
	private static final Integer NoHTTPS = 2;

	public Network(DataDownloadCommand commands, int mode, Context context,
			String Token) {
		this.commands = commands;
		this.Mode = mode;
		this.context = context;
		this.Token = Token;
	}

	public Network(DataDownloadCommand commands, int mode,
			List<BasicNameValuePair> data, Context context, String Token) {
		this.commands = commands;
		this.Mode = mode;
		this.HeaderData = data;
		this.context = context;
		this.Token = Token;
	}

	public static String getToken(String json) {
		if (json.indexOf("{\"token\":\"") != -1) {
			try {
				return json.substring(10, 30);
			} catch (IndexOutOfBoundsException e) {
				return null;
			}
		} else
			return null;
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

		return null;
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
		if (result != null)
			commands.after_exec(result);
		else
			Log.e(TAG, "No Response");
	}

	private HttpClient sslClient(HttpClient client) {
		try {
			X509TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] xcs,
						String string) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] xcs,
						String string) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory ssf = new MirakelSSLSocketFactory(ctx);
			ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = client.getConnectionManager();
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https", ssf, 443));
			sr.register(new Scheme("http", ssf, 80));
			return new DefaultHttpClient(ccm, client.getParams());
		} catch (Exception ex) {
			return null;
		}
	}

	private String downloadUrl(String myurl) throws IOException,
			URISyntaxException {
		if (Token != null) {
			myurl += "?authentication_key=" + Token;
		}
		if (myurl.indexOf("https") == -1) {
			Integer[] t = { NoHTTPS };
			publishProgress(t);
		}
		HttpParams params = new BasicHttpParams();
		params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION,
				HttpVersion.HTTP_1_1);
		HttpConnectionParams.setTcpNoDelay(params, true);
		DefaultHttpClient client = new DefaultHttpClient(params);
		HttpClient httpClient = sslClient(client);
		httpClient.getParams().setParameter("http.protocol.content-charset",
				HTTP.UTF_8);

		HttpResponse response;
		try {
			switch (Mode) {
			case Mirakel.HttpMode.GET:
				Log.v(TAG, "GET " + myurl);
				HttpGet get = new HttpGet();
				get.setURI(new URI(myurl));
				response = httpClient.execute(get);
				break;
			case Mirakel.HttpMode.PUT:
				Log.v(TAG, "PUT " + myurl);
				HttpPut put = new HttpPut();
				put.setURI(new URI(myurl));
				put.setEntity(new UrlEncodedFormEntity(HeaderData, HTTP.UTF_8));
				response = httpClient.execute(put);
				break;
			case Mirakel.HttpMode.POST:
				Log.v(TAG, "POST " + myurl);
				HttpPost post = new HttpPost();
				post.setURI(new URI(myurl));
				post.setEntity(new UrlEncodedFormEntity(HeaderData, HTTP.UTF_8));
				response = httpClient.execute(post);
				break;
			case Mirakel.HttpMode.DELETE:
				Log.v(TAG, "DELETE " + myurl);
				HttpDelete delete = new HttpDelete();
				delete.setURI(new URI(myurl));
				response = httpClient.execute(delete);
				break;
			default:
				Log.e("HTTP-MODE", "Unknown Http-Mode");
				return null;
			}
		} catch (Exception e) {
			Log.w(TAG, Log.getStackTraceString(e));
			return "";
		}
		Log.v(TAG, "Http-Status: " + response.getStatusLine().getStatusCode());
		if (response.getEntity() == null)
			return "";
		String r = EntityUtils.toString(response.getEntity());
		if (Mirakel.DEBUG)
			Log.d(TAG, r);
		return r;
	}
}
