/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.callback;

import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ThirdPartyMFASessionClearanceService.class, Provisioning.class, SoapProvisioning.class, LC.class,
        ZimbraLog.class, KnownKey.class})
@SuppressStaticInitializationFor({"com.zimbra.common.localconfig.LC", "com.zimbra.common.util.ZimbraLog"})
public class ThirdPartyMFASessionClearanceServiceTest {

    private static final class TestServer extends Server {
        private String serviceHostname;

        private int adminPort;

        private String serverName;

        private Set<String> enabledServices = new HashSet<>();

        private TestServer() {
            super("testServer", "1234", new HashMap<>(), new HashMap<>(), null);
        }

        void setEnabledServices(String... serviceNames) {
            if (enabledServices == null) {
                enabledServices = new HashSet<>();
            }
            enabledServices.clear();
            if (serviceNames != null) {
                enabledServices.addAll(Arrays.asList(serviceNames));
            }
        }

        @Override
        public String getServiceHostname() {
            return serviceHostname;
        }

        @Override
        public int getAdminPort() {
            return adminPort;
        }

        @Override
        public String getName() {
            return serverName;
        }

        @Override
        public Set<String> getMultiAttrSet(String name) {
            if (enabledServices == null) {
                return Collections.emptySet();
            }
            return new HashSet<>(enabledServices);
        }
    }

    private ExecutorService executor;

    private Future<?> future;

    private Provisioning provisioning;

    private Server server1;

    private Server server2;

    private SoapProvisioning soapProvisioning;

    private Log accountLog;

    private KnownKey schemeKey;

    @Before
    public void setUp() throws Exception {
        executor = PowerMockito.mock(ExecutorService.class);
        future = PowerMockito.mock(Future.class);
        provisioning = PowerMockito.mock(Provisioning.class);
        server1 = Whitebox.newInstance(TestServer.class);
        server2 = Whitebox.newInstance(TestServer.class);
        Whitebox.setInternalState(server1, "serviceHostname", "mailbox1.example.com");
        Whitebox.setInternalState(server1, "adminPort", 7071);
        Whitebox.setInternalState(server1, "serverName", "mailbox1");
        ((TestServer) server1).setEnabledServices(Provisioning.SERVICE_MAILBOX);
        Whitebox.setInternalState(server2, "serviceHostname", "mailbox2.example.com");
        Whitebox.setInternalState(server2, "adminPort", 9071);
        Whitebox.setInternalState(server2, "serverName", "mailbox2");
        ((TestServer) server2).setEnabledServices(Provisioning.SERVICE_MAILBOX);
        soapProvisioning = PowerMockito.mock(SoapProvisioning.class);
        accountLog = PowerMockito.mock(Log.class);
        schemeKey = PowerMockito.mock(KnownKey.class);
        Whitebox.setInternalState(ZimbraLog.class, "account", accountLog);
        when(accountLog.isDebugEnabled()).thenReturn(true);
        when(schemeKey.value()).thenReturn("https://");

        Whitebox.setInternalState(ThirdPartyMFASessionClearanceService.class, "EXECUTOR", executor);
        doReturn(future).when(executor).submit(any(Runnable.class));

        PowerMockito.mockStatic(Provisioning.class);
        when(Provisioning.getInstance()).thenReturn(provisioning);

        Whitebox.setInternalState(LC.class, "zimbra_admin_service_scheme", schemeKey);


    }

    @Test
    public void clearSessionsForDomainNullDomainDoesNothing() {
        ThirdPartyMFASessionClearanceService.clearSessionsForDomain(null, "1234");
        verify(executor, never()).submit(any(Runnable.class));
    }

    @Test
    public void clearSessionsForDomainEmptyDomainDoesNothing() {
        ThirdPartyMFASessionClearanceService.clearSessionsForDomain("", "1234");
        verify(executor, never()).submit(any(Runnable.class));
    }

    @Test
    public void clearSessionsForDomainValidDomainSubmitsBackgroundTask() {
        ThirdPartyMFASessionClearanceService.clearSessionsForDomain("example.com", "1234");
        verify(executor, times(1)).submit(any(Runnable.class));
    }

    @Test
    public void clearSessionsForDomainAsyncNullServerListReturnsGracefully() throws Exception {
        when(provisioning.getAllServers(Provisioning.SERVICE_MAILBOX)).thenReturn(null);
        Whitebox.invokeMethod(ThirdPartyMFASessionClearanceService.class,
                "clearSessionsForDomainAsync", "example.com", "1234");
    }

    @Test
    public void clearSessionsForDomainAsyncEmptyServerListReturnsGracefully() throws Exception {
        when(provisioning.getAllServers(Provisioning.SERVICE_MAILBOX)).thenReturn(Collections.emptyList());
        Whitebox.invokeMethod(ThirdPartyMFASessionClearanceService.class,
                "clearSessionsForDomainAsync", "example.com", "1234");
    }

    @Test
    public void clearSessionsForDomainAsyncContinuesWhenOneServerFails() throws Exception {
        when(provisioning.getAllServers(Provisioning.SERVICE_MAILBOX)).thenReturn(Arrays.asList(server1, server2));
        PowerMockito.whenNew(SoapProvisioning.class)
                .withAnyArguments()
                .thenReturn(soapProvisioning);
        when(soapProvisioning.clearMFASessionForDomain(anyString(), anyString()))
                .thenThrow(new RuntimeException("boom"))
                .thenReturn(null);

        Whitebox.invokeMethod(ThirdPartyMFASessionClearanceService.class,
                "clearSessionsForDomainAsync", "example.com", "1234");

        verify(soapProvisioning, times(2)).clearMFASessionForDomain("example.com", "1234");
    }

