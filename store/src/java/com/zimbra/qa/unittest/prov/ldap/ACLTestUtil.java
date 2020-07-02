/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest.prov.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCookieStore;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.account.accesscontrol.TargetType;

import junit.framework.AssertionFailedError;

public class ACLTestUtil {

    /*
     * if not running from ZimbraServer/build.xml, do:
     *
     * zmlocalconfig -e debug_running_unittest=true
     * cp /Users/pshao/p4/main/ZimbraServer/data/unittest/ldap/rights-unittest.xml /opt/zimbra/conf/rights
     *
     */

    // user rights
    static Right USER_LOGIN_AS;
    static Right USER_RIGHT;
    static Right USER_RIGHT_DISTRIBUTION_LIST;
    static Right USER_RIGHT_DOMAIN;
    static Right USER_RIGHT_RESTRICTED_GRANT_TARGET_TYPE;

    // admin preset rights
    static Right ADMIN_PRESET_LOGIN_AS;
    static Right ADMIN_PRESET_ACCOUNT;
    static Right ADMIN_PRESET_CALENDAR_RESOURCE;
    static Right ADMIN_PRESET_CONFIG;
    static Right ADMIN_PRESET_COS;
    static Right ADMIN_PRESET_DISTRIBUTION_LIST;
    static Right ADMIN_PRESET_DYNAMIC_GROUP;
    static Right ADMIN_PRESET_DOMAIN;
    static Right ADMIN_PRESET_GLOBALGRANT;
    static Right ADMIN_PRESET_SERVER;
    static Right ADMIN_PRESET_UC_SERVICE;
    static Right ADMIN_PRESET_XMPP_COMPONENT;
    static Right ADMIN_PRESET_ZIMLET;

    // admin attrs rights
    static Right ADMIN_ATTR_GETALL_ACCOUNT;
    static Right ADMIN_ATTR_SETALL_ACCOUNT;
    static Right ADMIN_ATTR_GETSOME_ACCOUNT;
    static Right ADMIN_ATTR_SETSOME_ACCOUNT;
    static Right ADMIN_ATTR_GETALL_CALENDAR_RESOURCE;
    static Right ADMIN_ATTR_SETALL_CALENDAR_RESOURCE;
    static Right ADMIN_ATTR_GETSOME_CALENDAR_RESOURCE;
    static Right ADMIN_ATTR_SETSOME_CALENDAR_RESOURCE;
    static Right ADMIN_ATTR_GETALL_CONFIG;
    static Right ADMIN_ATTR_SETALL_CONFIG;
    static Right ADMIN_ATTR_GETSOME_CONFIG;
    static Right ADMIN_ATTR_SETSOME_CONFIG;
    static Right ADMIN_ATTR_GETALL_COS;
    static Right ADMIN_ATTR_SETALL_COS;
    static Right ADMIN_ATTR_GETSOME_COS;
    static Right ADMIN_ATTR_SETSOME_COS;
    static Right ADMIN_ATTR_GETALL_DISTRIBUTION_LIST;
    static Right ADMIN_ATTR_SETALL_DISTRIBUTION_LIST;
    static Right ADMIN_ATTR_GETSOME_DISTRIBUTION_LIST;
    static Right ADMIN_ATTR_SETSOME_DISTRIBUTION_LIST;
    static Right ADMIN_ATTR_GETALL_DYNAMIC_GROUP;
    static Right ADMIN_ATTR_SETALL_DYNAMIC_GROUP;
    static Right ADMIN_ATTR_GETSOME_DYNAMIC_GROUP;
    static Right ADMIN_ATTR_SETSOME_DYNAMIC_GROUP;
    static Right ADMIN_ATTR_GETALL_DOMAIN;
    static Right ADMIN_ATTR_SETALL_DOMAIN;
    static Right ADMIN_ATTR_GETSOME_DOMAIN;
    static Right ADMIN_ATTR_SETSOME_DOMAIN;
    static Right ADMIN_ATTR_GETALL_SERVER;
    static Right ADMIN_ATTR_SETALL_SERVER;
    static Right ADMIN_ATTR_GETSOME_SERVER;
    static Right ADMIN_ATTR_SETSOME_SERVER;
    static Right ADMIN_ATTR_GETALL_UC_SERVICE;
    static Right ADMIN_ATTR_SETALL_UC_SERVICE;
    static Right ADMIN_ATTR_GETSOME_UC_SERVICE;
    static Right ADMIN_ATTR_SETSOME_UC_SERVICE;
    static Right ADMIN_ATTR_GETALL_ZIMLET;
    static Right ADMIN_ATTR_SETALL_ZIMLET;
    static Right ADMIN_ATTR_GETSOME_ZIMLET;
    static Right ADMIN_ATTR_SETSOME_ZIMLET;

