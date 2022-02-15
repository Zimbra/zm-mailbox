/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.servlet.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.fileupload.FileItem;

import com.google.common.base.Joiner;
import com.google.common.net.HttpHeaders;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.BlobMetaData;
import com.zimbra.common.util.BlobMetaDataEncodingException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.CsrfTokenKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.TokenUtil;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.ephemeral.EphemeralInput.AbsoluteExpiration;
import com.zimbra.cs.ephemeral.EphemeralInput.Expiration;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.CsrfFilter;



/**
 * @author zimbra
 *
 */
public final class CsrfUtil {

    protected static final String C_ID  = "id";
    protected static final String C_EXP = "exp";
    protected static final String C_SALT_ID = "sid";
    protected static final String PARAM_CSRF_TOKEN = "csrfToken";

    /**
     * Private constructor.
     */
    private CsrfUtil() {

    }

   /**
    *
    * @param req
    * @param allowedRefHost
    * @return
    * @throws MalformedURLException
    */
   public static boolean isCsrfRequestBasedOnReferrer(final HttpServletRequest req,
       final String [] allowedRefHost) throws MalformedURLException {

       List<String> allowedRefHostList = Arrays.asList(allowedRefHost);
       boolean csrfReq = false;

       String method = req.getMethod();
       if (!method.equalsIgnoreCase("POST")) {
           csrfReq = false;
           return csrfReq;
       }

       String host = getRequestHost(req);
       String referrer = req.getHeader(HttpHeaders.REFERER);
       String refHost = null;


       if (!StringUtil.isNullOrEmpty(referrer)) {
           URL refURL = null;
           if (referrer.contains("http") || referrer.contains("https")) {
               refURL = new URL(referrer);
           } else {
               refURL = new URL("http://" + referrer);
           }
           refHost = refURL.getHost().toLowerCase();
       }

       if (refHost == null) {
           csrfReq = false;
       }  else if (refHost.equalsIgnoreCase(host)) {
           csrfReq = false;
       } else {
           if (allowedRefHost != null && allowedRefHostList.contains(refHost)) {
               csrfReq = false;
           } else {
               csrfReq = true;
           }
       }

       if (ZimbraLog.soap.isDebugEnabled()) {
           ZimbraLog.soap.debug("Host : %s, Referrer host :%s, Allowed Hosts:[%s] Soap req is %s",
                   host, refHost, Joiner.on(',').join(allowedRefHostList), (csrfReq ? " not allowed." : " allowed."));
       }

       return csrfReq;
   }

   /**
   *
   * @param host
   * @return
   */
  public static String getRequestHost(final HttpServletRequest req) {

      String host = HttpUtil.getVirtualHost(req);
      if (host == null) {
          return host;
      }
      String temp = host;

      if (temp.indexOf(":") != -1) {
          int endIndex = temp.indexOf(":");
          temp = host.substring(0, endIndex);
      }
      if (ZimbraLog.soap.isTraceEnabled()) {
          ZimbraLog.soap.trace("Original host : " + host + " returning: " + temp);

      }
      temp = temp.toLowerCase();
      return temp;
  }

  /**
   *
   * @param req
   * @return
   */
  public static AuthToken getAuthTokenFromReq(HttpServletRequest req) {

      AuthToken at = null;
      try {
          boolean isAdminRequest = AuthUtil.isAdminRequest(req);
          at = AuthProvider.getAuthToken(req, isAdminRequest);
      } catch (ServiceException | AuthTokenException e) {
          ZimbraLog.security.info("Error extracting auth token from the request. " +
              e.getMessage());
      }
      return at;
  }


    /**
     *
     * @param req
     * @param authToken
     * @return
     * @throws MalformedURLException
     */
    public static boolean doCsrfCheck(final HttpServletRequest req,
        final AuthToken authToken)
            throws MalformedURLException {

        boolean csrfReq = true;
        csrfReq = isPostReq(req);
        if (csrfReq) {
            if (authToken != null) {
                if (!authToken.isCsrfTokenEnabled()) {
                    csrfReq = isCsrfTokenCreated(authToken);
                }
            } else {
               csrfReq = false;
            }
        }
        if (ZimbraLog.misc.isDebugEnabled()) {
            String reqUrl = req.getRequestURI();
            ZimbraLog.misc.debug("ReqURL : " + reqUrl
                + (csrfReq ? " needs to " : " does not need to ")
                + "pass through CSRF check");
        }
        return csrfReq;
    }

