package com.liquidsys.coco.util;

import java.io.UnsupportedEncodingException;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.Domain;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.ServiceException;

public class AccountUtil {

    public static InternetAddress getOutgoingFromAddress(Account acct) throws UnsupportedEncodingException {
        // check "displayName" for personal part, and fall back to "cn" if not present
        String personalPart = acct.getAttr(Provisioning.A_displayName);
        if (personalPart == null)
            personalPart = acct.getAttr(Provisioning.A_cn);
        // catch the case where no real name was present and so cn was defaulted to the username
        if (personalPart == null || personalPart.trim().equals("") || personalPart.equals(acct.getAttr("uid")))
            personalPart = null;

        String fromAddress;
        try {
            fromAddress = getCanonicalAddress(acct);
        } catch (ServiceException se) {
            LiquidLog.misc.warn("unexpected exception canonicalizing address, will use account name", se);
            fromAddress = acct.getName();
        }

        return new InternetAddress(fromAddress, personalPart);
    }

    public static boolean isDirectRecipient(Account acct, MimeMessage mm) throws ServiceException, MessagingException {
        return isDirectRecipient(acct, mm, -1);
    }
    
    public static boolean isDirectRecipient(Account acct, MimeMessage mm, int maxToCheck) throws ServiceException, MessagingException {
        String accountAddress = acct.getName();
        String canonicalAddress = getCanonicalAddress(acct);
        String[] accountAliases = acct.getAliases();
        Address[] recipients = mm.getAllRecipients();

        int numRecipientsToCheck = (maxToCheck <= 0 ? recipients.length : Math.min(recipients.length, maxToCheck));
        for (int i = 0; i < numRecipientsToCheck; i++) {
            String msgAddress = ((InternetAddress) recipients[i]).getAddress();
            if (addressMatchesAccount(accountAddress, canonicalAddress, accountAliases, msgAddress)) 
                return true;
        }
        return false;
    }

    /* We do a more lightweight canonicalization that postfix, because
     * we except to set the LDAP attributes only in certain ways.  For
     * instance we do not canonicalize the local part by itself.
     */
    public static String getCanonicalAddress(Account account) throws ServiceException {
        // If account has a canonical address, let's use that.
        String ca = account.getAttr(Provisioning.A_liquidMailCanonicalAddress);
        
        // But we still have to canonicalize domain names, so do that with account address
        if (ca == null) {
            ca = account.getName();
        }
        
        String[] parts = EmailUtil.getLocalPartAndDomain(ca);
        if (parts == null) {
            return ca;
        }
        
        Domain domain = Provisioning.getInstance().getDomainByName(parts[1]);
        if (domain == null) {
            return ca;
        }

        String domainCatchAll = domain.getAttr(Provisioning.A_liquidMailCatchAllCanonicalAddress);
        if (domainCatchAll != null) {
            return parts[0] + domainCatchAll;
        }
        
        return ca;
    }
    
    /**
     * True if this address matches some address for this account (aliases, domain re-writes, etc)
     */
    public static boolean addressMatchesAccount(Account acct, String givenAddress) throws ServiceException {
        String accountAddress = acct.getName();
        String canonicalAddress = getCanonicalAddress(acct);
        String[] accountAliases = acct.getAliases();
        return addressMatchesAccount(accountAddress, canonicalAddress, accountAliases, givenAddress);
    }
    
    private static boolean addressMatchesAccount(String accountAddress, String canonicalAddress, String[] accountAliases, String givenAddress) throws ServiceException {
        if (givenAddress.equalsIgnoreCase(accountAddress)) {
            return true;
        }
        if (givenAddress.equalsIgnoreCase(canonicalAddress)) {
            return true;
        }
        for (int j = 0; j < accountAliases.length; j++) {
            if (givenAddress.equalsIgnoreCase(accountAliases[j]))
                return true;
        }
        return false;
    }
    
//    /**
//     * True if this mime message has at least one recipient that is NOT the same as the specified account
//     * 
//     * @param acct
//     * @param mm
//     * @return
//     * @throws ServiceException
//     */
//    public static boolean hasExternalRecipient(Account acct, MimeMessage mm) throws ServiceException, MessagingException
//    {
//        int maxToCheck = -1;
//        String accountAddress = acct.getName();
//        String canonicalAddress = getCanonicalAddress(acct);
//        String[] accountAliases = acct.getAliases();
//        Address[] recipients = mm.getAllRecipients();
//        
//        if (recipients != null) {
//            int numRecipientsToCheck = (maxToCheck <= 0 ? recipients.length : Math.min(recipients.length, maxToCheck));
//            for (int i = 0; i < numRecipientsToCheck; i++) {
//                String msgAddress = ((InternetAddress) recipients[i]).getAddress();
//                if (!addressMatchesAccount(accountAddress, canonicalAddress, accountAliases, msgAddress)) 
//                    return true;
//            }
//        }
//        return false;
//    }
}
