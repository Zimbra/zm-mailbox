package com.zimbra.cs.service.account;
import java.util.Map;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;

import org.apache.http.client.config.RequestConfig;

public class GetLoginDetails extends AccountDocumentHandler {
	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext zc = getZimbraSoapContext(context);	
		
		 Element a = request.getElement(AccountConstants.E_ACCOUNT);
	        String key = a.getAttribute(AccountConstants.A_BY);	        
	        String value = a.getText();
	        
	        Element a1 = request.getElement(AccountConstants.A_LOGIN_FROM_TIME);	       
	        String value1 = a1.getText();
	        
	        Element a2 = request.getElement(AccountConstants.A_LOGIN_TO_TIME);	       
	        String value2 = a2.getText();

	        if (Strings.isNullOrEmpty(value)) {
	            throw ServiceException.INVALID_REQUEST(
	                "no text specified for the " + AccountConstants.E_ACCOUNT + " element", null);
	        }
	        Provisioning prov = Provisioning.getInstance();
	        Account account = prov.get(AccountBy.fromString(key), value, zc.getAuthToken());
	        

	        // prevent directory harvest attack, mask no such account as permission denied
	        if (account == null)
	            throw ServiceException.PERM_DENIED("can not access account");
	        
	        HttpPost post = new HttpPost(LC.es_host.value()+"/_search?pretty");

	        post.addHeader("content-type", "application/json");
	        String body = body(value,value1,value2,"");
	        String json_response ="";
	        try {
	            post.setEntity(new StringEntity(body.toString()));
	            // 5 seconds timeout
	            RequestConfig requestConfig = RequestConfig.custom()
	                    .setConnectionRequestTimeout(5000)
	                    .setConnectTimeout(5000)
	                    .setSocketTimeout(5000)
	                    .build();
	      
	            
	            post.setConfig(requestConfig);

	            try (CloseableHttpClient httpClient = HttpClients.createDefault();
	                    CloseableHttpResponse response = httpClient.execute(post)) {
	                json_response = EntityUtils.toString(response.getEntity(),"UTF-8");	               
	            }
	        } catch (Exception e) {
	        	 ZimbraLog.account.error("ERROR "+e.getMessage());
	        }
		

        Element response = getResponseElement(zc);
        StringBuffer strBuff = new StringBuffer();
       
        response.addAttribute("ES", json_response);

		return response;
	}
	static String body(String user_email, String from_time, String to_time,String attr) {
		String response = "";
		  String body = "{\n"
	                + "  \"version\": false,\n"
	                + "  \"sort\": [\n"
	                + "    {\n"
	                + "      \"@logintime_zimbra\": {\n"
	                + "        \"order\": \"desc\",\n"
	                + "        \"unmapped_type\": \"boolean\"\n"
	                + "      }\n"
	                + "    }\n"
	                + "  ],\n"
	                + "  \"_source\": [\"logtime\",\"@logintime_zimbra\",\"user_email\",\"ua\",\"client_ip\"],\n"
	                + "  \"query\": {\n"
	                + "    \"bool\": {\n"
	                + "      \"must\": [],\n"
	                + "      \"filter\": [\n"
	                + "        {\n"
	                + "          \"bool\": {\n"
	                + "            \"filter\": [\n"
	                + "              {\n"
	                + "                \"bool\": {\n"
	                + "                  \"should\": [\n"
	                + "                    {\n"
	                + "                      \"match\": {\n"
	                + "                        \"user_email.keyword\": \""+user_email+"\"\n"
	                + "                      }\n"
	                + "                    }\n"
	                + "                  ],\n"
	                + "                  \"minimum_should_match\": 1\n"
	                + "                }\n"
	                + "              },\n"
	                + "              {\n"
	                + "                \"bool\": {\n"
	                + "                  \"should\": [\n"
	                + "                    {\n"
	                + "                      \"match\": {\n"
	                + "                        \"tags.keyword\": \"login_success\"\n"
	                + "                      }\n"
	                + "                    }\n"
	                + "                  ],\n"
	                + "                  \"minimum_should_match\": 1\n"
	                + "                }\n"
	                + "              }\n"
	                + "            ]\n"
	                + "          }\n"
	                + "        },\n"
	                + "        {\n"
	                + "          \"range\": {\n"
	                + "            \"@logintime_zimbra\": {\n"
	                + "              \"format\": \"strict_date_optional_time\",\n"
	                + "              \"gte\": \""+from_time+"\",\n"
	                + "              \"lte\": \""+to_time+"\"\n"
	                + "            }\n"
	                + "          }\n"
	                + "        }\n"
	                + "      ],\n"
	                + "      \"should\": [],\n"
	                + "      \"must_not\": []\n"
	                + "    }\n"
	                + "  }\n"
	                + "}";
		
		return response;
	}
}