    // admin combo rights
    static Right ADMIN_COMBO_ACCOUNT;
    static Right ADMIN_COMBO_CALENDAR_RESOURCE;
    static Right ADMIN_COMBO_CONFIG;
    static Right ADMIN_COMBO_COS;
    static Right ADMIN_COMBO_DISTRIBUTION_LIST;
    static Right ADMIN_COMBO_DYNAMIC_GROUP;
    static Right ADMIN_COMBO_DOMAIN;
    static Right ADMIN_COMBO_GLOBALGRANT;
    static Right ADMIN_COMBO_SERVER;
    static Right ADMIN_COMBO_UC_SERVICE;
    static Right ADMIN_COMBO_XMPP_COMPONENT;
    static Right ADMIN_COMBO_ZIMLET;
    // static Right ADMIN_COMBO_ALL;


    /*
     * init rights defined in rights-unittest.xml
     */
    static void initTestRights() throws Exception {
        USER_LOGIN_AS                 = User.R_loginAs;
        USER_RIGHT                    = getRight("test-user");
        USER_RIGHT_DISTRIBUTION_LIST  = getRight("test-user-distributionlist");
        USER_RIGHT_DOMAIN             = User.R_createDistList;
        USER_RIGHT_RESTRICTED_GRANT_TARGET_TYPE = User.R_sendAs;

        ADMIN_PRESET_LOGIN_AS          = Admin.R_adminLoginAs;
        ADMIN_PRESET_ACCOUNT           = getRight("test-preset-account");
        ADMIN_PRESET_CALENDAR_RESOURCE = getRight("test-preset-calendarresource");
        ADMIN_PRESET_CONFIG            = getRight("test-preset-globalconfig");
        ADMIN_PRESET_COS               = getRight("test-preset-cos");
        ADMIN_PRESET_DISTRIBUTION_LIST = getRight("test-preset-distributionlist");
        ADMIN_PRESET_DYNAMIC_GROUP     = getRight("test-preset-dynamicgroup");
        ADMIN_PRESET_DOMAIN            = getRight("test-preset-domain");
        ADMIN_PRESET_GLOBALGRANT       = getRight("test-preset-globalgrant");
        ADMIN_PRESET_SERVER            = getRight("test-preset-server");
        ADMIN_PRESET_UC_SERVICE        = getRight("test-preset-ucservice");
        ADMIN_PRESET_XMPP_COMPONENT    = getRight("test-preset-xmppcomponent");
        ADMIN_PRESET_ZIMLET            = getRight("test-preset-zimlet");

        ADMIN_ATTR_GETALL_ACCOUNT               = Admin.R_getAccount;
        ADMIN_ATTR_SETALL_ACCOUNT               = Admin.R_modifyAccount;
        ADMIN_ATTR_GETSOME_ACCOUNT              = getRight("test-getAttrs-account");
        ADMIN_ATTR_SETSOME_ACCOUNT              = getRight("test-setAttrs-account");
        ADMIN_ATTR_GETALL_CALENDAR_RESOURCE     = Admin.R_getCalendarResource;
        ADMIN_ATTR_SETALL_CALENDAR_RESOURCE     = Admin.R_modifyCalendarResource;
        ADMIN_ATTR_GETSOME_CALENDAR_RESOURCE    = getRight("test-getAttrs-calendarresource");
        ADMIN_ATTR_SETSOME_CALENDAR_RESOURCE    = getRight("test-setAttrs-calendarresource");
        ADMIN_ATTR_GETALL_CONFIG                = Admin.R_getGlobalConfig;
        ADMIN_ATTR_SETALL_CONFIG                = Admin.R_modifyGlobalConfig;
        ADMIN_ATTR_GETSOME_CONFIG               = getRight("test-getAttrs-globalconfig");
        ADMIN_ATTR_SETSOME_CONFIG               = getRight("test-setAttrs-globalconfig");
        ADMIN_ATTR_GETALL_COS                   = Admin.R_getCos;
        ADMIN_ATTR_SETALL_COS                   = Admin.R_modifyCos;
        ADMIN_ATTR_GETSOME_COS                  = getRight("test-getAttrs-cos");
        ADMIN_ATTR_SETSOME_COS                  = getRight("test-setAttrs-cos");
        ADMIN_ATTR_GETALL_DISTRIBUTION_LIST     = Admin.R_getDistributionList;
        ADMIN_ATTR_SETALL_DISTRIBUTION_LIST     = Admin.R_modifyDistributionList;
        ADMIN_ATTR_GETSOME_DISTRIBUTION_LIST    = getRight("test-getAttrs-distributionlist");
        ADMIN_ATTR_SETSOME_DISTRIBUTION_LIST    = getRight("test-setAttrs-distributionlist");
        ADMIN_ATTR_GETALL_DYNAMIC_GROUP         = Admin.R_getGroup;
        ADMIN_ATTR_SETALL_DYNAMIC_GROUP         = Admin.R_modifyGroup;
        ADMIN_ATTR_GETSOME_DYNAMIC_GROUP        = getRight("test-getAttrs-dynamicgroup");
        ADMIN_ATTR_SETSOME_DYNAMIC_GROUP        = getRight("test-setAttrs-dynamicgroup");
        ADMIN_ATTR_GETALL_DOMAIN                = Admin.R_getDomain;
        ADMIN_ATTR_SETALL_DOMAIN                = Admin.R_modifyDomain;
        ADMIN_ATTR_GETSOME_DOMAIN               = getRight("test-getAttrs-domain");
        ADMIN_ATTR_SETSOME_DOMAIN               = getRight("test-setAttrs-domain");
        ADMIN_ATTR_GETALL_SERVER                = Admin.R_getServer;
        ADMIN_ATTR_SETALL_SERVER                = Admin.R_modifyServer;
        ADMIN_ATTR_GETSOME_SERVER               = getRight("test-getAttrs-server");
        ADMIN_ATTR_SETSOME_SERVER               = getRight("test-setAttrs-server");
        ADMIN_ATTR_GETALL_UC_SERVICE            = Admin.R_getUCService;
        ADMIN_ATTR_SETALL_UC_SERVICE            = Admin.R_modifyUCService;
        ADMIN_ATTR_GETSOME_UC_SERVICE           = getRight("test-getAttrs-ucservice");
        ADMIN_ATTR_SETSOME_UC_SERVICE           = getRight("test-setAttrs-ucservice");
        ADMIN_ATTR_GETALL_ZIMLET                = Admin.R_getZimlet;
        ADMIN_ATTR_SETALL_ZIMLET                = Admin.R_modifyZimlet;
        ADMIN_ATTR_GETSOME_ZIMLET               = getRight("test-getAttrs-zimlet");
        ADMIN_ATTR_SETSOME_ZIMLET               = getRight("test-setAttrs-zimlet");

        ADMIN_COMBO_ACCOUNT           = getRight("test-combo-account");
        ADMIN_COMBO_CALENDAR_RESOURCE = getRight("test-combo-calendarresource");
        ADMIN_COMBO_CONFIG            = getRight("test-combo-globalconfig");
        ADMIN_COMBO_COS               = getRight("test-combo-cos");
        ADMIN_COMBO_DISTRIBUTION_LIST = getRight("test-combo-distributionlist");
        ADMIN_COMBO_DYNAMIC_GROUP     = getRight("test-combo-dynamicgroup");
        ADMIN_COMBO_DOMAIN            = getRight("test-combo-domain");
        ADMIN_COMBO_GLOBALGRANT       = getRight("test-combo-globalgrant");
        ADMIN_COMBO_SERVER            = getRight("test-combo-server");
        ADMIN_COMBO_UC_SERVICE        = getRight("test-combo-ucservice");
        ADMIN_COMBO_XMPP_COMPONENT    = getRight("test-combo-xmppcomponent");
        ADMIN_COMBO_ZIMLET            = getRight("test-combo-zimlet");
        // ADMIN_COMBO_ALL               = getRight("test-combo-all");
    }

