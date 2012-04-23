/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.store.triton;

import org.apache.commons.httpclient.HttpMethod;

/**
 * String wrapper so Mozy token can be passed around and updated on each request if necessary
 *
 */
public class MozyServerToken {
    private String token;

    public MozyServerToken(String token) {
        super();
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    /**
     * Set token value based on TDS response header contained in HttpMethod
     */
    public void setToken(HttpMethod method) {
        this.token = method.getResponseHeader(TritonHeaders.SERVER_TOKEN).getValue();
    }
}
