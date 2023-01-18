package com.zimbra.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.admin.type.VolumeExternalInfo;
import com.zimbra.soap.admin.type.VolumeInfo;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(MockitoJUnitRunner.class)
@PrepareForTest({ ExternalVolumeInfoHandler.class })
public class ExternalVolumeInfoHandlerTest {

    // Static JSON having no unified structure
    private static final String TEST_GLOBAL_BUCKETS = "{\"global/s3BucketConfigurations\":[{\"globalBucketUUID\": \"aaaa-bbbb-cccc-1111\", \"bucketName\": \"AWS_BUCKET1\", \"storeProvider\": \"AWS_S3\", \"protocol\": \"HTTPS\", \"storeType\": \"S3\", \"accessKey\": \"testAccess\", \"secret\": \"testSecret\", \"region\": \"US_EAST_1\", \"destinationPath\": \"somepath\", \"url\": \"https://aws.com\", \"bucketStatus\": \"ACTIVE\"}, {\"globalBucketUUID\": \"aaaa-bbbb-cccc-2222\", \"bucketName\": \"AWS_BUCKET2\", \"storeProvider\": \"AWS_S3\", \"protocol\": \"HTTPS\", \"accessKey\": \"testAccess\", \"secret\": \"testSecret\", \"region\": \"US_EAST_1\", \"destinationPath\": \"somepath\", \"url\": \"https://aws.com\", \"bucketStatus\": \"ACTIVE\"} ] }";
    // Static JSON having 1 server and 1 volume
    private static final String TEST_GLOBAL_BUCKETS_UNIFIED_1 = "{\"global/s3BucketConfigurations\":[{\"globalBucketUUID\": \"aaaa-bbbb-cccc-1111\", \"bucketName\": \"AWS_BUCKET1\", \"storeProvider\": \"AWS_S3\", \"protocol\": \"HTTPS\", \"storeType\": \"S3\", \"accessKey\": \"testAccess\", \"secret\": \"testSecret\", \"region\": \"US_EAST_1\", \"destinationPath\": \"somepath\", \"url\": \"https://aws.com\", \"bucketStatus\": \"ACTIVE\"}, {\"globalBucketUUID\": \"aaaa-bbbb-cccc-2222\", \"bucketName\": \"AWS_BUCKET2\", \"storeProvider\": \"AWS_S3\", \"protocol\": \"HTTPS\", \"accessKey\": \"testAccess\", \"secret\": \"testSecret\", \"region\": \"US_EAST_1\", \"destinationPath\": \"somepath\", \"url\": \"https://aws.com\", \"bucketStatus\": \"ACTIVE\"} ],\"unified/volumes\":{\"4b16223d-8b97-4562-8eeb-3f7727fa5d56\":[{\"globalBucketUUID\":\"GLOBAL_BUCKET_ID\",\"volumePrefix\":\"/JunitPrefix\",\"volumeId\":19}]} }";
    // Static JSON having 1 server and 2 volumes
    private static final String TEST_GLOBAL_BUCKETS_UNIFIED_2 = "{\"global/s3BucketConfigurations\":[{\"globalBucketUUID\": \"aaaa-bbbb-cccc-1111\", \"bucketName\": \"AWS_BUCKET1\", \"storeProvider\": \"AWS_S3\", \"protocol\": \"HTTPS\", \"storeType\": \"S3\", \"accessKey\": \"testAccess\", \"secret\": \"testSecret\", \"region\": \"US_EAST_1\", \"destinationPath\": \"somepath\", \"url\": \"https://aws.com\", \"bucketStatus\": \"ACTIVE\"}, {\"globalBucketUUID\": \"aaaa-bbbb-cccc-2222\", \"bucketName\": \"AWS_BUCKET2\", \"storeProvider\": \"AWS_S3\", \"protocol\": \"HTTPS\", \"accessKey\": \"testAccess\", \"secret\": \"testSecret\", \"region\": \"US_EAST_1\", \"destinationPath\": \"somepath\", \"url\": \"https://aws.com\", \"bucketStatus\": \"ACTIVE\"} ],\"unified/volumes\":{\"2da1249b-daa0-40cb-aa80-d90223c8d51b\":[{\"globalBucketUUID\":\"GLOBAL_BUCKET_ID\",\"volumePrefix\":\"/JunitPrefix\",\"volumeId\":18}, {\"globalBucketUUID\":\"GLOBAL_BUCKET_ID_1\",\"volumePrefix\":\"/JunitPrefix\",\"volumeId\":19}]} }";
    // Static JSON having 2 servers and multiple volumes
    private static final String TEST_GLOBAL_BUCKETS_UNIFIED_2Server = "{\"global/s3BucketConfigurations\":[{\"globalBucketUUID\": \"aaaa-bbbb-cccc-1111\", \"bucketName\": \"AWS_BUCKET1\", \"storeProvider\": \"AWS_S3\", \"protocol\": \"HTTPS\", \"storeType\": \"S3\", \"accessKey\": \"testAccess\", \"secret\": \"testSecret\", \"region\": \"US_EAST_1\", \"destinationPath\": \"somepath\", \"url\": \"https://aws.com\", \"bucketStatus\": \"ACTIVE\"}, {\"globalBucketUUID\": \"aaaa-bbbb-cccc-2222\", \"bucketName\": \"AWS_BUCKET2\", \"storeProvider\": \"AWS_S3\", \"protocol\": \"HTTPS\", \"accessKey\": \"testAccess\", \"secret\": \"testSecret\", \"region\": \"US_EAST_1\", \"destinationPath\": \"somepath\", \"url\": \"https://aws.com\", \"bucketStatus\": \"ACTIVE\"} ],\"unified/volumes\":{\"2da1249b-daa0-40cb-aa80-d90223c8d51b\":[{\"globalBucketUUID\":\"GLOBAL_BUCKET_ID\",\"volumePrefix\":\"/JunitPrefix\",\"volumeId\":20}, {\"globalBucketUUID\":\"GLOBAL_BUCKET_ID_1\",\"volumePrefix\":\"/JunitPrefix\",\"volumeId\":21}], \"2da1249b-daa0-40cb-aa80-d90223c8d51b\":[{\"globalBucketUUID_3\":\"GLOBAL_BUCKET_ID\",\"volumePrefix\":\"/JunitPrefix\",\"volumeId\":22}]} }";