    static Right getRight(String right) throws ServiceException {
        return RightManager.getInstance().getRight(right);
    }

    static enum AllowOrDeny {
        ALLOW(true, false),
        DELEGABLE(true, true),
        DENY(false, false);

        boolean mAllow;
        boolean mDelegable;

        AllowOrDeny(boolean allow, boolean delegable) {
            mAllow = allow;
            mDelegable = delegable;
        }

        boolean deny() {
            return !mAllow;
        }

        boolean allow() {
            return mAllow;
        }

        boolean delegable() {
            return mDelegable;
        }

        RightModifier toRightModifier() {
            if (deny())
                return RightModifier.RM_DENY;
            else if (delegable())
                return RightModifier.RM_CAN_DELEGATE;
            else
                return null;
        }
    }

    static enum GetOrSet {
        GET(true),
        SET(false);

        boolean mGet;

        GetOrSet(boolean get) {
            mGet = get;
        }

        boolean isGet() {
            return mGet;
        }
    }

    protected static enum AsAdmin {
        AS_ADMIN(true),
        AS_USER(false);

        boolean mAsAdmin;

        AsAdmin(boolean asAdmin) {
            mAsAdmin = asAdmin;
        }

        boolean yes()  {
            return mAsAdmin;
        }
    }

    /*
     * for testing key grantees
     */
    public static class KeyAuthToken extends AuthToken {

        private final String mName;
        private final String mAccessKey;

        public KeyAuthToken(String name, String accessKey) {
            mName = name;
            mAccessKey = accessKey;
        }

        @Override
        public Usage getUsage() {
            return null;
        }

