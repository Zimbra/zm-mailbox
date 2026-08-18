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

package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.cs.account.soap.SoapProvisioning;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ClearMFARejectionCache}.
 *
 * <p>This operation is restricted to global admins only.
 * Domain admins are not permitted to invoke this command.
 */
public class ClearMFARejectionCacheTest {

    private SoapProvisioning sp;

    private Element mockResponse;

    @Before
    public void setUp() throws Exception {
        sp = spy(new SoapProvisioning());
        mockResponse = new XMLElement(AdminConstants.CLEAR_MFA_REJECTION_CACHE_RESPONSE);
        mockResponse.addAttribute(AdminConstants.A_STATUS, "ok");
        mockResponse.addAttribute(AdminConstants.A_ENTRIES_CLEARED, "5");
    }

    @Test
    public void globalAdminClearForAccountByName() throws Exception {
        doReturn(mockResponse).when(sp).invoke(any(Element.class));
        ArgumentCaptor<Element> captor = ArgumentCaptor.forClass(Element.class);

        Element response = sp.clearMFARejectionCache("user@domain.com", "name");

        verify(sp).invoke(captor.capture());
        Element request = captor.getValue();

        assertEquals(AdminConstants.CLEAR_MFA_REJECTION_CACHE_REQUEST.getName(), request.getName());
        Element acctEl = request.getElement(AdminConstants.E_ACCOUNT);
        assertNotNull(acctEl);
        assertEquals("name", acctEl.getAttribute("by"));
        assertEquals("user@domain.com", acctEl.getText());
        assertEquals("ok", response.getAttribute(AdminConstants.A_STATUS));
        assertEquals("5", response.getAttribute(AdminConstants.A_ENTRIES_CLEARED));
    }

    @Test
    public void globalAdminClearForAccountById() throws Exception {
        doReturn(mockResponse).when(sp).invoke(any(Element.class));
        ArgumentCaptor<Element> captor = ArgumentCaptor.forClass(Element.class);
        sp.clearMFARejectionCache("some-zimbra-id-1234", "id");
        verify(sp).invoke(captor.capture());
        Element acctEl = captor.getValue().getElement(AdminConstants.E_ACCOUNT);
        assertEquals("id", acctEl.getAttribute("by"));
        assertEquals("some-zimbra-id-1234", acctEl.getText());
    }

    @Test
    public void globalAdminClearForAllAccounts() throws Exception {
        doReturn(mockResponse).when(sp).invoke(any(Element.class));
        ArgumentCaptor<Element> captor = ArgumentCaptor.forClass(Element.class);
        Element response = sp.clearMFARejectionCache(null, null);
        verify(sp).invoke(captor.capture());
        Element request = captor.getValue();
        assertEquals(AdminConstants.CLEAR_MFA_REJECTION_CACHE_REQUEST.getName(), request.getName());
        assertNull(request.getOptionalElement(AdminConstants.E_ACCOUNT));
        assertNotNull(response);
    }

    @Test(expected = ServiceException.class)
    public void globalAdminClearPropagatesServiceException() throws Exception {
        doThrow(ServiceException.FAILURE("SOAP fault", null))
                .when(sp).invoke(any(Element.class));

        sp.clearMFARejectionCache("user@domain.com", "name");
    }

    @Test
    public void globalAdminResponseStatusAndEntriesCleared() throws Exception {
        doReturn(mockResponse).when(sp).invoke(any(Element.class));

        Element response = sp.clearMFARejectionCache("user@domain.com", "name");

        assertEquals("ok", response.getAttribute(AdminConstants.A_STATUS, "unknown"));
        assertEquals("5", response.getAttribute(AdminConstants.A_ENTRIES_CLEARED, "0"));
    }

    @Test
    public void globalAdminResponseDefaults() throws Exception {
        Element emptyResponse = new XMLElement(AdminConstants.CLEAR_MFA_REJECTION_CACHE_RESPONSE);
        doReturn(emptyResponse).when(sp).invoke(any(Element.class));

        Element response = sp.clearMFARejectionCache(null, null);

        assertEquals("unknown", response.getAttribute(AdminConstants.A_STATUS, "unknown"));
        assertEquals("0", response.getAttribute(AdminConstants.A_ENTRIES_CLEARED, "0"));
    }

    @Test
    public void globalAdminRequestHasNoExtraElements() throws Exception {
        doReturn(mockResponse).when(sp).invoke(any(Element.class));
        ArgumentCaptor<Element> captor = ArgumentCaptor.forClass(Element.class);

        sp.clearMFARejectionCache(null, null);

        verify(sp).invoke(captor.capture());
        Element request = captor.getValue();

        assertTrue(request.listElements().isEmpty());
    }

    @Test
    public void globalAdminRequestHasOnlyOneAccount() throws Exception {
        doReturn(mockResponse).when(sp).invoke(any(Element.class));
        ArgumentCaptor<Element> captor = ArgumentCaptor.forClass(Element.class);

        sp.clearMFARejectionCache("user@domain.com", "name");

        verify(sp).invoke(captor.capture());
        Element request = captor.getValue();

        assertEquals(1, request.listElements(AdminConstants.E_ACCOUNT).size());
    }
}
