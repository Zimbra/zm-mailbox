/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
