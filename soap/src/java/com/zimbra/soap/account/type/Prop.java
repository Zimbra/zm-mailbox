/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.account.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_PROP, description="Proerty")
public class Prop {

    /**
     * @zm-api-field-tag prop-zimlet-name
     * @zm-api-field-description Zimlet name
     */
    @XmlAttribute(name=AccountConstants.A_ZIMLET /* zimlet */, required=true)
    private String zimlet;

    /**
     * @zm-api-field-tag prop-name
     * @zm-api-field-description Property name
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag prop-value
     * @zm-api-field-description Property value
     */
    @XmlValue
    private String value;
    @GraphQLQuery(name=GqlConstants.ZIMLET, description="Zimlet name")
    @GraphQLNonNull
    public String getZimlet() {
        return zimlet;
    }
    public void setZimlet(String zimlet) {
        this.zimlet = zimlet;
    }
    @GraphQLQuery(name=GqlConstants.NAME, description="Property name")
    @GraphQLNonNull
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    @GraphQLQuery(name=GqlConstants.VALUE, description="Property value")
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    private static final String SEPARATOR = ":";
    private String serialization;

    public Prop() {
        //required for jaxb binding
    }

    public Prop(String zimlet, String name, String value) {
            this.zimlet = zimlet;
            this.name = name;
            this.value = value;
            this.serialization = makeSerialization();
    }

    public Prop(String serialization) throws IllegalArgumentException {
        this.serialization = serialization;
        int sep1 = serialization.indexOf(SEPARATOR);
        int sep2 = serialization.indexOf(SEPARATOR, sep1+1);
        if (sep1 < 0 || sep2 < 0) {
            throw new IllegalArgumentException(serialization);
        }
        zimlet = serialization.substring(0, sep1);
        name = serialization.substring(sep1+1, sep2);
        value = serialization.substring(sep2+1);
    }

    private String makeSerialization() {
        return zimlet + SEPARATOR + name + SEPARATOR + value;
    }

    @GraphQLIgnore
    public String getSerialization() {
        if (serialization == null) {
            serialization = makeSerialization();
        }
        return serialization;
    }

    public boolean matches(Prop other) {
        return (zimlet.equals(other.zimlet) && name.equals(other.name));
    }
    public void replace(Prop other) {
        this.zimlet = other.zimlet;
        this.name = other.name;
        this.value = other.value;
        this.serialization = other.serialization;
    }

    public static Multimap<String, String> toMultimap(List<Prop> props, String userPropKey) {
        Multimap<String, String> map = ArrayListMultimap.create();
        for (Prop p : props) {
            map.put(userPropKey, p.getSerialization());
        }
        return map;
    }
}