    @Test
    public void clearSessionsForDomainSyncDoesNotUseExecutor() throws Exception {
        when(provisioning.getAllServers(Provisioning.SERVICE_MAILBOX)).thenReturn(Arrays.asList(server1, server2));
        PowerMockito.whenNew(SoapProvisioning.class)
                .withAnyArguments()
                .thenReturn(soapProvisioning);

        ThirdPartyMFASessionClearanceService.clearSessionsForDomainSync("example.com", "1234");

        verify(executor, never()).submit(any(Runnable.class));
        verify(soapProvisioning, times(2)).clearMFASessionForDomain("example.com", "1234");
    }

    @Test
    public void clearSessionsForDomainAsyncHandlesProvisioningFailure() throws Exception {
        when(provisioning.getAllServers(Provisioning.SERVICE_MAILBOX)).thenThrow(new RuntimeException("failed lookup"));
        Whitebox.invokeMethod(ThirdPartyMFASessionClearanceService.class,
                "clearSessionsForDomainAsync", "example.com", "1234");
    }

    @Test
    public void clearSessionsForDomainAsyncSkipsNonMailboxServers() throws Exception {
        TestServer ldapServer = Whitebox.newInstance(TestServer.class);
        ldapServer.serviceHostname = "ldap.example.com";
        ldapServer.adminPort = 7071;
        ldapServer.serverName = "ldap";
        ldapServer.setEnabledServices("ldap");

        TestServer mailboxServer = Whitebox.newInstance(TestServer.class);
        mailboxServer.serviceHostname = "mailbox.example.com";
        mailboxServer.adminPort = 7071;
        mailboxServer.serverName = "mailbox";
        mailboxServer.setEnabledServices(Provisioning.SERVICE_MAILBOX);

        when(provisioning.getAllServers(Provisioning.SERVICE_MAILBOX))
                .thenReturn(Arrays.asList(ldapServer, mailboxServer, null));
        PowerMockito.whenNew(SoapProvisioning.class)
                .withAnyArguments()
                .thenReturn(soapProvisioning);

        Whitebox.invokeMethod(ThirdPartyMFASessionClearanceService.class,
                "clearSessionsForDomainAsync", "example.com", "1234");

        verify(soapProvisioning, times(1)).clearMFASessionForDomain("example.com", "1234");
    }

    @Test
    public void clearOnServerBuildsSoapOptionsAndInvokesSoapCall() throws Exception {
        AtomicReference<SoapProvisioning.Options> capturedOptions = new AtomicReference<>();
        PowerMockito.whenNew(SoapProvisioning.class)
                .withParameterTypes(SoapProvisioning.Options.class)
                .withArguments(org.mockito.Matchers.any(SoapProvisioning.Options.class))
                .thenAnswer(invocation -> {
                    capturedOptions.set((SoapProvisioning.Options) invocation.getArguments()[0]);
                    return soapProvisioning;
                });

        Whitebox.invokeMethod(ThirdPartyMFASessionClearanceService.class,
                "clearOnServer", server1, "example.com", "5678");

        verify(soapProvisioning).clearMFASessionForDomain("example.com", "5678");
        assertNotNull(capturedOptions.get());
        assertEquals("https://mailbox1.example.com:7071" + AdminConstants.ADMIN_SERVICE_URI,
                capturedOptions.get().getUri());
        assertTrue(capturedOptions.get().getLocalConfigAuth());
    }

    @Test
    public void privateConstructorIsInvocableViaReflection() throws Exception {
        Constructor<ThirdPartyMFASessionClearanceService> ctor =
                ThirdPartyMFASessionClearanceService.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }

    @Test
    public void clearOnServerSkipsServerWithMissingHostname() throws Exception {
        TestServer badServer = Whitebox.newInstance(TestServer.class);
        badServer.serviceHostname = null;
        badServer.adminPort = 7071;
        badServer.serverName = "bad-host";
        badServer.setEnabledServices(Provisioning.SERVICE_MAILBOX);

        PowerMockito.whenNew(SoapProvisioning.class)
                .withAnyArguments()
                .thenReturn(soapProvisioning);

        Whitebox.invokeMethod(ThirdPartyMFASessionClearanceService.class,
                "clearOnServer", badServer, "example.com", "5678");

        verify(soapProvisioning, never()).clearMFASessionForDomain(anyString(), anyString());
    }

    @Test
    public void clearOnServerSkipsServerWithInvalidPort() throws Exception {
        TestServer badServer = Whitebox.newInstance(TestServer.class);
        badServer.serviceHostname = "mailbox.example.com";
        badServer.adminPort = 0;
        badServer.serverName = "bad-port";
        badServer.setEnabledServices(Provisioning.SERVICE_MAILBOX);

        PowerMockito.whenNew(SoapProvisioning.class)
                .withAnyArguments()
                .thenReturn(soapProvisioning);

        Whitebox.invokeMethod(ThirdPartyMFASessionClearanceService.class,
                "clearOnServer", badServer, "example.com", "5678");

        verify(soapProvisioning, never()).clearMFASessionForDomain(anyString(), anyString());
    }

    @Test
    public void clearOnServerSkipsNullServer() throws Exception {
        PowerMockito.whenNew(SoapProvisioning.class)
                .withAnyArguments()
                .thenReturn(soapProvisioning);

        Whitebox.invokeMethod(ThirdPartyMFASessionClearanceService.class,
                "clearOnServer", (Server) null, "example.com", "5678");


        verify(soapProvisioning, never()).clearMFASessionForDomain(anyString(), anyString());
    }
}