    /**
     * @return
     */
    public static boolean isPostReq(HttpServletRequest req) {
        boolean postReq = true;
        String method = req.getMethod();
        if (!method.equalsIgnoreCase("POST") && !method.equalsIgnoreCase("PUT")
            && !method.equalsIgnoreCase("DELETE")) {
            postReq = false;
        }
        return postReq;
    }


    /**
     * @param authToken
     * @return
     */
    public static boolean isCsrfTokenCreated(AuthToken authToken) {
        boolean csrfTokenCreated = false;
        ZimbraLog.misc.debug("isCsrfTokenCreated()");
       try {
            Account account = getAccount(authToken, Boolean.TRUE);
            if(account != null) {
                String crumb = authToken.getCrumb();
                csrfTokenCreated = account.hasCsrfTokenData(crumb);
            }
       } catch (ServiceException | AuthTokenException e) {
           ZimbraLog.ephemeral.info("Error fetching CSRF token data" + e.getMessage());
       }
        return csrfTokenCreated;
    }

    /**
     * @param authToken
     * @return
     * @throws ServiceException
     */
    public static Account getAccount(AuthToken authToken, boolean loadFromLdap) throws ServiceException {
        Account account = null;

        if (loadFromLdap) {
            Provisioning prov = Provisioning.getInstance();
            account = prov.get(AccountBy.id, authToken.getAccountId(), Boolean.TRUE);
            if(account != null) {
                prov.reload(account);
            }
        } else {
            account = authToken.getAccount();
        }
        return account;
    }

    public static boolean isValidCsrfToken(String csrfToken, AuthToken authToken) {
        if (StringUtil.isNullOrEmpty(csrfToken) || null == authToken) {
            return false;
        }

        String hmacFromToken = null;
        String crumb = null;
        String keyVersion = null;
        boolean validToken = false;
        boolean loadFromLdap = false;

        try {
            Pair<String, String> data = parseCsrfToken(csrfToken);
            hmacFromToken = data.getFirst();
            keyVersion = data.getSecond();
            crumb  = authToken.getCrumb();
            Account account = getAccount(authToken, loadFromLdap);
            if(account == null) {
                return false;
            }
            validToken = validateCsrfToken(hmacFromToken, crumb, keyVersion, validToken, account);
            if (!validToken) {
                // just recheck that we are looking at the latest Account object
                // cache of this server might be stale
                ZimbraLog.misc.info("CSRF token was invalid, rechecking with account object from LDAP.");
                loadFromLdap = true;
                account = getAccount(authToken, loadFromLdap);
                validToken = validateCsrfToken(hmacFromToken, crumb, keyVersion, validToken, account);
                if (ZimbraLog.misc.isDebugEnabled()) {
                    ZimbraLog.misc.debug("The csrfToken second check: " + (validToken ? "is valid." : " is invalid."));
                }
            }
        } catch (AuthTokenException | ServiceException e) {
            ZimbraLog.misc.info("Error decoding CSRF token, " +
                e.getMessage());
            validToken = false;
        }

        if (ZimbraLog.misc.isDebugEnabled() ) {
            ZimbraLog.misc.debug("The csrfToken: " + (validToken ? "is valid." : " is invalid."));
        }
        return validToken;
    }

    /**
     * @param hmacFromToken
     * @param crumb
     * @param keyVersion
     * @param validToken
     * @param account
     * @return
     * @throws ServiceException
     * @throws AuthTokenException
     */
    private static boolean validateCsrfToken(String hmacFromToken, String crumb, String keyVersion,
        boolean validToken, Account account) throws ServiceException, AuthTokenException {
        String csrfTokenData;
        csrfTokenData  = getTokenDataFromLdap(crumb, account);
        if (csrfTokenData != null) {
            CsrfTokenKey key = CsrfTokenKey.getVersion(keyVersion);
            if (key == null) {
                throw new AuthTokenException("unknown key version");
            }
            String computedHmac = TokenUtil.getHmac(csrfTokenData, key.getKey());

            if (computedHmac.equals(hmacFromToken)) {
                Map<?,?> decodedData = getAttrs(csrfTokenData);
                long expirationTime = Long.parseLong((String) decodedData.get(C_EXP));
                long currentTime = System.currentTimeMillis();
                if (currentTime < expirationTime) {
                    validToken = true;
                }
            }
        }
        return validToken;
    }

