What is preauth?
================

Preauth stands for pre-authentication, and is a mechanism to enable a trusted 
third party to "vouch" for a user's identity. For example, if a user has 
already signed into a portal and wants to enter the mail application, they 
should not have to be prompted again for their password.

This can be accomplished by having the mail link they click on in the portal
construct a special URL and redirect the user to the Zimbra server, which 
will then verify the data passed in the URL and create authentication token 
(the standard mechanism within Zimbra to identify users), save it in a cookie,
and redirect the user to the mail app.

How does it work?
=================

It works by having a key that is shared between the third party and Zimbra.
Knowing this key, the third party specifies the desired username, a timestamp
(to ensure the request is "fresh"), optionally an expiration time, and then
computes a SHA-1 HMAC over that data using  secret key.

The server computes the HMAC using the supplied data, and its key, to verify
that it matches the HMAC sent in the request. If it does, the server will
construct an auth token, save it in a cookie, and redirect the user to the 
mail application.

Preparing a domain for preauth
==============================

In order for preauth to be enabled for a domain, you need to run the
zmprov command and create a key:

    prov> gdpak domain.com
    preAuthKey: 4e2816f16c44fab20ecdee39fb850c3b0bb54d03f1d8e073aaea376a4f407f0c
    prov>

Make note of that key value, as you'll need to use it to generate the
computed-preauth values below. Also make sure you keep it secret, as it
can be used to generate valid auth tokens for any user in the given domain!

Behind the scenes, this command is simply generating 32 random bytes,
hex encoding them, then setting the "zimbraPreAuthKey" attr for the specified
domain with the value of the key.

After generating the key, you'll probably need to restart the ZCS server so it
picks up the updated value.

What are the interfaces?
========================

There are two interfaces. One interface is URL-based, for ease of integration.
The other interface is SOAP-based, for cases where the third party wants more
control over generating the auth token and redirecting the user.

We'll describe the URL interface first, followed by the SOAP interface.

URL Interface
=============

The URL interface uses the /service/reauth URL:

````
/service/preauth?
        account={account-identifier}
        &by={by-value}
        &timestamp={time}
        &expires={expires}
        [&admin=1]
        &preauth={computed-preauth}
````

The values are as follows:

````
 {account-identifier}   depends on the value of the "by" attr. If "by" is not
                        specified, it is the name (i.e., john.doe@domain.com).

 {by-value}             name|id|foreignPrincipal, same as AuthRequest. defaults
                        to name.

 {timestamp}            current time, in milliseconds. The timestamp must
                        be within 5 minutes of the server's time for the 
                        preauth to work.

 {expires}              expiration time of the authtoken, in milliseconds. 
                        set to 0 to use the default expiration time for the
                        account. Can be used to sync the auth token expiration
                        time with the external system's notion of expiration
                        (like a Kerberos TGT lifetime, for example).

 {admin}                set to "1" if this preauth is for admin console. This
                        only works if given account is an admin, and the request
                        comes in on the admin port (https 7071 by default).
                        If admin is specified, then include its value while computing the
                        HMAC below, after the "account" value, and before the "by" value.

{computed-preauth}      the computed pre-auth value. See below for details.
````

The preauth value is computed as follows:

  1. concat the values for account, by, expires, timestamp together 
     (in that order, order is definitely important!), separated by "|"

  2. compute the HMAC on that value using the shared key
     (the zimbraPreAuthKey value for the account's domain, generating one
      is described below).

  3. convert the HMAC value into a hex string.

For example, given the following values:

    key: 6b7ead4bd425836e8cf0079cd6c1a05acc127acd07c8ee4b61023e19250e929c

    account: john.doe@domain.com
    by: name
    expires: 0
    timestamp: 1135280708088

You would concat the account/by/expires/timestamp values together (alphabetical order, based on key name) to get:

    john.doe@domain.com|name|0|1135280708088

You would then compute the SHA-1 HMAC on that string, using the key:

    preauth = hmac("john.doe@domain.com|name|0|1135280708088", 
              "6b7ead4bd425836e8cf0079cd6c1a05acc127acd07c8ee4b61023e19250e929c");

