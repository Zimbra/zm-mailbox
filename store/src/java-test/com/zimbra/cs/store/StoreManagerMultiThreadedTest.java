/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.store;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.localconfig.LocalConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.store.helper.ClassHelper;
import org.apache.commons.io.FileUtils;
import org.apache.mina.util.ConcurrentHashSet;
import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zimbra.common.localconfig.LC.zimbra_class_store;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class StoreManagerMultiThreadedTest {

    private static final String TEST_CONFIG_FILE_DIR_PATH = "../store/src/java-test/";
    private static final String TEST_CONFIG_FILE_PATH = TEST_CONFIG_FILE_DIR_PATH + "localconfig-test.xml";
    private static final String TEST_CONFIG_FILE_COPY_PATH = TEST_CONFIG_FILE_DIR_PATH + "localconfig-test-copy.xml";

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty("zimbra.config", TEST_CONFIG_FILE_PATH);
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.getLocalServer().setSMRuntimeSwitchEnabled(true);
        prov.getLocalServer().setSMMultiReaderEnabled(true);
    }

    @After
    public void tearDownTest() throws IOException, DocumentException, ConfigException {
        FileUtils.copyFile(new File(TEST_CONFIG_FILE_COPY_PATH), new File(TEST_CONFIG_FILE_PATH), true);
        LC.reload();
    }

    @Test
    public void testGetInstanceWithoutReset() throws InterruptedException {
        Set<StoreManager> concurrentSet = new ConcurrentHashSet<>();
        int numberOfThreads = 200;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            service.execute(() -> {
                concurrentSet.add(StoreManager.getInstance());
                latch.countDown();
            });
        }
        latch.await();
        Assert.assertEquals(1, concurrentSet.size());
    }

    @Test
    @PrepareForTest({ClassHelper.class})
    public void testResetStoreManagerWithoutChangingLCValue() throws Throwable {
        List newList = Collections.synchronizedList(new ArrayList<>());
        int numberOfThreads = 200;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        AtomicInteger threadCounter = new AtomicInteger();
        AtomicInteger resetCallCounter = new AtomicInteger();
        mockStatic(ClassHelper.class);
        when(ClassHelper.getZimbraClassInstanceBy(zimbra_class_store.value())).thenReturn(new MockStoreManager());
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            service.execute(() -> {
                newList.add(StoreManager.getInstance());
                if (threadCounter.incrementAndGet() % 19 == 0) {
                    try {
                        // calling reset store manager
                        resetCallCounter.incrementAndGet();
                        StoreManager.resetStoreManager();
                    } catch (ServiceException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                latch.countDown();
            });
        }
        latch.await();
        newList.remove(null);
        Assert.assertEquals(numberOfThreads, newList.size());
    }


    @Test
    @PrepareForTest({ClassHelper.class})
    public void testResetStoreManagerWithChangingLCValue() throws Throwable {
        Set<String> uniqueSM = new ConcurrentHashSet<>();
        List smSynchronizedList = Collections.synchronizedList(new ArrayList<>());
        int numberOfThreads = 200;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        AtomicInteger threadCounter = new AtomicInteger();
        AtomicInteger resetCallCounter = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        // add current store manager
        uniqueSM.add(StoreManager.getInstance().getClass().getName());
        for (int i = 0; i < numberOfThreads; i++) {
            service.execute(() -> {
                int count = threadCounter.incrementAndGet();
                if (threadCounter.get() % 19 == 0) {
                    try {
                        LocalConfig localConfig = new LocalConfig(null);
                        localConfig.set(zimbra_class_store.key(), StoreManagerAfterReset.class.getName());
                        localConfig.save();
                        LC.reload();
                        // calling reset store manager
                        resetCallCounter.incrementAndGet();
                        StoreManager.resetStoreManager();
                    } catch (ServiceException | IOException | DocumentException | ConfigException e) {
                        e.printStackTrace();
                    }
                }
                StoreManager storeManager = StoreManager.getInstance();
                smSynchronizedList.add(storeManager);
                uniqueSM.add(storeManager.getClass().getName());
                latch.countDown();
            });
        }
        latch.await();
        verifyStatic(Mockito.times(resetCallCounter.get()));
        // verify that it was never null instance
        smSynchronizedList.remove(null);
        Assert.assertEquals(numberOfThreads, smSynchronizedList.size());
        // only two store managers
        Assert.assertEquals(2, uniqueSM.size());
    }
}
