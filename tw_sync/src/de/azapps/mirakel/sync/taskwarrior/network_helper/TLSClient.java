/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.sync.taskwarrior.network_helper;

import android.annotation.SuppressLint;
import android.util.Base64;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.StringBufferInputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import de.azapps.tools.Log;

public class TLSClient {
    public static class NoSuchCertificateException extends Exception {
        private static final long serialVersionUID = -4606663552584336235L;

    }

    private static final String TAG = "TLSClient";

    private static List<X509Certificate> generateCertificateFromPEM(final String cert)
    throws  NoSuchCertificateException {
        if (cert == null) {
            throw new NoSuchCertificateException();
        }
        final String[] parts  = cert.split("-----END CERTIFICATE-----");
        final List<X509Certificate> certs = new ArrayList<>(parts.length);
        for (final String part : parts) {
            if (part.trim().isEmpty()) {
                continue;
            }
            try {
                certs.add((X509Certificate) CertificateFactory.getInstance("X.509")
                          .generateCertificate(new StringBufferInputStream(part.trim() + "\n-----END CERTIFICATE-----")));
            } catch (final CertificateException e) {
                Log.wtf(TAG, "parsing failed:" + part, e);
                return certs;
            }
        }
        return certs;
    }

    private static RSAPrivateKey generatePrivateKeyFromPEM(final String key)
    throws ParseException {
        final byte[] keyBytes = parseDERFromPEM(key,
                                                "-----BEGIN RSA PRIVATE KEY-----",
                                                "-----END RSA PRIVATE KEY-----");
        final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        final KeyFactory factory;
        try {
            factory = KeyFactory.getInstance("RSA");
        } catch (final NoSuchAlgorithmException e) {
            Log.e(TAG, "RSA-Algorithm not found", e);
            return null;
        }
        try {
            return (RSAPrivateKey) factory.generatePrivate(spec);
        } catch (final InvalidKeySpecException e) {
            Log.e(TAG, "cannot parse key", e);
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
        try {
            return Base64.decode(tokens[0], Base64.NO_PADDING);
        } catch (final IllegalArgumentException ignored) {
            throw new ParseException("bad base-64", 0);
        }
    }

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
            Log.e(TAG, "Cannot close Socket", e);
        } catch (final NullPointerException e) {
            Log.e(TAG,
                  "Nullpointer, means there was no established connection", e);
        }
    }

    @SuppressLint("DefaultLocale")
    // copied form davdroid, 04.11.14
    private static void setReasonableEncryption(final SSLSocket ssl) {
        // set reasonable SSL/TLS settings before the handshake:

        // - enable all supported protocols (enables TLSv1.1 and TLSv1.2 on Android <4.4.3, if available)
        // - remove all SSL versions (especially SSLv3) because they're insecure now
        final List<String> protocols = new LinkedList<>();
        for (final String protocol : ssl.getSupportedProtocols()) {
            if (!protocol.toUpperCase().contains("SSL")) {
                protocols.add(protocol);
            }
        }
        //Log.v(TAG, "Setting allowed TLS protocols: " + TextUtils.join(", ", protocols));
        ssl.setEnabledProtocols(protocols.toArray(new String[protocols.size()]));

        // choose secure cipher suites
        final List<String> allowedCiphers = Arrays.asList(
                                                // allowed secure ciphers according to NIST.SP.800-52r1.pdf Section 3.3.1 (see docs directory)
                                                // TLS 1.2
                                                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                                                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                                                "TLS_ECHDE_RSA_WITH_AES_128_GCM_SHA256",
                                                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                                                "TLS_RSA_WITH_AES_256_GCM_SHA384",
                                                "TLS_RSA_WITH_AES_128_GCM_SHA256",
                                                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",

                                                // maximum interoperability
                                                "TLS_RSA_WITH_3DES_EDE_CBC_SHA",
                                                "TLS_RSA_WITH_AES_128_CBC_SHA",
                                                // additionally
                                                "TLS_RSA_WITH_AES_256_CBC_SHA",
                                                "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
                                                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                                                "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
                                                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"
                                            );

        final List<String> availableCiphers = Arrays.asList(ssl.getSupportedCipherSuites());

        // preferred ciphers = allowed Ciphers \ availableCiphers
        final HashSet<String> preferredCiphers = new HashSet<>(allowedCiphers);
        preferredCiphers.retainAll(availableCiphers);

        // add preferred ciphers to enabled ciphers
        // for maximum security, preferred ciphers should *replace* enabled ciphers,
        // but I guess for the security level of DAVdroid, disabling of insecure
        // ciphers should be a server-side task
        final HashSet<String> enabledCiphers = new HashSet<>(Arrays.asList(
                    ssl.getEnabledCipherSuites()));
        enabledCiphers.addAll(preferredCiphers);

        //Log.v(TAG, "Setting allowed TLS ciphers: " + TextUtils.join( ", ", enabledCiphers));
        if (preferredCiphers.isEmpty()) {
            ssl.setEnabledCipherSuites(enabledCiphers.toArray(new String[enabledCiphers.size()]));
        } else {
            ssl.setEnabledCipherSuites(preferredCiphers.toArray(new String[preferredCiphers.size()]));
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    public void connect(final String host, final int port) throws IOException {
        Log.i(TAG, "connect");
        if (this._socket != null) {
            try {
                this._socket.close();
            } catch (final IOException e) {
                Log.e(TAG, "cannot close socket", e);
            }
        }
        try {
            Log.d(TAG, "connected to " + host + ':' + port);
            this._socket = (SSLSocket) this.sslFact.createSocket();
            setReasonableEncryption(this._socket);
            this._socket.setUseClientMode(true);
            this._socket.setEnableSessionCreation(true);
            this._socket.setNeedClientAuth(true);
            this._socket.setTcpNoDelay(true);
            this._socket.connect(new InetSocketAddress(host, port));
            this._socket.startHandshake();
            this.out = this._socket.getOutputStream();
            this.in = this._socket.getInputStream();
            Log.d(TAG, "connected to " + host + ':' + port);
            return;
        } catch (final UnknownHostException e) {
            Log.e(TAG, "Unknown Host", e);
        } catch (final ConnectException e) {
            Log.e(TAG, "Cannot connect to Host", e);
        } catch (final SocketException e) {
            Log.e(TAG, "IO Error", e);
        }
        throw new IOException();
    }

    // //////////////////////////////////////////////////////////////////////////////
    public void init(final String root, final String userCA,
                     final String userKey) throws ParseException, CertificateException,
        NoSuchCertificateException {
        try {

            final List<X509Certificate> ROOT = generateCertificateFromPEM(root);
            final X509Certificate USER_CERT = (X509Certificate) CertificateFactory.getInstance("X.509")
                                              .generateCertificate(new StringBufferInputStream(userCA));
            final RSAPrivateKey USER_KEY = generatePrivateKeyFromPEM(userKey);
            final KeyStore trusted = KeyStore.getInstance(KeyStore
                                     .getDefaultType());
            trusted.load(null);
            final Certificate[] chain = new Certificate[ROOT.size() + 1];
            int i = chain.length - 1;
            for (final X509Certificate cert : ROOT) {
                trusted.setCertificateEntry("taskwarrior-ROOT", cert);
                chain[i--] = cert;
            }
            trusted.setCertificateEntry("taskwarrior-USER", USER_CERT);
            chain[0] = USER_CERT;


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
            Log.w(TAG, "no matching algorithm found");
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
        Log.i(TAG, "reveive data from " + this._socket.getLocalAddress() + ':'
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
            Log.e(TAG, "cannot read Inputstream", e);
        }
        return null;
    }

    // //////////////////////////////////////////////////////////////////////////////
    public void send(final String data) {
        final DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(out));
        if (!this._socket.isConnected()) {
            Log.e(TAG, "socket not connected");
            return;
        }
        try {
            final byte[] utf8 = data.getBytes("UTF-8");
            dos.writeInt(utf8.length);
            dos.write(utf8);
        } catch (final IOException e) {
            Log.e(TAG, "cannot write data to outputstream", e);
        }
        try {
            dos.flush();
            this.out.flush();
        } catch (final IOException e) {
            Log.e(TAG, "cannot flush data to outputstream", e);
        }
    }
}
