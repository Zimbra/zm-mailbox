/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.util.yauth;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import com.zimbra.common.httpclient.HttpClientUtil;

/**
 * An implementation of the v1 Yahoo Token Auth API.  This is mostly deprecated, but still used by ymsgr among others
 * 
 * see generic_authapi.doc on backyard for details, but basically:
 * 
 * https://login.yahoo.com/config/pwtoken_get?src=ymsgr&login=USERNAMNE&passwd=PLAINTEXT_PASSWORD_URLESCAPED
 *     get token (ymsgr=TOKEN in HTTP response body) from response
 *     
 * Token is good until password is changed (ie, store the token, don't store the passwd!)    
 *      
 * https://login.yahoo.com/config/pwtoken_login?src=ymsgr&token=TOKEN
 *     get crumb,Y,T from HTTP response body
 *     
 * Use crumb,Y,T to login.       
 */
public class TokenAuthenticateV1 {
    
    /**
     * @param username
     * @param passwd
     * @return The token
     */
    public static String getToken(String username, String passwd) throws IOException, HttpException {
        HttpGet method = new HttpGet("https://login.yahoo.com/config/pwtoken_get?src=ymsgr&login="+username+"&passwd="+passwd);
        HttpResponse response = HttpClientUtil.executeMethod(method);

        if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300) { 
            String body = EntityUtils.toString(response.getEntity());
            
            HashMap<String,String> map = new HashMap<String, String>();
            map.put("ymsgr", null);

            parseResponseBody(body, map);
            
            return map.get("ymsgr");
        } else {
            throw new IOException("HTTPClient response: "+response);
        }
    }
    
    private static void parseResponseBody(String responseBody, HashMap<String,String> map) {
        String[] lines = responseBody.split("\n");
        for (String line : lines) {
            int eqIdx = line.indexOf('=');
            if (eqIdx > 0) {
                String[] cols = new String[2];
                line = line.trim();
                cols[0] = line.substring(0, eqIdx);
                cols[1] = "";
                if (eqIdx < line.length()-1) {
                    cols[1] = line.substring(eqIdx+1);
                    if (map.containsKey(cols[0]) && map.get(cols[0]) == null) 
                        // only pay attention to the first instance of a value 
                        map.put(cols[0], cols[1]);
                }
            }
        }
    }
    
    /**
     * @param username
     * @param token THIS IS NOT THE PASSWORD -- use the static getToken() method
     *              to get the user's token
     * @return
     */
    public static TokenAuthenticateV1 doAuth(String username, String token) throws IOException, HttpException {
        HttpGet method = new HttpGet("https://login.yahoo.com/config/pwtoken_login?src=ymsgr&token="+token);
        HttpResponse response = HttpClientUtil.executeMethod(method);
        
        if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300) { 
            String body = EntityUtils.toString(response.getEntity());
            
            HashMap<String,String> map = new HashMap<String, String>();
            map.put("crumb", null);
            map.put("Y", null);
            map.put("T", null);

            parseResponseBody(body, map);
            
            return new TokenAuthenticateV1(map.get("crumb"), map.get("Y"), map.get("T"));
        } else {
            throw new IOException("HTTPClient response: "+response);
        }
    }
    
    public String toString() {
        return "YToken(crumb="+mCrumb+",Y="+mY+",T="+mT+")";
    }
    
    private TokenAuthenticateV1(String crumb, String Y, String T) {
        if (crumb == null || Y == null || T == null)
            throw new IllegalArgumentException("Missing part of auth response");
        
        mCrumb = crumb; 
        mY = Y; //mY = mY.substring(0,mY.indexOf(';')); 
        mT = T; //mT = mT.substring(0,mT.indexOf(';')); 
    }
    
    public String getCrumb() { return mCrumb; };
    public String getY(){ return mY; }
    public String getT() {return mT; }
   
    private String mCrumb;
    private String mY;
    private String mT;
    
    public static void main(String[] argv) {
        try {
            String ymsgr = TokenAuthenticateV1.getToken("XXXX", "XXXX");
            System.out.println("Got token: "+ymsgr);
            
            TokenAuthenticateV1 ta = TokenAuthenticateV1.doAuth("XXXX", ymsgr);
            
            System.out.println("Got token: "+ta.toString());
        } catch (Exception e) {
            System.out.println("Caught exception"+e);
            e.printStackTrace();
        }
    }
}