    private MockProvisioning mockProvisioning;
    private ExternalVolumeInfoHandler externalVolumeInfoHandler = null;
    String serverExternalStoreConfigJson = null;
    String serverId = null;

    @Before
    public void setUp() throws ServiceException {
        mockProvisioning = new MockProvisioning();
        Provisioning.setInstance(mockProvisioning);
        externalVolumeInfoHandler = new ExternalVolumeInfoHandler(mockProvisioning);
        mockProvisioning.getConfig().setGlobalExternalStoreConfig(TEST_GLOBAL_BUCKETS);
        serverExternalStoreConfigJson = mockProvisioning.getLocalServer().getServerExternalStoreConfig();
        serverId = mockProvisioning.getLocalServer().getId();
    }

    //If the unified/volume does not exist
    @Test
    public void testEditGlobalConfigOnAddVolume_AddServerVolume() {
        VolumeInfo volInfo = mockVolumeInfo();
        String expectedServerExternalStoreConfigJson = TEST_GLOBAL_BUCKETS_UNIFIED_1;
        String actualServerExternalStoreConfigJson = externalVolumeInfoHandler.editGlobalConfigOnAddVolume(volInfo,
                serverId, TEST_GLOBAL_BUCKETS);
        Assert.assertEquals(expectedServerExternalStoreConfigJson, actualServerExternalStoreConfigJson);
    }

    //If the unified/volume exist and adding 1 additional volume in existing server
    @Test
    public void testEditGlobalConfigOnAddVolume_AddAdditionalVolume() {
        VolumeInfo volInfo = mockVolumeInfo();
        String expectedServerExternalStoreConfigJson = TEST_GLOBAL_BUCKETS_UNIFIED_2;
        String actualServerExternalStoreConfigJson = externalVolumeInfoHandler.editGlobalConfigOnAddVolume(volInfo,
                serverId, TEST_GLOBAL_BUCKETS_UNIFIED_1);
        Assert.assertEquals(expectedServerExternalStoreConfigJson, actualServerExternalStoreConfigJson);
    }

