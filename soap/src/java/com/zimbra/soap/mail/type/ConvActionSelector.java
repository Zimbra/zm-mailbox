/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ConvActionSelector extends ActionSelector {

    /**
     * @zm-api-field-tag account-relative-path
     * @zm-api-field-description In case of "move" operation, this attr can also be used to specify the target folder,
     * in terms of the relative path from the account / data source's root folder. The target account / data source is
     * identified based on where the messages in this conversation already reside. If a conversation contains messages
     * belonging of multiple accounts / data sources then it would not be affected by this operation.
     */
    @XmlElement(name=MailConstants.A_ACCT_RELATIVE_PATH, required=false)
    private String acctRelativePath;

    /**
     * no-argument constructor wanted by JAXB
     */
    protected ConvActionSelector() {
        super();
    }

    protected ConvActionSelector(String ids, String operation) {
        super(ids, operation);
    }

    public static ConvActionSelector createForIdsAndOperation(String ids, String operation) {
        return new ConvActionSelector(ids, operation);
    }

    public String getAcctRelativePath() {
        return acctRelativePath;
    }

    public void setAcctRelativePath(String acctRelativePath) {
        this.acctRelativePath = acctRelativePath;
    }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
                .add("acctRelativePath", acctRelativePath);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