Finally, you would take the returned hmac (which is most likely an array of
bytes), and convert it to a hex string:

    preauth-value : b248f6cfd027edd45c5369f8490125204772f844

The resulting URL would be:

    /service/preauth?account=john.doe@domain.com&expires=0\
        &timestamp=1135280708088&preauth=b248f6cfd027edd45c5369f8490125204772f844

Hitting that URL on the ZCS server will cause the preauth value to be verified,
followed by a redirect to /zimbra/mail with the auth token in a cookie.

If a URL other then /zimbra/mail is desired, then you can also pass in a redirectURL:

    ...&redirectURL=/zimbra/h/


If you are pre-authing an admin, the value to use with the hmac would be:

    john.doe@domain.com|1|name|0|1135280708088

and you would include "&admin=1" in the URL.

If you are not pre-authing an admin, then you must *NOT* include any value for it in string passed to the hmac.

SOAP Interface
==============

The SOAP interface uses the standard AuthRequest message, but instead of
passing in a password, you specify <preauth> data.

For example:

````
<AuthRequest xmlns="urn:zimbraAccount">
  <account by="name|id|foreignPrincipal">{account-identifier}</account>
  <preauth timestamp="{timestamp}"
           expires="{expires}">{computed-preauth}</preauth>
</AuthRequest>
````

The values are exactly the same as they were for the URL case:

````
<AuthRequest xmlns="urn:zimbraAccount">
  <account>john.doe@domain.com</account>
  <preauth timestamp="1135280708088"
           expires="0}">b248f6cfd027edd45c5369f8490125204772f844</preauth>
</AuthRequest>
````

The auth token will be return in the AuthResponse. At which point, you can
"inject" it into the app via the URL interface:

    https://server/service/preauth?isredirect=1&authtoken={...}

Going to this URL will set the cookie and redirect to /zimbra/mail. If a URL other
then /zimbra/mail is desired, then you can also pass in a redirectURL:

    ...&redirectURL=/zimbra/h/

Sample Java code for computing the preauth value
================================================

The following Java Code (1.5) will compute the pre-auth value.

````
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

public class test {
 
 public static void main(String args[]) { 
     HashMap<String,String> params = new HashMap<String,String>();
     params.put("account", "john.doe@domain.com");
     params.put("by", "name"); // needs to be part of hmac
     params.put("timestamp", "1135280708088");
     params.put("expires", "0");
     String key = 
        "6b7ead4bd425836e8cf0079cd6c1a05acc127acd07c8ee4b61023e19250e929c";
     System.out.printf("preAuth: %s\n", computePreAuth(params, key)); 
  }

