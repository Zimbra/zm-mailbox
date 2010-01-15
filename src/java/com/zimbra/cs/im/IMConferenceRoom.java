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
package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jivesoftware.wildfire.forms.FormField;
import org.jivesoftware.wildfire.forms.spi.XDataFormImpl;
import org.xmpp.packet.IQ;

import com.zimbra.common.soap.Element;

/**
 * 
 */
public class IMConferenceRoom {
    private enum ConfigType {
        bool,
        string;
    }
    
    private enum Cardinality {
        single,
        multi;
    }
    
    public enum RoomConfig {
        open("muc_open", null, ConfigType.bool),

        // READ-ONLY
        creationdate("x-muc#roominfo_creationdate", null, ConfigType.string),
        
        publicroom("muc_public", "muc#roomconfig_publicroom", ConfigType.bool),

        moderated("muc_moderated", "muc#roomconfig_moderatedroom", ConfigType.bool),
        
        persistent("muc_persistent", "muc#roomconfig_persistentroom", ConfigType.bool),
        
        membersonly("muc_membersonly", "muc#roomconfig_membersonly", ConfigType.bool),
        
        noanonymous("muc_nonanonymous", null, ConfigType.bool),
        semianonymous("muc_semianonymous", null, ConfigType.bool),
        
        passwordprotect("muc_passwordprotected", "muc#roomconfig_passwordprotectedroom", ConfigType.bool),
        password(null, "muc#roomconfig_roomsecret", ConfigType.string), // extended
        
        numoccupants("muc#roominfo_occupants", null, ConfigType.string), // READ-ONLY, extended
        
        maxusers("muc#roominfo_maxusers", "muc#roomconfig_maxusers", ConfigType.string), // extended
        
        longname(null, "muc#roomconfig_roomname", ConfigType.string), // extended
        description("muc#roominfo_description", "muc#roomconfig_roomdesc", ConfigType.string),
        
        // subject is settable by sending a message to the room with a <subject> in it
        subject("muc#roominfo_subject", null, ConfigType.string),
        // are Occupants allowed to change the suject?
        subjectmodifyable("muc#roominfo_changesubject", "muc#roomconfig_changesubject", ConfigType.string),

        allowinvites(null, "muc#roomconfig_allowinvites", ConfigType.bool),
        
        presencebroadcast(null, "muc#roomconfig_presencebroadcast", ConfigType.string, Cardinality.multi),


        // url for logging
        logsurl("muc#roominfo_logs", null, ConfigType.string),

        // JIDs with owner affiliation
        owners(null, "muc#roomconfig_roomowners", ConfigType.string, Cardinality.multi), //extended
        // JIDs w/ admin affiliation
        admins(null, "muc#roomconfig_roomadmins", ConfigType.string, Cardinality.multi),
        
        // Roles/Affiliations allowed to get real JIDs from the room
        whois(null, "muc#roomconfig_whois", ConfigType.string, Cardinality.multi),
        
        enablelogging(null, "muc#roomconfig_enablelogging", ConfigType.string),
        reservednick(null, "x-muc#roomconfig_reservednick", ConfigType.bool),
        canchangenick(null, "x-muc#roomconfig_canchangenick", ConfigType.bool),
        allowregister(null, "x-muc#roomconfig_registration", ConfigType.bool),
        

        /////////////////////////////////////
        // NOT SURE IF THESE ARE WORKING:
        ////////////////////////////////////
        getmemberlist(null, "muc#roomconfig_getmemberlist", ConfigType.string, Cardinality.multi),
        
        // contact info for room
        contactid("muc#roominfo_contactjid", null, ConfigType.string),
        ;
        
        
        RoomConfig(String infoName, String configName, ConfigType configType) {
            this(infoName, configName, configType, Cardinality.single);
        }
        RoomConfig(String infoName, String configName, ConfigType configType, Cardinality cardinality) {
            this.infoName = infoName;
            this.configName = configName;
            this.configType = configType;
            this.cardinality = cardinality; 
        }
        
