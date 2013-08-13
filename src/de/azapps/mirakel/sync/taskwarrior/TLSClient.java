package de.azapps.mirakel.sync.taskwarrior;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.SSLSocketFactory;

import de.azapps.mirakel.helper.Log;

public class TLSClient {

	private static final String TAG = "TLSClient";
//	private String _ca;
	// private gnutls_certificate_credentials_t _credentials;
	// private gnutls_session_t _session;
	private SSLSocket _socket;
	private SSLSocketFactory sslFact;

	static void gnutls_log_function(int level, String message) {
		Log.d(TAG, "c: " + message);
	}

	// //////////////////////////////////////////////////////////////////////////////
	public TLSClient() {
		_socket = null;
		sslFact=null;
	}

	// //////////////////////////////////////////////////////////////////////////////
	@Override
	protected void finalize() {
		try {
			_socket.close();
		} catch (IOException e) {
			Log.e(TAG, "Cannot close Socket");
		}
	}



	// //////////////////////////////////////////////////////////////////////////////
	public void init (final String ca)
	{
//	    sslFact = (SSLSocketFactory) TaskWarriorSSLSocketFactory.getSocketFactory();

	        InputStream certificateStream;
			try {
				certificateStream = new FileInputStream(new File(ca));
			} catch (FileNotFoundException e) {
				Log.e(TAG, "no cert-file found");
				return;
			}

	        KeyStore keyStore;
			try {
				keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			} catch (KeyStoreException e) {
				Log.e(TAG, "cannot read keystore");
				try {
					certificateStream.close();
				} catch (IOException e1) {
					Log.e(TAG, "cannot close filestream");
					return;
				}
				return;
			}
	        try {
				keyStore.load(certificateStream, null);
			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG, "Algorithm not implemented");
				return;
			} catch (CertificateException e) {
				Log.e(TAG, "malformed cert");
				return;
			} catch (IOException e) {
				Log.e(TAG, "cannot write cert to keystore");
				return;
			}

	        try {
				System.out.println("I have loaded [" + keyStore.size() + "] certificates");
			} catch (KeyStoreException e1) {
				Log.e(TAG, "cannot read keystore");
				return;
			}

	        KeyManagerFactory keyManagerFactory;
			try {
				keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			} catch (NoSuchAlgorithmException e1) {
				Log.e(TAG, "Algorithm not implemented");
				return;
			}
	        try {
				keyManagerFactory.init(keyStore, null);
			} catch (UnrecoverableKeyException e) {
				Log.e(TAG, "cannot read cert from keystore");
				return;
			} catch (KeyStoreException e) {
				Log.e(TAG, "cannot read keystore");
				return;
			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG, "no cert-file found");
				return;
			}
		    @SuppressWarnings("unused")
			X509TrustManager manager = null;

		    TrustManagerFactory trustManagerFactory = null;
			try {
				trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG, "Algorithm not implemented");
				return;
			}
		    

		    try {
				trustManagerFactory.init(keyStore);
			} catch (KeyStoreException e) {
				Log.e(TAG, "cannot read keystore");
				return;
			}
		    TrustManager[] managers = trustManagerFactory.getTrustManagers();

		    for (TrustManager tm : managers)
		    {
		        if (tm instanceof X509TrustManager) 
		        {
		            manager = (X509TrustManager) tm;
		            break;
		        }
		    }

	        try {
				sslFact = new SSLSocketFactory(keyStore);
			} catch (KeyManagementException e) {
				Log.e(TAG, "cannot read keystore");
				return;
			} catch (UnrecoverableKeyException e) {
				Log.e(TAG, "cannot read cert from keystore");
				return;
			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG, "Algorithm not implemented");
				return;
			} catch (KeyStoreException e) {
				Log.e(TAG, "cannot read keystore");
				return;
			}

	}
	// //////////////////////////////////////////////////////////////////////////////
	public void connect(final String host, final int port) {

		Socket s = new Socket();
		if (_socket != null) {
			try {
				_socket.close();
			} catch (IOException e) {
				Log.e(TAG, "cannot close socket");
			}
		}
		try {
			_socket = (SSLSocket) sslFact.createSocket(s, host, port, false);
		} catch (UnknownHostException e) {
			Log.e(TAG, "Unkown Host");
		} catch (IOException e) {
			Log.e(TAG, "Cannot create Socket");
		}
	}

	// //////////////////////////////////////////////////////////////////////////////
//	public void bye() {
//		_socket.gnutls_bye(_session, GNUTLS_SHUT_RDWR);
//	}

	// //////////////////////////////////////////////////////////////////////////////
	public void send(final String data) {
		OutputStream out = null;
		try {
			out = _socket.getOutputStream();
		} catch (IOException e) {
			Log.e(TAG, "cannot get outputstream");
			return;
		}
		try {
			out.write(data.getBytes());
		} catch (IOException e) {
			Log.e(TAG, "cannot write data to outputstream");
		}
		try {
			out.flush();
		} catch (IOException e) {
			Log.e(TAG, "cannot flush data to outputstream");
		}
		try {
			out.close();
		} catch (IOException e) {
			Log.e(TAG, "cannot close outputstream");
		}
	}

	// //////////////////////////////////////////////////////////////////////////////
	public String recv() {
		String ret = "";
		InputStream in;
		try {
			in = _socket.getInputStream();
		} catch (IOException e) {
			Log.e(TAG, "cannot open Inputstream");
			return ret;
		}
		BufferedReader buff = new BufferedReader(new InputStreamReader(in));
		try {
			while (buff.ready()) {
				ret += buff.readLine();
			}
		} catch (IOException e1) {
			Log.e(TAG, "cannot read buffer");
		}
		try {
			in.close();
		} catch (IOException e) {
			Log.e(TAG, "cannot close Inputstream");
		}

		return ret;
	}
}
