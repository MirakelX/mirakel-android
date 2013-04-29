package de.azapps.mirakel.sync;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;

public class Network extends AsyncTask<String, Integer, String> {
	private String TAG="GetData";
	protected DataDownloadCommand commands;
	protected String Email;
	protected String Password;
	protected List<BasicNameValuePair> HeaderData;
	protected int Mode;
	protected Context context;	

	public Network(DataDownloadCommand commands, String email, String password,
			int mode,Context context) {
		this.commands = commands;
		this.Email = email;
		this.Password = password;
		this.Mode = mode;
		this.context=context;
	}

	public Network(DataDownloadCommand commands, String email, String password,
			int mode, List<BasicNameValuePair> data, Context context) {
		this.commands = commands;
		this.Email = email;
		this.Password = password;
		this.Mode = mode;
		this.HeaderData = data;
		this.context=context;
	}

	@Override
	protected String doInBackground(String... urls) {
		try {
			return downloadUrl(urls[0]);
		} catch (IOException e) {
			Log.e(TAG,"Unable to retrieve web page. URL may be invalid.");
		} catch (URISyntaxException e) {
			Log.e(TAG,"Invalid UrlSyntax");
		}
		Integer[] t={1};
		publishProgress(t);

		return null;
	}
	
	@Override
	protected void onProgressUpdate(Integer... integers) {
	  if(integers[0] == 1) {
		  Toast.makeText(context, context.getString(R.string.NoNetwork), Toast.LENGTH_LONG).show(); 
	  }
	}

	@Override
	protected void onPostExecute(String result) {
		if(result!=null)
			commands.after_exec(result);
		else
			Log.e(TAG,"No Response");
	}

	private String downloadUrl(String myurl) throws IOException,
			URISyntaxException {
		HttpParams params = new BasicHttpParams();
		params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		HttpConnectionParams.setTcpNoDelay(params, true);
		DefaultHttpClient client=new DefaultHttpClient(params);
		client.getCredentialsProvider().setCredentials(new AuthScope(null, -1),
				new UsernamePasswordCredentials(Email, Password));
		HttpResponse response;
		switch (Mode) {
		case Mirakel.Http_Mode.GET:
			Log.v(TAG,"GET "+myurl);
			HttpGet get = new HttpGet();
			get.setURI(new URI(myurl));
			response= client.execute(get);
			break;
		case Mirakel.Http_Mode.PUT:
			Log.v(TAG,"PUT "+myurl);
			HttpPut put = new HttpPut();
			put.setURI(new URI(myurl));
			put.setEntity(new UrlEncodedFormEntity(HeaderData));
			response=client.execute(put);
			break;
		case Mirakel.Http_Mode.POST:
			Log.v(TAG,"POST "+myurl);
			HttpPost post = new HttpPost();
			post.setURI(new URI(myurl));
			post.setEntity(new UrlEncodedFormEntity(HeaderData));
			response=client.execute(post);
			break;
		case Mirakel.Http_Mode.DELETE:
			Log.v(TAG,"DELETE "+myurl);
			HttpDelete delete = new HttpDelete();
			delete.setURI(new URI(myurl));
			response= client.execute(delete);
			break;
		default:
			Log.e("HTTP-MODE", "Unknown Http-Mode");
			return null;
		}
		Log.v(TAG,"Http-Status: "+response.getStatusLine().getStatusCode());
		if(response.getEntity()==null)
			return "";
		return EntityUtils.toString(response.getEntity());
	}
}
