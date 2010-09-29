/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.zclient;

import com.zimbra.common.util.ExceptionToString;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ZJSONObject {

    JSONObject mJO;

    public ZJSONObject() {
        mJO = new JSONObject();
    }

    public JSONObject getJSONObject() {
        return mJO;
    }

    public ZJSONObject put(String key, ToZJSONObject value) throws JSONException {
        if (value != null)
            mJO.put(key, value.toZJSONObject().getJSONObject());
        return this;
    }

    public ZJSONObject put(String key, JSONObject value) throws JSONException {
        if (value != null)
            mJO.put(key, value);
        return this;
    }

    public ZJSONObject put(String key, ZJSONObject value) throws JSONException {
        if (value != null)
            mJO.put(key, value.getJSONObject());
        return this;
    }

    public ZJSONObject put(String key, String value) throws JSONException {
        mJO.put(key, value);
        return this;
    }

    public ZJSONObject put(String key, long value) throws JSONException {
        mJO.put(key, value);
        return this;
    }

    public ZJSONObject put(String key, boolean value) throws JSONException {
        mJO.put(key, value);
        return this;
    }

    public ZJSONObject put(String key, float value) throws JSONException {
        mJO.put(key, value);
        return this;
    }

    public ZJSONObject put(String key, double value) throws JSONException {
        mJO.put(key, value);
        return this;
    }

    public ZJSONObject putMap(String key, Map<String,String> attrs) throws JSONException {
        JSONObject ja = new JSONObject();
        mJO.put(key, ja);
        if (attrs != null) {
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                ja.put(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    public ZJSONObject put(String key, List<? extends ToZJSONObject> items) throws JSONException {
        JSONArray ja = new JSONArray();
        mJO.put(key, ja);
        if (items != null) {
            for (ToZJSONObject item : items) {
                ja.put(item.toZJSONObject().getJSONObject());
            }
        }
        return this;
    }

    public ZJSONObject put(String key, String[] items) throws JSONException {
        JSONArray ja = new JSONArray();
        mJO.put(key, ja);
        if (items != null) {
            for (String item : items) {
                ja.put(item);
            }
        }
        return this;
    }

    public ZJSONObject putMapList(String key, Map<String, List<String>> attrs) throws JSONException {
        JSONObject obj = new JSONObject();
        mJO.put(key, obj);
        if (attrs != null) {
            for (Map.Entry<String, List<String>> entry : attrs.entrySet()) {
                JSONArray ja = new JSONArray();
                obj.put(key, ja);
                if (entry.getValue() != null) {
                    for (String item : entry.getValue()) {
                        ja.put(item);
                    }
                }
            }
        }
        return this;
    }

    public ZJSONObject putList(String key, List<String> items) throws JSONException {
        JSONArray ja = new JSONArray();
        mJO.put(key, ja);
        if (items != null) {
            for (String item : items) {
                ja.put(item);
            }
        }
        return this;
    }
    /*
    public ZJSONObject putListObj(String key, List<Object> items) throws JSONException {
        JSONArray ja = new JSONArray();
        mJO.put(key, ja);
        if (items != null) {
            for (Object item : items) {
                ja.put(item.toString());
            }
        }
        return this;
    }
    */

    public String toString() {
        try {
            return mJO.toString(5);
        } catch (JSONException e) {
            return ExceptionToString.ToString(e);
        }
    }

    public static String toString(ToZJSONObject zjo) {
        try {
            return zjo.toZJSONObject().toString();
        } catch (JSONException e) {
            return ExceptionToString.ToString(e);
        }
    }

}
