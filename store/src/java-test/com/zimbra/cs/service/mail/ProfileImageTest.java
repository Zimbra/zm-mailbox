package com.zimbra.cs.service.mail;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Maps;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.account.GetInfo;
import com.zimbra.cs.util.ZTestWatchman;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.GetInfoRequest;
import com.zimbra.soap.account.message.GetInfoResponse;
import com.zimbra.soap.mail.message.SaveProfileImageRequest;
import com.zimbra.soap.mail.message.SaveProfileImageResponse;

import junit.framework.Assert;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MimeDetect.class)
@PowerMockIgnore({ "javax.crypto.*", "javax.xml.bind.annotation.*" })
public class ProfileImageTest {

    @Rule
    public TestName testName = new TestName();
    @Rule
    public MethodRule watchman = new ZTestWatchman();

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println(testName.getMethodName());
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = Maps.newHashMap();
        prov.createDomain("zimbra.com", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("testZCS3545@zimbra.com", "secret", attrs);

        // this MailboxManager does everything except actually send mail
        MailboxManager.setInstance(new MailboxManager() {

            @Override
            protected Mailbox instantiateMailbox(MailboxData data) {
                return new Mailbox(data) {

                    @Override
                    public MailSender getMailSender() {
                        return new MailSender() {

                            @Override
                            protected Collection<Address> sendMessage(Mailbox mbox, MimeMessage mm,
                                Collection<RollbackData> rollbacks) {
                                try {
                                    return Arrays.asList(getRecipients(mm));
                                } catch (Exception e) {
                                    return Collections.emptyList();
                                }
                            }
                        };
                    }
                };
            }
        });
    }

    @Test
    public void testZCS3545() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("testZCS3545@zimbra.com");
        InputStream is = getClass().getResourceAsStream("img.jpg");
        FileUploadServlet.Upload up = FileUploadServlet.saveUpload(is, "img.jpg", "image/jpeg",
            acct.getId());
        PowerMockito.stub(PowerMockito.method(MimeDetect.class, "detect", InputStream.class))
            .toReturn("image/jpeg");
        SaveProfileImageRequest requestJaxb = new SaveProfileImageRequest();
        requestJaxb.setUploadId(up.getId());
        Element request = JaxbUtil.jaxbToElement(requestJaxb);
        Element response = new SaveProfileImage().handle(request,
            ServiceTestUtil.getRequestContext(acct));
        SaveProfileImageResponse responseJaxb = JaxbUtil.elementToJaxb(response);
        int profileItemId = responseJaxb.getItemId();
        GetInfoRequest getRequestJaxb = new GetInfoRequest();
        Element getRequest = JaxbUtil.jaxbToElement(getRequestJaxb);
        Element getResponse = new GetInfo().handle(getRequest,
            ServiceTestUtil.getRequestContext(acct));
        GetInfoResponse getResponseJaxb = JaxbUtil.elementToJaxb(getResponse);
        Assert.assertEquals(profileItemId, getResponseJaxb.getProfileImageId());
    }

    @After
    public void tearDown() {
        try {
            MailboxTestUtil.clearData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
