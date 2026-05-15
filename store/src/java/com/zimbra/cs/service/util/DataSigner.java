/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;

public class DataSigner {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static byte[] signData(byte[] data, byte[] digiCert, char[] expPass)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException, OperatorCreationException, CMSException {
        byte[] signedData = null;
        try (InputStream targetStream = new ByteArrayInputStream(digiCert)) {
            KeyStore pkcs12Store = KeyStore.getInstance("PKCS12");
            pkcs12Store.load(targetStream, expPass);
            String alias = null;
            for (Enumeration<String> en = pkcs12Store.aliases(); en.hasMoreElements();) {
                alias = (String) en.nextElement();
            }
            Certificate[] chain = pkcs12Store.getCertificateChain(alias);
            X509Certificate[] x509Chain = (chain != null && chain.length > 0)
                    ? Arrays.copyOf(chain, chain.length, X509Certificate[].class)
                    : new X509Certificate[]{(X509Certificate) pkcs12Store.getCertificate(alias)};
            PrivateKey privKey = (PrivateKey) pkcs12Store.getKey(alias, expPass);
            signedData = signData(data, x509Chain, privKey);
        }
        return signedData;
    }

    public static byte[] signData(byte[] data, X509Certificate[] certChain, PrivateKey signingKey)
            throws CertificateEncodingException, OperatorCreationException, CMSException, IOException {
        CMSTypedData cmsData = new CMSProcessableByteArray(data);
        List<X509Certificate> certList = new ArrayList<>(Arrays.asList(certChain));
        Store certs = new JcaCertStore(certList);
        CMSSignedDataGenerator cmsGenerator = new CMSSignedDataGenerator();
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(signingKey);
        cmsGenerator.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                        .build(contentSigner, certChain[0]));
        cmsGenerator.addCertificates(certs);
        CMSSignedData cms = cmsGenerator.generate(cmsData, true);
        return cms.getEncoded();
    }

    public static byte[] signData(byte[] data, X509Certificate signingCertificate, PrivateKey signingKey)
            throws CertificateEncodingException, OperatorCreationException, CMSException, IOException {
        return signData(data, new X509Certificate[]{signingCertificate}, signingKey);
    }

}
