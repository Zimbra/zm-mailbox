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
package com.zimbra.cs.zclient;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.account.type.Signature;
import com.zimbra.soap.account.type.SignatureContent;

public class ZSignature implements Comparable<ZSignature>, ToZJSONObject {

    private static final String DEFAULT_CONTENT_TYPE = "text/plain";
    
    private Signature data;

    public ZSignature(Signature sig) {
        data = sig;
    }

    public ZSignature(String id, String name, String value, String type) {
        data = new Signature(id, name, value, type);
    }
    
    public ZSignature(String id, String name, String value) {
        this(id, name, value, DEFAULT_CONTENT_TYPE);
    }

    public ZSignature(String name, String value) {
        this(null, name, value, DEFAULT_CONTENT_TYPE);
    }
    
    public Signature getData() {
        return new Signature(data);
    }
    
    public String getName() {
        return data.getName();
    }

    public String getId() {
        return data.getId();
    }    

    public String getValue() {
        SignatureContent content = getFirstContent();
        if (content == null) {
            return null;
        }
        return content.getContent();
    }
    
    public String getType() {
        SignatureContent content = getFirstContent();
        if (content == null) {
            return DEFAULT_CONTENT_TYPE;
        }
        return SystemUtil.getValue(content.getContentType(), DEFAULT_CONTENT_TYPE);
    }
    
    private SignatureContent getFirstContent() {
        if (data.getContent().isEmpty()) {
            return null;
        }
        return data.getContent().get(0);
    }

    public void setType(String type) {
        SignatureContent content = getFirstContent();
        if (content == null) {
            content = new SignatureContent(null, type);
            data.addContent(content);
        }
        content.setContentType(type);
    }
    
    public Map<String, Object> getAttrs() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraSignatureId, data.getId());
        attrs.put(Provisioning.A_zimbraSignatureName, data.getName());
        
        String type = null;
        String value = null;
        SignatureContent content = getFirstContent();
        if (content != null) {
            type = content.getContentType();
            value = content.getContent();
        }
        attrs.put(com.zimbra.cs.account.Signature.mimeTypeToAttrName(type), value);
        return attrs;
    }
    
    public Element toElement(Element parent) {
        Element sig = parent.addElement(AccountConstants.E_SIGNATURE);
        sig.addAttribute(AccountConstants.A_NAME, getName());
        if (getId() != null) sig.addAttribute(AccountConstants.A_ID, getId());
        if (getName() != null) sig.addAttribute(AccountConstants.A_NAME, getName());
        if (getValue() != null) {
            Element content = sig.addElement(AccountConstants.E_CONTENT);
            content.addAttribute(AccountConstants.A_TYPE, getType());
            content.setText(getValue());
        }
        return sig;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("name", getName());
        zjo.put("id", getId());
        zjo.put("value", getValue());
        zjo.put("type", getType());
        return zjo;
    }

    public String toString() {
        return String.format("[ZSignature %s]", getName());
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public int compareTo(ZSignature other) {
        return getName().compareTo(other.getName());
    }
}
