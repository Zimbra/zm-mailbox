/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
