/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.soap.account.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Joiner;
import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.account.type.InfoSection;

/**
 * <GetInfoRequest [sections="mbox,prefs,attrs,zimlets,props,idents,sigs,dsrcs,children"]/>
 */
@XmlRootElement(name="GetInfoRequest")
public class GetInfoRequest {
    private static Joiner COMMA_JOINER = Joiner.on(",");
    
    private List<InfoSection> sections = new ArrayList<InfoSection>();
    
    public GetInfoRequest() {
    }
    
    public GetInfoRequest(Iterable<InfoSection> sections) {
        addSections(sections);
    }
    
    @XmlAttribute public String getSections() {
        return COMMA_JOINER.join(sections);
    }
    
    public GetInfoRequest setSections(String sections)
    throws ServiceException {
        this.sections.clear();
        if (sections != null) {
            addSections(sections.split(","));
        }
        return this;
    }
    
    public GetInfoRequest addSection(String sectionName)
    throws ServiceException {
        addSection(InfoSection.fromString(sectionName));
        return this;
    }
    
    public GetInfoRequest addSection(InfoSection section) {
        sections.add(section);
        return this;
    }
    
    public GetInfoRequest addSections(String ... sectionNames)
    throws ServiceException {
        for (String sectionName : sectionNames) {
            addSection(sectionName);
        }
        return this;
    }

    public GetInfoRequest addSections(Iterable<InfoSection> sections) {
        if (sections != null) {
            for (InfoSection section : sections) {
                addSection(section);
            }
        }
        return this;
    }
}
