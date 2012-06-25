/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.ServerBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.rmgmt.RemoteCommands;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.rmgmt.RemoteResult;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author Greg Solovyev
 */
public class GetServerNIFs extends AdminDocumentHandler {
    private static final Pattern ADDR_PATTERN = Pattern.compile("(addr):([0-9a-z\\.:]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MASK_PATTERN = Pattern.compile("(mask):([0-9\\.xabcdef]+)", Pattern.CASE_INSENSITIVE);
    private static final int KEY_GROUP = 1;
    private static final int VALUE_GROUP = 2;
    private static final String IPV4 = "ipv4";
    private static final String IPV6 = "ipv6";
	
	public Element handle(Element request, Map<String, Object> context)
			throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);

		String ipAddressType = request.getAttribute(AdminConstants.A_TYPE, null);
		boolean ipV4 = false, ipV6 = false;
		if (StringUtil.equalIgnoreCase(ipAddressType, IPV6)) {
			ipV6 = true;
		} else if (StringUtil.equalIgnoreCase(ipAddressType, "both")) {
			ipV4 = true;
			ipV6 = true;
		} else {
			// ipv4 is the default type
			ipV4 = true;
		}
	    Element serverEl = request.getElement(AdminConstants.E_SERVER);
	    String method = serverEl.getAttribute(AdminConstants.A_BY);
	    String serverName = serverEl.getText();
	    Provisioning prov = Provisioning.getInstance();
		Server server = prov.get(Key.ServerBy.fromString(method), serverName);
		if (server == null) {
			throw ServiceException.INVALID_REQUEST("Cannot find server record for the host: " + serverName, null);
		}	    
		
		RemoteManager rmgr = RemoteManager.getRemoteManager(server);
		RemoteResult rr = rmgr.execute(RemoteCommands.ZM_SERVER_IPS);
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rr.getMStdout())));
        String line;
        
        Element response = lc.createElement(AdminConstants.GET_SERVER_NIFS_RESPONSE);
        try {
			while ((line = in.readLine()) != null) {
				Matcher IPmatcher = ADDR_PATTERN.matcher(line);
				Matcher maskMatcher = MASK_PATTERN.matcher(line);
				if (IPmatcher.find() && maskMatcher.find()) {
					String ipAddress = IPmatcher.group(VALUE_GROUP).toLowerCase();
					InetAddress addressType = InetAddress.getByName(ipAddress);
					if (addressType instanceof Inet6Address && !ipV6) {
						continue;
					} else if (addressType instanceof Inet4Address && !ipV4) {
						continue;
					}
					String type =  (addressType instanceof Inet4Address) ? IPV4 : IPV6;
					Element elNIF = response.addElement(AdminConstants.E_NI);
					elNIF.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, IPmatcher.group(KEY_GROUP).toLowerCase()).addAttribute(AdminConstants.A_TYPE, type).setText(ipAddress);
			    	elNIF.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, maskMatcher.group(KEY_GROUP).toLowerCase()).setText(maskMatcher.group(VALUE_GROUP));
				}
			}
		} catch (IOException e) {
			throw ServiceException.FAILURE("exception occurred handling CLI command", e);
		}
		return response;
	}

	@Override
	public void docRights(List<AdminRight> relatedRights, List<String> notes) {
	    notes.add(AdminRightCheckPoint.Notes.ALLOW_ALL_ADMINS);
	}
}
