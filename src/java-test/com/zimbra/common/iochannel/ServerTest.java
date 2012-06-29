package com.zimbra.common.iochannel;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.iochannel.Server.NotifyCallback;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.LogFactory;

public class ServerTest {

    private static class CountDownCallback implements NotifyCallback {
        private final CountDownLatch latch;
        public CountDownCallback(CountDownLatch latch) {
            this.latch = latch;
        }
        @Override
        public void dataReceived(String header, ByteBuffer buf) {
            latch.countDown();
        }
    }

    private static class TestConfig extends Config {
        private int port;
        private static final int mainPort = 7185;
        private static final int alternatePort = 7187;

        private enum C { main, alternate };

        public TestConfig(C conf) {
            switch (conf) {
            case main:
                port = mainPort;
                break;
            case alternate:
                port = alternatePort;
                break;
            }
        }
        @Override
        public ServerConfig getLocalConfig() {
            return new ServerConfig("localhost", "localhost", port);
        }

        @Override
        public Collection<ServerConfig> getPeerServers() {
            ArrayList<ServerConfig> peers = new ArrayList<ServerConfig>();
            peers.add(new ServerConfig("self", "localhost", port));
            peers.add(new ServerConfig("copy2", "localhost", port));
            peers.add(new ServerConfig("peer1", "localhost", mainPort));
            peers.add(new ServerConfig("peer2", "localhost", alternatePort));
            return peers;
        }
    }

    private static Server s1;
    private static Server s2;
    private static Client c;

    @BeforeClass public static void setup() throws Exception {
        LogFactory.init();
        LogFactory.getLog("iochannel").setLevel(Level.debug);
        s1 = Server.start(new TestConfig(TestConfig.C.main));
        s2 = Server.start(new TestConfig(TestConfig.C.alternate));
        c = Client.start(new TestConfig(TestConfig.C.main));
        c.setWaitInterval(1000);
    }

    @AfterClass public static void shutdown() throws Exception {
        c.shutdown();
        s1.shutdown();
        s2.shutdown();
    }

    private static final String[] messages = {
        "The World Bank chose Korean-American physician Jim Yong Kim as its next chief Monday in a decision that surprised few despite the first-ever challenge to the US lock on the Bank's presidency.",
        "The 2012 Pulitzer Prize winners and nominated finalists were announced on Monday. The New York Times won a pair of Pulitzers--for explanatory writing (David Kocieniewski's series on tax loopholes for the wealthy) and international reporting (Jeffrey Gettleman's 'vivid reports, often at personal peril, on famine and conflict in East Africa')--while the Associated Press was recognized for an investigative series--by Matt Apuzzo, Adam Goldman, Chris Hawley and Eileen Sullivan--outlining the New York Police Departments surveillance of minority and Muslim neighborhoods since the 9/11 terror attacks.",
        "Clean-up efforts were underway across the Midwest after dozens of tornadoes tore through the region over the weekend of Apr. 14, killing five people in Oklahoma and nearly destroying one small Iowa town.",
        "A Michigan woman who continued to get food stamps after winning a lottery jackpot was arraigned Tuesday on welfare fraud charges. Amanda Clayton, 25, of Lincoln Park was arrested on Monday. If convicted of the two felony charges, she could face up to four years in prison.",
    };

    @Test public void testConfig() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        s1.registerCallback(new CountDownCallback(latch));
        s2.registerCallback(new CountDownCallback(latch));
        Exception e = null;
        try {
            c.getPeer("bogus").sendMessage(messages[0]);
            Assert.fail("bogus config");
        } catch (IOChannelException ex) {
            e = ex;
        }
        Assert.assertNotNull(e);
    }

    @Test public void testOne() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        s1.registerCallback(new CountDownCallback(latch));
        c.getPeer("self").sendMessage(messages[0]);
        latch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
    }

    @Test public void testMultiClient() throws Exception {
        CountDownLatch latch = new CountDownLatch(6);
        s1.registerCallback(new CountDownCallback(latch));
        c.getPeer("self").sendMessage(messages[0]);
        c.getPeer("copy2").sendMessage(messages[0]);
        c.getPeer("self").sendMessage(messages[1]);
        c.getPeer("copy2").sendMessage(messages[1]);
        c.getPeer("self").sendMessage(messages[2]);
        c.getPeer("copy2").sendMessage(messages[2]);
        latch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
    }

    @Test public void testMultiServer() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(2);
        CountDownLatch latch2 = new CountDownLatch(2);
        s1.registerCallback(new CountDownCallback(latch1));
        s2.registerCallback(new CountDownCallback(latch2));
        c.getPeer("peer1").sendMessage(messages[0]);
        c.getPeer("peer2").sendMessage(messages[1]);
        c.getPeer("peer1").sendMessage(messages[2]);
        c.getPeer("peer2").sendMessage(messages[3]);
        latch1.await(2, TimeUnit.SECONDS);
        latch2.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch1.getCount());
        Assert.assertEquals(0, latch2.getCount());
    }

    @Test public void testConnectionRefused() throws Exception {
        s1.shutdown();
        Thread.sleep(1000);
        c.getPeer("self").sendMessage(messages[0]);
        Thread.sleep(1000);
        CountDownLatch latch = new CountDownLatch(1);
        s1 = Server.start(new TestConfig(TestConfig.C.main));
        s1.registerCallback(new CountDownCallback(latch));
        latch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
    }

    @Test public void testServerDown() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        s1.registerCallback(new CountDownCallback(latch));
        c.getPeer("self").sendMessage(messages[0]);
        latch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
        s1.shutdown();
        Thread.sleep(1000);
        c.getPeer("self").sendMessage(messages[1]);
        Thread.sleep(1000);
        latch = new CountDownLatch(1);
        s1 = Server.start(new TestConfig(TestConfig.C.main));
        s1.registerCallback(new CountDownCallback(latch));
        latch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
    }

    @Test public void testOneServerDown() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(2);
        CountDownLatch latch2 = new CountDownLatch(2);
        s1.registerCallback(new CountDownCallback(latch1));
        s2.registerCallback(new CountDownCallback(latch2));
        c.getPeer("peer1").sendMessage(messages[0]);
        c.getPeer("peer2").sendMessage(messages[1]);
        Thread.sleep(1000);
        s1.shutdown();
        Thread.sleep(1000);
        c.getPeer("peer1").sendMessage(messages[2]);
        c.getPeer("peer2").sendMessage(messages[3]);
        Thread.sleep(1000);
        s1 = Server.start(new TestConfig(TestConfig.C.main));
        s1.registerCallback(new CountDownCallback(latch1));
        latch1.await(2, TimeUnit.SECONDS);
        latch2.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch1.getCount());
        Assert.assertEquals(0, latch2.getCount());
    }

    @Test public void testLargeMessage() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        s1.registerCallback(new CountDownCallback(latch));
        String msg = messages[1];
        while (msg.length() < Packet.maxMessageSize) {
            msg += msg;
        }
        c.getPeer("self").sendMessage(msg);
        Thread.sleep(2000);
        c.getPeer("self").sendMessage(messages[0]);
        latch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
    }
}
