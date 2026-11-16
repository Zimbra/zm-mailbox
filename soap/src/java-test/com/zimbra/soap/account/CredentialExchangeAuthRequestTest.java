package com.zimbra.soap.account;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.CredentialExchangeAuthRequest;
import com.zimbra.soap.type.AccountBy;
import com.zimbra.soap.type.AccountSelector;
import org.junit.Assert;
import org.junit.Test;

public class CredentialExchangeAuthRequestTest {
    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    private static final String CREDENTIAL = "zcx1." + VERIFIER;

    private static final String ACCOUNT = "alice@example.com";

    private static CredentialExchangeAuthRequest sample(String virtualHost) {
        return new CredentialExchangeAuthRequest().setCredential(CREDENTIAL).setVerifier(VERIFIER)
                .setAccount(new AccountSelector(AccountBy.name, ACCOUNT)).setVirtualHost(virtualHost);
    }

    @Test
    public void marshalsUnderTheNewName() throws Exception {
        final Element el = JaxbUtil.jaxbToElement(sample(null));
        Assert.assertEquals(AccountConstants.CREDENTIAL_EXCHANGE_AUTH_REQUEST, el.getQName());
        Assert.assertEquals(CREDENTIAL, el.getElement(AccountConstants.E_CREDENTIAL).getText());
        Assert.assertEquals(VERIFIER, el.getElement(AccountConstants.E_VERIFIER).getText());
        final Element account = el.getElement(AccountConstants.E_ACCOUNT);
        Assert.assertEquals("name", account.getAttribute(AccountConstants.A_BY));
        Assert.assertEquals(ACCOUNT, account.getText());
        Assert.assertNull(el.getOptionalElement(AccountConstants.E_VIRTUAL_HOST));
    }

    @Test
    public void roundTrips() throws Exception {
        final CredentialExchangeAuthRequest back =
                JaxbUtil.elementToJaxb(JaxbUtil.jaxbToElement(sample("mail.example.com")));
        Assert.assertEquals(CREDENTIAL, back.getCredential());
        Assert.assertEquals(VERIFIER, back.getVerifier());
        Assert.assertEquals(AccountBy.name, back.getAccount().getBy());
        Assert.assertEquals(ACCOUNT, back.getAccount().getKey());
        Assert.assertEquals("mail.example.com", back.getVirtualHost());
    }

    @Test
    public void parsesTheClientWireFormat() throws Exception {
        final Element el = Element.parseXML("<CredentialExchangeAuthRequest xmlns=\"urn:zimbraAccount\">"
                + "<credential>" + CREDENTIAL + "</credential>"
                + "<verifier>" + VERIFIER + "</verifier>"
                + "<account by=\"name\">" + ACCOUNT + "</account>"
                + "</CredentialExchangeAuthRequest>");
        final CredentialExchangeAuthRequest req = JaxbUtil.elementToJaxb(el);
        Assert.assertNotNull("JaxbUtil knows the class", req);
        Assert.assertEquals(CREDENTIAL, req.getCredential());
        Assert.assertEquals(ACCOUNT, req.getAccount().getKey());
        Assert.assertNull(req.getVirtualHost());
    }
}
