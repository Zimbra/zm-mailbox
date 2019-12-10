/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.account;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.zimbra.common.account.ZAttrProvisioning.AccountStatus;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.PublishedShareInfoVisitor;
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.account.ShareInfoData;
import com.zimbra.cs.account.names.NameUtil;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.GetShareInfoRequest;
import com.zimbra.soap.account.message.GetShareInfoResponse;
import com.zimbra.soap.base.GetShareInfoResponseInterface;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.GranteeChooser;

public class GetShareInfo  extends AccountDocumentHandler {

    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) throws ServiceException {

        if (isInternal(request)) {
            // we have been proxied to this server because the specified
            // "owner account" is homed here.
            // Do not proxy in this case.
            return null;
        } else {
            // call super class, go the normal route to proxy to
            // the requested account's home server
            return super.proxyIfNecessary(request, context);
        }
    }

    private boolean isInternal(Element request) throws ServiceException {
        return request.getAttributeBool(AccountConstants.A_INTERNAL, false);
    }

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account)) {
            throw ServiceException.PERM_DENIED("can not access account");
        }

        GetShareInfoResponse response = new GetShareInfoResponse();
        doGetShareInfo(zsc, context, account, request, response);

        return zsc.jaxbToElement(response);
    }

    /**
     * @param zsc
     * @param targetAcct
     * @param request
     * @param response
     */
    private void doGetShareInfo(ZimbraSoapContext zsc, Map<String, Object> context,
            Account targetAcct, Element request, GetShareInfoResponse response) throws ServiceException {

        Provisioning prov = Provisioning.getInstance();

        GetShareInfoRequest req = zsc.elementToJaxb(request);
        GranteeChooser granteeChooser = req.getGrantee();
        byte granteeType = getGranteeType(granteeChooser);
        String granteeId = granteeChooser == null ? null : granteeChooser.getId();
        String granteeName = granteeChooser == null ? null : granteeChooser.getName();

        Account owner = null;
        AccountSelector ownerSelector = req.getOwner();
        if (ownerSelector != null) {
            owner = prov.get(ownerSelector);

            // to defend against harvest attacks return "no shares" instead of error
            // when an invalid user name/id is used.
            if ((owner == null) || (owner.isAccountExternal())) {
                return;
            } else {
                AccountStatus status = owner.getAccountStatus();
                if (status != null && status.isClosed()) {
                    return;
                }
            }
        }

        OperationContext octxt = getOperationContext(zsc, context);


        ShareInfo.MountedFolders mountedFolders = null;
        if (!Boolean.TRUE.equals(req.getInternal())) {
            // this (new ShareInfo.MountedFolders) should be executed on the requested
            // account's home server.
            // If we get here, we should be proxied to the right server naturally by the framework.
            mountedFolders = new ShareInfo.MountedFolders(octxt, targetAcct);
        }

        ResultFilter resultFilter;
        if (Boolean.FALSE.equals(req.getIncludeSelf())) {
            resultFilter = new ResultFilterByTargetExcludeSelf(granteeId, granteeName, targetAcct);
        } else {
            resultFilter = new ResultFilterByTarget(granteeId, granteeName);
        }

        String filterDomain = null;
        if (LC.PUBLIC_SHARE_VISIBILITY.samePrimaryDomain.equals(LC.getPublicShareAdvertisingScope())) {
            filterDomain = targetAcct.getDomainName();
        }
        ResultFilter resultFilter2 = new ResultFilterForPublicShares(filterDomain);

        ShareInfoVisitor visitor = new ShareInfoVisitor(prov, response, mountedFolders,
                resultFilter, resultFilter2);
        if (owner == null) {
            // retrieve from published share info
            ShareInfo.Published.get(prov, targetAcct, granteeType, owner, visitor);
        } else {
            // iterate all folders of the owner, this should be proxied to the owner account's
            // home server if the owner account does not reside on the same server as the requesting
            // account.

            if (targetAcct.getId().equals(owner.getId()))
                throw ServiceException.INVALID_REQUEST("cannot discover shares on self", null);

            if (Provisioning.onLocalServer(owner))
                ShareInfo.Discover.discover(octxt, prov, targetAcct, granteeType, owner, visitor);
            else {
                // issue an GetShareInfoRequest to the home server of the owner, and tell it *not*
                // to proxy to the requesting account's mailbox server.
                fetchRemoteShareInfo(context, request, owner.getId(), visitor);
            }
        }

        visitor.finish();
    }

    private void fetchRemoteShareInfo(Map<String, Object> context,
            Element request, String ownerId, ShareInfoVisitor visitor)
    throws ServiceException {

        /*
         * hack, there is no way to tell the proxying code to set
         * the <targetServer> element in the SOAP context so the
         * request won't be proxied again back to this server.
         *
         * mark the proxy request "internal" to indicate to the
         * handler to:
         *
         * 1. Do not proxy the request again (normal flow would be to
         *    proxy to the home server of the requested account, which is this server)
         *
         * 2. Do not get the mounted info of the requesting account, because
         *    it won't be able to access the mailbox of the requested account,
         *    which lives on this server.
         */
        request.addAttribute(AccountConstants.A_INTERNAL, true);

        Element response = proxyRequest(request, context, ownerId);
        for (Element eShare : response.listElements(AccountConstants.E_SHARE)) {
            ShareInfoData sid = ShareInfoData.fromXML(eShare);
            visitor.visit(sid);
        }
    }

    public static byte getGranteeType(GranteeChooser granteeChooser) throws ServiceException {
        String granteeType = null;
        if (granteeChooser != null) {
            granteeType = granteeChooser.getType();
        }
        return getGranteeType(granteeType);
    }

    public static byte getGranteeType(String granteeType) throws ServiceException {
        return (granteeType == null) ? 0 : ACL.stringToType(granteeType);
    }

    public static interface ResultFilter {
        /*
         * return true if filtered in, false if filtered out
         */
        public boolean check(ShareInfoData sid);
    }

    public static class ResultFilterByTarget implements ResultFilter {
        String mGranteeId;
        String mGranteeName;

        public ResultFilterByTarget(String granteeId, String granteeName) {
            mGranteeId = granteeId;
            mGranteeName = granteeName;
        }

        @Override
        public boolean check(ShareInfoData sid) {
            if (mGranteeId != null && !mGranteeId.equals(sid.getGranteeId()))
                return false;

            if (mGranteeName != null && !mGranteeName.equals(sid.getGranteeName()))
                return false;

            return true;
        }
    }

    public static class ResultFilterByTargetExcludeSelf extends ResultFilterByTarget {
        Account mSelf;

        public ResultFilterByTargetExcludeSelf(String granteeId, String granteeName, Account self) {
            super(granteeId, granteeName);
            mSelf = self;
        }

        @Override
        public boolean check(ShareInfoData sid) {
            if (mSelf.getId().equals(sid.getOwnerAcctId()))
                return false;

            return super.check(sid);
        }
    }

    /**
     * Used to restrict visibility of public shares
     */
    public static class ResultFilterForPublicShares implements ResultFilter {
        private final String ownerDomainFilter;
        private final boolean hideAllPublicShares;

        public ResultFilterForPublicShares(String ownerDomainFilter) {
            this.ownerDomainFilter = ownerDomainFilter;
            hideAllPublicShares =
                    LC.PUBLIC_SHARE_VISIBILITY.none.equals(LC.getPublicShareAdvertisingScope());
        }

        @Override
        public boolean check(ShareInfoData sid) {
            if (ACL.GRANTEE_PUBLIC != sid.getGranteeTypeCode()) {
                return true; // only interested in filtering out public shares
            }
            if (hideAllPublicShares) {
                ZimbraLog.misc.debug("Skipping unmounted public share '%s'", sid.getName());
                return false;
            }
            if (ownerDomainFilter == null) {
                return true; // nothing to filter by
            }
            String ownerDomain;
            try {
                ownerDomain = NameUtil.EmailAddress.getDomainNameFromEmail(sid.getOwnerAcctEmail());
                if (ownerDomainFilter.equalsIgnoreCase(ownerDomain)) {
                    return true;
                } else {
                    ZimbraLog.misc.debug("Skipping public share '%s' - owner domain '%s' is not the same as '%s'",
                            sid.getName(), ownerDomain, ownerDomainFilter);
                }
            } catch (ServiceException e) {
                ZimbraLog.misc.debug("Problem checking domain of share owner '%s'", sid.getOwnerAcctEmail(), e);
            }
            return false;
        }
    }

    public static class ShareInfoVisitor implements PublishedShareInfoVisitor {
        Provisioning mProv;
        GetShareInfoResponseInterface mResp;
        ShareInfo.MountedFolders mMountedFolders;
        ResultFilter mResultFilter;
        ResultFilter resultFilterForUnmounted;
        SortedSet<ShareInfoData> mSortedShareInfo = new TreeSet<ShareInfoData>(new ShareInfoComparator());

        private static class ShareInfoComparator implements Comparator<ShareInfoData> {
            @Override
            public int compare(ShareInfoData a, ShareInfoData b) {
                int r = a.getPath().compareToIgnoreCase(b.getPath());
                if (r == 0) {
                    r = a.getOwnerAcctEmail().compareToIgnoreCase(b.getOwnerAcctEmail());
                }
                if (r == 0) {
                    if (a.getGranteeName() != null && b.getGranteeName() != null) {
                        r = a.getGranteeName().compareToIgnoreCase(b.getGranteeName());
                    } else if (a.getGranteeName() == null) {
                        r = b.getGranteeName() == null ? 0 : -1;
                    } else {
                        r = 1;
                    }
                }
                if (r == 0) {
                    r = a.getGranteeType().compareTo(b.getGranteeType());
                }
                return r;
            }
        }

        public ShareInfoVisitor(Provisioning prov, GetShareInfoResponseInterface resp,
                ShareInfo.MountedFolders mountedFolders, ResultFilter resultFilter) {
            this(prov, resp, mountedFolders, resultFilter, null);
        }

        public ShareInfoVisitor(Provisioning prov, GetShareInfoResponseInterface resp,
                ShareInfo.MountedFolders mountedFolders, ResultFilter resultFilter,
                ResultFilter resultFilter2) {
            mProv = prov;
            mResp = resp;
            mMountedFolders = mountedFolders;
            mResultFilter = resultFilter;
            this.resultFilterForUnmounted = resultFilter2;
        }

        // sorting and filtering visitor
        // note: if grnteeType is filtered at ShareInfo
        @Override
        public void visit(ShareInfoData sid) throws ServiceException {
            // add the result if there is no filter or the result passes the filter test
            if (mResultFilter == null || mResultFilter.check(sid)) {
                // secondary filter can be used to hide some unmounted
                if ((resultFilterForUnmounted == null) || (getMountpointId(sid) != null) ||
                            resultFilterForUnmounted.check(sid)) {
                    mSortedShareInfo.add(sid);
                }
            }
        }

        public void finish() throws ServiceException {
            for (ShareInfoData sid : mSortedShareInfo) {
                doVisit(sid);
            }
        }

        // the real visitor
        private void doVisit(ShareInfoData sid) throws ServiceException {
            // Also adds mountpoint id to XML if the share is already mounted
            mResp.addShare(sid.toJAXB(getMountpointId(sid)));
        }

        private Integer getMountpointId(ShareInfoData sid) throws ServiceException {
            Integer mptId = null;
            if (mMountedFolders != null) {
                mptId = mMountedFolders.getLocalFolderId(sid.getOwnerAcctId(), sid.getItemId());
            }
            return mptId;
        }
    }

}
