/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.callback;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.soap.admin.type.CacheEntryType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FcmConfigRestriction.class)
@PowerMockIgnore("javax.management.*")
public class FcmConfigRestrictionTest {

    private FcmConfigRestriction callback;

    private Config config;

    private Provisioning originalProvisioning;

    private Provisioning provisioning;

    private Server localServer;

    private Server remoteServer;

    private SoapProvisioning soapProvisioning;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        callback = new FcmConfigRestriction();
        originalProvisioning = Provisioning.getInstance();
        provisioning = mock(Provisioning.class);
        config = new Config(new HashMap<String, Object>(), provisioning);
        localServer = server("local.example.com", "local-id");
        remoteServer = server("remote.example.com", "remote-id");
        when(provisioning.getLocalServer()).thenReturn(localServer);
        when(provisioning.getAllMailClientServers()).thenReturn(Collections.singletonList(localServer));
        Provisioning.setInstance(provisioning);
        soapProvisioning = mock(SoapProvisioning.class);
        PowerMockito.whenNew(SoapProvisioning.class).withNoArguments().thenReturn(soapProvisioning);
    }

    @After
    public void tearDown() {
        Provisioning.setInstance(originalProvisioning);
    }

    private Server server(String name, String id) {
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, id);
        attrs.put(Provisioning.A_zimbraServiceHostname, name);
        attrs.put(Provisioning.A_zimbraAdminPort, "7071");
        return new Server(name, id, attrs, Collections.<String, Object>emptyMap(), provisioning);
    }

    @Test
    public void preModifyAllowsGlobalConfigEntryWhenNotCreatingDomain() throws Exception {
        CallbackContext ctx = new CallbackContext(CallbackContext.Op.MODIFY);

        callback.preModify(ctx, "zimbraFcmEnabled", "TRUE", new HashMap<String, Object>(), config);
    }

    @Test
    public void preModifyRejectsNonConfigEntry() throws Exception {
        CallbackContext ctx = new CallbackContext(CallbackContext.Op.MODIFY);

        try {
            callback.preModify(ctx, "zimbraFcmEnabled", "TRUE", new HashMap<String, Object>(), null);
            fail("expected PERM_DENIED for non-config entry");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertEquals("permission denied: 'zimbraFcmEnabled' can only be configured at the global config level.",
                    e.getMessage());
        }
    }

    @Test
    public void preModifyRejectsDomainCreateContextEvenForConfigEntry() throws Exception {
        CallbackContext ctx = new CallbackContext(CallbackContext.Op.CREATE);
        ctx.setCreatingEntryType(Domain.class);

        try {
            callback.preModify(ctx, "zimbraFcmEnabled", "TRUE", new HashMap<String, Object>(), config);
            fail("expected PERM_DENIED for domain create context");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertEquals("permission denied: 'zimbraFcmEnabled' can only be configured at the global config level.",
                    e.getMessage());
        }
    }

    @Test
    public void postModifyRefreshesLocalCacheWithoutSoap() throws Exception {
        Server localAlias = server("local-alias.example.com", "local-id");
        when(provisioning.getAllMailClientServers()).thenReturn(Arrays.asList(localServer, localAlias));
        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY), "zimbraFcmEnabled", config);

        verify(provisioning).flushCache(CacheEntryType.config, null);
        PowerMockito.verifyNew(SoapProvisioning.class, never()).withNoArguments();
        verifyZeroInteractions(soapProvisioning);
    }

    @Test
    public void postModifyRefreshesLocalCacheBeforeRemoteServerOncePerBatch() throws Exception {
        when(provisioning.getAllMailClientServers()).thenReturn(Arrays.asList(remoteServer, localServer));
        CallbackContext ctx = new CallbackContext(CallbackContext.Op.MODIFY);

        callback.postModify(ctx, "zimbraFcmEnabled", config);
        callback.postModify(ctx, "zimbraFcmServiceAccountJSON", config);

        InOrder order = inOrder(provisioning, soapProvisioning);
        order.verify(provisioning).flushCache(CacheEntryType.config, null);
        order.verify(soapProvisioning).soapSetURI(
                URLUtil.getAdminURL(remoteServer, AdminConstants.ADMIN_SERVICE_URI, true));
        order.verify(soapProvisioning).soapZimbraAdminAuthenticate();
        order.verify(soapProvisioning).flushCache("config", null, false, false);
        verify(provisioning, times(1)).flushCache(CacheEntryType.config, null);
        verifyNoMoreInteractions(soapProvisioning);
        PowerMockito.verifyNew(SoapProvisioning.class, times(1)).withNoArguments();
    }

    @Test
    public void postModifyContinuesAfterRemoteAuthenticationFailure() throws Exception {
        Server otherServer = server("other.example.com", "other-id");
        when(provisioning.getAllMailClientServers()).thenReturn(Arrays.asList(remoteServer, localServer, otherServer));
        doThrow(ServiceException.FAILURE("authentication failed", null)).doNothing()
                .when(soapProvisioning).soapZimbraAdminAuthenticate();

        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY), "zimbraFcmEnabled", config);

        verify(soapProvisioning).soapSetURI(URLUtil.getAdminURL(otherServer, AdminConstants.ADMIN_SERVICE_URI, true));
        verify(soapProvisioning, times(2)).soapZimbraAdminAuthenticate();
        verify(soapProvisioning).flushCache("config", null, false, false);
    }

    @Test
    public void postModifyContinuesAfterRemoteFlushFailure() throws Exception {
        Server otherServer = server("other.example.com", "other-id");
        when(provisioning.getAllMailClientServers()).thenReturn(Arrays.asList(remoteServer, otherServer, localServer));
        doThrow(ServiceException.FAILURE("flush failed", null)).doNothing()
                .when(soapProvisioning).flushCache("config", null, false, false);

        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY), "zimbraFcmEnabled", config);

        verify(soapProvisioning).soapSetURI(URLUtil.getAdminURL(otherServer, AdminConstants.ADMIN_SERVICE_URI, true));
        verify(soapProvisioning, times(2)).flushCache("config", null, false, false);
    }

    @Test
    public void postModifySkipsNullEntry() throws Exception {
        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY), "zimbraFcmEnabled", null);

        PowerMockito.verifyNew(SoapProvisioning.class, never()).withNoArguments();
        verify(provisioning, never()).flushCache(CacheEntryType.config, null);
        verifyZeroInteractions(soapProvisioning);
    }

    @Test
    public void postModifyPreservesDomainCacheSelector() throws Exception {
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "domain-id");
        Domain domain = new Domain("example.com", "domain-id", attrs, Collections.<String, Object>emptyMap(),
                provisioning);
        when(provisioning.getAllMailClientServers()).thenReturn(Arrays.asList(localServer, remoteServer));

        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY), "zimbraFcmEnabled", domain);

        ArgumentCaptor<Provisioning.CacheEntry[]> entries = ArgumentCaptor.forClass(Provisioning.CacheEntry[].class);
        InOrder order = inOrder(provisioning, soapProvisioning);
        order.verify(provisioning).flushCache(eq(CacheEntryType.domain), entries.capture());
        order.verify(soapProvisioning).soapSetURI(
                URLUtil.getAdminURL(remoteServer, AdminConstants.ADMIN_SERVICE_URI, true));
        assertEquals(1, entries.getValue().length);
        assertEquals(Key.CacheEntryBy.id, entries.getValue()[0].mEntryBy);
        assertEquals("domain-id", entries.getValue()[0].mEntryIdentity);
        verify(soapProvisioning).flushCache(eq("domain"), entries.capture(), eq(false), eq(false));
        assertEquals(1, entries.getValue().length);
        assertEquals(Key.CacheEntryBy.id, entries.getValue()[0].mEntryBy);
        assertEquals("domain-id", entries.getValue()[0].mEntryIdentity);
    }

    @Test
    public void postModifyRefreshesLocalCacheWhenNotListedAmongMailClientServers() throws Exception {
        when(provisioning.getAllMailClientServers()).thenReturn(Collections.singletonList(remoteServer));

        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY), "zimbraFcmEnabled", config);

        InOrder order = inOrder(provisioning, soapProvisioning);
        order.verify(provisioning).flushCache(CacheEntryType.config, null);
        order.verify(soapProvisioning).soapSetURI(
                URLUtil.getAdminURL(remoteServer, AdminConstants.ADMIN_SERVICE_URI, true));
        verify(soapProvisioning).flushCache("config", null, false, false);
    }

    @Test
    public void postModifyContinuesToRemoteServerAfterLocalRefreshFailure() throws Exception {
        when(provisioning.getAllMailClientServers()).thenReturn(Arrays.asList(localServer, remoteServer));
        doThrow(ServiceException.FAILURE("local refresh failed", null))
                .when(provisioning).flushCache(CacheEntryType.config, null);

        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY), "zimbraFcmEnabled", config);

        InOrder order = inOrder(provisioning, soapProvisioning);
        order.verify(provisioning).flushCache(CacheEntryType.config, null);
        order.verify(soapProvisioning).soapSetURI(
                URLUtil.getAdminURL(remoteServer, AdminConstants.ADMIN_SERVICE_URI, true));
        verify(soapProvisioning).flushCache("config", null, false, false);
    }
}
