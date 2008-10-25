/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.wildfire.forms.FormField;
import org.jivesoftware.wildfire.forms.spi.XDataFormImpl;
import org.xmpp.packet.IQ;

/**
 * 
 */
public class IMConferenceRoom {
    private boolean register_form_supported = false;
    private boolean roomconfig_form_supported = false;
    private boolean roominfo_form_supported = false;
    private boolean hidden = false;
    private boolean membersonly = false;
    private boolean moderated = false;
    private boolean nonanonymous = false;
    private boolean open = false;
    private boolean passwordprotected = false;
    private boolean persistent = false;
    private boolean isPublic = false;
    private boolean semianonymous = false;
    private boolean temporary = false;
    private boolean unmoderated = false;
    private boolean unsecured = false;
    private int numOccupants = 0;
    private String name;
    private String jid;
    
    public boolean isRegister_form_supported() {
        return register_form_supported;
    }
    public boolean isRoomconfig_form_supported() {
        return roomconfig_form_supported;
    }
    public boolean isRoominfo_form_supported() {
        return roominfo_form_supported;
    }
    public boolean isHidden() {
        return hidden;
    }
    public boolean isMembersonly() {
        return membersonly;
    }
    public boolean isModerated() {
        return moderated;
    }
    public boolean isNonanonymous() {
        return nonanonymous;
    }
    public boolean isOpen() {
        return open;
    }
    public boolean isPasswordprotected() {
        return passwordprotected;
    }
    public boolean isPersistent() {
        return persistent;
    }
    public boolean isIspublic() {
        return isPublic;
    }
    public boolean isSemianonymous() {
        return semianonymous;
    }
    public boolean isTemporary() {
        return temporary;
    }
    public boolean isUnmoderated() {
        return unmoderated;
    }
    public boolean isUnsecured() {
        return unsecured;
    }
    public int getNumOccupants() {
        return numOccupants;
    }
    public String getName() { 
        return name;
    }
    public String getJid() {
        return jid;
    }
    
    public String toString() {
        return "Room("+name+" "+jid+" with "+numOccupants+" occupants)";
    }

    public static IMConferenceRoom parseRoomInfo(IQ iq) {
        org.dom4j.Element child = iq.getChildElement();
        
        IMConferenceRoom room = new IMConferenceRoom();
        org.dom4j.Element identity = child.element("identity");
        room.name = identity.attributeValue("name");
        room.jid = iq.getFrom().toBareJID();
        
        for (Iterator<org.dom4j.Element> iter = (Iterator<org.dom4j.Element>)child.elementIterator("feature");iter.hasNext();) {
            org.dom4j.Element item = iter.next();
            String var = item.attributeValue("var");
            if (var.equals("http://jabber.org/protocol/muc#register")) 
                room.register_form_supported = true;
            else if (var.equals("http://jabber.org/protocol/muc#roomconfig"))
                room.roomconfig_form_supported = true;
            else if (var.equals("http://jabber.org/protocol/muc#roominfo"))
                room.roominfo_form_supported = true;
            else if (var.equals("muc_hidden")) 
                room.hidden = true;
            else if (var.equals("muc_membersonly")) 
                room.membersonly = true;
            else if (var.equals("muc_moderated")) 
                room.moderated = true;
            else if (var.equals("muc_nonanonymous"))
                room.nonanonymous = true;
            else if (var.equals("muc_open"))
                room.open = true;
            else if (var.equals("muc_passwordprotected"))
                room.passwordprotected = true;
            else if (var.equals("muc_persistent"))
                room.persistent = true;
            else if (var.equals("muc_public"))
                room.isPublic = true;
            else if (var.equals("muc_semianonymous"))
                room.semianonymous = true;
            else if (var.equals("muc_temporary"))
                room.temporary = true;
            else if (var.equals("muc_unmoderated"))
                room.unmoderated = true;
            else if (var.equals("muc_unsecured"))
                room.unsecured = true;
        }
        for (Iterator<org.dom4j.Element> iter = (Iterator<org.dom4j.Element>)child.elementIterator("x");iter.hasNext();) {
            org.dom4j.Element x = iter.next();
            if ("jabber:x:data".equals(x.getNamespaceURI())) {
                XDataFormImpl form = new XDataFormImpl();
                form.parse(x);
                for (Iterator fieldIter = form.getFields(); fieldIter.hasNext();) {
                    FormField field = (FormField)fieldIter.next();
                    if ("muc#roominfo_occupants".equals(field.getVariable())) {
                        Iterator<String> valueIter = field.getValues();
                        if (valueIter.hasNext()) {
                            room.numOccupants = Integer.parseInt(valueIter.next());
                        }
                    }
                }
            }
        }
        
        return room;
    }
}
