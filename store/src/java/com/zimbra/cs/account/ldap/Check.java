/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.account.ldap;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.fb.ExchangeEWSFreeBusyProvider;
import com.zimbra.cs.fb.ExchangeFreeBusyProvider;
import com.zimbra.common.service.ServiceException;

public class Check {

    public static final String STATUS_OK = "check.OK";

    /** unknown hostname */
    public static final String STATUS_UNKNOWN_HOST = "check.UNKNOWN_HOST";
    
    /** connection was refused */
    public static final String STATUS_CONNECTION_REFUSED = "check.CONNECTION_REFUSED";
    
    /** SSL problem (most likely untrusted certificate) */
    public static final String STATUS_SSL_HANDSHAKE_FAILURE = "check.SSL_HANDSHAKE_FAILURE";
    
    /** generic communication failure */
    public static final String STATUS_COMMUNICATION_FAILURE = "check.COMMUNICATION_FAILURE";    
    
    /** authentication failed. invalid credentials (bad dn/password) */
    public static final String STATUS_AUTH_FAILED= "check.AUTH_FAILED";

    /** authentication flavor not supported. */
    public static final String STATUS_AUTH_NOT_SUPPORTED = "check.AUTH_NOT_SUPPORTED";

    /** jndi name not found. most likley an invalid search base */
    public static final String STATUS_NAME_NOT_FOUND = "check.NAME_NOT_FOUND";

    /** jndi invalid search filter. */
    public static final String STATUS_INVALID_SEARCH_FILTER = "check.INVALID_SEARCH_FILTER";

    /** some other error occurred  */
    public static final String STATUS_FAILURE = "check.FAILURE";

    /** HTTP error codes */
    public static final String STATUS_BAD_URL = "check.BAD_URL";
    public static final String STATUS_FORBIDDEN = "check.FORBIDDEN";
    
    public static String getRequiredAttr(Map attrs, String name) throws ServiceException {
        String value = (String) attrs.get(name);
        if (value == null)
            throw ServiceException.INVALID_REQUEST("must specifiy: "+name, null);
        return value;
    }

    public static String[] getRequiredMultiAttr(Map attrs, String name) throws ServiceException {
        Object v = attrs.get(name);
        if (v instanceof String) return new String[] {(String)v};
        else if (v instanceof String[]) {
            String value[] = (String[]) v;
            if (value != null && value.length > 0)
                return value;
        }
        throw ServiceException.INVALID_REQUEST("must specifiy: "+name, null);
    }

    public static Provisioning.Result checkHostnameResolve(String hostname) {
        try {
            InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            return new Provisioning.Result(STATUS_UNKNOWN_HOST, e, (String)null);
        }
        return new Provisioning.Result(STATUS_OK, "", (String) null);
    }

    public static Provisioning.Result checkExchangeAuth(ExchangeFreeBusyProvider.ServerInfo sinfo, Account acct) throws ServiceException {
    	try {
        	int code = ExchangeFreeBusyProvider.checkAuth(sinfo, acct).getStatusLine().getStatusCode();
        	switch (code) {
        	case 400:
        	case 404:
                return new Provisioning.Result(STATUS_BAD_URL, "", null);
        	case 401:
        	case 403:
                return new Provisioning.Result(STATUS_AUTH_FAILED, "", null);
        	}
    	} catch (IOException | HttpException e) {
    	    return toResult(e, "");
        }
    	return new Provisioning.Result(STATUS_OK, "", null);
    }
    
    public static Provisioning.Result checkExchangeEWSAuth(ExchangeFreeBusyProvider.ServerInfo sinfo, Account acct) {
    	try {
        	int code = ExchangeEWSFreeBusyProvider.checkAuth(sinfo, acct);
        	switch (code) {
        	case 400:
        	case 404:
                return new Provisioning.Result(STATUS_BAD_URL, "", null);
        	case 401:
        	case 403:
                return new Provisioning.Result(STATUS_AUTH_FAILED, "", null);
        	}
    	} catch (Exception e) {
    	    return toResult(e, "");
        }
    	return new Provisioning.Result(STATUS_OK, "", null);
    }    
    
    public static Provisioning.Result toResult(Exception e, String dn) {
        if (e instanceof UnknownHostException) {
            return new Provisioning.Result(STATUS_UNKNOWN_HOST, e, dn);
        } else if (e instanceof ConnectException) {
            return new Provisioning.Result(STATUS_CONNECTION_REFUSED, e, dn);
        } else if (e instanceof SSLHandshakeException) {
            return new Provisioning.Result(STATUS_SSL_HANDSHAKE_FAILURE, e, dn);
        } else {
            return new Provisioning.Result(STATUS_COMMUNICATION_FAILURE, e, dn);
        }
    }
    
    private static void testCheckHostnameResolve() {
        Provisioning.Result r = checkHostnameResolve("slapshot");
        System.out.println(r);
    }
   
    public static void main(String args[]) {
        testCheckHostnameResolve();
        //testCheckGal();       
    }
}
