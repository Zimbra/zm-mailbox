/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.operation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.SpamHandler;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.ZimbraSoapContext;

public class ItemActionOperation extends Operation {

	// each of these is "per operation" -- real load is calculated as LOAD * local.size()
	private static final int DELETE_MULT = 3;
	private static final int SPAM_MULT = 4;

	protected static int LOAD = 3;
	private static int MAX = 20;
	private static int SCALE = 10;
	static {
		Operation.Config c = loadConfig(ItemActionOperation.class);
		if (c != null) {
			LOAD = c.mLoad;
			if (c.mScale > 0)
				SCALE = c.mScale;
			if (c.mMaxLoad > 0)
				MAX = c.mMaxLoad;
		}
	}
	
	public static ItemActionOperation TAG(ZimbraSoapContext zc, Session session, OperationContext oc,
				Mailbox mbox, Requester req, 
				List<Integer> ids, byte type, 
				boolean flagValue, TargetConstraint tcon, 
				int tagId) throws ServiceException {
		ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
					LOAD, ids, Op.TAG,  type, flagValue, tcon);
		ia.setTagId(tagId);
		ia.schedule();
		return ia;
	}
	
	public static ItemActionOperation FLAG(ZimbraSoapContext zc, Session session, OperationContext oc,
				Mailbox mbox, Requester req, 
				List<Integer> ids, byte type, 
				boolean flagValue, TargetConstraint tcon) throws ServiceException {
		ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
					LOAD, ids, Op.FLAG,  type, flagValue, tcon);
		ia.schedule();
		return ia;
	}
	
	public static ItemActionOperation READ(ZimbraSoapContext zc, Session session, OperationContext oc,
				Mailbox mbox, Requester req, 
				List<Integer> ids, byte type, 
				boolean flagValue, TargetConstraint tcon) throws ServiceException {
		ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
					LOAD, ids, Op.READ,  type, flagValue, tcon);
		ia.schedule();
		return ia;
	}
	
	public static ItemActionOperation COLOR(ZimbraSoapContext zc, Session session, OperationContext oc,
				Mailbox mbox, Requester req, 
				List<Integer> ids, byte type, 
				boolean flagValue, TargetConstraint tcon,
				byte color) throws ServiceException {
		ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
					LOAD, ids, Op.COLOR,  type, flagValue, tcon);
		ia.setColor(color);
		ia.schedule();
		return ia;
	}

	public static ItemActionOperation HARD_DELETE(ZimbraSoapContext zc, Session session, OperationContext oc,
				Mailbox mbox, Requester req, 
				List<Integer> ids, byte type, 
				boolean flagValue, TargetConstraint tcon) throws ServiceException {
		ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
					DELETE_MULT*LOAD, ids, Op.HARD_DELETE,  type, flagValue, tcon);
		ia.schedule();
		return ia;
	}
	
	public static ItemActionOperation MOVE(ZimbraSoapContext zc, Session session, OperationContext oc,
				Mailbox mbox, Requester req, 
				List<Integer> ids, byte type, 
				boolean flagValue, TargetConstraint tcon,
				ItemId iidFolder) throws ServiceException {
		ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
					LOAD, ids, Op.MOVE,  type, flagValue, tcon);
		ia.setIidFolder(iidFolder);
		ia.schedule();
		return ia;
	}

	public static ItemActionOperation SPAM(ZimbraSoapContext zc, Session session, OperationContext oc,
				Mailbox mbox, Requester req, 
				List<Integer> ids, byte type, 
				boolean flagValue, TargetConstraint tcon,
				int folderId) throws ServiceException {
		ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
					SPAM_MULT*LOAD, ids, Op.SPAM,  type, flagValue, tcon);
		ia.setFolderId(folderId);
		ia.schedule();
		return ia;
	}
	
	public static ItemActionOperation UPDATE(ZimbraSoapContext zc, Session session, OperationContext oc,
				Mailbox mbox, Requester req, List<Integer> ids, byte type, 
				TargetConstraint tcon, ItemId iidFolder,
				String flags, String tags, byte color) throws ServiceException {
		ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
					LOAD, ids, Op.UPDATE, type, true, tcon);
		ia.setIidFolder(iidFolder);
		ia.setFlags(flags);
		ia.setTags(tags);
		ia.setColor(color);
		ia.schedule();
		return ia;
	}
				
	
	public static enum Op {
		TAG("tag"),
		FLAG("flag"),
		READ("read"),
		COLOR("color"),
		HARD_DELETE("delete"),
		MOVE("move"),
		SPAM("spam"),
		UPDATE("update")
		;
		
		private Op(String str) {
			mStr = str;
		}
		
		public String toString() { return mStr; }
		
		private String mStr;
	}
	
    protected String mResult;
	
    protected int[] mIds;
	protected Op mOp;
    protected byte mType;
    protected boolean mFlagValue;
    protected TargetConstraint mTcon;

	// only when Op=TAG
    protected int mTagId;
	 
	// only when OP=COLOR or OP=UPDATE
    protected byte mColor;
	
	// only when OP=MOVE or OP=UPDATE
    protected ItemId mIidFolder; 
	
	// only when OP=SPAM
    protected int mFolderId;

	// only when OP=UPDATE
    protected String mFlags;
    protected String mTags;
	
	// TEMPORARY -- just until dan implements the ItemIdFormatter
    protected ZimbraSoapContext mZc;
	
	
	public String toString() {
		StringBuilder toRet = new StringBuilder(super.toString());
		
		toRet.append(" Op=").append(mOp.toString());
		toRet.append(" Type=").append(mType);
		toRet.append(" FlagValue=").append(mFlagValue);
		if (mTcon != null) 
			toRet.append(" TargetConst=").append(mTcon.toString());
		
		if (mOp == Op.TAG) 
			toRet.append(" TagId=").append(mTagId);
		
		if (mOp == Op.COLOR || mOp == Op.UPDATE)
			toRet.append(" Color=").append(mColor);
		
		if (mOp == Op.MOVE || mOp == Op.UPDATE) 
			toRet.append(" iidFolder=").append(mIidFolder);
		
		if (mOp == Op.SPAM) 
			toRet.append(" folderId=").append(mFolderId);
		
		if (mOp == Op.UPDATE) {
			if (mFlags != null) 
				toRet.append(" flags=").append(mFlags);
			if (mTags != null) 
				toRet.append(" tags=").append(mTags);
		}
		return toRet.toString();
	}
	
	public void setTagId(int tagId) {
		assert(mOp == Op.TAG);
		mTagId = tagId;
	}
	public void setColor(byte color) { 
		assert((mOp == Op.COLOR) || (mOp == Op.UPDATE));
		mColor = color; 
	}
	public void setIidFolder(ItemId iidFolder)  { 
		assert((mOp == Op.MOVE) || (mOp == Op.UPDATE));
		mIidFolder = iidFolder; 
	}
	public void setFolderId(int folderId) {
		assert(mOp == Op.SPAM);
		mFolderId = folderId; 
	}
	public void setFlags(String flags) {
		assert(mOp == Op.UPDATE);
		mFlags = flags; 
	}
	public void setTags(String tags) {                        
		assert(mOp == Op.UPDATE);
		mTags = tags; 
	}
	
	ItemActionOperation(ZimbraSoapContext zsc, Session session, OperationContext oc,
				Mailbox mbox, Requester req, int baseLoad, 
				List<Integer> ids, Op op, byte type, 
				boolean flagValue, TargetConstraint tcon)
    throws ServiceException {
		super(session, oc, mbox, req, req.getPriority(), Math.min(ids.size() > 0 ? ids.size() * (baseLoad / SCALE): baseLoad, MAX));
		mZc = zsc;

        int i = 0;
		mIds = new int[ids.size()];
        for (int id : ids)
            mIds[i++] = id;

		mOp = op;
		if (mOp == null)
			throw ServiceException.INVALID_REQUEST("unknown operation: null", null);
		mType = type;
		mFlagValue = flagValue;
		mTcon = tcon;
	}
	
	protected void callback() throws ServiceException {
		StringBuilder successes = new StringBuilder();
		
		// iterate over the local items and perform the requested operation
		switch (mOp) {
			case FLAG:
				getMailbox().alterTag(getOpCtxt(), mIds, mType, Flag.ID_FLAG_FLAGGED, mFlagValue, mTcon);
				break;
			case READ:
				getMailbox().alterTag(getOpCtxt(), mIds, mType, Flag.ID_FLAG_UNREAD, !mFlagValue, mTcon);
				break;
			case TAG:
				getMailbox().alterTag(getOpCtxt(), mIds, mType, mTagId, mFlagValue, mTcon);
				break;
			case COLOR:
				getMailbox().setColor(getOpCtxt(), mIds, mType, mColor);
				break;
			case HARD_DELETE:
				getMailbox().delete(getOpCtxt(), mIds, mType, mTcon);
				break;
			case MOVE:
				getMailbox().move(getOpCtxt(), mIds, mType, mIidFolder.getId(), mTcon);
				break;
			case SPAM:
				getMailbox().move(getOpCtxt(), mIds, mType, mFolderId, mTcon);
                for (int id : mIds)
                    SpamHandler.getInstance().handle(getMailbox(), id, mType, mFlagValue);
				break;
			case UPDATE:
				if (!mIidFolder.belongsTo(getMailbox()))
					throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
				
				if (mIidFolder.getId() > 0)
					getMailbox().move(getOpCtxt(), mIds, mType, mIidFolder.getId(), mTcon);
				if (mTags != null || mFlags != null)
					getMailbox().setTags(getOpCtxt(), mIds, mType, mFlags, mTags, mTcon);
				if (mColor >= 0)
					getMailbox().setColor(getOpCtxt(), mIds, mType, mColor);
				break;
			default:
				throw ServiceException.INVALID_REQUEST("unknown operation: " + mOp, null);
		}

        for (int id : mIds)
            successes.append(successes.length() > 0 ? "," : "").append(mZc.formatItemId(id));
		mResult = successes.toString();
	}
	
	public String getResult() {
		return mResult;
	}
}
