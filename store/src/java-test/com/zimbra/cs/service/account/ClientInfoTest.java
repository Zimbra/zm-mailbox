package com.zimbra.cs.service.account;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.common.soap.Element.XMLElement;

public class ClientInfoTest {

    protected static final String KEY_SKIP_LOGOFF = "zimbraWebClientSkipLogoff";

    protected ClientInfo toTest = new ClientInfo();

    protected Element parent;

    @Before
    public void setup() throws Exception {
        parent =  new XMLElement(AccountConstants.CLIENT_INFO_RESPONSE);
    }

    @Test
    public void testEncodeAttrSkipLogoffSingleLogoffURL() throws Exception {
        String logoutURL = "https://localhost/service/extension/samllogout";

        // test single known logoff url
        toTest.encodeAttrSkipLogoff(parent, logoutURL, new String[] { logoutURL });

        // verify skip logoff is true since the webclient logout url is a known logoff handler
        List<KeyValuePair> pairs = parent.listKeyValuePairs(AccountConstants.E_ATTR, AccountConstants.E_NAME);
        KeyValuePair pair = pairs.stream()
            .filter(p -> KEY_SKIP_LOGOFF.equals(p.getKey()))
            .findFirst().get();
        assertTrue(Boolean.valueOf(pair.getValue()));
    }

    @Test
    public void testEncodeAttrSkipLogoffMultipleLogoffURLs() throws Exception {
        String url = "https://localhost/service/extension/samllogout";

        // test multiple known logoff urls
        toTest.encodeAttrSkipLogoff(parent, url, new String[] {
            "https://localhost/service/extension/saml2slo",
            url
        });

        // verify skip logoff is true since the webclient logout url is a known logoff handler
        List<KeyValuePair> pairs = parent.listKeyValuePairs(AccountConstants.E_ATTR, AccountConstants.E_NAME);
        KeyValuePair pair = pairs.stream()
            .filter(p -> KEY_SKIP_LOGOFF.equals(p.getKey()))
            .findFirst().get();
        assertTrue(Boolean.valueOf(pair.getValue()));
    }

    @Test
    public void testEncodeAttrSkipLogoffMultipleLogoffURLsNotInUse() throws Exception {
        String url = "https://localhost/service/extension/samllogout";

        // test multiple known logoff urls
        toTest.encodeAttrSkipLogoff(parent, url, new String[] {
            "https://localhost/service/extension/saml2slo",
            "https://localhost/service/extension/extra",
            "https://localhost/service/extension/example"
        });

        // verify skip logoff is false when logout url is not a known logoff handler
        List<KeyValuePair> pairs = parent.listKeyValuePairs(AccountConstants.E_ATTR, AccountConstants.E_NAME);
        KeyValuePair pair = pairs.stream()
            .filter(p -> KEY_SKIP_LOGOFF.equals(p.getKey()))
            .findFirst().get();
        assertFalse(Boolean.valueOf(pair.getValue()));
    }

    @Test
    public void testEncodeAttrSkipLogoffNoLogoffURLsNone() throws Exception {
        String url = "https://localhost/service/extension/samllogout";

        // test null known logoff urls
        toTest.encodeAttrSkipLogoff(parent, url, new String[] {});

        // verify skip logoff is false when logout url is not a known logoff handler
        List<KeyValuePair> pairs = parent.listKeyValuePairs(AccountConstants.E_ATTR, AccountConstants.E_NAME);
        KeyValuePair pair = pairs.stream()
            .filter(p -> KEY_SKIP_LOGOFF.equals(p.getKey()))
            .findFirst().get();
        assertFalse(Boolean.valueOf(pair.getValue()));
    }

    @Test
    public void testEncodeAttrSkipLogoffNoLogoffURLsNull() throws Exception {
        String url = "https://localhost/service/extension/samllogout";

        // test null known logoff urls
        toTest.encodeAttrSkipLogoff(parent, url, null);

        // verify skip logoff is false when logout url is not a known logoff handler
        List<KeyValuePair> pairs = parent.listKeyValuePairs(AccountConstants.E_ATTR, AccountConstants.E_NAME);
        KeyValuePair pair = pairs.stream()
            .filter(p -> KEY_SKIP_LOGOFF.equals(p.getKey()))
            .findFirst().get();
        assertFalse(Boolean.valueOf(pair.getValue()));
    }

    @Test
    public void testEncodeAttrSkipLogoffNoLogoffURLsEmpty() throws Exception {
        String url = "https://localhost/service/extension/samllogout";

        // test empty known logoff urls
        toTest.encodeAttrSkipLogoff(parent, url, new String[]{ "" });

        // verify skip logoff is false when logout url is not a known logoff handler
        List<KeyValuePair> pairs = parent.listKeyValuePairs(AccountConstants.E_ATTR, AccountConstants.E_NAME);
        KeyValuePair pair = pairs.stream()
            .filter(p -> KEY_SKIP_LOGOFF.equals(p.getKey()))
            .findFirst().get();
        assertFalse(Boolean.valueOf(pair.getValue()));
    }
}
