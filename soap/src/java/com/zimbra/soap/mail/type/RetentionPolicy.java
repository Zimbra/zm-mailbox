/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.type;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlRootElement(name=MailConstants.E_RETENTION_POLICY, namespace=MailConstants.NAMESPACE_STR)
@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name="RetentionPolicy", description="The retention policy")
public class RetentionPolicy {

    /**
     * @zm-api-field-description "Keep" retention policies
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=MailConstants.E_KEEP, required=false)
    @XmlElement(name=MailConstants.E_POLICY, required=false)
    @GraphQLQuery(name="keep", description="`Keep` retention policies")
    private List<Policy> keep = Lists.newArrayList();

    /**
     * @zm-api-field-description "Purge" retention policies
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=MailConstants.E_PURGE, required=false)
    @XmlElement(name=MailConstants.E_POLICY, required=false)
    @GraphQLQuery(name="purge", description="`Purge` retention policies")
    private List<Policy> purge = Lists.newArrayList();

    public RetentionPolicy() {
    }

    public RetentionPolicy(Element e)
    throws ServiceException {
        final Element keepEl = e.getOptionalElement(MailConstants.E_KEEP);
        if (keepEl != null) {
            for (final Element p : keepEl.listElements(MailConstants.E_POLICY)) {
                keep.add(new Policy(p));
            }
        }
        final Element purgeEl = e.getOptionalElement(MailConstants.E_PURGE);
        if (purgeEl != null) {
            for (final Element p : purgeEl.listElements(MailConstants.E_POLICY)) {
                purge.add(new Policy(p));
            }
        }
    }

    public RetentionPolicy(Iterable<Policy> keep, Iterable<Policy> purge) {
        this.keep.clear();
        this.purge.clear();
        if (keep != null) {
            Iterables.addAll(this.keep, keep);
        }
        if (purge != null) {
            Iterables.addAll(this.purge, purge);
        }
    }

    @GraphQLQuery(name="keep", description="`Keep` retention policies")
    public List<Policy> getKeepPolicy() {
        return Collections.unmodifiableList(keep);
    }

    @GraphQLQuery(name="purge", description="`Purge` retention policies")
    public List<Policy> getPurgePolicy() {
        return Collections.unmodifiableList(purge);
    }

    public Policy getPolicyById(String id) {
        for (final Policy p : keep) {
            if (Objects.equal(p.getId(), id)) {
                return p;
            }
        }
        for (final Policy p : purge) {
            if (Objects.equal(p.getId(), id)) {
                return p;
            }
        }
        return null;
    }

    @GraphQLIgnore
    public boolean isSet() {
        return !(keep.isEmpty() && purge.isEmpty());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("keep", keep)
            .add("purge", purge).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RetentionPolicy)) {
            return false;
        }
        final RetentionPolicy other = (RetentionPolicy) o;
        return keep.equals(other.keep) && purge.equals(other.purge);
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
