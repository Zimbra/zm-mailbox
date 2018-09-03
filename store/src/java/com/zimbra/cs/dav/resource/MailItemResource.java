/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.service.ServiceException.Argument;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.property.Acl;
import com.zimbra.cs.dav.property.Acl.Ace;
import com.zimbra.cs.dav.property.CalDavProperty.CalComponent;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.ACL.Grant;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.DavNames;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.cs.service.mail.ItemActionHelper;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;

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
    }

    public MailItemResource(String path, String acct) {
        super(path, acct);
        mDeadProps = new HashMap<QName,Element>();
        try {
            Account account = Provisioning.getInstance().getAccountByName(acct);
            if (account != null) {
                mOwnerId = account.getId();
            }
        } catch (ServiceException ignore) {
        }
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
        if (account == null) {
            // Anti-account name harvesting.
            ZimbraLog.dav.info("Failing GET of mailbox for item resource - no such account '%s'", mOwnerId);
            throw new DavException("Request denied", HttpServletResponse.SC_NOT_FOUND, null);
        }
        return MailboxManager.getInstance().getMailboxByAccount(account);
    }

    protected MailItem getMailItem(DavContext ctxt) throws ServiceException, DavException {
        Mailbox mbox = getMailbox(ctxt);
        return mbox.getItemById(ctxt.getOperationContext(), mId, MailItem.Type.UNKNOWN);
    }

    /* Deletes this resource by moving to Trash folder. Hard deletes if the item is in Trash folder.*/
    @Override
    public void delete(DavContext ctxt) throws DavException {
        if (mId == 0) {
            throw new DavException("cannot delete resource", HttpServletResponse.SC_FORBIDDEN, null);
        }
        try {
            Mailbox mbox = getMailbox(ctxt);
            if (DebugConfig.enableDAVclientCanChooseResourceBaseName) {
                DavNames.remove(mbox.getId(), mId);
            }
            // hard delete if the item is in Trash.
            if (getMailItem(ctxt).inTrash()) {
                hardDelete(ctxt);
                return;
            }
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
        ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
        if (zmbx != null) {
            zmbx.setName(acct.getName()); /* need this when logging in using another user's auth */
        }
        return zmbx;
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

    private Map<QName,Element> getDeadProps(DavContext ctxt, MailItem item) throws IOException, ServiceException {
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
        ByteArrayInputStream in = new ByteArrayInputStream(configVal.getBytes(StandardCharsets.UTF_8));
        org.dom4j.Document doc = W3cDomUtil.parseXMLToDom4jDocUsingSecureProcessing(in);
        Element e = doc.getRootElement();
        if (e == null)
            return props;
        for (Object obj : e.elements()) {
            if (obj instanceof Element) {
                Element elem = (Element) obj;
                elem.detach();
                props.put(elem.getQName(), elem);
            }
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
        List<QName> reqProps = new ArrayList<QName>();
        for (QName n : remove) {
            mDeadProps.remove(n);
            reqProps.add(n);
        }
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
            } else if (name.equals(DavElements.E_SUPPORTED_CALENDAR_COMPONENT_SET)) {
                // change default view
                @SuppressWarnings("unchecked")
                List<Element> elements = e.elements(DavElements.E_COMP);
                boolean isTodo = false;
                boolean isEvent = false;
                for (Element element : elements) {
                    Attribute attr = element.attribute(DavElements.P_NAME);
                    if (attr != null && CalComponent.VTODO.name().equals(attr.getValue())) {
                        isTodo = true;
                    } else if (attr != null && CalComponent.VEVENT.name().equals(attr.getValue())) {
                        isEvent = true;
                    }
                }
                if (isEvent ^ isTodo) { // we support a calendar collection of type event or todo, not both or none.
                    Type type = (isEvent) ? Type.APPOINTMENT : Type.TASK;
                    try {
                        Mailbox mbox = getMailbox(ctxt);
                        mbox.setFolderDefaultView(ctxt.getOperationContext(), mId, type);
                        // Update the view for this collection. This collection may get cached if display name is modified.
                        // See UrlNamespace.addToRenamedResource()
                        if (this instanceof Collection) {
                            ((Collection)this).view = type;
                        }
                    } catch (ServiceException se) {
                        ctxt.getResponseProp().addPropError(name, new DavException(se.getMessage(), DavProtocol.STATUS_FAILED_DEPENDENCY));
                    }
                } else {
                    ctxt.getResponseProp().addPropError(name, new DavException.CannotModifyProtectedProperty(name));
                }
                continue;
            }
            mDeadProps.put(name, e);
            reqProps.add(name);
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
        Mailbox mbox = null;
        try {
            mbox = getMailbox(ctxt);
            mbox.lock.lock();
            Metadata data = mbox.getConfig(ctxt.getOperationContext(), CONFIG_KEY);
            if (data == null) {
                data = new Metadata();
            }
            data.put(Integer.toString(mId), configVal);
            mbox.setConfig(ctxt.getOperationContext(), CONFIG_KEY, data);
        } catch (ServiceException se) {
            for (QName qname : reqProps)
                ctxt.getResponseProp().addPropError(qname, new DavException(se.getMessage(), HttpServletResponse.SC_FORBIDDEN));
        } finally {
            if (mbox != null)
                mbox.lock.release();
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
        ACL effectiveAcl = f.getEffectiveACL();
        if (effectiveAcl == null) {
            return aces;
        }
        List<Grant> grants = effectiveAcl.getGrants();
        if (grants == null) {
            return aces;
        }
        for (ACL.Grant g : grants) {
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
