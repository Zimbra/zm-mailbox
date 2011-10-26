/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.service.ServiceException.Argument;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.DavProtocol.Compliance;
import com.zimbra.cs.dav.property.Acl;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.property.Acl.Ace;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.cs.service.mail.ItemActionHelper;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.client.ZMailbox;

/**
 * Abstraction of DavResource that maps to MailItem in the mailbox.
 * Supports the dead properties that can be saved for DavResource.
 *
 * @author jylee
 *
 */
public abstract class MailItemResource extends DavResource {
    protected int  mFolderId;
    protected int  mId;
    protected MailItem.Type type;
    protected Color mColor;
    protected String mEtag;
    protected String mSubject;
    protected String mPath;
    protected long mModifiedDate;
    protected String mOwnerId;
    protected Map<QName,Element> mDeadProps;

    private static final String CONFIG_KEY = "caldav";
    private static final int PROP_LENGTH_LIMIT = 1024;

    private static final String BLUE   = "#0252D4FF";
    private static final String CYAN   = "#008284FF";
    private static final String GRAY   = "#848284FF";
    private static final String GREEN  = "#2CA10BFF";
    private static final String ORANGE = "#F57802FF";
    private static final String PINK   = "#B027AEFF";
    private static final String PURPLE = "#492BA1FF";
    private static final String RED    = "#E51717FF";
    private static final String YELLOW = "#848200FF";
    private static final String DEFAULT_COLOR = ORANGE;

    protected static final String[] COLOR_MAP = {
        DEFAULT_COLOR, BLUE, CYAN, GREEN, PURPLE, RED, YELLOW, PINK, GRAY, ORANGE
    };

    protected static final ArrayList<String> COLOR_LIST = new ArrayList<String>();

    static {
        Collections.addAll(COLOR_LIST, COLOR_MAP);
    }

    public MailItemResource(DavContext ctxt, MailItem item) throws ServiceException {
        this(ctxt, getItemPath(ctxt, item), item);
    }

    public MailItemResource(DavContext ctxt, String path, MailItem item) throws ServiceException {
        super(path, ctxt.getUser());
        mFolderId = item.getFolderId();
        mId = item.getId();
        type = item.getType();
        mEtag = MailItemResource.getEtag(item);
        mSubject = item.getSubject();
        mPath = item.getPath();
        mModifiedDate = item.getChangeDate();
        mOwnerId = item.getAccount().getId();
        try {
            mDeadProps = getDeadProps(ctxt, item);
        } catch (Exception e) {
            ZimbraLog.dav.warn("can't get dead properties for MailItem id="+mId, e);
        }
        setProperty(DavElements.P_GETETAG, mEtag);
        if (mModifiedDate > 0)
            setLastModifiedDate(mModifiedDate);
        mColor = item.getRgbColor();
        setProperty(DavElements.E_DISPLAYNAME, item.getName());
        addProperty(Acl.getPrincipalUrl(this));
        if (ctxt.isSchedulingEnabled())
            mDavCompliance.add(Compliance.calendar_schedule);
    }

    public MailItemResource(String path, String acct) {
        super(path, acct);
    }

    private static String getItemPath(DavContext ctxt, MailItem item) {
        String path = ctxt.getCollectionPath();
        if (path != null) {
            if (!path.endsWith("/"))
                path += "/";
            path += item.getName();
        } else {
            path = ctxt.getPath();
        }
        if ((item.getType() == MailItem.Type.CONTACT || item.getType() == MailItem.Type.FOLDER ||
                item.getType() == MailItem.Type.MOUNTPOINT) && !path.endsWith("/")) {
            path += "/";
        }
        if (item.getType() == MailItem.Type.CONTACT) {
            path += VCard.getUrl((Contact)item) + AddressObject.VCARD_EXTENSION;
        }
        return path;
    }

    @Override
    public boolean hasEtag() {
        return true;
    }

    @Override
    public String getEtag() {
        return mEtag;
    }

    @Override
    public String getName() {
        return mSubject;
    }

