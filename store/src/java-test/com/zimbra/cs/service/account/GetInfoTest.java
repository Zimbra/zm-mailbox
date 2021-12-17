package com.zimbra.cs.service.account;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class GetInfoTest {

    private static Server server = null;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MockProvisioning prov = new MockProvisioning();
        Provisioning.setInstance(prov);
        server = Provisioning.getInstance().getLocalServer();
    }

    /**
     * spellcheck should return true when zimbraSpellCheckURL on the current server returns non-empty list of urls.
     */
    @Test
    public void trueWhenZimbraSpellCheckURLReturnsNonEmptyList() {
        String[] spellCheckUrls = {"http://zcs.example.one:7780/aspell.php", " http://zcs.example.two:7780/aspell.php", "http://zcs.example.three:7780/aspell.php"};
        Map<String, Object> map = new HashMap<>();
        map.put("zimbraSpellCheckURL", spellCheckUrls);
        server.setAttrs(map);
        GetInfo getInfo = new GetInfo();
        boolean value = getInfo.isSpellCheckServiceAvailable(server);
        Assert.assertEquals(true, value);
    }

    /**
     * spellcheck should return false when zimbraSpellCheckURL on the current server returns an empty list of urls.
     */
    @Test
    public void falseWhenZimbraSpellCheckURLReturnsEmptyList() {
        String[] spellCheckUrls = new String[]{};
        Map<String, Object> map = new HashMap<>();
        map.put("zimbraSpellCheckURL", spellCheckUrls);
        server.setAttrs(map);
        GetInfo getInfo = new GetInfo();
        boolean value = getInfo.isSpellCheckServiceAvailable(server);
        Assert.assertEquals(false, value);
    }

    /**
     * spellcheck should return false when zimbraSpellCheckURL on the current server returns an empty list of urls.
     */
    @Test
    public void falseWhenZimbraSpellCheckURLReturnsNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("zimbraSpellCheckURL", null);
        server.setAttrs(map);
        GetInfo getInfo = new GetInfo();
        boolean value = getInfo.isSpellCheckServiceAvailable(server);
        Assert.assertEquals(false, value);
    }

    /**
     * spellcheck should return true when zimbraSpellCheckURL on the current server returns non-empty list of urls.
     * zimbraServiceInstalled and zimbraServiceEnabled shouldn't impact the return value of this method even "spell" service is not available on current server.
     */
    @Test
    public void trueWhenZimbraSpellCheckURLReturnsNonEmptyListWhenEnabled() {
        String[] spellCheckUrls = {"http://zcs.example.one:7780/aspell.php", " http://zcs.example.two:7780/aspell.php", "http://zcs.example.three:7780/aspell.php"};
        String[] servicesEnabled = { "amavis", "antivirus", "opendkim", "stats", "mailbox", "proxy"};
        String[] servicesInstalled = {"amavis", "antivirus", "opendkim", "stats", "mailbox", "proxy"};
        Map<String, Object> map = new HashMap<>();
        map.put("zimbraSpellCheckURL", spellCheckUrls);
        map.put("zimbraServiceEnabled", servicesEnabled);
        map.put("zimbraServiceInstalled", servicesInstalled);
        server.setAttrs(map);
        GetInfo getInfo = new GetInfo();
        boolean value = getInfo.isSpellCheckServiceAvailable(server);
        Assert.assertEquals(true, value);
    }

    /**
     * spellcheck should return false when zimbraSpellCheckURL on the current server returns with empty list of urls.
     * zimbraServiceInstalled and zimbraServiceEnabled shouldn't impact the return value of this method even "spell" service is available on current server.
     */
    @Test
    public void falseWhenZimbraSpellCheckURLReturnsEmptyListWhenEnabled() {
        String[] spellCheckUrls = {};
        String[] servicesEnabled = {"spell", "amavis", "antivirus", "opendkim", "stats", "mailbox", "proxy"};
        String[] servicesInstalled = {"spell", "amavis", "antivirus", "opendkim", "stats", "mailbox", "proxy"};
        Map<String, Object> map = new HashMap<>();
        map.put("zimbraSpellCheckURL", spellCheckUrls);
        map.put("zimbraServiceEnabled", servicesEnabled);
        map.put("zimbraServiceInstalled", servicesInstalled);
        server.setAttrs(map);
        GetInfo getInfo = new GetInfo();
        boolean value = getInfo.isSpellCheckServiceAvailable(server);
        Assert.assertEquals(false, value);
    }
}
