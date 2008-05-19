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

        PermUtil.modifyACEs(account, aces);
        Element response = zsc.createElement(MailConstants.GRANT_PERMISSIONS_RESPONSE);

        return response;
    }
    
    ZimbraACE handleACE(Element eACE, ZimbraSoapContext zsc) throws ServiceException {
        Right right = Right.fromCode(eACE.getAttribute(MailConstants.A_RIGHT));
        GranteeType gtype = GranteeType.fromCode(eACE.getAttribute(MailConstants.A_GRANT_TYPE));
        String zid = eACE.getAttribute(MailConstants.A_ZIMBRA_ID, null);
        boolean deny = eACE.getAttributeBool(MailConstants.A_DENY, false);
        
        NamedEntry nentry = null;
        if (gtype == GranteeType.GT_PUBLIC) {
            if (zid == null)
                zid = ACL.GUID_PUBLIC;
            if (!ACL.GUID_PUBLIC.equals(zid))
                throw ServiceException.INVALID_REQUEST("invalid id for public grantee type", null);
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
