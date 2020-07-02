/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class DistributionListDocumentHandler extends AdminDocumentHandler {

    private static final String GROUP = "__GROUP__";

    abstract protected Group getGroup(Element request) throws ServiceException;

    protected final Group getGroupAndCacheInContext(Element request, Map<String, Object> context)
    throws ServiceException {
        Group group = getGroup(request);
        if (group != null) {
            context.put(GROUP, group);
        }
        return group;
    }

    protected Group getGroupFromContext(Map<String, Object> context) throws ServiceException {
        return (Group) context.get(GROUP);
    }

    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context)
    throws ServiceException {
        // if we've explicitly been told to execute here, don't proxy
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        // check whether we need to proxy to the home server of a group
        try {
            Group group = getGroupAndCacheInContext(request, context);

            if (group != null && !Provisioning.onLocalServer(group)) {
                String targetPod = Provisioning.affinityServerForZimbraId(group.getId());
                ZimbraSoapContext pxyCtxt = new ZimbraSoapContext(zsc);
                if (targetPod == null) {
                    throw ServiceException.PROXY_ERROR(
                            AccountServiceException.NO_SUCH_SERVER, "");
                }
                return proxyRequest(request, context, targetPod, pxyCtxt);
            }

            return super.proxyIfNecessary(request, context);
        } catch (ServiceException e) {
            // if something went wrong proxying the request, just execute it locally
            if (ServiceException.PROXY_ERROR.equals(e.getCode())) {
                return null;
            }
            // but if it's a real error, it's a real error
            throw e;
        }
    }
}