    protected Mailbox getMailbox(DavContext ctxt) throws ServiceException, DavException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.id, mOwnerId);
        if (account == null)
            throw new DavException("no such account "+mOwnerId, HttpServletResponse.SC_NOT_FOUND, null);
        return MailboxManager.getInstance().getMailboxByAccount(account);
    }

    protected MailItem getMailItem(DavContext ctxt) throws ServiceException, DavException {
        Mailbox mbox = getMailbox(ctxt);
        return mbox.getItemById(ctxt.getOperationContext(), mId, MailItem.Type.UNKNOWN);
    }

    /* Deletes this resource by moving to Trash folder. */
    @Override
    public void delete(DavContext ctxt) throws DavException {
        if (mId == 0)
            throw new DavException("cannot delete resource", HttpServletResponse.SC_FORBIDDEN, null);
        try {
            Mailbox mbox = getMailbox(ctxt);
            mbox.move(ctxt.getOperationContext(), mId, MailItem.Type.UNKNOWN, Mailbox.ID_FOLDER_TRASH);
        } catch (ServiceException se) {
            if (se.getCode().equals(MailServiceException.ALREADY_EXISTS)) {
                hardDelete(ctxt);
                return;
            }
            int resCode = se instanceof MailServiceException.NoSuchItemException ?
                    HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_FORBIDDEN;
            throw new DavException("cannot delete item", resCode, se);
        }
    }

    /* hard delete */
    public void hardDelete(DavContext ctxt) throws DavException {
        if (mId == 0)
            throw new DavException("cannot hard delete resource", HttpServletResponse.SC_FORBIDDEN, null);
        try {
            Mailbox mbox = getMailbox(ctxt);
            mbox.delete(ctxt.getOperationContext(), mId, MailItem.Type.UNKNOWN);
        } catch (ServiceException se) {
            int resCode = se instanceof MailServiceException.NoSuchItemException ?
                    HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_FORBIDDEN;
            throw new DavException("cannot delete item", resCode, se);
        }
    }
    
    private static ZMailbox getZMailbox(DavContext ctxt, Collection col) throws ServiceException {
        AuthToken authToken = AuthProvider.getAuthToken(ctxt.getAuthAccount());
        Account acct = Provisioning.getInstance().getAccountById(col.getItemId().getAccountId());
        ZMailbox.Options zoptions = new ZMailbox.Options(authToken.toZAuthToken(), AccountUtil.getSoapUri(acct));
        zoptions.setNoSession(true);
        zoptions.setTargetAccount(acct.getId());
        zoptions.setTargetAccountBy(Key.AccountBy.id);
        return ZMailbox.getMailbox(zoptions);       
    }
    private void deleteDestinationItem(DavContext ctxt, Collection dest, int id) throws ServiceException, DavException {
        Mailbox mbox = getMailbox(ctxt);
        if (dest.getItemId().belongsTo(mbox)) {
            mbox.delete(ctxt.getOperationContext(), id, MailItem.Type.UNKNOWN, null);
        } else {
            ZMailbox zmbx = getZMailbox(ctxt, dest);
            ItemId itemId = new ItemId(dest.getItemId().getAccountId(), id);
            zmbx.deleteItem(itemId.toString(), null);
        }
    }
    
    /* Moves this resource to another Collection. */
    public void move(DavContext ctxt, Collection dest, String newName) throws DavException {
        try {
            Mailbox mbox = getMailbox(ctxt);
            ArrayList<Integer> ids = new ArrayList<Integer>();
            ids.add(mId);
            if (newName != null)
                ItemActionHelper.RENAME(ctxt.getOperationContext(), mbox, SoapProtocol.Soap12, ids, type, null, newName, dest.getItemId());
            else     
                ItemActionHelper.MOVE(ctxt.getOperationContext(), mbox, SoapProtocol.Soap12, ids, type, null, dest.getItemId());
        } catch (ServiceException se) {
            int resCode = se instanceof MailServiceException.NoSuchItemException ?
                    HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_FORBIDDEN;
            if (se.getCode().equals(MailServiceException.ALREADY_EXISTS))
                resCode = HttpServletResponse.SC_PRECONDITION_FAILED;
            throw new DavException("cannot move item", resCode, se);
        }
    }

    public void moveORcopyWithOverwrite(DavContext ctxt, Collection dest, String newName, boolean deleteOriginal) throws DavException {
        try {
            if (deleteOriginal)
                move(ctxt, dest, newName);
            else 
                copy(ctxt, dest, newName);
        } catch (DavException e) {
            if (e.getStatus() == HttpServletResponse.SC_PRECONDITION_FAILED) {
                // in case of name conflict, delete the existing mail item and
                // attempt the move operation again.
                // return if the error is not ALREADY_EXISTS
                ServiceException se = (ServiceException) e.getCause();
                int id = 0;
                try {
                    if (se.getCode().equals(MailServiceException.ALREADY_EXISTS) == false)
                        throw e;
                    else { // get the conflicting item-id
                        if (se instanceof SoapFaultException) { // destination belongs other mailbox.
                            String itemIdStr = ((SoapFaultException) se).getArgumentValue("id");
                            ItemId itemId = new ItemId(itemIdStr, dest.getItemId().getAccountId());
                            id = itemId.getId();
                        } else { // destination belongs to same mailbox.         
                            String name = null;
                            for (Argument arg: se.getArgs()) {
                                if (arg.name != null && arg.value != null && arg.value.length() > 0) {
                                    if (arg.name.equals("name"))
                                        name = arg.value;
                                    /* commented out since the exception is giving wrong itemId for copy.
                                       If the the item is conflicting with an existing item we want the 
                                       id of the existing item. But, the exception has the proposed id of
                                       the new item which does not exist yet. 
                                     else if (arg.mName.equals("itemId"))
                                        id = Integer.parseInt(arg.mValue);
                                     */   
                                }
                            }
                            if (id <= 0) {                                
                                if (name == null && !deleteOriginal) { 
                                    // in case of copy get the id from source name since we don't support copy with rename.
                                    name = ctxt.getItem();
                                }
                                if (name != null) {
                                    Mailbox mbox = getMailbox(ctxt);
                                    MailItem item = mbox.getItemByPath(ctxt.getOperationContext(), name, dest.getId());
                                    id = item.getId();
                                } else
                                    throw e;
                            }    
                        }
                    }
                    deleteDestinationItem(ctxt, dest, id);
                } catch (ServiceException se1) {
                    throw new DavException("cannot move/copy item", HttpServletResponse.SC_FORBIDDEN, se1);
                }
                if (deleteOriginal)
                    move(ctxt, dest, newName);
                else 
                    copy(ctxt, dest, newName);
            } else {
                throw e;
            }
        }
    }

    /* Copies this resource to another Collection. */
    public void copy(DavContext ctxt, Collection dest, String newName) throws DavException {
        try {
            Mailbox mbox = getMailbox(ctxt);
            ArrayList<Integer> ids = new ArrayList<Integer>();            
            if (newName == null) {
                ids.add(mId);
                ItemActionHelper.COPY(ctxt.getOperationContext(), mbox, SoapProtocol.Soap12, ids, type, null, dest.getItemId());
            } else { //copy with rename.
                // TODO add COPY with RENAME (e.g> cp a.txt b.txt) functionality in ItemActionHelper
                throw MailServiceException.CANNOT_COPY(mId);
            }
        } catch (ServiceException se) {
            int resCode = se instanceof MailServiceException.NoSuchItemException ?
                    HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_FORBIDDEN;
            if (se.getCode().equals(MailServiceException.ALREADY_EXISTS))
                resCode = HttpServletResponse.SC_PRECONDITION_FAILED;
            throw new DavException("cannot copy item", resCode, se);
        }
    }

    public int getId() {
        return mId;
    }

    public ItemId getItemId() {
        return new ItemId(mOwnerId, mId);
    }

    private Map<QName,Element> getDeadProps(DavContext ctxt, MailItem item) throws DocumentException, IOException, ServiceException {
        HashMap<QName,Element> props = new HashMap<QName,Element>();
        Mailbox mbox = item.getMailbox();
        Metadata data = mbox.getConfig(ctxt.getOperationContext(), CONFIG_KEY);
        if (data == null)
            return props;
        String configVal = data.get(Integer.toString(item.getId()), null);
        if (configVal == null)
            return props;
        if (configVal.length() == 0)
            return props;
        ByteArrayInputStream in = new ByteArrayInputStream(configVal.getBytes("UTF-8"));
        org.dom4j.Document doc = com.zimbra.common.soap.Element.getSAXReader().read(in);
        Element e = doc.getRootElement();
        if (e == null)
            return props;
        for (Object obj : e.elements())
            if (obj instanceof Element) {
                Element elem = (Element) obj;
                elem.detach();
                props.put(elem.getQName(), elem);
            }
        return props;
    }

    /* Modifies the set of dead properties saved for this resource.
     * Properties in the parameter 'set' are added to the existing properties.
     * Properties in 'remove' are removed.
     */
    @Override
    public void patchProperties(DavContext ctxt, java.util.Collection<Element> set, java.util.Collection<QName> remove)
            throws DavException, IOException {
        for (QName n : remove)
                mDeadProps.remove(n);
        for (Element e : set) {
            QName name = e.getQName();
            if (name.equals(DavElements.E_DISPLAYNAME) &&
                    (type == MailItem.Type.FOLDER || type == MailItem.Type.MOUNTPOINT)) {
                // rename folder
                try {
                    String val = e.getText();
                    String uri = getUri();
                    Mailbox mbox = getMailbox(ctxt);
                    mbox.rename(ctxt.getOperationContext(), mId, type, val, mFolderId);
                    setProperty(DavElements.P_DISPLAYNAME, val);
                    UrlNamespace.addToRenamedResource(getOwner(), uri, this);
                    UrlNamespace.addToRenamedResource(getOwner(), uri.substring(0, uri.length()-1), this);
                } catch (ServiceException se) {
                    ctxt.getResponseProp().addPropError(DavElements.E_DISPLAYNAME, new DavException(se.getMessage(),
                            DavProtocol.STATUS_FAILED_DEPENDENCY));
                }
                mDeadProps.remove(name);
                continue;
            } else if (name.equals(DavElements.E_CALENDAR_COLOR) &&
                    (type == MailItem.Type.FOLDER || type == MailItem.Type.MOUNTPOINT)) {
                // change color
                String colorStr = e.getText();
                Color color = new Color(colorStr.substring(0, 7));
                byte col = (byte) COLOR_LIST.indexOf(colorStr);
                if (col >= 0)
                    color.setColor(col);
                try {
                    Mailbox mbox = getMailbox(ctxt);
                    mbox.setColor(ctxt.getOperationContext(), new int[] { mId }, type, color);
                } catch (ServiceException se) {
                    ctxt.getResponseProp().addPropError(DavElements.E_CALENDAR_COLOR, new DavException(se.getMessage(),
                            DavProtocol.STATUS_FAILED_DEPENDENCY));
                }
                mDeadProps.remove(name);
                continue;
            }
            mDeadProps.put(name, e);
        }
        String configVal = "";
        if (mDeadProps.size() > 0) {
            org.dom4j.Document doc = org.dom4j.DocumentHelper.createDocument();
            Element top = doc.addElement(CONFIG_KEY);
            for (Map.Entry<QName,Element> entry : mDeadProps.entrySet())
                top.add(entry.getValue().detach());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputFormat format = OutputFormat.createCompactFormat();
            XMLWriter writer = new XMLWriter(out, format);
            writer.write(doc);
            configVal = new String(out.toByteArray(), "UTF-8");

            if (configVal.length() > PROP_LENGTH_LIMIT)
                for (Map.Entry<QName,Element> entry : mDeadProps.entrySet())
                    ctxt.getResponseProp().addPropError(entry.getKey(), new DavException("prop length exceeded", DavProtocol.STATUS_INSUFFICIENT_STORAGE));
        }
        try {
            Mailbox mbox = getMailbox(ctxt);
            mbox.lock.lock();
            try {
                Metadata data = mbox.getConfig(ctxt.getOperationContext(), CONFIG_KEY);
                if (data == null) {
                    data = new Metadata();
                }
                data.put(Integer.toString(mId), configVal);
                mbox.setConfig(ctxt.getOperationContext(), CONFIG_KEY, data);
            } finally {
                mbox.lock.release();
            }
        } catch (ServiceException se) {
            throw new DavException("unable to patch properties", HttpServletResponse.SC_FORBIDDEN, se);
        }
    }

    @Override
    public ResourceProperty getProperty(QName prop) {
        ResourceProperty rp = null;
        if (mDeadProps != null) {
            Element e = mDeadProps.get(prop);
            if (e != null)
                rp = new ResourceProperty(e);
        }
        if (rp == null)
            rp = super.getProperty(prop);
        return rp;
    }

    public static String getEtag(MailItem item) {
        return getEtag(Long.toString(item.getModifiedSequence()), Long.toString(item.getSavedSequence()));
    }
    public static String getEtag(String modMetadata, String modContent) {
        return "\""+modMetadata+"-"+modContent+"\"";
    }

    public void setAce(DavContext ctxt, List<Ace> aceList) throws ServiceException, DavException {
        ACL acl = new ACL();
        for (Ace ace : aceList) {
            if (ace.getRights() > 0)
                acl.grantAccess(ace.getZimbraId(), ace.getGranteeType(), ace.getRights(), null);
        }    
        Mailbox mbox = getMailbox(ctxt);
        mbox.setPermissions(ctxt.getOperationContext(), getId(), acl);
    }

    public List<Ace> getAce(DavContext ctxt) throws ServiceException, DavException {
        ArrayList<Ace> aces = new ArrayList<Ace>();
        Mailbox mbox = getMailbox(ctxt);
        MailItem item = mbox.getItemById(ctxt.getOperationContext(), mId, MailItem.Type.UNKNOWN);
        Folder f = null;
        if (item.getType() == MailItem.Type.FOLDER)
            f = (Folder)item;
        else
            f = mbox.getFolderById(ctxt.getOperationContext(), item.getParentId());
        for (ACL.Grant g : f.getEffectiveACL().getGrants()) {
            if (!g.hasGrantee())
                continue;
            aces.add(new Ace(g.getGranteeId(), g.getGrantedRights(), g.getGranteeType()));
        }
        return aces;
    }

    protected ResourceProperty getIcalColorProperty() {
        /*
        if (mDeadProps != null) {
            Element e = mDeadProps.get(DavElements.E_CALENDAR_COLOR);
            if (e != null)
                return new ResourceProperty(e);
        }
         */
        ResourceProperty color = new ResourceProperty(DavElements.E_CALENDAR_COLOR);
        if (mColor.hasMapping())
            // if it's a mapped color use iCal's native color
            color.setStringValue(COLOR_MAP[mColor.getMappedColor()]);
        else
            // use rgb value.
            color.setStringValue(mColor.toString() + "FF");
        return color;
    }
}
