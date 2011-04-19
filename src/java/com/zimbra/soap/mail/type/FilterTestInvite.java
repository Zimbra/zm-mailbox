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

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class FilterTestInvite extends FilterTestInfo {

    @XmlElement(name=MailConstants.E_METHOD, required=false)
    private List<String> methods = Lists.newArrayList();

    public FilterTestInvite() {
    }

    public void setMethods(Iterable <String> methods) {
        this.methods.clear();
        if (methods != null) {
            Iterables.addAll(this.methods,methods);
        }
    }

    public FilterTestInvite addMethod(String method) {
        this.methods.add(method);
        return this;
    }

    public List<String> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("methods", methods)
            .toString();
    }
}