        private static Map<String, RoomConfig> infoToConfigMap;
        private static Map<String, RoomConfig> configToConfigMap;
        
        static {
            infoToConfigMap = new HashMap<String, RoomConfig>();
            for (RoomConfig config : RoomConfig.values()) {
                if (config.getRoominfoName() != null)
                    infoToConfigMap.put(config.getRoominfoName(), config);
            }

            configToConfigMap = new HashMap<String, RoomConfig>();
            for (RoomConfig config : RoomConfig.values()) {
                if (config.getRoomconfigName() != null)
                    configToConfigMap.put(config.getRoomconfigName(), config);
            }
            
        }
        public static RoomConfig lookupFromInfoName(String xmppName) {
            return infoToConfigMap.get(xmppName);
        }
        public static RoomConfig lookupFromConfigName(String xmppName) {
            return configToConfigMap.get(xmppName);
        }
        
        public String getRoominfoName() { return infoName; }
        public String getRoomconfigName() { return configName; }
        public boolean isMulti() { return cardinality == Cardinality.multi; }
        public ConfigType getConfigType() { return configType; }
        
        private String infoName; // the name of the value when parsed from the roominfo, NULL if none
        private String configName; // the name of the value used to SET in roomconfig, NULL if none
        private ConfigType configType;
        private Cardinality cardinality;
    }
    
    private String threadId;
    private IMChat chat;
    private String addr; 
    private Map<RoomConfig, Object> data = new HashMap<RoomConfig, Object>();
    private boolean haveOwnerConfig = false;
    
    /**
     * Express this room configuration as XML suitable for sending as SOAP/JSON to the client
     * @param parent
     * @return
     */
    public Element toXML(Element parent) {
        Element toRet = parent.addElement("room");
        
        if (chat != null) {
            toRet.addAttribute("threadId", chat.getThreadId());
            toRet.addAttribute("addr", chat.getDestAddr());
        } else {
            toRet.addAttribute("addr", addr);
        }
        if (haveOwnerConfig)
            toRet.addAttribute("owner", true);
        
        for (Map.Entry<RoomConfig, Object> entry : data.entrySet()) {
            Element var = toRet.addElement("var");
            RoomConfig config = entry.getKey();
            
            var.addAttribute("name", config.name());
            if (!config.isMulti()) {
                String value = (String)entry.getValue();
                    var.setText(value);
            } else {
                var.addAttribute("multi", true);
                List<String> values = (List<String>)entry.getValue();
                for (String value : values) {
                    Element valueElt = var.addElement("value");
                    valueElt.setText(value);
                }
            }
        }
        return parent;
    }
    
    public String toString() {
        return "Room("+threadId+" "+(chat != null ? chat.getDestAddr() : addr)+")";
    }
    
    IMConferenceRoom(IMChat chat) {
        this.chat = chat;
        addr = null;
    }
    
    IMConferenceRoom(String addr) {
        this.addr = addr;
        chat = null;
    }
    
    
    private void parseNonExtended(org.dom4j.Element item) {
        String var = item.attributeValue("var");
        RoomConfig config = RoomConfig.lookupFromInfoName(var);
        if (config == null) {
            // special-case public, temporary and unmoderated
            if ("muc_hidden".equals(var)) {
                data.put(RoomConfig.publicroom, "0");
            } else if ("muc_unmoderated".equals(var)) {
                data.put(RoomConfig.moderated, "0");
            } else if ("muc_temporary".equals(var)) {
                data.put(RoomConfig.persistent, "0");
            }
        } else {
            data.put(config, "1");
        }
    }
    
    private void parseExtended(String name, List<String> values) {
        RoomConfig config = RoomConfig.lookupFromInfoName(name);
        if (config != null && values.size() > 0) {
            if (config.isMulti())
                data.put(config, values);
            else
                data.put(config, values.get(0));
        }
    }
    
