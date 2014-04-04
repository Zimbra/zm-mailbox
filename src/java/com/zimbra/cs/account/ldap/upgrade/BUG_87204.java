/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

package com.zimbra.cs.account.ldap.upgrade;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

/**
 * @author zimbra
 * 
 */
public class BUG_87204 extends UpgradeOp {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.zimbra.cs.account.ldap.upgrade.UpgradeOp#doUpgrade()
	 */
	@Override
	void doUpgrade() throws ServiceException {

		Config config = prov.getConfig();
		Set<String> upstreamEwsServers = config
				.getMultiAttrSet(Provisioning.A_zimbraReverseProxyUpstreamEwsServers);

		Server upgradeServer = Provisioning.getInstance().getLocalServer();
		String newEwsServer = upgradeServer.getName();
		Map<String, Object> attrs = new HashMap<String, Object>();
		if (!upstreamEwsServers.contains(newEwsServer)) {
			StringUtil.addToMultiMap(attrs, "+"
					+ Provisioning.A_zimbraReverseProxyUpstreamEwsServers,
					newEwsServer);
		}
		modifyAttrs(config, attrs);

	}

}
