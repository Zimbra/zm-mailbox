package com.zimbra.cs.store;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.localconfig.LocalConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.store.helper.ClassHelper;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import org.apache.commons.io.FileUtils;
import org.apache.mina.util.ConcurrentHashSet;
import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zimbra.common.localconfig.LC.zimbra_class_store;
import static org.powermock.api.mockito.PowerMockito.*;

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
        prov.getLocalServer().setMultiReaderSMEnabled(true);
    }

    @After
    public void tearDownTest() throws IOException, DocumentException, ConfigException {
        FileUtils.copyFile(new File(TEST_CONFIG_FILE_COPY_PATH), new File(TEST_CONFIG_FILE_PATH), true);
        LC.reload();
        StoreManager.flushVolumeStoreManagerCache();
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
                        //calling reset store manager
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
                if ( threadCounter.get() % 19 == 0) {
                    try {
                        LocalConfig localConfig = new LocalConfig(null);
                        localConfig.set(zimbra_class_store.key(), StoreManagerAfterReset.class.getName());
                        localConfig.save();
                        LC.reload();
                        //calling reset store manager
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

    @Test
    @PrepareForTest({ClassHelper.class})
    public void testGetStoreManagerForVolumeCacheEnabled() throws Throwable {
        Volume volume3 = Volume.builder().setId((short) 200)
                .setName("TEST300")
                .setPath("/test/300", false)
                .setType((short) 1)
                .setStoreType(Volume.StoreType.INTERNAL)
                .setStoreManagerClass(MockStoreManager.class.getName()).build();
        Volume volume4 = Volume.builder().setId((short) 201)
                .setName("TEST400")
                .setPath("/test/400", false)
                .setType((short) 1)
                .setStoreType(Volume.StoreType.EXTERNAL)
                .setStoreManagerClass(MockStoreManager.class.getName()).build();

        VolumeManager.getInstance().create(volume3);
        VolumeManager.getInstance().create(volume4);

        mockStatic(ClassHelper.class);
        when(ClassHelper.getZimbraClassInstanceBy(Mockito.anyString())).thenReturn(new MockStoreManager());

        StoreManager.getStoreManagerForVolume(volume3.getId(), true);
        StoreManager.getStoreManagerForVolume(volume3.getId(), true);
        StoreManager.getStoreManagerForVolume(volume3.getId(), true);
        StoreManager.getStoreManagerForVolume(volume3.getId(), true);
        StoreManager.getStoreManagerForVolume(volume3.getId(), true);
        StoreManager.getStoreManagerForVolume(volume4.getId(), true);
        StoreManager.getStoreManagerForVolume(volume4.getId(), true);
        StoreManager.getStoreManagerForVolume(volume4.getId(), true);
        StoreManager.getStoreManagerForVolume(volume4.getId(), true);
        StoreManager.getStoreManagerForVolume(volume4.getId(), true);
        StoreManager.getStoreManagerForVolume(volume4.getId(), true);

        // delete volumes
        VolumeManager.getInstance().delete(volume3.getId());
        VolumeManager.getInstance().delete(volume4.getId());
        // make sure only twice it was called
        verifyStatic(Mockito.times(2));
    }

    @Test
    @PrepareForTest({ClassHelper.class})
    public void testGetStoreManagerForVolumeCacheDisabled() throws Throwable {
        Volume volume3 = Volume.builder().setId((short) 200)
                .setName("TEST300")
                .setPath("/test/300", false)
                .setType((short) 1)
                .setStoreType(Volume.StoreType.INTERNAL)
                .setStoreManagerClass(MockStoreManager.class.getName()).build();
        Volume volume4 = Volume.builder().setId((short) 201)
                .setName("TEST400")
                .setPath("/test/400", false)
                .setType((short) 1)
                .setStoreType(Volume.StoreType.EXTERNAL)
                .setStoreManagerClass(MockStoreManager.class.getName()).build();

        VolumeManager.getInstance().create(volume3);
        VolumeManager.getInstance().create(volume4);

        mockStatic(ClassHelper.class);
        when(ClassHelper.getZimbraClassInstanceBy(Mockito.anyString())).thenReturn(new MockStoreManager());

        StoreManager.getStoreManagerForVolume(volume3.getId(), true);
        StoreManager.getStoreManagerForVolume(volume3.getId(), true);
        StoreManager.getStoreManagerForVolume(volume3.getId(), true);
        StoreManager.getStoreManagerForVolume(volume3.getId(), true);
        StoreManager.getStoreManagerForVolume(volume3.getId(), true);
        StoreManager.getStoreManagerForVolume(volume4.getId(), true);
        StoreManager.getStoreManagerForVolume(volume4.getId(), true);
        StoreManager.getStoreManagerForVolume(volume4.getId(), true);
        StoreManager.getStoreManagerForVolume(volume4.getId(), true);
        StoreManager.getStoreManagerForVolume(volume4.getId(), true);
        StoreManager.getStoreManagerForVolume(volume4.getId(), true);

        // delete volumes
        VolumeManager.getInstance().delete(volume3.getId());
        VolumeManager.getInstance().delete(volume4.getId());
        // make sure only twice it was called
        verifyStatic(Mockito.times(11));
    }
}
