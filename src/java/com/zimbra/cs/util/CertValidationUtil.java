/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;

public class CertValidationUtil {

    public static void validateCertificate(X509Certificate cert, boolean revocationCheckEnabled, Set<TrustAnchor> trustedCertsSet)
            throws CertificateException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertPathValidatorException {

            cert.checkValidity();

            if(revocationCheckEnabled) {
                List<X509Certificate> certificates = new ArrayList<X509Certificate>();
                certificates.add(cert);
                CertificateFactory cf;
                CertPath cp;
                cf = CertificateFactory.getInstance("X509");
                cp = cf.generateCertPath(certificates);

                // init PKIX parameters
                PKIXParameters params;
                params = new PKIXParameters(trustedCertsSet);
                params.setRevocationEnabled(revocationCheckEnabled);

                // perform validation
                CertPathValidator cpv;
                cpv = CertPathValidator.getInstance("PKIX");
                PKIXCertPathValidatorResult cpv_result = (PKIXCertPathValidatorResult) cpv.validate(cp, params);
                ZimbraLog.account.debug("Certificate Validation Result %s", cpv_result.toString());
            }
    }

    public static Set<TrustAnchor> loadTrustedAnchors(char[] pass, String keystorePath) throws KeyStoreException,
           NoSuchAlgorithmException, CertificateException, IOException {

        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(keystorePath);
            ks.load(fis, pass);
        } finally {
            ByteUtil.closeStream(fis);
        }

        Set<TrustAnchor> trustedCertsSet = new HashSet<TrustAnchor>();
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            X509Certificate rootCACert = (X509Certificate)ks.getCertificate(alias);
            TrustAnchor ta = new TrustAnchor(rootCACert, null);
            trustedCertsSet.add(ta);
            ZimbraLog.account.debug("adding certificate with issuer DN: %s , signature name: %s", rootCACert.getIssuerDN().toString(), rootCACert.getSigAlgName());
        }
        return trustedCertsSet;
    }

    public static String getSubjectDN(X509Certificate cert) {
        String subjectDn = null;
        Principal principal = cert.getSubjectDN();
        if (principal != null) {
            subjectDn = principal.getName();
        }

        if (subjectDn == null) {
            subjectDn = "";
        }

        return subjectDn;
    }
}
