/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.script;

public class ProvisioningOptions {

    private String mUsername;
    private String mPassword;
    private String mSoapURI;
    private String mUserAgent;
    private String mUserAgentVersion;

    public String getUsername() { return mUsername; }
    public String getPassword() { return mPassword; }
    public String getSoapURI() { return mSoapURI; }
    public String getUserAgent() { return mUserAgent; }
    public String getUserAgentVersion() { return mUserAgentVersion; }
    
    public ProvisioningOptions setUsername(String username) {
        mUsername = username;
        return this;
    }
    
    public ProvisioningOptions setPassword(String password) {
        mPassword = password;
        return this;
    }
    
    public ProvisioningOptions setSoapURI(String soapURI) {
        mSoapURI = soapURI;
        return this;
    }
    
    public ProvisioningOptions setUserAgent(String userAgent) {
        mUserAgent = userAgent;
        return this;
    }
    
    public ProvisioningOptions setUserAgentVersion(String version) {
        mUserAgentVersion = version;
        return this;
    }
}
