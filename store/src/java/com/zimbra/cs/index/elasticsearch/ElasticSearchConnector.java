/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index.elasticsearch;

import java.io.IOException;
import java.net.ConnectException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.IndexStoreException;

public class ElasticSearchConnector {
    private int statusCode = -1;
    private String body = null;

    public ElasticSearchConnector () {
    }

    public int executeMethod(HttpRequestBase method) throws IndexStoreException, IOException {
        String reqBody = "";
        if (ZimbraLog.elasticsearch.isTraceEnabled() && method instanceof HttpEntityEnclosingRequestBase) {
            HttpEntityEnclosingRequestBase eem = (HttpEntityEnclosingRequestBase) method;
            HttpEntity re = eem.getEntity();
            if (re instanceof StringEntity) {
                reqBody = Strings.nullToEmpty(EntityUtils.toString(re));
                if (reqBody.length() > 0) {
                    reqBody = String.format("\nREQUEST BODY=%s", reqBody);
                }
            }
        }
        HttpResponse response = null;
        try {
            HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient().build();
            response = client.execute(method);
            statusCode = response.getStatusLine().getStatusCode();
        } catch (ConnectException ce) {
            throw new ZimbraElasticSearchDownException(ce);
        } catch (NoHttpResponseException nhre) {
            // managed to connect to the ElasticSearch service but it either crashed or timed out before
            // we got sent back the response.
            // Could be a temporary problem or a problem with this particular request.
            // In the longer term we need to track failures related to particular items at a higher level and discard
            // them after retrying a number of times.
            throw new ZimbraElasticSearchNoResponseException(nhre);
        }
        body = EntityUtils.toString(response.getEntity());
        ZimbraLog.elasticsearch.trace("ElasticSearch request:%s %s - statusCode=%d%s\nRESPONSE BODY=%s",
                method.getMethod(), method.getURI(), statusCode, reqBody, body);
        return statusCode;
    }

    public class ZimbraElasticSearchDownException extends IndexStoreException {
        private static final long serialVersionUID = -1564956672861500861L;

        public ZimbraElasticSearchDownException(Exception e) {
            super("ElasticSearch service is temporarily unavailable", e);
        }
    }

    public class ZimbraElasticSearchNoResponseException extends IndexStoreException {
        private static final long serialVersionUID = -1564956672861500861L;

        public ZimbraElasticSearchNoResponseException(Exception e) {
            super("No Response from ElasticSearch service", e);
        }
    }

    private JSONObject getParentObject(String[] path) throws JSONException {
        JSONObject currObj = new JSONObject(body);
        for (int ndx = 0;ndx < path.length - 1; ndx++) {
            currObj = currObj.getJSONObject(path[ndx]);
            if (currObj == null) {
                ZimbraLog.elasticsearch.error("Problem locating '%s' in JSON response body=%s",
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
            ZimbraLog.elasticsearch.error("Problem locating '%s' in JSON response body=%s",
                    Joiner.on("/").skipNulls().join(path), body);
            return null;
        }
    }

    public boolean getBooleanAtJsonPath(String[] path, boolean defaultValue) {
        try {
            JSONObject parent = getParentObject(path);
            return (parent == null) ? defaultValue : parent.getBoolean(path[path.length - 1]);
        } catch (JSONException e) {
            ZimbraLog.elasticsearch.error("Problem locating '%s' in JSON response body=%s",
                    Joiner.on("/").skipNulls().join(path), body);
            return defaultValue;
        }
    }

    public int getIntAtJsonPath(String[] path, int defaultValue) {
        try {
            JSONObject parent = getParentObject(path);
            return (parent == null) ? defaultValue : parent.getInt(path[path.length - 1]);
        } catch (JSONException e) {
            ZimbraLog.elasticsearch.error("Problem locating '%s' in JSON response body=%s",
                    Joiner.on("/").skipNulls().join(path), body);
            return defaultValue;
        }
    }

    public JSONObject getObjectAtJsonPath(String[] path) {
        try {
            JSONObject parent = getParentObject(path);
            return (parent == null) ? null : parent.getJSONObject(path[path.length - 1]);
        } catch (JSONException e) {
            ZimbraLog.elasticsearch.error("Problem locating '%s' in JSON response body=%s",
                    Joiner.on("/").skipNulls().join(path), body);
            return null;
        }
    }

    public JSONArray getArrayAtJsonPath(String[] path) {
        try {
            JSONObject parent = getParentObject(path);
            return (parent == null) ? null : parent.getJSONArray(path[path.length - 1]);
        } catch (JSONException e) {
            ZimbraLog.elasticsearch.error("Problem locating '%s' in JSON response body=%s",
                    Joiner.on("/").skipNulls().join(path), body);
            return null;
        }
    }

    public static String actualUrl(String url) {
        if (!ZimbraLog.elasticsearch.isTraceEnabled()) {
            return url;
        }
        if (url.indexOf('?') > -1) {
            return String.format("%s&pretty", url);
        } else {
            return String.format("%s?pretty", url);
        }
    }
}
