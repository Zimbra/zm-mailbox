/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
