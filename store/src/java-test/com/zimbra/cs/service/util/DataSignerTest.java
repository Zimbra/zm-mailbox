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
 */package com.zimbra.cs.service.util;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Iterator;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.util.Store;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.zimbra.cs.mailbox.MailboxTestUtil;

import junit.framework.Assert;

public class DataSignerTest {

    @Test
    public void testSignData() {
        try {
            String serverdir = MailboxTestUtil.getZimbraServerDir("");
            FileInputStream p12Stream = new FileInputStream(serverdir + "data/unittest/certificate/sign1_digitalid.p12");
            char[] expPass = "test123export".toCharArray();
            byte[] certBytes = ByteStreams.toByteArray(p12Stream);
            byte[] signedData = DataSigner.signData("hello world".getBytes(), certBytes, expPass);
            // validate signed data
            ByteArrayInputStream inputStream = new ByteArrayInputStream(signedData);
            try (ASN1InputStream asnInputStream = new ASN1InputStream(inputStream)) {
                CMSSignedData cmsSignedData = new CMSSignedData(ContentInfo.getInstance(asnInputStream.readObject()));
                Store certs = cmsSignedData.getCertificates();
                SignerInformationStore signers = cmsSignedData.getSignerInfos();
                Collection<SignerInformation> c = signers.getSigners();
                Iterator<SignerInformation> it = c.iterator();
                SignerInformation signer = it.next();
                Collection<X509CertificateHolder> certCollection = certs.getMatches(signer.getSID());
                X509CertificateHolder certHolder = certCollection.iterator().next();
                boolean verify = signer.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certHolder));
                Assert.assertTrue(verify);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("data sign test failed");
        }
    }
}
