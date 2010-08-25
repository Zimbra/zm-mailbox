/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import org.dom4j.DocumentException;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.ZimbraSoapContext;

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
        Element response = zsc.createElement(AdminConstants.RELOAD_LOCAL_CONFIG_RESPONSE);
        return response;
    }

}
