/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.doc.soap.apidesc;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.common.base.Strings;
import com.zimbra.doc.soap.Command;

public class SoapApiCommand
implements Comparable<SoapApiCommand> {

    private String name;
    private String namespace;
    private String description;
    private String deprecationInformation;
    private boolean networkEditionOnly;
    private SoapApiElement request;
    private SoapApiElement response;

    /* no-argument constructor needed for deserialization */
    @SuppressWarnings("unused")
    private SoapApiCommand() {
    }

    public SoapApiCommand(Command cmd) {
        name = cmd.getName();
        namespace = cmd.getNamespace();
        description = cmd.getDescription();
        request = new SoapApiElement(cmd.getRequest());
        response = new SoapApiElement(cmd.getResponse());
        cmd.getResponse();
        // cmd.getShortDescription();
        deprecationInformation = cmd.getDeprecation();
        if (Strings.isNullOrEmpty(deprecationInformation)) {
            deprecationInformation = null;
        }
        networkEditionOnly = cmd.isNetworkEdition();
    }

    public String getName() { return name; }
    public String getNamespace() { return namespace; }
    public String getDescription() { return description; }
    public String getDeprecationInformation() { return deprecationInformation; }
    public boolean isNetworkEditionOnly() { return networkEditionOnly; }

    @JsonIgnore
    public String getId() {
        StringBuilder id = new StringBuilder();
        if (namespace != null) {
            id.append(namespace).append(":");
        }
        id.append(name);
        return id.toString();
    }

    @JsonIgnore
    public String getDocApiLinkFragment() {
        StringBuilder sb = new StringBuilder();
        sb.append(namespace.replaceFirst("urn:", "")).append('/').append(name).append(".html");
        return sb.toString();
    }

    public SoapApiElement getRequest() { return request; }
    public SoapApiElement getResponse() { return response; }

    @Override
    public int compareTo(SoapApiCommand o) {
        return request.getJaxb().compareTo(o.request.getJaxb());
    }
}
