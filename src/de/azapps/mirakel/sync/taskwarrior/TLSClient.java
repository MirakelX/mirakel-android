package de.azapps.mirakel.sync.taskwarrior;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import android.util.Base64;
import de.azapps.mirakel.helper.Log;

public class TLSClient {

	private static final String TAG = "TLSClient";
	// private String _ca;
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
		sslFact = null;
	}

	// //////////////////////////////////////////////////////////////////////////////
	@Override
	protected void finalize() {
		if (_socket == null){
			Log.e(TAG, "socket null");
			return;
		}
		try {
			_socket.close();
		} catch (IOException e) {
			Log.e(TAG, "Cannot close Socket");
		}
	}
	
	private static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
	    String data = new String(pem);
	    String[] tokens = data.split(beginDelimiter);
	    tokens = tokens[1].split(endDelimiter);
	    return Base64.decode(tokens[0], Base64.NO_PADDING);        
	}

	private static RSAPrivateKey generatePrivateKeyFromPEM(byte[] keyBytes) {
		keyBytes = parseDERFromPEM(keyBytes,"-----BEGIN RSA PRIVATE KEY-----","-----END RSA PRIVATE KEY-----");
	    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

	    KeyFactory factory;
		try {
			factory = KeyFactory.getInstance("RSA", "BC");
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "RSA-Algorithm not found");
			return null;
		} catch (NoSuchProviderException e) {
			Log.e(TAG,"BC not found");
			return null;
		}

	    try {
			return (RSAPrivateKey)factory.generatePrivate(spec);
		} catch (InvalidKeySpecException e) {
			Log.e(TAG,"cannot parse key");
			Log.e(TAG,Log.getStackTraceString(e));
			return null;
		}        
	}

	private static X509Certificate generateCertificateFromPEM(byte[] certBytes)  {
		certBytes=parseDERFromPEM(certBytes,"-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
	    CertificateFactory factory;
		try {
			factory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			return null;
		}

	    try {
			return (X509Certificate)factory.generateCertificate(new ByteArrayInputStream(certBytes));
		} catch (CertificateException e) {
			return null;
		}      
	}
	
	private byte[] fileToBytes(final File f){
	    int size = (int) f.length();
	    byte[] bytes = new byte[size];
	        BufferedInputStream buf;
			try {
				buf = new BufferedInputStream(new FileInputStream(f));
			} catch (FileNotFoundException e1) {
				Log.e(TAG,"cannot get BufferedInputStream");
				return bytes;
			}
	        try {
				buf.read(bytes, 0, bytes.length);
			} catch (IOException e) {
				Log.e(TAG,"cannot read bytes from file");
			} finally{
				try {
					buf.close();
				} catch (IOException e) {
					Log.e(TAG, "cannot close BufferedInputStream");
				}
			}
	    return bytes;
	}

	// //////////////////////////////////////////////////////////////////////////////
	public void init(final File ca, final File Key) {
		Security.addProvider(new BouncyCastleProvider());
		SSLContext context = null;
		try {
			context = SSLContext.getInstance("TLS");
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "algorithm not implemented");
		}
		X509Certificate cert = generateCertificateFromPEM(fileToBytes(ca));
		RSAPrivateKey key = generatePrivateKeyFromPEM(fileToBytes(Key));
		try {
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			keystore.load(null);
			keystore.setCertificateEntry("taskwarrior-CERT", cert);
			Certificate[] c = new Certificate[] { cert };
			keystore.setKeyEntry("taskwarrior-KEY", key,
					"geheim".toCharArray(), c);

			KeyManagerFactory kmf = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keystore, "geheim".toCharArray());

			KeyManager[] km = kmf.getKeyManagers();
			context.init(km, null, null);
			sslFact = context.getSocketFactory();
		} catch (KeyStoreException e) {
			Log.e(TAG, "cannot access keystore");
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "Algorithm not implemented");
		} catch (CertificateException e) {
			Log.e(TAG, "Certificate invalid");
		} catch (IOException e) {
			Log.e(TAG, "cannot load keystore");
		} catch (UnrecoverableKeyException e) {
			Log.e(TAG, "cannot read Key");
		} catch (KeyManagementException e) {
			Log.e(TAG, "cannot write key to keystore");
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
			Log.e(TAG,Log.getStackTraceString(e));
		}
	}

	// //////////////////////////////////////////////////////////////////////////////
	// public void bye() {
	// _socket.gnutls_bye(_session, GNUTLS_SHUT_RDWR);
	// }

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
