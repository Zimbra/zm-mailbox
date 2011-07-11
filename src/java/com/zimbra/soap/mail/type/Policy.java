/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.Folder.View;

@XmlAccessorType(XmlAccessType.FIELD)
public class Policy {

    @XmlEnum
    public enum Type {
        @XmlEnumValue("user") USER ("user"),
        @XmlEnumValue("system") SYSTEM ("system");

        private static Map<String, View> nameToView = new HashMap<String, View>();

        static {
            for (View v : View.values()) {
                nameToView.put(v.toString(), v);
            }
        }

        private String name;

        private Type(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static Type fromString(String name) {
            if (name.equals("user")) {
                return USER;
            } else if (name.equals("system")){
                return SYSTEM;
            } else {
                throw new IllegalArgumentException("Invalid Type value: " + name);
            }
        }
    };

    @XmlAttribute(name=MailConstants.A_RETENTION_POLICY_TYPE, required=false)
    private Type type;
    
    @XmlAttribute(name=MailConstants.A_ID, required=false)
    private String id;
    
    @XmlAttribute(name=MailConstants.A_NAME, required=false)
    private String name;
    
    @XmlAttribute(name=MailConstants.A_LIFETIME, required=false)
    private String lifetime;
    
    /**
     * No-argument constructor required by JAXB.
     */
    private Policy() {
    }
    
    public Policy(Element e)
    throws ServiceException {
        type = Type.fromString(e.getAttribute(MailConstants.A_RETENTION_POLICY_TYPE));
        if (type == Type.USER) {
            lifetime = e.getAttribute(MailConstants.A_LIFETIME);
        } else {
            id = e.getAttribute(MailConstants.A_ID);
        }
    }
    
    public static Policy newUserPolicy(String lifetime) {
        Policy p = new Policy();
        p.type = Type.USER;
        p.lifetime = lifetime;
        return p;
    }
    
    public static Policy newSystemPolicy(String id) {
        Policy p = new Policy();
        p.type = Type.SYSTEM;
        p.id = id;
        return p;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getLifetime() {
        return lifetime;
    }
}
