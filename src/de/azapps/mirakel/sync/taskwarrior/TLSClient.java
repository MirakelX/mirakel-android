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
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.util.Scanner;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import android.os.Build;
import android.util.Base64;
import de.azapps.tools.Log;

public class TLSClient {

	private static final String TAG = "TLSClient";

	private static byte[] fileToBytes(final File f) {
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

	private static X509Certificate generateCertificateFromPEM(byte[] certBytes)
			throws ParseException {
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

	private static RSAPrivateKey generatePrivateKeyFromPEM(byte[] keyBytes)
			throws ParseException {
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

	static void gnutls_log_function(String message) {
		Log.d(TAG, "c: " + message);
	}

	private static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter,
			String endDelimiter) throws ParseException {
		String data = new String(pem);
		String[] tokens = data.split(beginDelimiter);
		if (tokens.length < 2)
			throw new ParseException("Wrong PEM format", 0);
		tokens = tokens[1].split(endDelimiter);
		return Base64.decode(tokens[0], Base64.NO_PADDING);
	}

	// private String _ca;
	// private gnutls_certificate_credentials_t _credentials;
	// private gnutls_session_t _session;
	private SSLSocket _socket;

	private InputStream in;

	private OutputStream out;

	private javax.net.ssl.SSLSocketFactory sslFact;

	// //////////////////////////////////////////////////////////////////////////////
	public TLSClient() {
		this._socket = null;
		this.sslFact = null;
		this.in = null;
		this.out = null;
	}

	public void close() {
		if (this._socket == null) {
			Log.e(TAG, "socket null");
			return;
		}
		try {
			this.out.flush();
			this.in.close();
			this.out.close();
			this._socket.close();
			this._socket = null;
		} catch (IOException e) {
			Log.e(TAG, "Cannot close Socket");
		} catch (NullPointerException e) {
			Log.e(TAG, "Nullpointer, means there was no established connection");
		}

	}

	// //////////////////////////////////////////////////////////////////////////////
	public void connect(String host, int port) throws IOException {
		Log.i(TAG, "connect");
		if (this._socket != null) {
			try {
				this._socket.close();
			} catch (IOException e) {
				Log.e(TAG, "cannot close socket");
			}
		}
		try {
			Log.d(TAG, "connected to " + host + ":" + port);
			this._socket = (SSLSocket) this.sslFact.createSocket();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				this._socket.setEnabledProtocols(new String[] { "TLSv1.2",
						"TLSv1.1" });
			}
			this._socket.setUseClientMode(true);
			this._socket.setEnableSessionCreation(true);
			this._socket.setNeedClientAuth(true);
			this._socket.setTcpNoDelay(true);
			this._socket.connect(new InetSocketAddress(host, port));
			this._socket.startHandshake();
			this.out = this._socket.getOutputStream();
			this.in = this._socket.getInputStream();
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
	@Override
	protected void finalize() {
		close();
	}

	// //////////////////////////////////////////////////////////////////////////////
	public void init(final File root, final File user, final File user_key)
			throws ParseException, CertificateException {
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
			this.sslFact = context.getSocketFactory();
		} catch (UnrecoverableKeyException e) {
			Log.w(TAG, "cannot restore key");
			throw new CertificateException(e);
		} catch (KeyManagementException e) {
			Log.w(TAG, "cannot access key");
			throw new CertificateException(e);
		} catch (KeyStoreException e) {
			Log.w(TAG, "cannot handle keystore");
			throw new CertificateException(e);
		} catch (NoSuchAlgorithmException e) {
			Log.w(TAG, "no matching algorithm founr");
			throw new CertificateException(e);
		} catch (CertificateException e) {
			Log.w(TAG, "certificat not readable");
			throw new CertificateException(e);
		} catch (IOException e) {
			Log.w(TAG, "general io problem");
			throw new CertificateException(e);
		}

	}

	// //////////////////////////////////////////////////////////////////////////////
	public String recv() {
		Log.i(TAG, "reveive data from " + this._socket.getLocalAddress() + ":"
				+ this._socket.getLocalPort());
		if (!this._socket.isConnected()) {
			Log.e(TAG, "not connected");
			return null;
		}
		try {
			byte[] header = new byte[4];
			this.in.read(header);
			Scanner scanner = new Scanner(this.in);
			Scanner s = scanner.useDelimiter("\\A");
			String result = s.hasNext() ? s.next() : "";
			s.close();
			scanner.close();
			return result;
		} catch (IOException e) {
			Log.e(TAG, "cannot read Inputstream");
		}
		return null;
	}

	// //////////////////////////////////////////////////////////////////////////////
	public void send(String data) {
		DataOutputStream dos = new DataOutputStream(this.out);
		Log.i(TAG, "send data");
		if (!this._socket.isConnected()) {
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
			this.out.flush();
		} catch (IOException e) {
			Log.e(TAG, "cannot flush data to outputstream");
		}
	}
}
