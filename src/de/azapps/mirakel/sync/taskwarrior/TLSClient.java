package de.azapps.mirakel.sync.taskwarrior;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Scanner;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import android.os.Build;
import android.util.Base64;
import de.azapps.mirakel.helper.Log;

public class TLSClient {

	private static final String TAG = "TLSClient";
	// private String _ca;
	// private gnutls_certificate_credentials_t _credentials;
	// private gnutls_session_t _session;
	private SSLSocket _socket;
	private javax.net.ssl.SSLSocketFactory sslFact;
	private InputStream in;
	private OutputStream out;

	static void gnutls_log_function(int level, String message) {
		Log.d(TAG, "c: " + message);
	}

	// //////////////////////////////////////////////////////////////////////////////
	public TLSClient() {
		_socket = null;
		sslFact = null;
		in = null;
		out = null;
	}

	// //////////////////////////////////////////////////////////////////////////////
	@Override
	protected void finalize() {
		close();
	}

	public void close() {
		if (_socket == null) {
			Log.e(TAG, "socket null");
			return;
		}
		try {
			out.flush();
			in.close();
			out.close();
			_socket.close();
			_socket = null;
		} catch (IOException e) {
			Log.e(TAG, "Cannot close Socket");
		} catch (NullPointerException e) {
			Log.e(TAG, "Nullpointer, means there was no established connection");
		}

	}

	private static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter,
			String endDelimiter) {
		String data = new String(pem);
		String[] tokens = data.split(beginDelimiter);
		tokens = tokens[1].split(endDelimiter);
		return Base64.decode(tokens[0], Base64.NO_PADDING);
	}

	private static RSAPrivateKey generatePrivateKeyFromPEM(byte[] keyBytes) {
		keyBytes = parseDERFromPEM(keyBytes, "-----BEGIN RSA PRIVATE KEY-----",
				"-----END RSA PRIVATE KEY-----");
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

		KeyFactory factory;
		try {
			factory = KeyFactory.getInstance("RSA", "BC");
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "RSA-Algorithm not found");
			return null;
		} catch (NoSuchProviderException e) {
			Log.e(TAG, "BC not found");
			return null;
		}

		try {
			return (RSAPrivateKey) factory.generatePrivate(spec);
		} catch (InvalidKeySpecException e) {
			Log.e(TAG, "cannot parse key");
			Log.e(TAG, Log.getStackTraceString(e));
			return null;
		}
	}

	private static X509Certificate generateCertificateFromPEM(byte[] certBytes) {
		certBytes = parseDERFromPEM(certBytes, "-----BEGIN CERTIFICATE-----",
				"-----END CERTIFICATE-----");
		CertificateFactory factory;
		try {
			factory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			return null;
		}

		try {
			return (X509Certificate) factory
					.generateCertificate(new ByteArrayInputStream(certBytes));
		} catch (CertificateException e) {
			return null;
		}
	}

	private byte[] fileToBytes(final File f) {
		int size = (int) f.length();
		byte[] bytes = new byte[size];
		BufferedInputStream buf;
		try {
			buf = new BufferedInputStream(new FileInputStream(f));
		} catch (FileNotFoundException e1) {
			Log.e(TAG, "cannot get BufferedInputStream");
			return bytes;
		}
		try {
			buf.read(bytes, 0, bytes.length);
		} catch (IOException e) {
			Log.e(TAG, "cannot read bytes from file");
		} finally {
			try {
				buf.close();
			} catch (IOException e) {
				Log.e(TAG, "cannot close BufferedInputStream");
			}
		}
		return bytes;
	}

	// //////////////////////////////////////////////////////////////////////////////
	public void init(final File root, final File user, final File user_key) {
		Log.i(TAG, "init");
		try {
			X509Certificate ROOT = generateCertificateFromPEM(fileToBytes(root));
			X509Certificate USER_CERT = generateCertificateFromPEM(fileToBytes(user));
			RSAPrivateKey USER_KEY = generatePrivateKeyFromPEM(fileToBytes(user_key));
			KeyStore trusted = KeyStore.getInstance(KeyStore.getDefaultType());
			trusted.load(null);
			trusted.setCertificateEntry("taskwarrior-ROOT", ROOT);
			trusted.setCertificateEntry("taskwarrior-USER", USER_CERT);
			Certificate[] chain = { USER_CERT, ROOT };
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			// Hack to get it working on android 2.2
			String pwd = "secret";
			trusted.setEntry("user", new KeyStore.PrivateKeyEntry(USER_KEY,
					chain), new KeyStore.PasswordProtection(pwd.toCharArray()));

			keyManagerFactory.init(trusted, pwd.toCharArray());

			SSLContext context = SSLContext.getInstance("TLS");
			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trusted);
			TrustManager[] trustManagers = tmf.getTrustManagers();
			context.init(keyManagerFactory.getKeyManagers(), trustManagers,
					new SecureRandom());
			sslFact = context.getSocketFactory();
		} catch (Exception e) {
			throw new AssertionError(e);
		}

	}

	// //////////////////////////////////////////////////////////////////////////////
	public void connect(String host, int port) throws IOException {
		Log.i(TAG, "connect");
		if (_socket != null) {
			try {
				_socket.close();
			} catch (IOException e) {
				Log.e(TAG, "cannot close socket");
			}
		}
		try {
			Log.d(TAG, "connected to " + host + ":" + port);
			_socket = (SSLSocket) sslFact.createSocket();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
				_socket.setEnabledProtocols(new String[] { "TLSv1.2", "TLSv1.1" });
			_socket.setUseClientMode(true);
			_socket.setEnableSessionCreation(true);
			_socket.setNeedClientAuth(true);
			_socket.setTcpNoDelay(true);
			_socket.connect(new InetSocketAddress(host, port));
			_socket.startHandshake();
			out = _socket.getOutputStream();
			in = _socket.getInputStream();
			Log.d(TAG, "connected to " + host + ":" + port);
			return;
		} catch (UnknownHostException e) {
			Log.e(TAG, "Unkown Host");
		} catch (ConnectException e) {
			Log.e(TAG, "Cannot connect to Host");

		} catch (IOException e) {
			Log.e(TAG, "IO Error");
		}
		throw new IOException();
	}

	// //////////////////////////////////////////////////////////////////////////////
	public void send(String data) {
		DataOutputStream dos = new DataOutputStream(out);
		Log.i(TAG, "send data");
		if (!_socket.isConnected()) {
			Log.e(TAG, "socket not connected");
			return;
		}
		try {
			dos.writeInt(data.getBytes().length);
			dos.write(data.getBytes());
		} catch (IOException e) {
			Log.e(TAG, "cannot write data to outputstream");
		}
		try {
			dos.flush();
			dos.close();
			out.flush();
		} catch (IOException e) {
			Log.e(TAG, "cannot flush data to outputstream");
		}
	}

	// //////////////////////////////////////////////////////////////////////////////
	public String recv() {
		Log.i(TAG, "reveive data from " + _socket.getLocalAddress() + ":"
				+ _socket.getLocalPort());
		if (!_socket.isConnected()) {
			Log.e(TAG, "not connected");
			return null;
		}
		try {
			byte[] header = new byte[4];
			in.read(header);
			Scanner scanner = new Scanner(in);
			Scanner s = scanner.useDelimiter("\\A");
			String result = s.hasNext() ? s.next() : "";
			scanner.close();
			return result;
		} catch (IOException e) {
			Log.e(TAG, "cannot read Inputstream");
		}
		return null;
	}

	@SuppressWarnings("unused")
	private static int unsignedToBytes(byte b) {
		return b & 0xFF;
	}
}
