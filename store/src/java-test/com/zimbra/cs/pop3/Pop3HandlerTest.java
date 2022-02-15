package com.zimbra.cs.pop3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.server.ServerThrottle;
import com.zimbra.cs.util.ZTestWatchman;
import com.zimbra.qa.unittest.TestUtil;

import junit.framework.Assert;


public class Pop3HandlerTest {
    private static final String LOCAL_USER = "localpoptest@zimbra.com";

    @Rule public TestName testName = new TestName();
    @Rule public MethodRule watchman = new ZTestWatchman();

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        String[] hosts = {"localhost", "127.0.0.1"};
        ServerThrottle.configureThrottle(new Pop3Config(false).getProtocol(), 100, 100, Arrays.asList(hosts), Arrays.asList(hosts));
    }

    @Before
    public void setUp() throws Exception {
        System.out.println(testName.getMethodName());
        Provisioning prov = Provisioning.getInstance();
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, "12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
        prov.createAccount(LOCAL_USER, "secret", attrs);
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testLogin3() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
        Pop3Handler handler = new MockPop3Handler();

        acct.setPop3Enabled(true);
        acct.setPrefPop3Enabled(true);
        handler.authenticate(LOCAL_USER, null, "secret", null);
        Assert.assertEquals(Pop3Handler.STATE_TRANSACTION, handler.state);
    }

    @Test(expected = Pop3CmdException.class)
    public void testLogin4() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
        Pop3Handler handler = new MockPop3Handler();

        acct.setPop3Enabled(true);
        acct.setPrefPop3Enabled(false);
        handler.authenticate(LOCAL_USER, null, "secret", null);
    }

    @Test(expected = Pop3CmdException.class)
    public void testLogin7() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
        Pop3Handler handler = new MockPop3Handler();

        acct.setPop3Enabled(false);
        acct.setPrefPop3Enabled(true);
        handler.authenticate(LOCAL_USER, null, "secret", null);
    }

    @Test(expected = Pop3CmdException.class)
    public void testLogin8() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
        Pop3Handler handler = new MockPop3Handler();

        acct.setPop3Enabled(false);
        acct.setPrefPop3Enabled(false);
        handler.authenticate(LOCAL_USER, null, "secret", null);
    }
}

