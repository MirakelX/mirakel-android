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
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.apache.http.conn.ssl.SSLSocketFactory;

import javax.net.ssl.SSLSocket;

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

	@SuppressWarnings("unused")
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
	public void init(final File root) {	
		Log.i(TAG, "init");
	    try {
	    	X509Certificate ROOT = generateCertificateFromPEM(fileToBytes(root));
	    	KeyStore trusted = KeyStore.getInstance(KeyStore.getDefaultType());
	    	trusted.load(null);
			trusted.setCertificateEntry("taskwarrior-ROOT", ROOT);
	        sslFact= new SSLSocketFactory(trusted);
	      } catch (Exception e) {
	        throw new AssertionError(e);
	      }

	}

	// //////////////////////////////////////////////////////////////////////////////
	public void connect(String host, final int port) {
		Log.i(TAG, "connect");
		if (_socket != null) {
			try {
				_socket.close();
			} catch (IOException e) {
				Log.e(TAG, "cannot close socket");
			}
		}
		try {
			_socket = (SSLSocket)sslFact.createSocket();
			_socket.connect(new InetSocketAddress(host, port));
			_socket.startHandshake();
			Log.d(TAG, "connected to "+host+":"+port);
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
	public void send(String data) {
		Log.i(TAG, "send data");
		OutputStream out = null;
		if(!_socket.isConnected()){
			Log.e(TAG,"socket not connected");
			return;
		}
		try {
			out = _socket.getOutputStream();
		} catch (IOException e) {
			Log.e(TAG, "cannot get outputstream");
			return;
		}
		try {
			long l=data.length();
//			data=(l>>>24)+""+(l>>>16)+""+(l>>>8)+""+l+data;
			data="XXXX"+data;//TODO write length to first 4 bytes
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
		Log.i(TAG, "reveive data");
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
		Log.i(TAG, "res: "+ret);
		return ret;
	}
}
