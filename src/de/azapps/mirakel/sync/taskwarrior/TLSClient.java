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
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.net.ssl.SSLSocket;

import org.apache.http.conn.ssl.SSLSocketFactory;

import android.util.Base64;
import de.azapps.mirakel.helper.Log;

public class TLSClient {

	private static final String TAG = "TLSClient";
	// private String _ca;
	// private gnutls_certificate_credentials_t _credentials;
	// private gnutls_session_t _session;
	private SSLSocket _socket;
	private SSLSocketFactory sslFact;
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

	@SuppressWarnings("unused")
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
	public void init(final File root, final File user) {
		Log.i(TAG, "init");
		try {
			X509Certificate ROOT = generateCertificateFromPEM(fileToBytes(root));
			X509Certificate USER = generateCertificateFromPEM(fileToBytes(user));
			KeyStore trusted = KeyStore.getInstance(KeyStore.getDefaultType());
			trusted.load(null);
			trusted.setCertificateEntry("taskwarrior-ROOT", ROOT);
			trusted.setCertificateEntry("taskwarrior-USER", USER);
			sslFact = new SSLSocketFactory(trusted);
		} catch (Exception e) {
			throw new AssertionError(e);
		}

	}

	// //////////////////////////////////////////////////////////////////////////////
	public void connect(String host, final int port) throws IOException {
		Log.i(TAG, "connect");
		if (_socket != null) {
			try {
				_socket.close();
			} catch (IOException e) {
				Log.e(TAG, "cannot close socket");
			}
		}
		try {
			_socket = (SSLSocket) sslFact.createSocket();
			// _socket.setEnabledProtocols(new String[]{"TLSv1.2"});
			_socket.setUseClientMode(true);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new IOException();
	}

	// //////////////////////////////////////////////////////////////////////////////
	// public void bye() {
	// _socket.gnutls_bye(_session, GNUTLS_SHUT_RDWR);
	// }

	// //////////////////////////////////////////////////////////////////////////////
	public void send(String data) {
		DataOutputStream dos = new DataOutputStream(out);
		Log.i(TAG, "send data");
		if (!_socket.isConnected()) {
			Log.e(TAG, "socket not connected");
			return;
		}
		try {
			// long l=data.length();
			// TODO write length to first 4 bytes
			// data=(l>>>24)+""+(l>>>16)+""+(l>>>8)+""+l+data;
			// data="XXXX"+data;
			// out.write(data.getBytes());
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
		Log.i(TAG, "reveive data to " + _socket.getLocalAddress() + ":"
				+ _socket.getLocalPort());
		if (!_socket.isConnected()) {
			Log.e(TAG, "not connected");
			return null;
		}
		try {
			byte[] header = new byte[4];

			in.read(header);
			long expected = (unsignedToBytes(header[0]) << 24)
					| (unsignedToBytes(header[1]) << 16)
					| (unsignedToBytes(header[2]) << 8)
					| unsignedToBytes(header[3]);
			// TODO remove cast
			byte[] data = new byte[(int) expected];
			in.read(data);
			return new String(data);
		} catch (IOException e) {
			Log.e(TAG, "cannot read Inputstream");
		}
		return null;
	}

	private static int unsignedToBytes(byte b) {
		return b & 0xFF;
	}
}
