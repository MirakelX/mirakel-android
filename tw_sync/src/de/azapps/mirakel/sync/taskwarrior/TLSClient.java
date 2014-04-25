package de.azapps.mirakel.sync.taskwarrior;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
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
	public static class NoSuchCertificateException extends Exception {
		private static final long serialVersionUID = -4606663552584336235L;

	}

	private static final String TAG = "TLSClient";

	private static X509Certificate generateCertificateFromPEM(final String cert)
			throws ParseException, NoSuchCertificateException {
		if (cert == null) {
			throw new NoSuchCertificateException();
		}
		final byte[] certBytes = parseDERFromPEM(cert,
				"-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
		CertificateFactory factory;
		try {
			factory = CertificateFactory.getInstance("X.509");
		} catch (final CertificateException e) {
			return null;
		}

		try {
			return (X509Certificate) factory
					.generateCertificate(new ByteArrayInputStream(certBytes));
		} catch (final CertificateException e) {
			Log.wtf(TAG, "parsing failed");
			return null;
		}
	}

	private static RSAPrivateKey generatePrivateKeyFromPEM(final String key)
			throws ParseException {
		final byte[] keyBytes = parseDERFromPEM(key,
				"-----BEGIN RSA PRIVATE KEY-----",
				"-----END RSA PRIVATE KEY-----");
		final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory factory;
		try {
			factory = KeyFactory.getInstance("RSA", "BC");
		} catch (final NoSuchAlgorithmException e) {
			Log.e(TAG, "RSA-Algorithm not found");
			return null;
		} catch (final NoSuchProviderException e) {
			Log.e(TAG, "BC not found");
			return null;
		}

		try {
			return (RSAPrivateKey) factory.generatePrivate(spec);
		} catch (final InvalidKeySpecException e) {
			Log.e(TAG, "cannot parse key");
			Log.e(TAG, Log.getStackTraceString(e));
			return null;
		}
	}

	private static byte[] parseDERFromPEM(final String pem,
			final String beginDelimiter, final String endDelimiter)
			throws ParseException {
		String[] tokens = pem.split(beginDelimiter);
		if (tokens.length < 2) {
			throw new ParseException("Wrong PEM format", 0);
		}
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
		} catch (final IOException e) {
			Log.e(TAG, "Cannot close Socket");
		} catch (final NullPointerException e) {
			Log.e(TAG, "Nullpointer, means there was no established connection");
		}

	}

	// //////////////////////////////////////////////////////////////////////////////
	public void connect(final String host, final int port) throws IOException {
		Log.i(TAG, "connect");
		if (this._socket != null) {
			try {
				this._socket.close();
			} catch (final IOException e) {
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
		} catch (final UnknownHostException e) {
			Log.e(TAG, "Unkown Host");
		} catch (final ConnectException e) {
			Log.e(TAG, "Cannot connect to Host");
		} catch (final IOException e) {
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
	public void init(final String root, final String user_ca,
			final String user_key) throws ParseException, CertificateException,
			NoSuchCertificateException {
		Log.i(TAG, "init");
		try {
			final X509Certificate ROOT = generateCertificateFromPEM(root);
			final X509Certificate USER_CERT = generateCertificateFromPEM(user_ca);
			final RSAPrivateKey USER_KEY = generatePrivateKeyFromPEM(user_key);
			final KeyStore trusted = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trusted.load(null);
			trusted.setCertificateEntry("taskwarrior-ROOT", ROOT);
			trusted.setCertificateEntry("taskwarrior-USER", USER_CERT);
			final Certificate[] chain = { USER_CERT, ROOT };
			final KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			// Hack to get it working on android 2.2
			final String pwd = "secret";
			trusted.setEntry("user", new KeyStore.PrivateKeyEntry(USER_KEY,
					chain), new KeyStore.PasswordProtection(pwd.toCharArray()));

			keyManagerFactory.init(trusted, pwd.toCharArray());

			final SSLContext context = SSLContext.getInstance("TLS");
			final TrustManagerFactory tmf = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trusted);
			final TrustManager[] trustManagers = tmf.getTrustManagers();
			context.init(keyManagerFactory.getKeyManagers(), trustManagers,
					new SecureRandom());
			this.sslFact = context.getSocketFactory();
		} catch (final UnrecoverableKeyException e) {
			Log.w(TAG, "cannot restore key");
			throw new CertificateException(e);
		} catch (final KeyManagementException e) {
			Log.w(TAG, "cannot access key");
			throw new CertificateException(e);
		} catch (final KeyStoreException e) {
			Log.w(TAG, "cannot handle keystore");
			throw new CertificateException(e);
		} catch (final NoSuchAlgorithmException e) {
			Log.w(TAG, "no matching algorithm founr");
			throw new CertificateException(e);
		} catch (final CertificateException e) {
			Log.w(TAG, "certificat not readable");
			throw new CertificateException(e);
		} catch (final IOException e) {
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
			final byte[] header = new byte[4];
			this.in.read(header);
			final Scanner scanner = new Scanner(this.in);
			final Scanner s = scanner.useDelimiter("\\A");
			final String result = s.hasNext() ? s.next() : "";
			s.close();
			scanner.close();
			return result;
		} catch (final IOException e) {
			Log.e(TAG, "cannot read Inputstream");
		}
		return null;
	}

	// //////////////////////////////////////////////////////////////////////////////
	public void send(final String data) {
		final DataOutputStream dos = new DataOutputStream(this.out);
		Log.i(TAG, "send data");
		if (!this._socket.isConnected()) {
			Log.e(TAG, "socket not connected");
			return;
		}
		try {
			dos.writeInt(data.getBytes().length);
			dos.write(data.getBytes());
		} catch (final IOException e) {
			Log.e(TAG, "cannot write data to outputstream");
		}
		try {
			dos.flush();
			dos.close();
			this.out.flush();
		} catch (final IOException e) {
			Log.e(TAG, "cannot flush data to outputstream");
		}
	}
}
