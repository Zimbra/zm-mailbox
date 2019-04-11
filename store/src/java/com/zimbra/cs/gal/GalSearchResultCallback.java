/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.gal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.account.type.MemberOfSelector;

public class GalSearchResultCallback implements GalContact.Visitor {
    private Element mResponse;
	protected ItemIdFormatter mFormatter;
	private boolean mIdOnly;
	private final GalOp mOp;
	private Account mAuthAcct;
	private boolean mNeedCanExpand;
	private boolean mNeedIsOwner;
    private MemberOfSelector mNeedIsMember;
	private boolean mNeedSMIMECerts;

	private Set<String> mOwnerOfGroupsIds = null;

    public GalSearchResultCallback(GalSearchParams params) {
    	reset(params);
    	mOp = params.getOp();
    }

    public void reset(GalSearchParams params) {
    	if (params.getSoapContext() != null) {
    		mResponse = params.getSoapContext().createElement(params.getResponseName());
        	mFormatter = new ItemIdFormatter(params.getSoapContext());
    	} else {
    	    mResponse = Element.XMLElement.mFactory.createElement(params.getResponseName());
    	    mFormatter = new ItemIdFormatter();
    	}
    	params.setGalResult(SearchGalResult.newSearchGalResult(this));
    	mIdOnly = params.isIdOnly();

    	try {
    	    mAuthAcct = params.getAuthAccount();
        } catch (ServiceException e) {
            ZimbraLog.gal.warn("unable to get authed account", e);
        }
        mNeedCanExpand = params.getNeedCanExpand();
        mNeedIsOwner = params.getNeedIsOwner();
        mNeedIsMember = params.getNeedIsMember();
        mNeedSMIMECerts = params.getNeedSMIMECerts();
    }

    @Override
    public void visit(GalContact c) throws ServiceException {
    	handleContact(c);
    }

    public Element getResponse() {
    	return mResponse;
    }

    public boolean passThruProxiedGalAcctResponse() {
        return false;
    }

    public void handleProxiedResponse(Element resp) {
        assert(false);
    }

    public Element handleContact(Contact c) throws ServiceException {
        Element eContact;
    	if (mIdOnly) {
    	    eContact = mResponse.addNonUniqueElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, mFormatter.formatItemId(c));
    	} else if (mOp == GalOp.sync) {
    	    eContact = ToXML.encodeContact(mResponse, mFormatter, null, c, true, c.getAllFields().keySet());
    	} else if (mNeedSMIMECerts) {
    		// this is the case only when proxying SearcgGalRequest for the call from
    	    // GetSMIMEPublicCerts (in ZimbraNetwork)
    	    Set<String> fieldSet = new HashSet<String>(c.getFields().keySet());
    		fieldSet.addAll(Contact.getSMIMECertFields());
    	    eContact = ToXML.encodeContact(mResponse, mFormatter, null, c, true, fieldSet);
    	} else {
    	    Set<String> fieldSet = new HashSet<String>(c.getFields().keySet());
            fieldSet.removeAll(Contact.getSMIMECertFields());
    	    eContact = ToXML.encodeContact(mResponse, mFormatter, null, c, true, fieldSet);
    	}

    	eContact.addAttribute(AccountConstants.A_REF, c.get(ContactConstants.A_dn));

    	if (c.isGroup()) {
    	    String zimbraId = c.get(ContactConstants.A_zimbraId);

        	if (mNeedCanExpand) {
        	    boolean canExpand = GalSearchControl.canExpandGalGroup(
        	            c.get(ContactConstants.A_email), zimbraId, mAuthAcct);
                eContact.addAttribute(AccountConstants.A_EXP, canExpand);
        	}

        	if (mNeedIsOwner) {
        	    boolean isOwner = isOwner(zimbraId);
        	    eContact.addAttribute(AccountConstants.A_IS_OWNER, isOwner);
        	}

        	if (MemberOfSelector.none != mNeedIsMember) {
        	    boolean isMember = isMember(zimbraId);
                eContact.addAttribute(AccountConstants.A_IS_MEMBER, isMember);
        	}
    	}

