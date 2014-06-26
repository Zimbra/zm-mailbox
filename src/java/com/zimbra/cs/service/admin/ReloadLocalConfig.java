/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import org.dom4j.DocumentException;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ReloadLocalConfigResponse;

/**
 * Reload the local config file on the fly.
 * <p>
 * After successfully reloading a new local config file, subsequent
 * {@link LC#get(String)} calls should receive new value. However, if you store/
 * cache those values (e.g. keep them as class member or instance member), new
 * values are of course not reflected.
 *
 * @author ysasaki
 */
public final class ReloadLocalConfig extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        try {
            LC.reload();
        } catch (DocumentException e) {
            ZimbraLog.misc.error("Failed to reload LocalConfig", e);
            throw AdminServiceException.FAILURE("Failed to reload LocalConfig", e);
        } catch (ConfigException e) {
            ZimbraLog.misc.error("Failed to reload LocalConfig", e);
            throw AdminServiceException.FAILURE("Failed to reload LocalConfig", e);
        }
        ZimbraLog.misc.info("LocalConfig reloaded");

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        return zsc.jaxbToElement(new ReloadLocalConfigResponse());
    }

}
