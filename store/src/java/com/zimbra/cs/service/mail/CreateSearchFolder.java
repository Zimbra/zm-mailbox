/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.index.history.SavedSearchPromptLog;
import com.zimbra.cs.index.history.SearchHistory;
import com.zimbra.cs.index.history.ZimbraSearchHistory;
import com.zimbra.cs.index.history.SavedSearchPromptLog.SavedSearchStatus;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.CreateSearchFolderRequest;
import com.zimbra.soap.mail.type.NewSearchFolderSpec;

/**
 * @since May 26, 2004
 */
public class CreateSearchFolder extends MailDocumentHandler  {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_SEARCH, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_FOLDER_PATH;
    }

    @Override
    protected boolean checkMountpointProxy(Element request) {
        return true;
    }

    @Override
    protected String[] getResponseItemPath() {
        return RESPONSE_ITEM_PATH;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        CreateSearchFolderRequest req = zsc.elementToJaxb(request);
        NewSearchFolderSpec spec = req.getSearchFolder();
        Byte color       = spec.getColor() != null ? spec.getColor() : MailItem.DEFAULT_COLOR;
        Color itemColor = spec.getRgb() != null ? new Color(spec.getRgb()) : new Color(color);
        ItemId iidParent = new ItemId(spec.getParentFolderId(), zsc);

        SearchFolder search = mbox.createSearchFolder(octxt, iidParent.getId(),
            spec.getName(), spec.getQuery(), spec.getSearchTypes(), spec.getSortBy(), Flag.toBitmask(spec.getFlags()),
            itemColor);

        mbox.setSavedSearchPromptStatus(octxt, spec.getQuery(), SavedSearchStatus.CREATED);

        Element response = zsc.createElement(MailConstants.CREATE_SEARCH_FOLDER_RESPONSE);
        if (search != null)
            ToXML.encodeSearchFolder(response, ifmt, search);
        return response;
    }
}
