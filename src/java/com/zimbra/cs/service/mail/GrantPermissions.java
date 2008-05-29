package com.zimbra.cs.service.mail;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.PermUtil;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.soap.ZimbraSoapContext;

public class GrantPermissions extends MailDocumentHandler {
    
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");
        
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        for (Element eACE : request.listElements(MailConstants.E_ACE)) {
            ZimbraACE ace = handleACE(eACE, zsc);
            aces.add(ace);
        }

        Set<ZimbraACE> granted = PermUtil.grantAccess(account, aces);
        Element response = zsc.createElement(MailConstants.GRANT_PERMISSIONS_RESPONSE);
        if (aces != null) {
            for (ZimbraACE ace : granted)
                ToXML.encodeACE(response, ace);
        }
        /*
         * This is done in FolderAction.OP_GRANT, should we do the same?
         * 
        // kinda hacky -- return the zimbra id and name of the grantee in the response
        result.addAttribute(MailConstants.A_ZIMBRA_ID, zid);
        if (nentry != null)
            result.addAttribute(MailConstants.A_DISPLAY, nentry.getName());
        else if (gtype == ACL.GRANTEE_GUEST)
            result.addAttribute(MailConstants.A_DISPLAY, zid);
     
        */ 
        return response;
    }
    
    // orig: FolderAction
    ZimbraACE handleACE(Element eACE, ZimbraSoapContext zsc) throws ServiceException {
        Right right = RightManager.getInstance().getRight(eACE.getAttribute(MailConstants.A_RIGHT));
        GranteeType gtype = GranteeType.fromCode(eACE.getAttribute(MailConstants.A_GRANT_TYPE));
        String zid = eACE.getAttribute(MailConstants.A_ZIMBRA_ID, null);
        boolean deny = eACE.getAttributeBool(MailConstants.A_DENY, false);
        String password = null;
        NamedEntry nentry = null;
        
        if (gtype == GranteeType.GT_AUTHUSER) {
            zid = ACL.GUID_AUTHUSER;
        } else if (gtype == GranteeType.GT_PUBLIC) {
            zid = ACL.GUID_PUBLIC;
        } else if (gtype == GranteeType.GT_GUEST) {
            zid = eACE.getAttribute(MailConstants.A_DISPLAY);
            if (zid == null || zid.indexOf('@') < 0)
                throw ServiceException.INVALID_REQUEST("invalid guest id or password", null);
            // make sure they didn't accidentally specify "guest" instead of "usr"
            try {
                nentry = PermUtil.lookupGranteeByName(zid, GranteeType.GT_USER, zsc);
                zid = nentry.getId();
                gtype = nentry instanceof DistributionList ? GranteeType.GT_GROUP : GranteeType.GT_USER;
            } catch (ServiceException e) {
                // this is the normal path, where lookupGranteeByName throws account.NO_SUCH_USER
                password = eACE.getAttribute(MailConstants.A_ARGS);
            }
        } else if (zid != null) {
            nentry = PermUtil.lookupGranteeByZimbraId(zid, gtype);
        } else {
            nentry = PermUtil.lookupGranteeByName(eACE.getAttribute(MailConstants.A_DISPLAY), gtype, zsc);
            zid = nentry.getId();
            // make sure they didn't accidentally specify "usr" instead of "grp"
            if (gtype == GranteeType.GT_USER && nentry instanceof DistributionList)
                gtype = GranteeType.GT_GROUP;
        }
        
        return new ZimbraACE(zid, gtype, right, deny);

    }

}