    /**
    *
    * @param csrfToken
    * @param authToken
    * @return
    * @throws AuthTokenException
    */
    public static Pair<String, String> parseCsrfToken(String csrfToken) throws AuthTokenException {

        int pos = csrfToken.indexOf('_');
        if (pos == -1) {
            throw new AuthTokenException("invalid authtoken format");
        }
        String ver = csrfToken.substring(0, pos);

        String hmac = csrfToken.substring(pos + 1);
        return new Pair<String, String>(hmac, ver);
    }

    /**
     * @param crumb
     * @param account
     * @return
     * @throws ServiceException
     */
    private static String getTokenDataFromLdap(String crumb, Account account) throws ServiceException {
        account.purgeCsrfTokenData();
        return account.getCsrfTokenData(crumb);
    }



    /**
     *
     * @param data
     * @return
     * @throws AuthTokenException
     */
    private static Map<?, ?> getAttrs(String data) throws AuthTokenException{
        try {
            String decoded = new String(Hex.decodeHex(data.toCharArray()));
            return BlobMetaData.decode(decoded);
        } catch (DecoderException e) {
            throw new AuthTokenException("decoding exception", e);
        } catch (BlobMetaDataEncodingException e) {
            throw new AuthTokenException("blob decoding exception", e);
        }
    }

    /**
     * @param sessionId
     * @param i
     * @return
     * @throws AuthTokenException
     * @throws ServiceException
     * @throws InvalidAlgorithmParameterException
     */
    public static String generateCsrfToken(String accountId, long authTokenExpiration, int tokenSalt,
        AuthToken at)  throws  ServiceException {

        try {
            String crumb = at.getCrumb();
            String tokenData = getExistingCsrfTokenForThisAuthToken(at, crumb);
            if (tokenData == null) {

                StringBuilder encodedBuff = new StringBuilder(64);
                BlobMetaData.encodeMetaData(C_ID, accountId, encodedBuff);
                BlobMetaData.encodeMetaData(C_EXP, Long.toString(authTokenExpiration), encodedBuff);
                BlobMetaData.encodeMetaData(C_SALT_ID, tokenSalt, encodedBuff);
                tokenData = new String(Hex.encodeHex(encodedBuff.toString().getBytes()));
            }
            CsrfTokenKey key = getCurrentKey();
            String hmac = TokenUtil.getHmac(tokenData, key.getKey());
            String encoded = key.getVersion() + "_" + hmac;
            storeTokenData(tokenData, at, authTokenExpiration, crumb);
            return encoded;
        } catch (AuthTokenException e) {
            throw ServiceException.FAILURE("Error generating Auth Token, "
                + e.getMessage(), e);
        }

    }

    /**
     * @param accountId
     * @param crumb
     * @return
     * @throws ServiceException
     */
    private static String getExistingCsrfTokenForThisAuthToken(AuthToken authToken, String crumb) throws ServiceException {
        Account account = getAccount(authToken, Boolean.TRUE);
        if(account != null) {
            return getTokenDataFromLdap(crumb, account);
        } else {
            return null;
        }
    }

    protected static CsrfTokenKey getCurrentKey() throws AuthTokenException {
        try {
            CsrfTokenKey key = CsrfTokenKey.getCurrentKey();
            return key;
        } catch (ServiceException e) {
            ZimbraLog.misc.debug("Unable to get latest CsrfTokenKey", e);
            throw new AuthTokenException("unable to get CsrfTokenKey", e);
        }
    }

    /**
     *
     * @param tokenSalt
     * @param accountId
     * @param authTokenExpiration
     * @param crumb
     * @throws ServiceException
     */
    private static void storeTokenData(String data, AuthToken authToken, long authTokenExpiration,
        String crumb) throws ServiceException {
        Account account = getAccount(authToken, Boolean.TRUE);
        if(account != null) {
            Expiration expiration = new AbsoluteExpiration(authTokenExpiration);
            boolean needToAdd = true;
            String curData = account.getCsrfTokenData(crumb);
            if (curData != null) {
                if (!data.equals(curData)) {
                    account.removeCsrfTokenData(crumb, curData);
                } else {
                    ZimbraLog.ephemeral.debug("CSRF token already stored in ephemeral storage");
                    needToAdd = false;
                }
            }
            if (needToAdd) {
                account.addCsrfTokenData(crumb, data, expiration);
            }
        }
    }

