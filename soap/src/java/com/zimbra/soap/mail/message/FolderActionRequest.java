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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.FolderActionSelector;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Perform an action on a folder
 * <br />
 * Actions:
 * <pre>
 *   &lt;action op="read" id="{list}"/>
 *     - mark all items in the folder as read
 *
 *   &lt;action op="delete" id="{list}"/>
 *     - hard-delete the folder, all items in the folder, and all the folder's subfolders
 *
 *   &lt;action op="empty" id="{list}" [recusive="{delete-subfolders}"]/>
 *     - hard-delete all items in the folder (and all the folder's subfolders if "recursive" is set)
 *
 *   &lt;action op="rename" id="{list}" name="{new-name}" [l="{new-folder}"]/>
 *     - change the folder's name (and optionally location);
 *       if {new-name} begins with '/', the folder is moved to the new path and any missing path elements are created
 *
 *   &lt;action op="move" id="{list}" l="{new-folder}"/>
 *     - move the folder to be a child of {target-folder}
 *
 *   &lt;action op="trash" id="{list}"/>
 *     - move the folder to the Trash, marking all contents as read and
 *       renaming the folder if a folder by that name is already present in the Trash
 *
 *   &lt;action op="color" id="{list}" color="{new-color} rgb="{new-color-in-rgb}"/>
 *     - see ItemActionRequest
 *
 *   &lt;action op="grant" id="{list}">
 *     &lt;grant perm="..." gt="..." zid="..." [expiry="{millis-since-epoch}"] [d="..."] [key="..."]/>
 *   &lt;/action>
 *     - add the &lt;grant> object to the folder
 *
 *   &lt;action op="!grant" id="{list}" zid="{grantee-zimbra-id}"/>
 *     - revoke access from {grantee-zimbra-id}
 *         (you can use "00000000-0000-0000-0000-000000000000" to revoke acces granted to "all"
 *         or use "99999999-9999-9999-9999-999999999999" to revoke acces granted to "pub" )
 *
 *   &lt;action op="revokeorphangrants" id="{folder-id}" zid="{grantee-zimbra-id}" gt="{grantee-type}"/>
 *     - revoke orphan grants on the folder hierarchy granted to the grantee specified by zid and gt
 *       "orphan grant" is a grant whose grantee object is deleted/non-existing.  Server will throw
 *       INVALID_REQUEST if zid points to an existing object,
 *       Only supported if gt is usr|grp|cos|dom; otherwise server will throw INVALID_REQUEST.
 *
 *   &lt;action op="url" id="{list}" url="{target-url}" [excludeFreeBusy="{exclude-free-busy-boolean}"]/>
 *     - set the synchronization url on the folder to {target-url}, empty the folder, and\
 *       synchronize the folder's contents to the remote feed, also sets {exclude-free-busy-boolean}
 *
 *   &lt;action op="sync" id="{list}"/>
 *     - synchronize the folder's contents to the remote feed specified by the folder's {url}
 *
 *   &lt;action op="import" id="{list}" url="{target-url}"/>
 *     - add the contents to the remote feed at {target-url} to the folder [1-time action]
 *
 *   &lt;action op="fb" id="{list}" excludeFreeBusy="{exclude-free-busy-boolean}"/>
 *     - set the excludeFreeBusy boolean for this folder (must specify {exclude-free-busy-boolean})
 *
 *   &lt;action op="[!]check" id="{list}"/>
 *     - set or unset the "checked" state of the folder in the UI
 *
 *   &lt;action op="[!]syncon" id="{list}"/>
 *     - set or unset the "sync" flag of the folder to sync a local folder with a remote source
 *     
 *   &lt;action op="[!]disableactivesync" id="{list}"/>
 *     - If set, disable access to the folder via activesync.
 *       Note: Only works for user folders, doesn't have any effect on system folders.
 *
 *   &lt;action op="webofflinesyncdays" id="{list}" numDays="{web-offline-sync-days}/>
 *     - set the number of days for which web client would sync folder data for offline use
 *       {web-offline-sync-days} must not be greater than value of zimbraWebClientOfflineSyncMaxDays account attribute
 *
 *   &lt;action op="update" id="{list}" [f="{new-flags}"] [name="{new-name}"]
 *                          [l="{target-folder}"] [color="{new-color}"] [view="{new-view}"]>
 *     [&lt;acl>&lt;grant .../>*&lt;/acl>]
 *   &lt;/action>
 *     - do several operations at once:
 *           name="{new-name}"        to change the folder's name
 *           l="{target-folder}"      to change the folder's location
 *           color="{new-color}"      to set the folder's color
 *           view="{new-view}"        to change folder's default view (useful for migration)
 *           f="{new-flags}"          to change the folder's exclude free/(b)usy, checked (#), and
 *                                    IMAP subscribed (*) state
 *           &lt;acl>&lt;grant ...>*&lt;/acl>  to replace the folder's existing ACL with a new ACL
 *
 *     {list} = on input, list of folders to act on, on output, list of folders that were acted on;
 *              list may only have 1 element for actions empty, sync, fb, check, !check, url, import, grant,
 *              !grant, revokeorphangrants, !flag, !tag, syncon, !syncon, retentionpolicy
 *
 * output of "grant" action includes the zimbra id the rights were granted on
 *
 * note that "delete", "empty", "rename", "move", "color", "update" can be used on search folders as well as standard
 * folders
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_FOLDER_ACTION_REQUEST)
public class FolderActionRequest {

    /**
     * @zm-api-field-description Select action to perform on folder
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_ACTION /* action */, required=true)
    private final FolderActionSelector action;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private FolderActionRequest() {
        this((FolderActionSelector) null);
    }

    public FolderActionRequest(FolderActionSelector action) {
        this.action = action;
    }

    public FolderActionSelector getAction() { return action; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("action", action)
            .toString();
    }
}