        @Override
        public void encode(HttpClient client, HttpRequestBase method,
                boolean isAdminReq, String cookieDomain) throws ServiceException {
        }

        @Override
        public void encode(BasicCookieStore state, boolean isAdminReq, String cookieDomain)
                throws ServiceException {
        }

        @Override
        public void encode(HttpServletResponse resp, boolean isAdminReq,
                boolean secureCookie, boolean rememberMe)
                throws ServiceException {
        }

        @Override
        public void encodeAuthResp(Element parent, boolean isAdmin)
                throws ServiceException {
        }

        @Override
        public String getAccountId() {
            return null;
        }

        @Override
        public String getAdminAccountId() {
            return null;
        }

        @Override
        public String getCrumb() throws AuthTokenException {
            return null;
        }

        @Override
        public String getDigest() {
            return null;
        }

        @Override
        public String getEncoded() throws AuthTokenException {
            return null;
        }

        @Override
        public long getExpires() {
            return 0;
        }

        @Override
        public String getExternalUserEmail() {
            return mName;
        }

        @Override
        public boolean isAdmin() {
            return false;
        }

        @Override
        public boolean isDomainAdmin() {
            return false;
        }

        @Override
        public boolean isDelegatedAdmin() {
            return false;
        }

        @Override
        public boolean isExpired() {
            return false;
        }

        @Override
        public boolean isZimbraUser() {
            return false;
        }

        @Override
        public String toString() {
            return null;
        }

        @Override
        public ZAuthToken toZAuthToken() throws ServiceException {
            return null;
        }

        @Override
        public String getAccessKey() {
            return mAccessKey;
        }

		@Override
		public void deRegister() {
		}

		@Override
		public boolean isRegistered() {
			return true;
		}
    }

    static class TestViaGrant extends ViaGrant {
        static final boolean POSITIVE = false;
        static final boolean NEGATIVE = true;

        String mTargetType;
        String mTargetName;
        String mGranteeType;
        String mGranteeName;
        String mRight;
        boolean mIsNegativeGrant;

        Set<TestViaGrant> mCanAlsoVia;

        TestViaGrant(TargetType targetType,
                     Entry target,
                     GranteeType granteeType,
                     String granteeName,
                     Right right,
                     boolean isNegativeGrant) {
            mTargetType = targetType.getCode();
            mTargetName = target.getLabel();
            mGranteeType = granteeType.getCode();
            mGranteeName = granteeName;
            mRight = right.getName();
            mIsNegativeGrant = isNegativeGrant;
        }

        @Override
        public String getTargetType() {
            return mTargetType;
        }

        @Override
        public String getTargetName() {
            return mTargetName;
        }

        @Override
        public String getGranteeType() {
            return mGranteeType;
        }

        @Override
        public String getGranteeName() {
            return mGranteeName;
        }

        @Override
        public String getRight() {
            return mRight;
        }

        @Override
        public boolean isNegativeGrant() {
            return mIsNegativeGrant;
        }

        public void addCanAlsoVia(TestViaGrant canAlsoVia) {
            if (mCanAlsoVia == null)
                mCanAlsoVia = new HashSet<TestViaGrant>();
            mCanAlsoVia.add(canAlsoVia);
        }

        public static void verifyEquals(TestViaGrant expected, ViaGrant actual) {
            if (expected == null) {
                assertNull(actual);
                return;
            } else {
                assertNotNull(actual);
            }
            expected.verify(actual);
        }

        public void verify(ViaGrant actual) {
            try {
                assertEquals(getTargetType(),   actual.getTargetType());
                assertEquals(getTargetName(),   actual.getTargetName());
                assertEquals(getGranteeType(),  actual.getGranteeType());
                assertEquals(getGranteeName(),  actual.getGranteeName());
                assertEquals(getRight(),        actual.getRight());
                assertEquals(isNegativeGrant(), actual.isNegativeGrant());
            } catch (AssertionError e) {
                if (mCanAlsoVia == null) {
                    throw e;
                }

                // see if any canAlsoVia matches
                for (TestViaGrant canAlsoVia : mCanAlsoVia) {
                    try {
                        canAlsoVia.verify(actual);
                        // good, at least one of the canAlsoVia matches
                        return;
                    } catch (AssertionFailedError     eAlso) {
                        // ignore, see if next one matches
                    }
                }
                // if we get here, none of the canAlsoVia matches
                // throw the assertion exception on the main via
                throw e;
            }
        }
    }


    /*
    static void installUnitTestRights() throws Exception {
        FileUtil.copy("data/unittest/ldap/rights-unittest.xml", "/opt/zimbra/conf/rights/rights-unittest.xml");
    }

    static void uninstallUnitTestRights() throws Exception {
        FileUtil.delete(new File("/opt/zimbra/conf/rights/rights-unittest.xml"));
    }
    */


}