    public static CsrfTokenKey getCsrfTokenKey() throws ServiceException {
        return CsrfTokenKey.getCurrentKey();
    }

    /**
     * @param resp
     * @return
     * @throws AuthTokenException
     */
    public static AuthToken getAuthTokenFromResponse(HttpServletResponse resp) throws AuthTokenException {
        List<String> headers = (List<String>) resp.getHeaders("Set-Cookie");
        AuthToken at = null;
        for (String s : headers) {
            if (!StringUtil.isNullOrEmpty(s)  && s.contains("ZM_AUTH_TOKEN")) {
                String temp [] = s.split("=");
                int index = temp[1].indexOf(";");
                String token = temp[1].substring(0, index);
                at = AuthToken.getAuthToken(token);
            }
        }
        return at;
    }


    public static String generateCsrfTokenTest(String accountId, long authTokenExpiration, int tokenSalt,
        String sessionId)
            throws AuthTokenException {

        StringBuilder encodedBuff = new StringBuilder(64);
        BlobMetaData.encodeMetaData(C_ID, accountId, encodedBuff);
        BlobMetaData.encodeMetaData(C_EXP, Long.toString(authTokenExpiration), encodedBuff);
        BlobMetaData.encodeMetaData(C_SALT_ID, tokenSalt, encodedBuff);


        String data = new String(Hex.encodeHex(encodedBuff.toString().getBytes()));
        CsrfTokenKey key = getCurrentKey();
        String hmac = TokenUtil.getHmac(data, key.getKey());
        String encoded = key.getVersion() + "_" + hmac + "_" + data;
        return encoded;

    }

    public static boolean checkCsrfInMultipartFileUpload(List<FileItem> items, AuthToken at) {
        for (FileItem item : items) {
            if (item.isFormField()) {
                if (item.getFieldName().equals(PARAM_CSRF_TOKEN)) {
                    if (item.getSize() < 128) { // if the value is larger, it is not a CSRF token
                        String csrfToken = item.getString();
                        if (CsrfUtil.isValidCsrfToken(csrfToken, at)) {
                            return true;
                        } else {
                            ZimbraLog.misc.debug("Csrf token : %s recd in file upload is invalid.", csrfToken);
                        }
                    }
                    break;
                }
            }
        }
        return false;
    }

    public static void setCSRFToken(HttpServletRequest httpReq, HttpServletResponse httpResp, AuthToken authToken, 
        boolean csrfSupport, Element response) throws ServiceException {
        if (csrfSupport && httpReq.getAttribute(Provisioning.A_zimbraCsrfTokenCheckEnabled) != null 
                && ((Boolean) httpReq.getAttribute(Provisioning.A_zimbraCsrfTokenCheckEnabled))) {
            String accountId = authToken.getAccountId();
            long authTokenExpiration = authToken.getExpires();
            int tokenSalt = (Integer) httpReq.getAttribute(CsrfFilter.CSRF_SALT);
            String token = generateCsrfToken(accountId, authTokenExpiration, tokenSalt, authToken);
            Element csrfResponse = response.addUniqueElement(HeaderConstants.E_CSRFTOKEN);
            csrfResponse.addText(token);
            httpResp.setHeader(Constants.CSRF_TOKEN, token);
        }
    }
    
    public static void main(String args[])
    {
        try {
        AuthToken at = ZimbraAuthToken .getAuthToken("0_f66f9e23c3d6ec89c0723375489c729b13b108d9_69643d33363a34313537336365352d303035352d343066362d626235372d6264396238663136663666393b6578703d31333a313430333935303235363538323b747970653d363a7a696d6272613b7469643d31303a313837363638363831333b76657273696f6e3d303a3b637372663d313a313b");
        String csrfToken = "0_a00d6f6af20bf183ab63911ab648a7869793158e";
        boolean result = CsrfUtil.isValidCsrfToken(csrfToken, at);
        System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
