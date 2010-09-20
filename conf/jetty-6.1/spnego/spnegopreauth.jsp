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
        "REPLACE-ME-WITH-THE-PREAUTH-KEY";


 public static String generateRedirect(HttpServletRequest request, String name) {
     HashMap params = new HashMap();
     String ts = System.currentTimeMillis()+"";
     params.put("account", name);
     params.put("by", "krb5Principal"); // needs to be part of hmac
     params.put("timestamp", ts);
     params.put("expires", "0"); // means use the default

     String preAuth = computePreAuth(params, DOMAIN_KEY);
     return request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+"/service/preauth/?" +
           "account="+name+
           "&by=krb5Principal"+
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

String redirect = generateRedirect(request, request.getUserPrincipal().getName());
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
