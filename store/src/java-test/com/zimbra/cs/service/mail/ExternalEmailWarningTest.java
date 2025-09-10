/*
 *
 *  * ***** BEGIN LICENSE BLOCK *****
 *  * Zimbra Collaboration Suite, Network Edition.
 *  * Copyright (C) 2025 Synacor, Inc.  All Rights Reserved.
 *  * ***** END LICENSE BLOCK *****
 *
 */

package com.zimbra.cs.service.mail;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.lmtpserver.ExternalEmailWarning;
import com.zimbra.cs.lmtpserver.LmtpAddress;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class ExternalEmailWarningTest extends TestCase{

    Provisioning prov = Provisioning.getInstance();
    List<LmtpAddress> recipients = new ArrayList<>();
    ExternalEmailWarning externalEmailWarning = ExternalEmailWarning.getInstance();
    String rcptEmail1;
    String rcptEmail2;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    protected void setUp() throws Exception {
        String recipient1 = "<\"test1.\"@domain1.com>";
        String recipient2 = "<\"test2.\"@domain2.com>";
        recipients.add(new LmtpAddress(recipient1, null, null));
        recipients.add(new LmtpAddress(recipient2, null, null));
        Map<String, Object> attrs1 = new HashMap<String, Object>();
        attrs1.put(Provisioning.A_zimbraFeatureExternalEmailWarningEnabled, "TRUE");
        rcptEmail1 = recipients.get(0).getEmailAddress();
        Map<String, Object> attrs2 = new HashMap<String, Object>();
        attrs2.put(Provisioning.A_zimbraFeatureExternalEmailWarningEnabled, "FALSE");
        rcptEmail2 = recipients.get(1).getEmailAddress();
        if (prov.getDomainByName("domain1.com") == null) {
            prov.createDomain("domain1.com", attrs1);
        }
        if (prov.getDomainByName("domain2.com") == null) {
            prov.createDomain("domain2.com", attrs2);
        }
        prov.createAccount(rcptEmail1, "test123", new HashMap<>());
        prov.createAccount(rcptEmail2, "test123", new HashMap<>());
        externalEmailWarning.findRecipientsWithEEWEnabled(recipients);
    }

    public void testIsEnabled() throws ServiceException {
        Account account1 = prov.get(Key.AccountBy.name, rcptEmail1);
        Assert.assertTrue(externalEmailWarning.isEnabled(account1));
        Account account2 = prov.get(Key.AccountBy.name, rcptEmail2);
        Assert.assertFalse(externalEmailWarning.isEnabled(account2));
    }

    public void testZimbraExternalEmailWarningMessage() throws ServiceException {
        Account account1 = prov.get(Key.AccountBy.name, rcptEmail1);
        Domain domain1 = prov.getDomain(account1);
        Assert.assertEquals("This message originated outside of your organization.", domain1.getExternalEmailWarningMessage());
    }

    public void testGetUpdatedContentWithBase64PlainText() throws Exception {
        Account account1 = prov.get(Key.AccountBy.name, rcptEmail1);

        // base warning configured on the domain
        Domain domain1 = prov.getDomain(account1);
        domain1.setExternalEmailWarningMessage("This message originated outside of your organization.");

        // original plain text body
        String originalBody = "Hello, this is a test email.";
        String base64Body = java.util.Base64.getMimeEncoder(76, "\r\n".getBytes())
                .encodeToString(originalBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // RFC822-compliant MIME message (simplified)
        String content =
                "Content-Type: text/plain; charset=\"UTF-8\"\r\n" +
                        "Content-Transfer-Encoding: base64\r\n" +
                        "\r\n" +
                        base64Body + "\r\n";

        // call the method under test
        String updatedContent = externalEmailWarning.getUpdatedContent(content, account1);

        // extract the re-encoded Base64 body
        String encodedPart = updatedContent.substring(updatedContent.indexOf("\r\n\r\n") + 4).trim();
        String decodedBody = new String(
                java.util.Base64.getMimeDecoder().decode(encodedPart),
                java.nio.charset.StandardCharsets.UTF_8
        );

        // assert the warning comes before the original message
        Assert.assertTrue("Updated body should contain warning",
                decodedBody.startsWith("This message originated outside of your organization."));

        Assert.assertTrue("Updated body should still contain original text",
                decodedBody.contains(originalBody));
    }

    public void testGetUpdatedContentWithBase64Html() throws Exception {
        Account account1 = prov.get(Key.AccountBy.name, rcptEmail1);

        // base warning configured on the domain
        Domain domain1 = prov.getDomain(account1);
        domain1.setExternalEmailWarningMessage("This message originated outside of your organization.");

        // original HTML body
        String originalHtml = "<html><body><p>Hello HTML world</p></body></html>";
        String base64Body = java.util.Base64.getMimeEncoder(76, "\r\n".getBytes())
                .encodeToString(originalHtml.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // RFC822-compliant MIME message
        String content =
                "Content-Type: text/html; charset=\"UTF-8\"\r\n" +
                        "Content-Transfer-Encoding: base64\r\n" +
                        "\r\n" +
                        base64Body + "\r\n";

        // call the method under test
        String updatedContent = externalEmailWarning.getUpdatedContent(content, account1);

        // extract the re-encoded Base64 body
        String encodedPart = updatedContent.substring(updatedContent.indexOf("\r\n\r\n") + 4).trim();
        String decodedBody = new String(
                java.util.Base64.getMimeDecoder().decode(encodedPart),
                java.nio.charset.StandardCharsets.UTF_8
        );

        // assert warning was inserted into HTML
        Assert.assertTrue("Updated HTML should contain warning div",
                decodedBody.contains("<div") && decodedBody.contains("This message originated outside of your organization."));

        Assert.assertTrue("Updated HTML should still contain original HTML text",
                decodedBody.contains("Hello HTML world"));
    }

}
