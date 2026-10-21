/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2026 Zimbra, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.store;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FileBlobStore.class, Volume.class, VolumeManager.class})
public class FileBlobStoreMethodTest {

    private VolumeManager manager;

    private Volume volume;

    private IncomingDirectory incomingDirectory;

    private FileBlobStore fileBlobStore;

    @Before
    public void setUp() {
        volume = PowerMockito.mock(Volume.class);
        incomingDirectory = PowerMockito.mock(IncomingDirectory.class);
        fileBlobStore = new FileBlobStore();
        PowerMockito.suppress(
                PowerMockito.method(FileBlobStore.class, "ensureDirExists")
        );
    }

    @Test
    public void testGetBlobBuilderWithVolume() throws Exception {
        File file = new File("testFile");
        when(volume.getIncomingDirectory()).thenReturn(incomingDirectory);
        when(incomingDirectory.getNewIncomingFile()).thenReturn(file);
        when(volume.getId()).thenReturn((short) 1);
        BlobBuilder builder = fileBlobStore.getBlobBuilder(volume);
        assertNotNull(builder);
    }

    @Test
    public void testGetBlobBuilderVolumeNullUsesManager() throws Exception {
        VolumeManager mockManager = PowerMockito.mock(VolumeManager.class);
        Volume defaultVolume = PowerMockito.mock(Volume.class);
        IncomingDirectory dir = PowerMockito.mock(IncomingDirectory.class);
        File file = new File("testFile");
        when(mockManager.getCurrentMessageVolume()).thenReturn(defaultVolume);
        when(defaultVolume.getIncomingDirectory()).thenReturn(dir);
        when(dir.getNewIncomingFile()).thenReturn(file);
        when(defaultVolume.getId()).thenReturn((short) 1);
        PowerMockito.field(FileBlobStore.class, "MANAGER").set(null, mockManager);
        BlobBuilder builder = fileBlobStore.getBlobBuilder(null);
        assertNotNull(builder);
        verify(mockManager).getCurrentMessageVolume();
        verify(defaultVolume).getIncomingDirectory();
        verify(dir).getNewIncomingFile();
    }

    @Test(expected = ServiceException.class)
    public void testGetBlobBuilderWhenIncomingDirectoryNull() throws Exception {
        when(volume.getIncomingDirectory()).thenReturn(null);
        when(volume.getName()).thenReturn("testVolume");
        fileBlobStore.getBlobBuilder(volume);
    }
}

