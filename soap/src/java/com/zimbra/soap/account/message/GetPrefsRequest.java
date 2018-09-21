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

package com.zimbra.soap.account.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.Pref;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Get preferences for the authenticated account
 * <br />
 * If no <b>&lt;pref></b> elements are provided, all known prefs are returned in the response.
 * <br />
 * If <b>&lt;pref></b> elements are provided, only those prefs are returned in the response.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_PREFS_REQUEST)
@GraphQLType(name=GqlConstants.CLASS_GET_PREFS_REQUEST, description="Get preferences for the authenticated account")
public class GetPrefsRequest {
    /**
     * @zm-api-field-description If any of these are specified then only get these preferences
     */
    @XmlElement(name=AccountConstants.E_PREF)
    @GraphQLQuery(name=GqlConstants.PREFERENCES, description="List of prefs that is wanted in the response")
    private List<Pref> pref;

    public void setPref(List<Pref> pref) {
        this.pref = pref;
    }

    public List<Pref> getPref() {
        return Collections.unmodifiableList(pref);
    }
}
