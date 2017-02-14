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

package com.zimbra.soap.util;

import java.util.List;

import com.google.common.collect.Lists;

import org.dom4j.Namespace;

/**
 * Stores information related to a particular Namespace used from WSDL
 */
public class WsdlInfoForNamespace {

    private String xsdNamespaceString; // e.g. urn:zimbraAccount
    private Namespace xsdNamespace;
    private String xsdPrefix;          // e.g. zimbraAdmin
    private String tag;                // e.g. Admin
    private WsdlServiceInfo svcInfo;
    private List <String> requests;

    public WsdlInfoForNamespace(String xsdNs, WsdlServiceInfo svcInfo, Iterable<String> requests) {
        this.xsdNamespaceString = xsdNs;
        this.svcInfo = svcInfo;
        this.requests = Lists.newArrayList(requests);
        this.xsdPrefix = xsdNs.replaceFirst("urn:", "");
        this.tag = xsdPrefix.replaceFirst("zimbra", "");
        this.xsdNamespace = new Namespace(getXsdPrefix(), this.getXsdNamespaceString());
    }

    public static WsdlInfoForNamespace create(
            String xsdNs, WsdlServiceInfo svcInfo, Iterable<String> requests) {
        return new WsdlInfoForNamespace(xsdNs, svcInfo, requests);
    }

    public List <String> getRequests() {
        return requests;
    }

    public String getXsdNamespaceString() {
        return xsdNamespaceString;
    }

    public Namespace getXsdNamespace() {
        return xsdNamespace;
    }

    public String getXsdFilename() {
        return xsdNamespaceString.substring(4) + ".xsd";
    }

    public String getXsdPrefix() {
        return xsdPrefix;
    }

    public WsdlServiceInfo getSvcInfo() {
        return svcInfo;
    }

    public String getTag() {
        return tag;
    }
}
