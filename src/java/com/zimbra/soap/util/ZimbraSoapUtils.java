/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.util;

import java.io.IOException;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.RemoteServiceException;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.soap.JaxbUtil;
/**
 * @author Greg Solovyev
 */
public class ZimbraSoapUtils {

    @SuppressWarnings("unchecked")
    public static <T> T invokeJaxb(SoapHttpTransport transport, Object jaxbObject) throws ServiceException {
        Element req = JaxbUtil.jaxbToElement(jaxbObject);
        Element res = invoke(transport,req);
        return (T) JaxbUtil.elementToJaxb(res);
    }

    private static Element invoke(SoapHttpTransport transport,Element request) throws ServiceException {
        return invoke(transport, request, null);
    }

    private static synchronized Element invoke(SoapHttpTransport transport, Element request, String requestedAccountId) throws ServiceException {
        try {
            Element response = transport.invoke(request, false, true, requestedAccountId);
            return response;
        } catch (SoapFaultException e) {
            ZimbraLog.misc.error(e);
            throw e;
        } catch (Exception e) {
            ZimbraLog.misc.error(e);
            Throwable t = SystemUtil.getInnermostException(e);
            RemoteServiceException.doConnectionFailures(transport.getURI(), t);
            RemoteServiceException.doSSLFailures(t.getMessage(), t);
            if (e instanceof IOException) {
                throw ZClientException.IO_ERROR(e.getMessage(), e);
            }
            throw ServiceException.FAILURE(e.getMessage(), e);
        } finally {
            transport.clearZimbraContext();
        }
    }

    /**
     * used to authenticate via admin AuthRequest. can only be called after setting the URI with setURI.
     *
     * @param name
     * @param password
     * @throws ServiceException
     */
    public static void soapAdminAuthenticate(SoapHttpTransport transport, String name, String password) throws ServiceException {
        transport.setVoidOnExpired(true);
        com.zimbra.soap.admin.message.AuthRequest req = new com.zimbra.soap.admin.message.AuthRequest(name, password);
        com.zimbra.soap.admin.message.AuthResponse resp = invokeJaxb(transport, req);
        transport.setAuthToken(resp.getAuthToken());
    }
}
