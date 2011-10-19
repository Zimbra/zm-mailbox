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

/**
 * Key details related to a WSDL Service advertised at a particular URL
 *
 */
public final class WsdlServiceInfo {

    private static final String zcsServiceName = "zcsService";
    private static final String zcsAdminServiceName = "zcsAdminService";
    private static final String localhostSoapHttpURL = "http://localhost:7070/service/soap";
    private static final String localhostSoapAdminHttpsURL = "https://localhost:7071/service/admin/soap";
    private static final String zcsPortTypeName = "zcsPortType";
    private static final String zcsAdminPortTypeName = "zcsAdminPortType";
    private static final String zcsBindingName = "zcsPortBinding";
    private static final String zcsAdminBindingName = "zcsAdminPortBinding";
    
    public static WsdlServiceInfo zcsService = WsdlServiceInfo.create(
            zcsServiceName, localhostSoapHttpURL, zcsPortTypeName, zcsBindingName);
    public static WsdlServiceInfo zcsAdminService = WsdlServiceInfo.create(
            zcsAdminServiceName, localhostSoapAdminHttpsURL, zcsAdminPortTypeName, zcsAdminBindingName);

    private String serviceName;
    private String soapAddressURL;
    private String portTypeName;
    private String bindingName;

    private WsdlServiceInfo(String svcName, String soapAddressURL, String portTypeName, String bindingName) {
        this.serviceName = svcName;
        this.soapAddressURL = soapAddressURL;
        this.portTypeName = portTypeName;
        this.bindingName = bindingName;
    }

    public static WsdlServiceInfo create(String svcName, String soapAddressURL, String portTypeName, String bindingName) {
        return new WsdlServiceInfo(svcName, soapAddressURL, portTypeName, bindingName);
    }

    public String getSoapAddressURL() {
        return soapAddressURL;
    }

    public String getPortTypeName() {
        return portTypeName;
    }

    public String getBindingName() {
        return bindingName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
