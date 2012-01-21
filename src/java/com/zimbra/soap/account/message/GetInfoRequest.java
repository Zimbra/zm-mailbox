/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.InfoSection;

/**
 * <GetInfoRequest [sections="mbox,prefs,attrs,zimlets,props,idents,sigs,dsrcs,children"]/>
 * @zm-api-command-description Get information about an account.
 * @zm-api-request-description By default, GetInfo returns all data; to limit the returned data, specify only the
 *     sections you want in the "sections" attr.
 */
@XmlRootElement(name=AccountConstants.E_GET_INFO_REQUEST)
public class GetInfoRequest {
    private static Joiner COMMA_JOINER = Joiner.on(",");

    private List<InfoSection> sections = new ArrayList<InfoSection>();

    private List<String> rights = new ArrayList<String>();

    public GetInfoRequest() {
    }

    public GetInfoRequest(Iterable<InfoSection> sections) {
        addSections(sections);
    }

    /**
     * @zm-api-field-description Comma separated list of sections to return information about.
     * <br />
     * Sections are: mbox,prefs,attrs,zimlets,props,idents,sigs,dsrcs,children
     */
    @XmlAttribute(name=AccountConstants.A_SECTIONS /* sections */, required=false)
    public String getSections() {
        return COMMA_JOINER.join(sections);
    }

    /**
     * @zm-api-field-description comma-separated-rights
     * @zm-api-field-description Comma separated list of rights to return information about.
     */
    @XmlAttribute(name=AccountConstants.A_RIGHTS) 
    public String getRights() {
        return COMMA_JOINER.join(rights);
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

    public GetInfoRequest setRights(String... rights)
    throws ServiceException {
        this.rights.clear();
        if (rights != null) {
            for (String right : rights) {
                addRight(right);
            }
        }
        return this;
    }

    public GetInfoRequest addRight(String right)
    throws ServiceException {
        rights.add(right);
        return this;
    }
}
