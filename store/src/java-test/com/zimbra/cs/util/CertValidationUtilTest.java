/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.util;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.CertPathValidatorException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

public class CertValidationUtilTest {

    private static X509Certificate rootCert;

    private static X509Certificate intermediateCert;

    private static X509Certificate leafCert;

    private static Set<TrustAnchor> trustAnchors;

    private static KeyPair intermediateKeyPair;

    private static X509Certificate intermediate1Cert;

    private static X509Certificate intermediate2Cert;

    private static X509Certificate deepLeafCert;

    private static KeyPair intermediate1KeyPair;

    private static KeyPair intermediate2KeyPair;

    @BeforeClass
    public static void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // root CA — isCA = true
        KeyPair rootKeyPair = generateKeyPair();
        rootCert = generateCert(rootKeyPair, "CN=Test Root CA",
                rootKeyPair, "CN=Test Root CA", -1, 365, true);

        // intermediate CA — isCA = true
        intermediateKeyPair = generateKeyPair();
        intermediateCert = generateCert(intermediateKeyPair,
                "CN=Test Intermediate CA",
                rootKeyPair, "CN=Test Root CA", -1, 365, true);

        // leaf cert — isCA = false
        KeyPair leafKeyPair = generateKeyPair();
        leafCert = generateCert(leafKeyPair, "CN=test@example.com",
                intermediateKeyPair, "CN=Test Intermediate CA",
                -1, 365, false);

        // Deeper chain: Root → Int1 → Int2 → DeepLeaf
        intermediate1KeyPair = generateKeyPair();
        intermediate1Cert = generateCert(intermediate1KeyPair,
                "CN=Test Intermediate CA L1",
                rootKeyPair, "CN=Test Root CA", -1, 365, true);

        intermediate2KeyPair = generateKeyPair();
        intermediate2Cert = generateCert(intermediate2KeyPair,
                "CN=Test Intermediate CA L2",
                intermediate1KeyPair, "CN=Test Intermediate CA L1",
                -1, 365, true);

        KeyPair deepLeafKeyPair = generateKeyPair();
        deepLeafCert = generateCert(deepLeafKeyPair,
                "CN=deep@example.com",
                intermediate2KeyPair, "CN=Test Intermediate CA L2",
                -1, 365, false);

        trustAnchors = new HashSet<>();
        trustAnchors.add(new TrustAnchor(rootCert, null));
    }

    // without intermediate cert, PKIX chain building fails with CertPathValidatorException
    @Test(expected = CertPathValidatorException.class)
    public void testValidateWithoutIntermediateCertFails() throws Exception {
        CertValidationUtil.validateCertificate(leafCert, true, trustAnchors, null);
    }

    // empty intermediate list behaves same as null — no NPE, no crash
    @Test
    public void testValidateWithEmptyIntermediates() throws Exception {
        CertValidationUtil.validateCertificate(leafCert, false, trustAnchors, new ArrayList<X509Certificate>());
    }

    // leaf + single intermediate — PKIX chain builds successfully
    @Test
    public void testValidateWithIntermediateCertSucceeds()
            throws Exception {
        List<X509Certificate> intermediates = new ArrayList<>();
        intermediates.add(intermediateCert);
        CertValidationUtil.validateCertificate(
                leafCert, false, trustAnchors, intermediates);
    }

    // deeper chain: leaf + two intermediate certs — PKIX chain builds
    @Test
    public void testValidateWithMultipleIntermediateCertsSucceeds()
            throws Exception {
        List<X509Certificate> intermediates = new ArrayList<>();
        intermediates.add(intermediate2Cert);
        intermediates.add(intermediate1Cert);
        CertValidationUtil.validateCertificate(
                deepLeafCert, false, trustAnchors, intermediates);
    }

    // deeper chain with missing intermediate — chain building fails
    @Test(expected = CertPathValidatorException.class)
    public void testValidateWithIncompleteChainFails()
            throws Exception {
        // only provide intermediate2, skip intermediate1
        List<X509Certificate> intermediates = new ArrayList<>();
        intermediates.add(intermediate2Cert);
        CertValidationUtil.validateCertificate(
                deepLeafCert, true, trustAnchors, intermediates);
    }

    // helper methods to generate test certificates
    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    private static X509Certificate generateCert(KeyPair subjectKP, String subjectDn, KeyPair issuerKP, String issuerDn,
            int startDaysOffset, int validDays, boolean isCA) throws Exception {
        long now = System.currentTimeMillis();
        long day = 86400000L;
        Date start = new Date(now + startDaysOffset * day);
        Date end = new Date(now + validDays * day);
        X509v3CertificateBuilder builder =
                new JcaX509v3CertificateBuilder(
                        new X500Name(issuerDn),
                        BigInteger.valueOf(
                                now + subjectDn.hashCode()),
                        start, end,
                        new X500Name(subjectDn),
                        subjectKP.getPublic());
        // add BasicConstraints: CA:TRUE for CA certs, CA:FALSE for leaf
        builder.addExtension(Extension.basicConstraints,
                true, new BasicConstraints(isCA));
        return new JcaX509CertificateConverter().setProvider("BC")
                .getCertificate(builder.build(
                        new JcaContentSignerBuilder("SHA256WithRSA")
                                .setProvider("BC")
                                .build(issuerKP.getPrivate())));
    }
}
