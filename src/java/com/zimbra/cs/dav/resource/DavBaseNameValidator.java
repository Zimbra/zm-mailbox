/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.nio.charset.Charset;
import java.util.regex.Pattern;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.UUIDUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * DAV Basenames associated with items have a bit of history and some that were allowed in the past had strange
 * characters which can cause problems for either us or for clients.
 * For backwards compatibility, we try to support anything pre-existing.  So, if a URL is already in use for
 * an existing item, we continue to allow using it.
 * For new items though, we validate that any suggested basename is not likely to cause problems.
 * If we decide it may cause problems, we will redirect to a URL that we do like.
 */
public class DavBaseNameValidator {
    protected final Collection collection;
    protected final DavContext ctxt;
    protected final Mailbox mbox;
    protected final String clientChoiceForDavBaseName;
    protected String suffix = "";
    protected String newHref = null;
    protected boolean haveMailItemForClientChosen = false;
    protected MailItem existingMailItemWithClientChosenDavBaseName;
    protected ITEM_IN_DB stateOfItemInDb = null;
    public enum ITEM_IN_DB { EXISTS, EXISTS_ELSEWHERE, NON_EXISTENT, NON_EXISTENT_NEEDS_REDIRECT, CONFLICTS };

    public DavBaseNameValidator(Collection collection, DavContext ctxt, Mailbox mbox,
            String clientChoiceForDavBaseName) {
        this.collection = collection;
        this.ctxt = ctxt;
        this.mbox = mbox;
        this.clientChoiceForDavBaseName = clientChoiceForDavBaseName;
        existingMailItemWithClientChosenDavBaseName = getMailItemWithStoredDavBaseName(clientChoiceForDavBaseName);
    }

    private MailItem getMailItemWithStoredDavBaseName(String davBaseName) {
        if (davBaseName == null) {
            return null;
        }
        try {
            MailItemResource mi = (MailItemResource) UrlNamespace.getMailItemResource(
                    ctxt, mbox.getAccount(), getPathForBaseName(davBaseName));
            if (mi == null) {
                return null;
            }
            return mbox.getItemById(ctxt.getOperationContext(), mi.getId(), mi.type);
        } catch (ServiceException | DavException e) {
            return null;
        }
    }

    protected MailItem getPreExistingWithUniqueId() {
        return null;
    }

    public MailItem getExistingMailItemWithClientChosenDavBaseName() {
        if (!haveMailItemForClientChosen) {
            existingMailItemWithClientChosenDavBaseName =
                    getMailItemWithStoredDavBaseName(clientChoiceForDavBaseName);
            haveMailItemForClientChosen = true;
        }
        return existingMailItemWithClientChosenDavBaseName;
    }

    public String getHrefForMailItem(MailItem mItem) {
        try {
            DavResource dr = UrlNamespace.getResourceFromMailItem(ctxt, mItem);
            if (dr != null) {
                return dr.getHref();
            }
        } catch (DavException de) {
            ZimbraLog.dav.debug("Exception thrown when looking for href for item", de);
        }
        return null;
    }

    /** e.g. /dav/dav1%40pan.local/Calendar/d1102-42a7-4283-b025-3376dabe53b3.ics */
    public String getHrefForBaseName(String baseName) {
        StringBuilder hrefForBaseName = new StringBuilder(collection.getHref());
        if ('/' != hrefForBaseName.charAt(hrefForBaseName.length() - 1)) {
            hrefForBaseName.append('/');
        }
        hrefForBaseName.append(baseName);
        return hrefForBaseName.toString();
    }

    /** e.g. /Calendar/d1102-42a7-4283-b025-3376dabe53b3.ics */
    public String getPathForBaseName(String baseName) {
        StringBuilder pathForBaseName = new StringBuilder(collection.getUri());
        if ('/' != pathForBaseName.charAt(pathForBaseName.length() - 1)) {
            pathForBaseName.append('/');
        }
        pathForBaseName.append(baseName);
        return pathForBaseName.toString();
    }

    public ITEM_IN_DB getStateOfItemInDB() {
        if (stateOfItemInDb != null) {
            return stateOfItemInDb;
        }
        MailItem withUniqueId = getPreExistingWithUniqueId();
        getExistingMailItemWithClientChosenDavBaseName();
        if (null != existingMailItemWithClientChosenDavBaseName) {
            if (null != withUniqueId) {
                if (withUniqueId.getId() == existingMailItemWithClientChosenDavBaseName.getId()) {
                    stateOfItemInDb = ITEM_IN_DB.EXISTS;
                    newHref = getHrefForBaseName(clientChoiceForDavBaseName);
                } else {
                    stateOfItemInDb = ITEM_IN_DB.EXISTS_ELSEWHERE;
                    newHref = getHrefForMailItem(withUniqueId);
                }
            } else {
                // for the simplest situation. assume can replace any pre-existing
                stateOfItemInDb = ITEM_IN_DB.EXISTS;
                newHref = getHrefForBaseName(clientChoiceForDavBaseName);
            }
        } else if (null != withUniqueId) {
            stateOfItemInDb = ITEM_IN_DB.EXISTS_ELSEWHERE;
            newHref = getHrefForMailItem(withUniqueId);
        } else {
            if (validateDavBaseName(clientChoiceForDavBaseName)) {
                stateOfItemInDb = ITEM_IN_DB.NON_EXISTENT;
                newHref = getHrefForBaseName(clientChoiceForDavBaseName);
            } else {
                stateOfItemInDb = ITEM_IN_DB.NON_EXISTENT_NEEDS_REDIRECT;
                newHref = getHrefForBaseName(chooseNewDavBaseName());
            }
        }
        ZimbraLog.dav.debug("State of item in DB=%s.  newHref='%s'", stateOfItemInDb, newHref);
        return stateOfItemInDb;
    }

    public String getNewHref() {
        if (newHref != null) {
            return newHref;
        }
        getStateOfItemInDB();
        return newHref;
    }

    public String getNewDavBaseName() {
        getNewHref();
        return newHref.substring(newHref.lastIndexOf('/') + 1);
    }

    /* Stick with chars from unreserved (see RFC 1738 Uniform Resource Locators) but be more restrictive with
     * first char (dots being the most obviously harmful)
     * safe           = "$" | "-" | "_" | "." | "+"
     * extra          = "!" | "*" | "'" | "(" | ")" | ","
     * unreserved     = alpha | digit | safe | extra
     */
    private static Pattern GOOD_DAV_BASE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_][a-zA-Z0-9$_.+!*'(),-]*$");
    public boolean validateDavBaseName(String davBaseName) {
        if (davBaseName == null) {
            return false;
        }
        return davBaseName.length() > 4 &&
                davBaseName.getBytes(Charset.forName("UTF-8")).length <= DbMailItem.MAX_DAV_BASENAME_LENGTH &&
                GOOD_DAV_BASE_NAME_PATTERN.matcher(davBaseName).matches();
    }

    public String chooseNewDavBaseName() {
        String newName;
        DavResource resource;
        do {
            newName = UUIDUtil.generateUUID() + suffix;
            resource = null;
            ZimbraLog.dav.debug("Trying candidate Dav Base Name '%s'", newName);
            try {
                resource = UrlNamespace.getResourceAt(ctxt, mbox.getAccount().getName(), getPathForBaseName(newName));
            } catch (DavException | ServiceException e) {
                resource = null;
            }
        } while (resource != null);
        return newName;
    }
}
