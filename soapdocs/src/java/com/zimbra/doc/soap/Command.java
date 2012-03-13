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

package com.zimbra.doc.soap;

import java.util.*;

import com.google.common.base.Strings;

public class Command implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /* Used to separate out the first part of the description for use as a short summary */
    private static final String REGEX_DESCRIPTION_SHORT_DELIM =
            "<br|<BR|<p>|<P>|<table|<TABLE|<ul|<UL|<ol|<OL|<pre>|<PRE>";

    private XmlElementDescription request = new XmlElementDescription();
    private XmlElementDescription response = new XmlElementDescription();

    private List<XmlElementDescription> allElements = new LinkedList<XmlElementDescription>();

    private Service service = null;
    private String name = null;
    private String namespace = null;

    private String description = null;
    private Boolean networkEdition = false;
    private String deprecation = null;

    Command(Service service, String name, String namespace) {
        this.service = service;
        this.name = name;
        this.namespace = namespace;
    }

    /**
     * Checks if the command is loaded.
     *
     * @return    <code>true</code> if the command is loaded
     */
    public boolean getLoaded() {
        if (this.response == null || this.request == null)
            return false;
        return true;
    }

    public XmlElementDescription getResponse() {
        return this.response;
    }

    public XmlElementDescription getRequest() {
        return this.request;
    }

    public String getDescription() {
        return Strings.nullToEmpty(this.description);
    }

    public Boolean hasDeprecationDescription() { return ! Strings.isNullOrEmpty(deprecation); }

    public String getDeprecation() {
        return Strings.nullToEmpty(this.deprecation);
    }
    /**
     * Gets the short description. This is the first line of the description.
     */
    public String getShortDescription() {
        String desc = getDescription();
        StringBuilder sb = new StringBuilder();

        String[] tokens = desc.split(REGEX_DESCRIPTION_SHORT_DELIM);
        if (tokens != null && tokens.length > 0 && tokens[0].length() > 0) {
            String retDesc = tokens[0].trim();
            sb.append(retDesc);
            if (!retDesc.endsWith(".")) {
                sb.append(".");
            }
        }
        if (networkEdition) {
            sb.append(" <font color=\"#dd0000\">[NetworkEdition only]</font>");
        }
        if (hasDeprecationDescription()) {
            sb.append("\n<br /><font color=\"#dd0000\">").append(getDeprecation()).append("</font>");
        }
        return sb.toString();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDeprecation(String deprecation) {
        this.deprecation = deprecation;
    }

    public void setRootRequestElement(XmlElementDescription request) {
        if (request != null) {
            this.request = request;
            this.allElements.add(request);
            loadAllElements(request);
        }
    }

    public void setRootResponseElement(XmlElementDescription response) {
        if (response != null) {
            this.response = response;
            this.allElements.add(response);
            loadAllElements(response);
        }
    }

    public void setRootElements(XmlElementDescription request, XmlElementDescription response) {
        setRootRequestElement(request);
        setRootResponseElement(response);
    }

    private void loadAllElements(DescriptionNode root) {
        Iterator it = root.getChildren().iterator();
        while(it.hasNext()) {
            DescriptionNode dn = (DescriptionNode) it.next();
            if (dn instanceof XmlElementDescription) {
                XmlElementDescription e = (XmlElementDescription) dn;
                if (this.allElements.contains(e) == false)
                    this.allElements.add(e);
                loadAllElements(e);
            } else {
                for (DescriptionNode dnc : dn.getChildren()) {
                    loadAllElements(dnc);
                }
            }
        }
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getName() {
        return this.name;
    }

    public String getRequestName() {
        return this.request.getName();
    }

    public String getResponseName() {
        return this.response.getName();
    }

    public Service getService() {
        return this.service;
    }

    public List<XmlElementDescription> getAllElements() {
        return this.allElements;
    }

    public List<XmlElementDescription> getAllSubElements() {
        List<XmlElementDescription> els = new LinkedList<XmlElementDescription>(this.allElements);
        els.remove(this.request);
        els.remove(this.response);
        return els;
    }

    public void setNetworkEdition(boolean networkEdition) { this.networkEdition = networkEdition; }
    public Boolean isNetworkEdition() { return networkEdition; }

    /**
     * Dumps the contents to <code>System.out.println</code>
     */
    public void dump() {
        System.out.println("Dump command...");
        System.out.println(this);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("[command");
        buf.append(";hashCode=");
        buf.append(hashCode());
        buf.append(";name=");
        buf.append(this.getName());
        buf.append(";namespace=");
        buf.append(this.getNamespace());
        buf.append(";requestName=");
        buf.append(this.getRequestName());
        buf.append(";responseName=");
        buf.append(this.getResponseName());
        buf.append(";request=");
        buf.append(this.getRequest());
        buf.append(";response=");
        buf.append(this.getResponse());
        buf.append(";allElements=");
        buf.append(this.getAllElements().size());
        buf.append(";allSubElements=");
        buf.append(this.getAllSubElements().size());
        buf.append("]");

        return    buf.toString();
    }

    public static class CommandComparator implements java.util.Comparator<Command> {

        @Override
        public int compare(Command c1, Command c2) {
            String n1 = c1.getName();
            String n2 = c2.getName();

            return    n1.compareTo(n2);
        }
    }
}