  public static  String computePreAuth(Map<String,String> params, String key) 
  {
        TreeSet<String> names = new TreeSet<String>(params.keySet());
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            if (sb.length() > 0) sb.append('|');
            sb.append(params.get(name));
        }
        return getHmac(sb.toString(), key.getBytes());
    }

    private static String getHmac(String data, byte[] key) {
        try {
            ByteKey bk = new ByteKey(key);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(bk);
            return toHex(mac.doFinal(data.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("fatal error", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("fatal error", e);
        }
    }
    
    static class ByteKey implements SecretKey {
        private byte[] mKey;
        
        ByteKey(byte[] key) {
            mKey = (byte[]) key.clone();;
        }
        
        public byte[] getEncoded() {
            return mKey;
        }

        public String getAlgorithm() {
            return "HmacSHA1";
        }

        public String getFormat() {
            return "RAW";
        }       
   }

    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (int i=0; i<data.length; i++ ) {
           sb.append(hex[(data[i] & 0xf0) >>> 4]);
           sb.append(hex[data[i] & 0x0f] );
        }
	return sb.toString();
    }

    private static final char[] hex = 
       { '0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' ,
         '8' , '9' , 'a' , 'b' , 'c' , 'd' , 'e' , 'f'};
}
````

You can also use zmprov while debugging to generate preAuth values for
comparison:

````
prov> gdpa domain.com john.doe@domain.com name 1135280708088 0 
account: john.doe@domain.com
by: name
timestamp: 1135280708088
expires: 0
preAuth: b248f6cfd027edd45c5369f8490125204772f844
prov> 
````
    
Here is a sample JSP that does everything needed for preauth. It has a hardcoded username, which should of course be changed to match
the username of the user you have "pre-authenticated".

To configure:

1. Generate a preauth domain key for your domain using zmprov:
 
		zmprov gdpak domain.com
		preAuthKey:  ee0e096155314d474c8a8ba0c941e9382bb107cc035c7a24838b79271e32d7b0
 
   Take that value, and set it below as the value of `DOMAIN_KEY`

2. restart server (only needed the first time you generate the domain pre-auth key)

3. redirect users to this (this, as in *this* file after you install it) JSP page:

		http://server/zimbra/preauth.jsp

And it will construct the preauth URL

	-->
	<%@ page import="java.security.InvalidKeyException" %>
	<%@ page import="java.security.NoSuchAlgorithmException" %>
	<%@ page import="java.security.SecureRandom" %>
	<%@ page import="java.util.HashMap" %>
	<%@ page import="java.util.Map" %>
	<%@ page import="java.util.Iterator" %>
	<%@ page import="java.util.TreeSet" %>
	<%@ page import="javax.crypto.Mac" %>
	<%@ page import="javax.crypto.SecretKey" %>
	<%!
	 public static final String DOMAIN_KEY =
		"ee0e096155314d474c8a8ba0c941e9382bb107cc035c7a24838b79271e32d7b0";


	 public static String generateRedirect(HttpServletRequest request, String name) {
	     HashMap params = new HashMap();
	     String ts = System.currentTimeMillis()+"";
	     params.put("account", name);
	     params.put("by", "name"); // needs to be part of hmac
	     params.put("timestamp", ts);
	     params.put("expires", "0"); // means use the default

	     String preAuth = computePreAuth(params, DOMAIN_KEY);
	     return request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+"/service/preauth/?" +
		   "account="+name+
		   "&by=name"+
		   "&timestamp="+ts+
		   "&expires=0"+
		   "&preauth="+preAuth;
	  }

	    public static  String computePreAuth(Map params, String key) {
		TreeSet names = new TreeSet(params.keySet());
		StringBuffer sb = new StringBuffer();
		for (Iterator it=names.iterator(); it.hasNext();) {
		    if (sb.length() > 0) sb.append('|');
		    sb.append(params.get(it.next()));
		}
		return getHmac(sb.toString(), key.getBytes());
	    }

	    private static String getHmac(String data, byte[] key) {
		try {
		    ByteKey bk = new ByteKey(key);
		    Mac mac = Mac.getInstance("HmacSHA1");
		    mac.init(bk);
		    return toHex(mac.doFinal(data.getBytes()));
		} catch (NoSuchAlgorithmException e) {
		    throw new RuntimeException("fatal error", e);
		} catch (InvalidKeyException e) {
		    throw new RuntimeException("fatal error", e);
		}
	    }


	    static class ByteKey implements SecretKey {
		private byte[] mKey;

		ByteKey(byte[] key) {
		    mKey = (byte[]) key.clone();;
		}

		public byte[] getEncoded() {
		    return mKey;
		}

		public String getAlgorithm() {
		    return "HmacSHA1";
		}

		public String getFormat() {
		    return "RAW";
		}
	   }

	    public static String toHex(byte[] data) {
		StringBuilder sb = new StringBuilder(data.length * 2);
		for (int i=0; i<data.length; i++ ) {
		   sb.append(hex[(data[i] & 0xf0) >>> 4]);
		   sb.append(hex[data[i] & 0x0f] );
		}
		return sb.toString();
	    }

	    private static final char[] hex =
	       { '0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' ,
		 '8' , '9' , 'a' , 'b' , 'c' , 'd' , 'e' , 'f'};


	%><%

	String redirect = generateRedirect(request, "user1@slapshot.liquidsys.com");
	response.sendRedirect(redirect);

	%>
	<html>
	<head>
	<title>Pre-auth redirect</title>
	</head>
	<body>

	You should never see this page.

	</body>
	</html>
