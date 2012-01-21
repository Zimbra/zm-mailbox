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

package com.zimbra.soap.account.type;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.soap.json.jackson.KeyAndValueListSerializer;

@XmlAccessorType(XmlAccessType.NONE)
abstract public class AttrsImpl implements Attrs {

    /**
     * @zm-api-field-description Attributes
     */
    @XmlElement(name=AdminConstants.E_A)
    @JsonSerialize(using=KeyAndValueListSerializer.class)
    @JsonProperty("_attrs")
    private List<Attr> attrs = Lists.newArrayList();

    public AttrsImpl() {
        this.setAttrs((Iterable<Attr>) null);
    }

    public AttrsImpl(Iterable<Attr> attrs) {
        this.setAttrs(attrs);
    }

    public AttrsImpl (Map<String, ? extends Object> attrs)
    throws ServiceException {
        this.setAttrs(attrs);
    }

    @Override
    public Attrs setAttrs(Iterable<? extends Attr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs, attrs);
        }
        return this;
    }

    @Override
    public Attrs setAttrs(Map<String, ? extends Object> attrs)
    throws ServiceException {
        this.setAttrs(Attr.fromMap(attrs));
        return this;
    }

    @Override
    public Attrs addAttr(Attr attr) {
        attrs.add(attr);
        return this;
    }

    @Override
    public List<Attr> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }

    @Override
    public Multimap<String, String> getAttrsMultimap() {
        return Attr.toMultimap(attrs);
    }

    /**
     * @param name name of attr to get
     * @return null if unset, or first value in list
     */
    @Override
    public String getFirstMatchingAttr(String name) {
        Collection<String> values = getAttrsMultimap().get(name);
        Iterator<String> iter = values.iterator();
        if (!iter.hasNext()) {
            return null;
        }
        return iter.next();
    }

    @Override
    public Map<String, Object> getAttrsAsOldMultimap() {
        return StringUtil.toOldMultimap(getAttrsMultimap());
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add(AdminConstants.E_A, attrs);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
