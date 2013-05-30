/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra, Inc.
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
package com.zimbra.cs.index.elasticsearch;

import java.io.IOException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;

public class ElasticSearchConnector {
    private int statusCode = -1;
    private String body = null;

    ElasticSearchConnector () {
    }

    public int executeMethod(HttpMethod method) throws HttpException, IOException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        String reqBody = "";
        if (ZimbraLog.index.isTraceEnabled() && method instanceof EntityEnclosingMethod) {
            EntityEnclosingMethod eem = (EntityEnclosingMethod) method;
            RequestEntity re = eem.getRequestEntity();
            if (re instanceof StringRequestEntity) {
                StringRequestEntity sre = (StringRequestEntity) re;
                reqBody = Strings.nullToEmpty(sre.getContent());
                if (reqBody.length() > 0) {
                    reqBody = String.format("\nREQUEST BODY=%s", reqBody);
                }
            }
        }
        statusCode = client.executeMethod(method);
        body = method.getResponseBodyAsString();
        ZimbraLog.index.trace("ElasticSearch request:%s %s - statusCode=%d%s\nRESPONSE BODY=%s",
                method.getName(), method.getURI(), statusCode, reqBody, body);
        return statusCode;
    }

    private JSONObject getParentObject(String[] path) throws JSONException {
        JSONObject currObj = new JSONObject(body);
        for (int ndx = 0;ndx < path.length - 1; ndx++) {
            currObj = currObj.getJSONObject(path[ndx]);
            if (currObj == null) {
                ZimbraLog.index.error("Problem locating '%s' in JSON response body=%s",
                    Joiner.on("/").skipNulls().join(path), body);
                return null;
            }
        }
        return currObj;
    }

    public JSONObject getJSONBody() throws JSONException {
        return new JSONObject(body);
    }

    public String getStringAtJsonPath(String[] path) {
        try {
            JSONObject parent = getParentObject(path);
            return (parent == null) ? null : parent.getString(path[path.length - 1]);
        } catch (JSONException e) {
            ZimbraLog.index.error("Problem locating '%s' in JSON response body=%s",
                    Joiner.on("/").skipNulls().join(path), body);
            return null;
        }
    }

    public boolean getBooleanAtJsonPath(String[] path, boolean defaultValue) {
        try {
            JSONObject parent = getParentObject(path);
            return (parent == null) ? defaultValue : parent.getBoolean(path[path.length - 1]);
        } catch (JSONException e) {
            ZimbraLog.index.error("Problem locating '%s' in JSON response body=%s",
                    Joiner.on("/").skipNulls().join(path), body);
            return defaultValue;
        }
    }

    public int getIntAtJsonPath(String[] path, int defaultValue) {
        try {
            JSONObject parent = getParentObject(path);
            return (parent == null) ? defaultValue : parent.getInt(path[path.length - 1]);
        } catch (JSONException e) {
            ZimbraLog.index.error("Problem locating '%s' in JSON response body=%s",
                    Joiner.on("/").skipNulls().join(path), body);
            return defaultValue;
        }
    }

    public JSONObject getObjectAtJsonPath(String[] path) {
        try {
            JSONObject parent = getParentObject(path);
            return (parent == null) ? null : parent.getJSONObject(path[path.length - 1]);
        } catch (JSONException e) {
            ZimbraLog.index.error("Problem locating '%s' in JSON response body=%s",
                    Joiner.on("/").skipNulls().join(path), body);
            return null;
        }
    }

    public JSONArray getArrayAtJsonPath(String[] path) {
        try {
            JSONObject parent = getParentObject(path);
            return (parent == null) ? null : parent.getJSONArray(path[path.length - 1]);
        } catch (JSONException e) {
            ZimbraLog.index.error("Problem locating '%s' in JSON response body=%s",
                    Joiner.on("/").skipNulls().join(path), body);
            return null;
        }
    }
}
