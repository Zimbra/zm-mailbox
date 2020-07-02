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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name="Policy", description="A policy")
public class Policy {

    @XmlEnum
    @GraphQLType(name="PolicyType", description="A policy type")
    public enum Type {
        @XmlEnumValue("user") USER ("user"),
        @XmlEnumValue("system") SYSTEM ("system");

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

    /**
     * @zm-api-field-tag retention-policy-type
     * @zm-api-field-description Retention policy type
     */
    @XmlAttribute(name=MailConstants.A_RETENTION_POLICY_TYPE /* type */, required=false)
    @GraphQLQuery(name="type", description="Policy type")
    private Type type;

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    @GraphQLQuery(name="id", description="Policy id")
    private String id;

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    @GraphQLQuery(name="name", description="Policy name")
    private String name;

    /**
     * @zm-api-field-tag policy-duration
     * @zm-api-field-description Duration
     */
    @XmlAttribute(name=MailConstants.A_LIFETIME /* lifetime */, required=false)
    @GraphQLQuery(name="lifetime", description="Policy duration")
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
        final Policy p = new Policy();
        p.type = Type.USER;
        p.lifetime = lifetime;
        return p;
    }

    public static Policy newSystemPolicy(String id) {
        final Policy p = new Policy();
        p.type = Type.SYSTEM;
        p.id = id;
        return p;
    }

    public static Policy newSystemPolicy(String name, String lifetime) {
        return newSystemPolicy(null, name, lifetime);
    }

    public static Policy newSystemPolicy(String id, String name, String lifetime) {
        final Policy p = newSystemPolicy(id);
        p.name = name;
        p.lifetime = lifetime;
        return p;
    }

    @GraphQLQuery(name="type", description="Policy type")
    public Type getType() {
        return type;
    }

    @GraphQLQuery(name="id", description="Policy id")
    public String getId() {
        return id;
    }

    @GraphQLQuery(name="name", description="Policy name")
    public String getName() {
        return name;
    }

    @GraphQLQuery(name="lifetime", description="Policy duration")
    public String getLifetime() {
        return lifetime;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", type)
            .add("id", id)
            .add("name", name)
            .add("lifetimeString", lifetime).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Policy)) {
            return false;
        }
        final Policy other = (Policy) o;
        return Objects.equal(id, other.id) &&
            Objects.equal(name, other.name) &&
            Objects.equal(lifetime, other.lifetime) &&
            Objects.equal(type, other.type);
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
