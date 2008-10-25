/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util.yauth;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;

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
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod("https://login.yahoo.com/config/pwtoken_get?src=ymsgr&login="+username+"&passwd="+passwd);
        int response = client.executeMethod(method);

        if (response >= 200 && response < 300) { 
            String body = method.getResponseBodyAsString();
            
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
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod("https://login.yahoo.com/config/pwtoken_login?src=ymsgr&token="+token);
        int response = client.executeMethod(method);
        
        if (response >= 200 && response < 300) { 
            String body = method.getResponseBodyAsString();
            
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
