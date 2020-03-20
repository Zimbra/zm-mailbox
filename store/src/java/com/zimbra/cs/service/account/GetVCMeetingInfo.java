package com.zimbra.cs.service.account;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;

public class GetVCMeetingInfo extends AccountDocumentHandler {


	private static final String contentType = "text/xml; charset=utf-8";

	private String vcResponse = null;

	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);

		Element accElement = request.getElement(AccountConstants.E_ACCOUNT);
        String key = accElement.getAttribute(AccountConstants.A_BY);
        String emailAddr = accElement.getText();

        if (StringUtil.isNullOrEmpty(emailAddr)) {
            throw ServiceException.INVALID_REQUEST("GetVCMeetingInfo.handle no text specified for the " + AccountConstants.E_ACCOUNT + " element", null);
        }
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.fromString(key), emailAddr, zsc.getAuthToken());

        if (account == null)
            throw ServiceException.PERM_DENIED("can not access account permission denied");

        ZimbraLog.account.debug("GetVCMeetingInfo.handle emailAddr: " + emailAddr);

        Element response = null;
        try {
        	String uId = fetchSunLdapUid(emailAddr);

            if(uId!=null) {
            	getVCMeetingInfo(uId);
            }
            ZimbraLog.account.debug("GetVCMeetingInfo.handle uId: "+uId+" for emailAddr: "+emailAddr);
        } catch (Exception e) {
        	vcResponse = "error";
			ZimbraLog.account.error("\n GetVCMeetingInfo.handle could not able call VC Metting API !\n", e);
		} 
        vcResponse = vcResponse==null?"":vcResponse.trim();
        ZimbraLog.account.debug("GetVCMeetingInfo.handle vcResponse: "+vcResponse);
        response = zsc.createElement(AccountConstants.GET_VC_MEETING_INFO_RESPONSE);
        response.addAttribute(AccountConstants.VC_RESPONSE, vcResponse);

		return response;
	}

	private void getVCMeetingInfo(String uId) {

		SOAPConnectionFactory soapConnectionFactory = null;
		SOAPConnection soapConnection = null;
		try {
			soapConnectionFactory = SOAPConnectionFactory.newInstance();
			soapConnection = soapConnectionFactory.createConnection();

			SOAPMessage soapEnvelope = createSoapEnvelope(uId);
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			soapEnvelope.writeTo(byteStream);
			String soapRequest = new String(byteStream.toByteArray());
			ZimbraLog.account.debug("GetVCMeetingInfo.getVCMeetingInfo soapRequest: " + soapRequest);

			HttpResponse response = callVCService(soapRequest);

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(response.getEntity().getContent());

			NodeList nodeList = document.getDocumentElement().getElementsByTagName("VidyoRoomLinkResponse");
			ZimbraLog.account.debug("GetVCMeetingInfo.getVCMeetingInfo nodeList: " + nodeList.getLength());
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				iterateNode(node);
			}

		} catch (Exception e) {
			vcResponse = "error";
			ZimbraLog.account.error("\n GetVCMeetingInfo.getVCMeetingInfo could not able call VC Metting API !\n", e);
			e.printStackTrace();
		} finally {
			try {
				soapConnection.close();
			} catch (Exception e) {}
		}

	}

	private SOAPMessage createSoapEnvelope(String uId) throws Exception {

		MessageFactory messageFactory = MessageFactory.newInstance();
		SOAPMessage soapMessage = messageFactory.createMessage();

		MimeHeaders headers = soapMessage.getMimeHeaders();
		SOAPPart part = soapMessage.getSOAPPart();

		SOAPEnvelope envelope = part.getEnvelope();
		SOAPBody body = envelope.getBody();

		headers.addHeader("SOAPAction", LC.vc_vidyo_soap_action.value());
		headers.setHeader("Content-Type", contentType);

		SOAPBodyElement element = body.addBodyElement(envelope.createName("VidyoRoomLink", "", LC.vc_vidyo_room_link.value()));
		element.addChildElement("SearchString").addTextNode(uId);
		element.addChildElement("username").addTextNode(LC.vc_vidyo_user_name.value());
		element.addChildElement("Password").addTextNode(LC.vc_vidyo_user_pass.value());

		soapMessage.saveChanges();

		return soapMessage;
	}

	private void iterateNode(Node node) throws Exception {

		NodeList childNodes = node.getChildNodes();
		for (int j = 0; j < childNodes.getLength(); j++) {
			Node chileNode = childNodes.item(j);
			if("a:InviteContent".equals(chileNode.getNodeName())) {
				ZimbraLog.account.debug("GetVCMeetingInfo.iterateNode InviteContent: "+chileNode.getNodeName() + " --  "+chileNode.getTextContent());
				vcResponse = chileNode.getTextContent();
				break;
			}
			iterateNode(chileNode);
		}
	}


	private HttpResponse callVCService(String body) {

		HttpResponse response = null;
		try {
			ZimbraLog.account.debug("GetVCMeetingInfo.callVCService request: " + body);
			StringEntity stringEntity = new StringEntity(body, "UTF-8");
			stringEntity.setChunked(true);

			// Request parameters and other properties.
			HttpPost httpPost = new HttpPost(LC.vc_vidyo_request_url.value());
			httpPost.setEntity(stringEntity);
			httpPost.addHeader("Content-Type", contentType);
			httpPost.addHeader("SOAPAction", LC.vc_vidyo_soap_action.value());

			// Execute and get the response.
			HttpClient httpClient = new DefaultHttpClient();
			response = httpClient.execute(httpPost);
			ZimbraLog.account.debug("GetVCMeetingInfo.callVCService response: " + response);
		} catch (Exception e) {
			vcResponse = "error";
			ZimbraLog.account.error("GetVCMeetingInfo.callVCService Unable to fetch data from vidyo api", e);
			e.printStackTrace();
		}
		return response;
	}

	private String fetchSunLdapUid(String email) {

		String uid = null;
		DirContext ctx = null;
		try {
			Hashtable<String, String> env = new Hashtable<>();

			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
	        if (LC.sun_ssl_enable.booleanValue()) {
	        	env.put(Context.PROVIDER_URL, "ldaps://" + LC.sun_host.value() + ":" + LC.sun_port.intValue());
	        	env.put(Context.SECURITY_AUTHENTICATION, "ssl");
	        } else {
	        	env.put(Context.PROVIDER_URL, "ldap://" + LC.sun_host.value() + ":" + LC.sun_port.intValue());
	        }
	        env.put(Context.SECURITY_PRINCIPAL, LC.sun_binddn.value());
	        env.put(Context.SECURITY_CREDENTIALS, LC.sun_bindpass.value());

			String[] attr = {"uid"};
			SearchControls sc = new SearchControls();
			sc.setReturningAttributes(attr);
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String searchFilter = "";
			searchFilter = "(|(mail=" + email + "))";

			ctx = new InitialDirContext(env);
			NamingEnumeration<SearchResult> enumerator = ctx.search(LC.sun_directory_base.value(), searchFilter, sc);
			while (enumerator.hasMoreElements()) {
				SearchResult searchResult = (SearchResult) enumerator.nextElement();
				uid = (String) searchResult.getAttributes().get("uid").get();
				if (uid != null && uid.length() > 0) {
					break;
				}
			}
			ZimbraLog.account.debug("GetVCMeetingInfo fetchSunLdapUid email: "+email+", uid: " + uid);
		} catch (Exception e) {
			vcResponse = "error";
			ZimbraLog.account.error("Unable to fetch data from sun ldap for email: "+email, e);
		} finally {
			try {
				ctx.close();
			} catch(Exception ex) {}
		}
		return uid;
	}
}
