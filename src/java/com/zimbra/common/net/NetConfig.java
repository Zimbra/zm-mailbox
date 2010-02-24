/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.net;

import com.zimbra.common.localconfig.LC;

/**
 * Network configuration settings.
 */
public final class NetConfig {
    private boolean socksEnabled;
    private boolean allowUntrustedCerts;
    private boolean allowMismatchedCerts;
    private boolean allowAcceptUntrustedCerts;
    private boolean useNativeProxySelector;

    private static NetConfig INSTANCE = new NetConfig();

    public static NetConfig getInstance() {
        return INSTANCE;
    }

    private NetConfig() {
        socksEnabled = LC.socks_enabled.booleanValue();
        allowUntrustedCerts = LC.ssl_allow_untrusted_certs.booleanValue();
        allowMismatchedCerts = LC.ssl_allow_mismatched_certs.booleanValue();
        allowAcceptUntrustedCerts = LC.ssl_allow_accept_untrusted_certs.booleanValue();
    }

    public boolean isSocksEnabled() {
        return socksEnabled;
    }

    public NetConfig setSocksEnabled(boolean socksEnabled) {
        this.socksEnabled = socksEnabled;
        return this;
    }

    public boolean isAllowUntrustedCerts() {
        return allowUntrustedCerts;
    }

    public NetConfig setAllowUntrustedCerts(boolean allowUntrustedCerts) {
        this.allowUntrustedCerts = allowUntrustedCerts;
        return this;
    }

    public boolean isAllowMismatchedCerts() {
        return allowMismatchedCerts;
    }

    public NetConfig setAllowMismatchedCerts(boolean allowMismatchedCerts) {
        this.allowMismatchedCerts = allowMismatchedCerts;
        return this;
    }

    public boolean isAllowAcceptUntrustedCerts() {
        return allowAcceptUntrustedCerts;
    }

    public NetConfig setAllowAcceptUntrustedCerts(boolean allowAcceptUntrustedCerts) {
        this.allowAcceptUntrustedCerts = allowAcceptUntrustedCerts;
        return this;
    }

    public boolean isUseNativeProxySelector() {
        return useNativeProxySelector;
    }

    public NetConfig setUseNativeProxySelector(boolean useNativeProxySelector) {
        this.useNativeProxySelector = useNativeProxySelector;
        return this;
    }
}
