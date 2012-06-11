/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
public final class WsdlServiceInfo implements Comparable<WsdlServiceInfo> {

    private static final String zcsServiceName = "zcsService";
    private static final String zcsAdminServiceName = "zcsAdminService";
    public static final String localhostSoapHttpURL = "http://localhost:7070/service/soap";
    public static final String localhostSoapAdminHttpsURL = "https://localhost:7071/service/admin/soap";
    private static final String zcsPortTypeName = "zcsPortType";
    private static final String zcsAdminPortTypeName = "zcsAdminPortType";
    private static final String zcsBindingName = "zcsPortBinding";
    private static final String zcsAdminBindingName = "zcsAdminPortBinding";
    
    public static WsdlServiceInfo zcsService = WsdlServiceInfo.createForSoap(localhostSoapHttpURL);
    public static WsdlServiceInfo zcsAdminService = WsdlServiceInfo.createForAdmin(localhostSoapAdminHttpsURL);
    
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

    public static WsdlServiceInfo createForAdmin(String soapAddressURL) {
        return new WsdlServiceInfo(zcsAdminServiceName, soapAddressURL, zcsAdminPortTypeName, zcsAdminBindingName);
    }

    public static WsdlServiceInfo createForSoap(String soapAddressURL) {
        return new WsdlServiceInfo(zcsServiceName, soapAddressURL, zcsPortTypeName, zcsBindingName);
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

    @Override
    public int compareTo(WsdlServiceInfo o) {
        return getServiceName().compareTo(o.getServiceName());
    }
}
