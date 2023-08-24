package com.zimbra.cs.service.util;

import static org.mockito.Mockito.when;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.helper.ClassHelper;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.cs.volume.VolumeServiceException;
import com.zimbra.soap.admin.message.GetAllVolumesInplaceUpgradeResponse;
import com.zimbra.soap.admin.message.GetAllVolumesResponse;
import com.zimbra.soap.admin.message.ModifyVolumeInplaceUpgradeRequest;
import com.zimbra.soap.admin.message.ModifyVolumeRequest;
import com.zimbra.soap.admin.type.VolumeExternalInfo;
import com.zimbra.soap.admin.type.VolumeInfo;
import com.zimbra.util.ExternalVolumeInfoHandler;
import junit.framework.Assert;

@RunWith(PowerMockRunner.class)
@PrepareForTest({VolumeConfigUtil.class})
@PowerMockIgnore("javax.management.*")
public class VolumeConfigUtilTest {

    private MockProvisioning mockProvisioning;
    private ExternalVolumeInfoHandler externalVolumeInfoHandler = null;
    private String serverId = null;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        mockProvisioning = new MockProvisioning();
        Provisioning.setInstance(mockProvisioning);
        externalVolumeInfoHandler = new ExternalVolumeInfoHandler(mockProvisioning);
        serverId = mockProvisioning.getLocalServer().getId();
    }

    private VolumeInfo mockVolumeInfo(short id, String name) {
        VolumeInfo volInfo = new VolumeInfo();
        volInfo.setId(id);
        volInfo.setName(name);
        volInfo.setRootPath("/junit/");
        volInfo.setType((short) 1);
        volInfo.setStoreType((short) 2);

        VolumeExternalInfo volumeExternalInfo = new VolumeExternalInfo();
        volumeExternalInfo.setGlobalBucketConfigurationId("GLOBAL_BUCKET_ID");
        volumeExternalInfo.setVolumePrefix("/JunitPrefix");
        volumeExternalInfo.setUnified(true);
        volumeExternalInfo.setStorageType(AdminConstants.A_VOLUME_S3);

        volInfo.setVolumeExternalInfo(volumeExternalInfo);
        return volInfo;
    }

    private VolumeInfo mockNonUnifiedExternalVolumeInfo(short id, String name) {
        VolumeInfo volInfo = new VolumeInfo();
        volInfo.setId(id);
        volInfo.setName(name);
        volInfo.setRootPath("/junit/");
        volInfo.setType((short) 1);
        volInfo.setStoreType((short) 2);

        VolumeExternalInfo volumeExternalInfo = new VolumeExternalInfo();
        volumeExternalInfo.setGlobalBucketConfigurationId("GLOBAL_BUCKET_ID");
        volumeExternalInfo.setVolumePrefix("/JunitPrefix");
        volumeExternalInfo.setUnified(false);
        volumeExternalInfo.setStorageType(AdminConstants.A_VOLUME_S3);

        volInfo.setVolumeExternalInfo(volumeExternalInfo);
        return volInfo;
    }

    /**
     * It throws VolumeServiceException because there is no volume JSON data available for external volume.
     * 
     * @throws ServiceException
     * @throws JSONException
     */
    @Test(expected = VolumeServiceException.class)
    public void testParseGetAllVolumesRequest() throws ServiceException, JSONException {
        short volumeId = 10;
        String name = "volume10";
        Volume volume = Volume.builder().setId(volumeId)
                .setName(name)
                .setPath("/test/10", false)
                .setType((short) 1)
                .setStoreType(Volume.StoreType.EXTERNAL)
                .setStoreManagerClass(MockStoreManager.class.getName()).build();

        VolumeManager.getInstance().create(volume);
        JSONObject properties = new JSONObject();
        properties.put(AdminConstants.A_VOLUME_STORAGE_TYPE, AdminConstants.A_VOLUME_S3);
        properties = externalVolumeInfoHandler.readServerProperties(volumeId);
        
        GetAllVolumesResponse getAllVolumesResponse = new GetAllVolumesResponse();
        VolumeConfigUtil.parseGetAllVolumesRequest(null, getAllVolumesResponse);
        VolumeManager.getInstance().delete(volume.getId());
    }

    /**
     * It returns internal and external volumes data even there is no details available in
     * volume JSON.
     * 
     * @throws ServiceException
     * @throws JSONException
     */
    @Test
    public void testParseGetAllVolumesInplaceUpgradeRequest() throws ServiceException, JSONException {
        short volumeId = 20;
        String name = "volume20";
        Volume volume = Volume.builder().setId(volumeId)
                .setName(name)
                .setPath("/test/20", false)
                .setType((short) 1)
                .setStoreType(Volume.StoreType.EXTERNAL)
                .setStoreManagerClass(MockStoreManager.class.getName()).build();

        VolumeManager.getInstance().create(volume);
        JSONObject properties = new JSONObject();
        properties.put(AdminConstants.A_VOLUME_STORAGE_TYPE, AdminConstants.A_VOLUME_S3);
        properties = externalVolumeInfoHandler.readServerProperties(volumeId);
        GetAllVolumesInplaceUpgradeResponse getAllVolumesInplaceUpgradeResponse = new GetAllVolumesInplaceUpgradeResponse();
        VolumeConfigUtil.parseGetAllVolumesInplaceUpgradeRequest(null, getAllVolumesInplaceUpgradeResponse);
        
        VolumeManager.getInstance().delete(volume.getId());
    }

    /**
     * It throws VolumeServiceException because there is no volume JSON data available for external volume.
     * 
     * @throws ServiceException
     * @throws JSONException
     */
    @Test(expected = VolumeServiceException.class)
    public void testParseModifyVolumeRequest() throws ServiceException, JSONException {
        short volumeId = 30;
        String name = "volume30";
        Volume volume = Volume.builder().setId(volumeId)
                .setName(name)
                .setPath("/test/30", false)
                .setType((short) 1)
                .setStoreType(Volume.StoreType.EXTERNAL)
                .setStoreManagerClass(MockStoreManager.class.getName()).build();

        VolumeManager.getInstance().create(volume);
        VolumeInfo volumeInfo = mockVolumeInfo(volumeId, name);
        JSONObject properties = new JSONObject();
        properties.put(AdminConstants.A_VOLUME_STORAGE_TYPE, AdminConstants.A_VOLUME_S3);
        properties = externalVolumeInfoHandler.readServerProperties(volumeId);
        
        ModifyVolumeRequest modifyVolumeRequest = Mockito.mock(ModifyVolumeRequest.class);
        when(modifyVolumeRequest.getVolumeInfo()).thenReturn(volumeInfo);
        when(modifyVolumeRequest.getId()).thenReturn(volumeId);
        properties = externalVolumeInfoHandler.readServerProperties(volumeId);
        VolumeConfigUtil.parseModifyVolumeRequest(modifyVolumeRequest);
        
        VolumeManager.getInstance().delete(volume.getId());
    }

    /**
     * It modify external volume data even there is no details available in
     * volume JSON. It creates volume JSON before modify if not available.
     * 
     * @throws ServiceException
     * @throws JSONException
     */
    @Test
    @PrepareForTest({ClassHelper.class})
    public void testParseModifyVolumeInplaceUpgradeRequest() throws ServiceException, JSONException {
        short volumeId = 40;
        String name = "volume40";
        Volume volume = Volume.builder().setId(volumeId)
                .setName(name)
                .setPath("/test/40", false)
                .setType((short) 2)
                .setStoreType(Volume.StoreType.EXTERNAL)
                .setStoreManagerClass(MockStoreManager.class.getName()).build();

        VolumeManager.getInstance().create(volume);
        VolumeInfo volumeInfo = mockVolumeInfo(volumeId, name);
        JSONObject properties = new JSONObject();
        properties.put(AdminConstants.A_VOLUME_STORAGE_TYPE, AdminConstants.A_VOLUME_S3);

        ModifyVolumeInplaceUpgradeRequest modifyVolumeInplaceUpgradeRequest = Mockito.mock(ModifyVolumeInplaceUpgradeRequest.class);
        when(modifyVolumeInplaceUpgradeRequest.getVolumeInfo()).thenReturn(volumeInfo);
        when(modifyVolumeInplaceUpgradeRequest.getId()).thenReturn(volumeId);
        properties = externalVolumeInfoHandler.readServerProperties(volumeId);

        PowerMockito.mockStatic(ClassHelper.class);
        when(ClassHelper.isClassExist(Mockito.anyString())).thenReturn(true);
        VolumeConfigUtil.parseModifyVolumeInplaceUpgradeRequest(modifyVolumeInplaceUpgradeRequest, serverId);
        VolumeManager.getInstance().delete(volume.getId());
    }

    @Test
    public void testAppendServerIdInUnifiedVolumePrefix() throws ServiceException {
        short volumeId = 40;
        String name = "volume40";
        Volume volume = Volume.builder().setId(volumeId)
                .setName(name)
                .setPath("/test/40", false)
                .setType((short) 1)
                .setStoreType(Volume.StoreType.EXTERNAL)
                .setStoreManagerClass(MockStoreManager.class.getName()).build();

        VolumeManager.getInstance().create(volume);
        VolumeInfo volumeInfo = mockVolumeInfo(volumeId, name);
        String expectedVolumePrefix = volumeInfo.getVolumeExternalInfo().getVolumePrefix();
        VolumeConfigUtil.appendServerIdInVolumePrefix(volumeInfo, serverId);
        String actualVolumePrefix = volumeInfo.getVolumeExternalInfo().getVolumePrefix();
        Assert.assertEquals(expectedVolumePrefix, actualVolumePrefix);
    }

    @Test
    public void testAppendServerIdInNonUnifiedExternalVolumePrefix() throws ServiceException {
        short volumeId = 50;
        String name = "volume50";
        Volume volume = Volume.builder().setId(volumeId)
                .setName(name)
                .setPath("/test/50", false)
                .setType((short) 1)
                .setStoreType(Volume.StoreType.EXTERNAL)
                .setStoreManagerClass(MockStoreManager.class.getName()).build();

        VolumeManager.getInstance().create(volume);
        VolumeInfo volumeInfo = mockNonUnifiedExternalVolumeInfo(volumeId, name);
        String expectedVolumePrefix = volumeInfo.getVolumeExternalInfo().getVolumePrefix() + "-" + serverId;
        VolumeConfigUtil.appendServerIdInVolumePrefix(volumeInfo, serverId);
        String actualVolumePrefix = volumeInfo.getVolumeExternalInfo().getVolumePrefix();
        Assert.assertEquals(expectedVolumePrefix, actualVolumePrefix);
    }

}