    //If the unified/volume exist and adding 1 additional volume in existing server
    @Test
    public void testEditGlobalConfigOnAddVolume_AddAdditionalServer() {
        VolumeInfo volInfo = mockVolumeInfo();
        String expectedServerExternalStoreConfigJson = TEST_GLOBAL_BUCKETS_UNIFIED_2Server;
        String actualServerExternalStoreConfigJson = externalVolumeInfoHandler.editGlobalConfigOnAddVolume(volInfo,
                serverId, TEST_GLOBAL_BUCKETS_UNIFIED_2);
        Assert.assertEquals(expectedServerExternalStoreConfigJson, actualServerExternalStoreConfigJson);
    }

    private VolumeInfo mockVolumeInfo() {
        VolumeInfo volInfo = new VolumeInfo();
        volInfo.setId((short) 20);
        volInfo.setName("JunitTest");
        volInfo.setRootPath("/junit/");
        volInfo.setType((short) 1);

        VolumeExternalInfo volumeExternalInfo = new VolumeExternalInfo();
        volumeExternalInfo.setGlobalBucketConfigurationId("GLOBAL_BUCKET_ID");
        volumeExternalInfo.setVolumePrefix("/JunitPrefix");
        volumeExternalInfo.setUnified(true);
        volumeExternalInfo.setStorageType("1");

        volInfo.setVolumeExternalInfo(volumeExternalInfo);
        return volInfo;
    }

    @Test
    public void testEditGlobalConfigOnAddVolume_AllNullCase() {
        VolumeInfo volInfo = null;
        String serverId = null;
        serverExternalStoreConfigJson = null;
        String expectedServerExternalStoreConfigJson = null;
        String actualServerExternalStoreConfigJson = externalVolumeInfoHandler.editGlobalConfigOnAddVolume(volInfo,
                serverId, serverExternalStoreConfigJson);
        Assert.assertEquals(expectedServerExternalStoreConfigJson, actualServerExternalStoreConfigJson);
    }

    @Test
    public void testEditGlobalConfigOnAddVolume_NullVolumeServer() {
        VolumeInfo volInfo = null;
        String serverId = null;
        serverExternalStoreConfigJson = TEST_GLOBAL_BUCKETS;
        String expectedServerExternalStoreConfigJson = null;
        String actualServerExternalStoreConfigJson = externalVolumeInfoHandler.editGlobalConfigOnAddVolume(volInfo,
                serverId, serverExternalStoreConfigJson);
        Assert.assertEquals(expectedServerExternalStoreConfigJson, actualServerExternalStoreConfigJson);
    }

    @Test
    public void testEditGlobalConfigOnAddVolume_NullServer() {
        VolumeInfo volInfo = new VolumeInfo();
        String serverId = null;
        serverExternalStoreConfigJson = TEST_GLOBAL_BUCKETS;
        String expectedServerExternalStoreConfigJson = null;
        String actualServerExternalStoreConfigJson = externalVolumeInfoHandler.editGlobalConfigOnAddVolume(volInfo,
                serverId, serverExternalStoreConfigJson);
        Assert.assertEquals(expectedServerExternalStoreConfigJson, actualServerExternalStoreConfigJson);
    }

    @Test
    public void testEditGlobalConfigOnAddVolume_Null_Volume() {
        VolumeInfo volInfo = null;
        String serverId = "abc";
        serverExternalStoreConfigJson = TEST_GLOBAL_BUCKETS;
        String expectedServerExternalStoreConfigJson = null;
        String actualServerExternalStoreConfigJson = externalVolumeInfoHandler.editGlobalConfigOnAddVolume(volInfo,
                serverId, serverExternalStoreConfigJson);
        Assert.assertEquals(expectedServerExternalStoreConfigJson, actualServerExternalStoreConfigJson);
    }

}
