package com.zimbra.client;

import java.util.Map;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.ZAttr;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.admin.type.Attr;
import com.zimbra.soap.admin.type.DomainInfo;

public class ZDomain {
     private Map<String,Object> attrs;

     public ZDomain(DomainInfo domainInfo) throws ServiceException {
            attrs =  Attr.collectionToMap(domainInfo.getAttrList());
     }

     public Map<String, Object> getAttrs() {
            return attrs;
     }

     @ZAttr(id=1141)
     public String[] getWebClientLoginURLAllowedUA() {
          return getMultiAttr(ZAttrProvisioning.A_zimbraWebClientLogoutURLAllowedUA);
       }

     @ZAttr(id=1352)
     public String[] getWebClientLoginURLAllowedIP() {
         return getMultiAttr(ZAttrProvisioning.A_zimbraWebClientLoginURLAllowedIP);
     }

     @ZAttr(id=1339)
     public int getWebClientMaxInputBufferLength() {
         return getIntAttr(ZAttrProvisioning.A_zimbraWebClientMaxInputBufferLength, 1024);
     }

     @ZAttr(id=1142)
     public String[] getWebClientLogoutURLAllowedUA() {
         return getMultiAttr(ZAttrProvisioning.A_zimbraWebClientLogoutURLAllowedUA);
     }

     @ZAttr(id=1353)
     public String[] getWebClientLogoutURLAllowedIP() {
         return getMultiAttr(ZAttrProvisioning.A_zimbraWebClientLogoutURLAllowedIP);
     }

     @ZAttr(id=355)
     public String getPrefSkin() {
         return getAttr(ZAttrProvisioning.A_zimbraPrefSkin);
     }

     @ZAttr(id=649)
     public String getSkinLogoURL() {
         return getAttr(ZAttrProvisioning.A_zimbraSkinLogoURL);
     }

     @ZAttr(id=671)
     public String getSkinLogoAppBanner() {
         return getAttr(ZAttrProvisioning.A_zimbraSkinLogoAppBanner);
     }

     @ZAttr(id=670)
     public String getSkinLogoLoginBanner() {
         return getAttr(ZAttrProvisioning.A_zimbraSkinLogoLoginBanner);
     }

     @ZAttr(id=696)
     public String getAdminConsoleLoginURL() {
         return getAttr(ZAttrProvisioning.A_zimbraAdminConsoleLoginURL);
     }

     @ZAttr(id=1687)
     public boolean isWebClientStaySignedInDisabled() {
         return getBooleanAttr(ZAttrProvisioning.A_zimbraWebClientStaySignedInDisabled, false);
     }

     @ZAttr(id=648)
     public String getSkinBackgroundColor() {
         return getAttr(ZAttrProvisioning.A_zimbraSkinBackgroundColor);
     }

     @ZAttr(id=647)
     public String getSkinForegroundColor() {
         return getAttr(ZAttrProvisioning.A_zimbraSkinForegroundColor);
     }

     @ZAttr(id=668)
     public String getSkinSecondaryColor() {
         return getAttr(ZAttrProvisioning.A_zimbraSkinSecondaryColor);
     }

     @ZAttr(id=669)
     public String getSkinSelectionColor() {
         return getAttr(ZAttrProvisioning.A_zimbraSkinSelectionColor);
     }

     @ZAttr(id=800)
     public String getSkinFavicon() {
          return getAttr(ZAttrProvisioning.A_zimbraSkinFavicon);
       }

     public boolean getBooleanAttr(String name, boolean defaultValue) {
         String v = getAttr(name);
         return v == null ? defaultValue : ProvisioningConstants.TRUE.equals(v);
     }

     private String[] getMultiAttr(String name) {
         String[] sEmptyMulti = new String[0];
         Object v = attrs.get(name);
         if (v == null) {
            return sEmptyMulti;
         } else if (v instanceof String) {
             return new String[]{(String) v};
         } else if (v instanceof String[]) {
             return (String[]) v;
         } else {
             return sEmptyMulti;
         }
     }

     private int getIntAttr(String name, int defaultValue) {
         String v = getAttr(name);
         try {
             return v == null ? defaultValue : Integer.parseInt(v);
         } catch (NumberFormatException e) {
             return defaultValue;
         }
     }

     private String getAttr(String name) {
         Object v = attrs.get(name);
         if (v == null) {
            return null;
         } else if (v instanceof String) {
             return (String) v;
         } else if (v instanceof String[]) {
             String[] a = (String[]) v;
             return a.length > 0 ? a[0] : null;
         } else {
             return null;
         }
     }
}