    	return eContact;
    }

    public void handleContact(GalContact c) throws ServiceException {
        String zimbraId = c.getSingleAttr(ContactConstants.A_zimbraId);
        boolean canExpand = GalSearchControl
            .canExpandGalGroup(c.getSingleAttr(ContactConstants.A_email), zimbraId, mAuthAcct);
		Element eGalContact = ToXML.encodeGalContact(mResponse, c, null, canExpand);
		eGalContact.addAttribute(AccountConstants.A_REF, c.getId());

		if (c.isGroup()) {
    		if (mNeedCanExpand) {
    		    eGalContact.addAttribute(AccountConstants.A_EXP, canExpand);
    		}

            if (mNeedIsOwner) {
                boolean isOwner = isOwner(zimbraId);
                eGalContact.addAttribute(AccountConstants.A_IS_OWNER, isOwner);
            }

            if (MemberOfSelector.none != mNeedIsMember) {
                boolean isMember = isMember(zimbraId);
                eGalContact.addAttribute(AccountConstants.A_IS_MEMBER, isMember);
            }
		}
    }

    public void handleElement(Element e) throws ServiceException {
    	mResponse.addNonUniqueElement(e.detach());
    }

    public void handleDeleted(ItemId id) {
    	mResponse.addNonUniqueElement(MailConstants.E_DELETED).addAttribute(MailConstants.A_ID, id.toString());
    }

    protected HashMap<String,Object> parseContactElement(Element e) {
    	HashMap<String,Object> map = new HashMap<String,Object>();
    	Iterator<Element> iter = e.elementIterator(MailConstants.E_ATTRIBUTE);
    	while (iter.hasNext()) {
    		Element elem = iter.next();
    		String key = elem.getAttribute(MailConstants.A_ATTRIBUTE_NAME, null);
    		String value = elem.getText();
    		if (key == null)
    			continue;
    		Object obj = map.get(key);
    		if (obj != null) {
    			if (obj instanceof String) {
    				String[] str = new String[2];
    				str[0] = (String)obj;
    				str[1] = value;
    				map.put(key, str);
    			} else if (obj instanceof String[]) {
    				ArrayList<String> arr = new ArrayList<String>();
    				arr.addAll(Arrays.asList((String[])obj));
    				arr.add(value);
    				map.put(key, arr.toArray(new String[0]));
    			}
    		} else {
    			map.put(key, value);
    		}
    	}
    	return map;
    }
    public void setNewToken(String newToken) {
    	setNewToken(new GalSyncToken(newToken));
    }
    public void setNewToken(GalSyncToken newToken) {
    	String oldToken = mResponse.getAttribute(MailConstants.A_TOKEN, null);
    	if (oldToken != null)
    		newToken.merge(new GalSyncToken(oldToken));
    	mResponse.addAttribute(MailConstants.A_TOKEN, newToken.toString());
    }
    public void setSortBy(String sortBy) {
        mResponse.addAttribute(MailConstants.A_SORTBY, sortBy);
    }
    public void setQueryOffset(int offset) {
        mResponse.addAttribute(MailConstants.A_QUERY_OFFSET, offset);
    }
    public void setHasMoreResult(boolean more) {
        mResponse.addAttribute(MailConstants.A_QUERY_MORE, more);
    }

    public void setThrottled(boolean throttled) {
        mResponse.addAttribute(MailConstants.A_GALSYNC_THROTTLED, throttled);
    }

    public void setFullSyncRecommended(boolean value) {
        mResponse.addAttribute(MailConstants.A_GALSYNC_FULLSYNC_RECOMMENDED, value);
    }

    public void setGalDefinitionLastModified(String timestamp) {
        mResponse.addAttribute(MailConstants.A_GAL_DEFINITION_LAST_MODIFIED, timestamp);
    }

    public void setRemain(int value) {
        mResponse.addAttribute(MailConstants.A_REMAIN, value);
    }

    private boolean isOwner(String groupZimbraId) throws ServiceException {
        if (mAuthAcct == null || groupZimbraId == null) {
            return false;
        }

        if (mOwnerOfGroupsIds == null) {
            mOwnerOfGroupsIds = Group.GroupOwner.getOwnedGroupsIds(mAuthAcct);
        }

        return mOwnerOfGroupsIds.contains(groupZimbraId);
    }

    private boolean isMember(String groupZimbraId) throws ServiceException {
        if (mAuthAcct == null || groupZimbraId == null) {
            return false;
        }
        return Provisioning.getInstance().inACLGroup(mAuthAcct, groupZimbraId);
    }

    public static abstract class PassThruGalSearchResultCallback extends GalSearchResultCallback {
        protected Element mProxiedResponse;
        protected boolean mPagingSupported; // default to false

        public PassThruGalSearchResultCallback(GalSearchParams params) {
            super(params);
        }

        @Override
        public boolean passThruProxiedGalAcctResponse() {
            return true;
        }

        @Override
        public void handleProxiedResponse(Element resp) {
            mProxiedResponse = resp;
            mProxiedResponse.detach();
        }

        @Override
        public Element getResponse() {
            if (mProxiedResponse != null)
                return mProxiedResponse;
            else {
                Element resp = super.getResponse();
                resp.addAttribute(AccountConstants.A_PAGINATION_SUPPORTED, mPagingSupported);
                return super.getResponse();
            }
        }

        @Override
        public void handleElement(Element e) throws ServiceException {
            // should never be called
            throw ServiceException.FAILURE("internal error, method should not be called", null);
        }

        @Override
        public void setQueryOffset(int offset) {
            super.setQueryOffset(offset);
            mPagingSupported = true;
        }

    }
}