    private void parseExtendedConfig(String name, List<String> values) {
        RoomConfig config = RoomConfig.lookupFromConfigName(name);
        if (config != null && values.size() > 0) {
            if (config.isMulti())
                data.put(config, values);
            else
                data.put(config, values.get(0));
        }
    }
    
    
    void parseConfigurationForm(org.dom4j.Element x) {
        if ("jabber:x:data".equals(x.getNamespaceURI())) {
            XDataFormImpl form = new XDataFormImpl();
            form.parse(x);
            for (Iterator fieldIter = form.getFields(); fieldIter.hasNext();) {
                FormField field = (FormField)fieldIter.next();
                List<String> values = new ArrayList<String>();
                for (Iterator<String> valueIter = field.getValues(); valueIter.hasNext();) 
                    values.add(valueIter.next());
                parseExtendedConfig(field.getVariable(), values);
            }
        }
        haveOwnerConfig = true;
    }
    
    public static IMConferenceRoom emptyRoom(IMChat chat) {
        return new IMConferenceRoom(chat);
    }
    
    public static IMConferenceRoom emptyRoom(String destAddr) {
        return new IMConferenceRoom(destAddr);
    }

    @SuppressWarnings("unchecked")
    public static IMConferenceRoom parseRoomInfo(IMChat chat, String addr, IQ iq) {
        assert(chat == null || addr == null);
        org.dom4j.Element child = iq.getChildElement();
        if (!"http://jabber.org/protocol/disco#info".equals(child.getNamespaceURI()))
            throw new IllegalArgumentException("Expecting a disco#info, got: "+iq.toXML());
        
        IMConferenceRoom room;
        if (chat != null)
            room = new IMConferenceRoom(chat);
        else
            room = new IMConferenceRoom(addr);
        
        for (Iterator<org.dom4j.Element> iter = (Iterator<org.dom4j.Element>)child.elementIterator("feature");iter.hasNext();) {
            org.dom4j.Element item = iter.next();
            room.parseNonExtended(item);
        }
            
        for (Iterator<org.dom4j.Element> iter = (Iterator<org.dom4j.Element>)child.elementIterator("x");iter.hasNext();) {
            org.dom4j.Element x = iter.next();
            if ("jabber:x:data".equals(x.getNamespaceURI())) {
                XDataFormImpl form = new XDataFormImpl();
                form.parse(x);
                for (Iterator fieldIter = form.getFields(); fieldIter.hasNext();) {
                    FormField field = (FormField)fieldIter.next();
                    List<String> values = new ArrayList<String>();
                    for (Iterator<String> valueIter = field.getValues(); valueIter.hasNext();) 
                        values.add(valueIter.next());
                    room.parseExtended(field.getVariable(), values);
                }
            }
        }
        
        return room;
    }
    
    @SuppressWarnings("unchecked")
    static IQ generateConfigIQ(IQ iq, Map<String, Object> data) {
        org.dom4j.Element query = iq.setChildElement("query", "http://jabber.org/protocol/muc#owner");
        org.dom4j.Element x = query.addElement("x", "jabber:x:data");
        x.addAttribute("type", "submit");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String var = entry.getKey();
            RoomConfig config = RoomConfig.valueOf(var);
            org.dom4j.Element fieldElt = x.addElement("field");
            
            fieldElt.addAttribute("var", config.getRoomconfigName());
            
            if (entry.getValue() instanceof String) {
                org.dom4j.Element valueElt = fieldElt.addElement("value");
                String value = (String)entry.getValue();
                if (config.getConfigType() == ConfigType.bool) {
                    if ("true".equalsIgnoreCase(value))
                        value = "1";
                    else if ("false".equalsIgnoreCase(value)) 
                        value = "0";
                    
                }
                valueElt.setText(value);
            } else {
                List<String> values = (List<String>)(entry.getValue());
                for (String value : values) {
                    org.dom4j.Element valueElt = fieldElt.addElement("value");
                    valueElt.setText(value);
                }
            }
        }
        
        return iq;
    }
}
