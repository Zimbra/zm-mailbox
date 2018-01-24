package com.zimbra.cs.ldap.unboundid;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.zimbra.common.mailbox.ContactConstants;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Entry.class})
public class UBIDUserCertificateAttributeTest {
    private String lookupAttr = ContactConstants.A_userCertificate + ";binary";
    private String certBase64 = "MIIEfzCCA2egAwIBAgIDEAACMA0GCSqGSIb3DQEBCwUAMH8xDzANBgNVBAoTBlppbWJyYTElMCMGCSqGSIb3DQEJARYWYWRtaW5AZXhjaGFuZ2UyMDEwLmxhYjESMBAGA1UEBxMJUGFsbyBBbHRvMQswCQYDVQQIEwJDQTELMAkGA1UEBhMCVVMxFzAVBgNVBAMTDnRlc3RDb21tb25OYW1lMB4XDTE3MDUwODA3MzYzMFoXDTE4MDUwODA3MzYzMFowgYAxCzAJBgNVBAYTAlVTMQwwCgYDVQQIDANOU1cxDzANBgNVBAoMBlppbWJyYTEXMBUGA1UEAwwOdGVzdENvbW1vbk5hbWUxKDAmBgkqhkiG9w0BCQEWGXZnMUB6bWV4Y2guZW5nLnZtd2FyZS5jb20xDzANBgNVBAsMBlppbWJyYTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKeGcXq5GqfQzVMemx9ADdUfq6wlD0G9zuBQVpUvl0sI/rwWxD4Yh9uSyCb/XItMUPVMSYVCvEvW6Xopxa/+Ecj4ExRDy9eH6rub/G8umvatwx5Zx/OyzQotCJ66hEXwCOp72ye/yX+Y6RCuMhNSeNRMcdxVFc0VFRtaEtwHo9qJ9UQu/SOr9VDm8ZMUOAIBlNEeCSFJ8L6XQnlKqOWouWpsmWe80emf+Bljf+G1LOJt92Nx5Mj6ky3PWu6BWnPtTYhO1LFtIQiFY9hKZ5yLnnfn5w/QokRMluCKXuFCXFCD2mNmJD31k+zTjwwYY0BiyYTjOeYwQAsecbBQfH+kXaMCAwEAAaOCAQAwgf0wCQYDVR0TBAIwADALBgNVHQ8EBAMCBeAwJAYDVR0RBB0wG4EZdmcxQHptZXhjaC5lbmcudm13YXJlLmNvbTAdBgNVHQ4EFgQUm0eV1WZ/4A5dfvGsmRdWjVt0FCMwgZ0GA1UdIwSBlTCBkqGBhKSBgTB/MQ8wDQYDVQQKEwZaaW1icmExJTAjBgkqhkiG9w0BCQEWFmFkbWluQGV4Y2hhbmdlMjAxMC5sYWIxEjAQBgNVBAcTCVBhbG8gQWx0bzELMAkGA1UECBMCQ0ExCzAJBgNVBAYTAlVTMRcwFQYDVQQDEw50ZXN0Q29tbW9uTmFtZYIJAIP73sPIjZijMA0GCSqGSIb3DQEBCwUAA4IBAQAA0iiImrDPPND5SgJOBl/IVfrgePzztMXDkvS9mi6fIMlC7B1nmxPVm2Uch5SLN8IShKj7H4hpUjtqZBp09qF+dv/Nb8Z5WtFNUsJ81Jf2fMSl3fQbS3t7bm93XCkNt10C42ruyTLKHZWBuoANX64XUU4BNz0slas/91sAq6G3zMA3xqs2yFCc+hcwfeGijtJDxgFKZPBU8VeKnU/K1eI5suirZ3WWmT4LtyVUz1nLKoyWOB17Y1S5SN6K/hrjs+vrICHA8MPyxtvLI1TiRanzv5itOTWvN76sJ3LbKPCEtGtNnK30jUn56TlgMYunJL77hWOveXwu24b2EYR5Yso5";

    @Test
    public void getMultiAttrStringShouldReturnCertificateForAttributeNameWithBinary() {
        Attribute attr = new Attribute(lookupAttr, certBase64);
        Entry entry = PowerMockito.mock(Entry.class);
        UBIDAttributes attributes = new UBIDAttributes(entry);
        Mockito.when(entry.getAttribute(ContactConstants.A_userCertificate)).thenReturn(null);//entry does not contain "userCertificate" attribute
        Mockito.when(entry.getAttribute(lookupAttr)).thenReturn(attr);//entry contains "userCertificate;binary" attribute
        try {
            assertEquals(attributes.getMultiAttrString(lookupAttr, false)[0], certBase64);
        } catch (com.zimbra.cs.ldap.LdapException e) {
            fail("Exception thrown");
        }
    }

    @Test
    public void getMultiAttrStringShouldReturnCertificateForAttributeNameWithoutBinary() {
        Attribute attr = new Attribute(ContactConstants.A_userCertificate, certBase64);
        Entry entry = PowerMockito.mock(Entry.class);
        UBIDAttributes attributes = new UBIDAttributes(entry);
        Mockito.when(entry.getAttribute(lookupAttr)).thenReturn(null);//entry does not contain "userCertificate;binary" attribute
        Mockito.when(entry.getAttribute(ContactConstants.A_userCertificate)).thenReturn(attr);//entry contains "userCertificate" attribute
        try {
            assertEquals(attributes.getMultiAttrString(lookupAttr, false)[0], certBase64);
        } catch (com.zimbra.cs.ldap.LdapException e) {
            fail("Exception thrown");
        }
    }
}