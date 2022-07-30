package com.zimbra.cs.store;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.store.helper.ClassHelper;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class CacheEnabledStoreManagerProviderTest {
    private static final String TEST_CONFIG_FILE_DIR_PATH = "../store/src/java-test/";
    private static final String TEST_CONFIG_FILE_PATH = TEST_CONFIG_FILE_DIR_PATH + "localconfig-test.xml";

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty("zimbra.config", TEST_CONFIG_FILE_PATH);
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.getLocalServer().setSMRuntimeSwitchEnabled(true);
        prov.getLocalServer().setSMMultiReaderEnabled(true);
    }

    @Test
    @PrepareForTest({ClassHelper.class})
    public void testGetStoreManagerForVolumeWithCacheSkip() throws Throwable {
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

        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume3.getId(), true);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume3.getId(), true);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume3.getId(), true);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume3.getId(), true);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume3.getId(), true);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume4.getId(), true);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume4.getId(), true);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume4.getId(), true);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume4.getId(), true);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume4.getId(), true);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume4.getId(), true);

        // delete volumes
        VolumeManager.getInstance().delete(volume3.getId());
        VolumeManager.getInstance().delete(volume4.getId());
        // make sure only twice it was called
        verifyStatic(Mockito.times(11));
    }

    @Test
    @PrepareForTest({ClassHelper.class})
    public void testGetStoreManagerForVolumeWithoutCacheSkip() throws Throwable {
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

        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume3.getId(), false);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume3.getId(), false);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume3.getId(), false);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume3.getId(), false);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume3.getId(), false);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume4.getId(), false);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume4.getId(), false);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume4.getId(), false);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume4.getId(), false);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume4.getId(), false);
        CacheEnabledStoreManagerProvider.getStoreManagerForVolume(volume4.getId(), false);

        // delete volumes
        VolumeManager.getInstance().delete(volume3.getId());
        VolumeManager.getInstance().delete(volume4.getId());
        // make sure only twice it was called
        verifyStatic(Mockito.times(2));
    }
}