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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.TargetSpec;


/*
 * Delete this class in bug 66989
 */
/**
 * @zm-api-command-deprecation-info Note: to be deprecated in Zimbra 9.  Use zimbraAccount CheckRights instead.
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Check if the authed user has the specified right(s) on a target.
 * <br />
 * If the specified target cannot be found:
 * <ul>
 * <li> if by is "id", throw NO_SUCH_ACCOUNT/NO_SUCH_CALENDAR_RESOURCE
 * <li> if by is "name", return the default permission for the right.
 * </ul>
 * e.g.  With user1's auth token, the following checks if user1 can invite user2 and view user2's free/busy.
 * <pre>
 *     &lt;CheckPermissionRequest>
 *       &lt;target type="account" by="name">user2@test.com&lt;/target>
 *       &lt;right>invite&lt;/right>
 *       &lt;right>viewFreeBusy&lt;/right>
 *     &lt;/CheckPermissionRequest>
 *
 *     &lt;CheckPermissionResponse allow="{1|0}">
 *       &lt;right allow="{1|0}">invite&lt;/right>
 *       &lt;right allow="{1|0}">viewFreeBusy&lt;/right>
 *     &lt;/CheckPermissionResponse>
 * </pre>
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CHECK_PERMISSION_REQUEST)
public class CheckPermissionRequest {

    /**
     * @zm-api-field-description Target specification
     */
    @XmlElement(name=MailConstants.E_TARGET /* target */, required=false)
    private TargetSpec target;

    /**
     * @zm-api-field-description Rights to check
     */
    @XmlElement(name=MailConstants.E_RIGHT /* right */, required=false)
    private List<String> rights = Lists.newArrayList();

    public CheckPermissionRequest() {
    }

    public void setTarget(TargetSpec target) { this.target = target; }
    public void setRights(Iterable <String> rights) {
        this.rights.clear();
        if (rights != null) {
            Iterables.addAll(this.rights,rights);
        }
    }

    public void addRight(String right) {
        this.rights.add(right);
    }

    public TargetSpec getTarget() { return target; }
    public List<String> getRights() {
        return rights;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("target", target)
            .add("rights", rights);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
