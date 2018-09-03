package com.zimbra.soap.mail;

import static org.junit.Assert.*;

import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

import com.zimbra.soap.mail.message.CreateDataSourceRequest;
import com.zimbra.soap.mail.message.GetDataSourcesResponse;
import com.zimbra.soap.mail.message.ImportDataRequest;
import com.zimbra.soap.mail.message.ModifyDataSourceRequest;
import com.zimbra.soap.mail.message.TestDataSourceRequest;
import com.zimbra.soap.mail.type.CalDataSourceNameOrId;
import com.zimbra.soap.mail.type.CaldavDataSourceNameOrId;
import com.zimbra.soap.mail.type.DataSourceNameOrId;
import com.zimbra.soap.mail.type.GalDataSourceNameOrId;
import com.zimbra.soap.mail.type.ImapDataSourceNameOrId;
import com.zimbra.soap.mail.type.MailCalDataSource;
import com.zimbra.soap.mail.type.MailCaldavDataSource;
import com.zimbra.soap.mail.type.MailDataSource;
import com.zimbra.soap.mail.type.MailGalDataSource;
import com.zimbra.soap.mail.type.MailImapDataSource;
import com.zimbra.soap.mail.type.MailPop3DataSource;
import com.zimbra.soap.mail.type.MailRssDataSource;
import com.zimbra.soap.mail.type.Pop3DataSourceNameOrId;
import com.zimbra.soap.mail.type.RssDataSourceNameOrId;
import com.zimbra.soap.type.DataSource;
import com.zimbra.soap.type.DataSource.ConnectionType;

public class DataSourceJaxbTest {

    @Test
    public void testCreateDataSourceRequest() throws JAXBException {
        JAXBContext jaxb = JAXBContext.newInstance(CreateDataSourceRequest.class);
        Unmarshaller unmarshaller = jaxb.createUnmarshaller();
        CreateDataSourceRequest req = (CreateDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("CreateUnknownDataSourceRequest.xml"));
        DataSource ds = req.getDataSource();
        assertNotNull("Generic DataSource should not be NULL", ds);
        assertTrue("DataSource should be an instance of MailDataSource", ds instanceof MailDataSource);
        assertEquals("wrong folder ID", "257", ds.getFolderId());
        assertEquals("wrong refresh token", "AAbbccdd1244eeffVdNHR.l0_jzuWvPNtAt0BCCcOm8w9wq1gdB", ds.getRefreshToken());
        assertEquals("wrong host", "yahoo.com", ds.getHost());
        assertEquals("wrong name", "blablah@yahoo.com", ds.getName());
        assertEquals("wrong import class", "com.synacor.zimbra.OAuthDataImport", ds.getImportClass());

        req = (CreateDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("CreateImapDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("IMAP DataSource should not be NULL", ds);
        assertTrue("DataSource should be an instance of MailImapDataSource", ds instanceof MailImapDataSource);
        assertEquals("wrong folder ID", "2", ds.getFolderId());
        assertEquals("wrong refresh token", "AAbbccdd1244eeffVdNHR.l0_jzuWvPNtAt0BCCcOm8w9wq1gdB", ds.getRefreshToken());
        assertEquals("wrong clientSecret", "test123", ((MailImapDataSource)ds).getClientSecret());
        assertEquals("wrong clietnId", "someclientid", ((MailImapDataSource)ds).getClientId());
        //using a string that's as long as a real OAuth2 token to make sure Jaxb does not truncate it
        assertEquals("wrong oauth token", "LoremIpsumZZZyyY123.MaybeLorem.53445.WWW.YelpcYE1p6L1iS5vwZAg8pntEPGs2M.FcbPqY7PxBxORvoYhcbiTLlK7YcRwMB.1gprP5vxYghvqPT9KKV_EJ2vpDQr881.dzLB896UWZYI3hLrqAwiePEhFdw1XXTNP4u6gJ8J6ErPSZJInN1SMK74smQZErEpBgNfyFD2kgNPfcdxjMLh5ODjvhMufHcsb_NI5liTCOWa7k693ziHVZQkyGCJdAzxhtYeIyaCxyTsvaj3ybeXcye.qWFvwpdZbqWj8U8DqRXpjrq6wnNsK1ljGafon_uuX06wWNLh5P0GmLkaW4Yrumh9L7ir59Wm9bQ98TuHxNQDnLLqCK42P3Grb8guWGFRetPiLV2mb4ZT0fPAOeYPmaJXc2_OEWtr.KlazJt.ig6Me1HWR0vHC_MnfHUFA028wooDD9URrhTeMrvizeoePMfd3tgMEw721AbAoH58tg5VebNH_F_.wlQKYkU9rXv4v5Mzw3vsCKvW6wniJzepaY6gwKpMjlWn4CxAQNwRMPyoCDR2gRUXriulDO7vKbQ1ku_g2H.Swq2XGyr0EKjOjRAKVNGWJAh.1flPkzF0W5n7kcgJnG1H3AopZJMAypTiNQEDEyjqDCF41jgN4rKo5q9skZMbRG0D5jL0T_SoKDp6jWoXbWAmeog4Tb0eCHD0QJJGOa1BfFZkXbDB0Eet8JxxfG.0vM23oIervWHqcBPlfKJtkxiI9CCdqOniDNPfhbxkaSlOq3bQRmFOq08jaOwswlsECm4U95bEDxAtKcnVl8AnrP8qsbqxmanHdTh.dehEnjJ1gXyGpaMEBA2Yrt_QEewyWEDuR_zBKsomeDotsSp1.tyw3WCqnMx27tArDDasdNNlPPdfRVQlqe6gX45HfK0C8PAXsPnqZUA07.pFTa8ifcucsdqbh3_UpXnuPj8Mn.67GxwvujJkO8WpD3nCQpwp4Fns6NOnoNoasdgyiSCcGpCsGs2l6ZIFJyUMvIf4q6cfEiJ18lyBjRxi3rpgMAYTt5sOmikOst.gttN1gettkvWyY8-", 
                ((MailImapDataSource)ds).getOAuthToken());
        assertEquals("wrong host", "imap.yahoo.com", ds.getHost());
        assertEquals("wrong name", "blablah2@yahoo.com", ds.getName());

        req = (CreateDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("CreateCalDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("Cal DataSource should not be NULL", ds);
        assertTrue("DataSource should be an instance of MailCalDataSource", ds instanceof MailCalDataSource);
        assertEquals("wrong folder ID", "5", ds.getFolderId());
        assertEquals("wrong host", "calendar.google.com", ds.getHost());
        assertEquals("wrong name", "someCalDS", ds.getName());

        req = (CreateDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("CreateGalDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("GAL DataSource should not be NULL", ds);
        assertTrue("DataSource should be an instance of MailGalDataSource", ds instanceof MailGalDataSource);
        assertEquals("wrong folder ID", "7", ds.getFolderId());
        assertEquals("wrong host", "ldap.zimbra.com", ds.getHost());
        assertEquals("wrong name", "zimbraGAL", ds.getName());
        assertEquals("wrong polling interval", "24h", ds.getPollingInterval());
        assertTrue("wrong isEnabled", ds.isEnabled());

        req = (CreateDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("CreatePop3DataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("POP3 DataSource should not be NULL", ds);
        assertTrue("DataSource should be an instance of MailPop3DataSource", ds instanceof MailPop3DataSource);
        assertEquals("wrong folder ID", "1", ds.getFolderId());
        assertEquals("wrong host", "pop.email.provider.domain", ds.getHost());
        assertEquals("wrong name", "pop3DSForTest", ds.getName());
        assertEquals("wrong polling interval", "24h", ds.getPollingInterval());
        assertTrue("wrong leaveOnServer", ((MailPop3DataSource)ds).isLeaveOnServer());
        assertFalse("wrong isEnabled", ds.isEnabled());

        req = (CreateDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("CreateCaldavDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("CalDAV DataSource should not be NULL", ds);
        assertTrue("DataSource should be an instance of MailCaldavDataSource", ds instanceof MailCaldavDataSource);
        assertEquals("wrong folder ID", "3", ds.getFolderId());
        assertEquals("wrong host", "some.cal.dav.host", ds.getHost());
        assertEquals("wrong name", "caldavDS", ds.getName());
        assertEquals("wrong polling interval", "1h", ds.getPollingInterval());
        assertFalse("wrong isEnabled", ds.isEnabled());

        req = (CreateDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("CreateRssDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("RSS DataSource should not be NULL", ds);
        assertTrue("DataSource should be an instance of MailRssDataSource", ds instanceof MailRssDataSource);
        assertEquals("wrong folder ID", "260", ds.getFolderId());
        assertEquals("wrong host", "some.rss.dav.host", ds.getHost());
        assertEquals("wrong name", "RssFeedDataSource", ds.getName());
        assertEquals("wrong polling interval", "30m", ds.getPollingInterval());
        assertTrue("wrong isEnabled", ds.isEnabled());
    }

    @Test
    public void testModifyDataSourceRequest() throws JAXBException {
        JAXBContext jaxb = JAXBContext.newInstance(ModifyDataSourceRequest.class);
        Unmarshaller unmarshaller = jaxb.createUnmarshaller();
        ModifyDataSourceRequest req = (ModifyDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("ModifyUnknownDataSourceRequest.xml"));
        DataSource ds = req.getDataSource();
        assertNotNull("Generic DataSource should not be NULL", ds);
        assertNotNull("Generic DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for generic DataSource", "11e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailDataSource", ds instanceof MailDataSource);
        assertEquals("wrong refresh token", "AAbbccdd1244eeffVdNHR.l0_jzuWvPNtAt0BCCcOm8w9wq1gdB", ds.getRefreshToken());
        assertEquals("wrong host", "yahoo.com", ds.getHost());
        assertEquals("wrong import class", "com.synacor.zimbra.OAuthDataImport", ds.getImportClass());
        assertEquals("wrong polling interval", "60s", ds.getPollingInterval());

        req = (ModifyDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("ModifyImapDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("IMAP DataSource should not be NULL", ds);
        assertNotNull("IMAP DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for IMAP DataSource", "71e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailImapDataSource", ds instanceof MailImapDataSource);
        assertEquals("wrong polling interval", "30m", ds.getPollingInterval());

        req = (ModifyDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("ModifyCalDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("Cal DataSource should not be NULL", ds);
        assertNotNull("Cal DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for Cal DataSource", "61e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailCalDataSource", ds instanceof MailCalDataSource);
        assertEquals("wrong folder ID", "333", ds.getFolderId());

        req = (ModifyDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("ModifyGalDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("GAL DataSource should not be NULL", ds);
        assertNotNull("GAL DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for GAL DataSource", "51e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailGalDataSource", ds instanceof MailGalDataSource);
        assertEquals("wrong polling interval", "69s", ds.getPollingInterval());

        req = (ModifyDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("ModifyPop3DataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("POP3 DataSource should not be NULL", ds);
        assertNotNull("POP3 DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for POP3 DataSource", "41e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailPop3DataSource", ds instanceof MailPop3DataSource);
        assertEquals("wrong polling interval", "1m", ds.getPollingInterval());
        assertFalse("wrong leaveOnServer", ((MailPop3DataSource)ds).isLeaveOnServer());

        req = (ModifyDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("ModifyCaldavDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("Caldav DataSource should not be NULL", ds);
        assertNotNull("Caldav DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for Caldav DataSource", "31e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailCaldavDataSource", ds instanceof MailCaldavDataSource);
        assertEquals("wrong polling interval", "60s", ds.getPollingInterval());

        req = (ModifyDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("ModifyRssDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("RSS DataSource should not be NULL", ds);
        assertNotNull("RSS DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for RSS DataSource", "21e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailRssDataSource", ds instanceof MailRssDataSource);
        assertEquals("wrong polling interval", "2d", ds.getPollingInterval());
        assertFalse("wrong isEnabled", ds.isEnabled());
    }

    @Test
    public void testTestDataSourceRequest() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(TestDataSourceRequest.class);
        Unmarshaller unmarshaller = jaxb.createUnmarshaller();
        TestDataSourceRequest req = (TestDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("TestUnknownDataSourceRequest.xml"));
        DataSource ds = req.getDataSource();
        assertNotNull("Generic DataSource should not be NULL", ds);
        assertNotNull("Generic DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for generic DataSource", "11e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailDataSource", ds instanceof MailDataSource);

        req = (TestDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("TestImapDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("IMAP DataSource should not be NULL", ds);
        assertNotNull("IMAP DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for IMAP DataSource", "71e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailImapDataSource", ds instanceof MailImapDataSource);

        req = (TestDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("TestCalDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("Cal DataSource should not be NULL", ds);
        assertNotNull("Cal DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for Cal DataSource", "31e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailCalDataSource", ds instanceof MailCalDataSource);

        req = (TestDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("TestGalDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("GAL DataSource should not be NULL", ds);
        assertNotNull("GAL DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for GAL DataSource", "51e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailGalDataSource", ds instanceof MailGalDataSource);

        req = (TestDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("TestPop3DataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("POP3 DataSource should not be NULL", ds);
        assertNotNull("POP3 DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for POP3 DataSource", "41e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailPop3DataSource", ds instanceof MailPop3DataSource);

        req = (TestDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("TestCaldavDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("Caldav DataSource should not be NULL", ds);
        assertNotNull("Caldav DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for Caldav DataSource", "31e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailCaldavDataSource", ds instanceof MailCaldavDataSource);

        req = (TestDataSourceRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("TestRssDataSourceRequest.xml"));
        ds = req.getDataSource();
        assertNotNull("RSS DataSource should not be NULL", ds);
        assertNotNull("RSS DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for RSS DataSource", "21e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSource should be an instance of MailRssDataSource", ds instanceof MailRssDataSource);
    }

    @Test
    public void testGetDataSourcesResponse() throws Exception {
        //response with one Unknown datasource
        JAXBContext jaxb = JAXBContext.newInstance(GetDataSourcesResponse.class);
        Unmarshaller unmarshaller = jaxb.createUnmarshaller();
        GetDataSourcesResponse resp = (GetDataSourcesResponse) unmarshaller.unmarshal(
                getClass().getResourceAsStream("GetUnknownDataSourcesResponse.xml"));
        List<DataSource> dsList = resp.getDataSources();
        assertNotNull("datasources should not be NULL", dsList);
        assertFalse("list of datasources should not be empty", dsList.isEmpty());
        assertEquals("expecting one datasource in the list", 1, dsList.size());
        DataSource ds = dsList.get(0);
        assertNotNull("Generic DataSource should not be NULL", ds);
        assertNotNull("Generic DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for generic DataSource", "8d17e182-fdc6-4f6c-b83f-d478c9b04bfd", ds.getId());
        assertTrue("DataSource should be an instance of MailDataSource", ds instanceof MailDataSource);
        assertEquals("wrong refresh token", "AAbbcdd22wkBVsVdNHR.l0_jzuWvPNiAt0DBOcRm7w9zLorEM", ds.getRefreshToken());
        assertEquals("wrong host", "yahoo.com", ds.getHost());
        assertEquals("wrong import class", "com.synacor.zimbra.OAuthDataImport", ds.getImportClass());
        assertEquals("wrong datasource name", "blablah@yahoo.com", ds.getName());

        //response with one IMAP datasource
        resp = (GetDataSourcesResponse) unmarshaller.unmarshal(
                getClass().getResourceAsStream("GetImapDataSourcesResponse.xml"));
        dsList = resp.getDataSources();
        assertNotNull("datasources should not be NULL", dsList);
        assertFalse("list of datasources should not be empty", dsList.isEmpty());
        assertEquals("expecting 1 datasource in the list", 1, dsList.size());
        ds = dsList.get(0);
        assertNotNull("IMAP DataSource should not be NULL", ds);
        assertNotNull("IMAP DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for IMAP DataSource", "d96e9a7d-6af9-4625-ba68-37bcd73fce6d", ds.getId());
        assertTrue("DataSource should be an instance of MailImapDataSource", ds instanceof MailImapDataSource);
        assertEquals("wrong connectionType", ConnectionType.cleartext, ds.getConnectionType());
        assertEquals("wrong host", "imap.zimbra.com", ds.getHost());
        assertEquals("wrong datasource name", "myIMAPSource", ds.getName());
        
        //response with an Unknown and an IMAP datasources
        resp = (GetDataSourcesResponse) unmarshaller.unmarshal(
                getClass().getResourceAsStream("GetTwoDataSourcesResponse.xml"));
        dsList = resp.getDataSources();
        assertNotNull("datasources should not be NULL", dsList);
        assertFalse("list of datasources should not be empty", dsList.isEmpty());
        assertEquals("expecting 2 datasources in the list", 2, dsList.size());
        ds = dsList.get(0);
        assertNotNull("Generic DataSource should not be NULL", ds);
        assertNotNull("Generic DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for generic DataSource", "8d17e182-fdc6-4f6c-b83f-d478c9b04bfd", ds.getId());
        assertTrue("DataSource should be an instance of MailDataSource", ds instanceof MailDataSource);
        assertEquals("wrong refresh token", "AAbbcdd22wkBVsVdNHR.l0_jzuWvPNiAt0DBOcRm7w9zLorEM", ds.getRefreshToken());
        assertEquals("wrong host", "yahoo.com", ds.getHost());
        assertEquals("wrong import class", "com.synacor.zimbra.OAuthDataImport", ds.getImportClass());
        assertEquals("wrong datasource name", "blablah@yahoo.com", ds.getName());

        ds = dsList.get(1);
        assertNotNull("IMAP DataSource should not be NULL", ds);
        assertNotNull("IMAP DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for IMAP DataSource", "d96e9a7d-6af9-4625-ba68-37bcd73fce6d", ds.getId());
        assertTrue("DataSource should be an instance of MailImapDataSource", ds instanceof MailImapDataSource);
        assertEquals("wrong connectionType", ConnectionType.cleartext, ds.getConnectionType());
        assertEquals("wrong host", "imap.zimbra.com", ds.getHost());
        assertEquals("wrong datasource name", "myIMAPSource", ds.getName());

        //Response with one element of each type of datasource
        resp = (GetDataSourcesResponse) unmarshaller.unmarshal(
                getClass().getResourceAsStream("GetOneEachDataSourcesResponse.xml"));
        dsList = resp.getDataSources();
        assertNotNull("datasources should not be NULL", dsList);
        assertFalse("list of datasources should not be empty", dsList.isEmpty());
        assertEquals("expecting 7 datasources in the list", 7, dsList.size());
        ds = dsList.get(0);
        assertNotNull("Generic DataSource should not be NULL", ds);
        assertNotNull("Generic DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for generic DataSource", "8d17e182-fdc6-4f6c-b83f-d478c9b04bfd", ds.getId());
        assertTrue("DataSource should be an instance of MailDataSource", ds instanceof MailDataSource);
        assertEquals("wrong refresh token", "AAbbcdd22wkBVsVdNHR.l0_jzuWvPNiAt0DBOcRm7w9zLorEM", ds.getRefreshToken());
        assertEquals("wrong host", "yahoo.com", ds.getHost());
        assertEquals("wrong import class", "com.synacor.zimbra.OAuthDataImport", ds.getImportClass());
        assertEquals("wrong datasource name", "blablah@yahoo.com", ds.getName());

        ds = dsList.get(1);
        assertNotNull("IMAP DataSource should not be NULL", ds);
        assertNotNull("IMAP DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for IMAP DataSource", "d96e9a7d-6af9-4625-ba68-37bcd73fce6d", ds.getId());
        assertTrue("DataSource should be an instance of MailImapDataSource", ds instanceof MailImapDataSource);
        assertEquals("wrong connectionType", ConnectionType.cleartext, ds.getConnectionType());
        assertEquals("wrong host", "imap.zimbra.com", ds.getHost());
        assertEquals("wrong datasource name", "myIMAPSource", ds.getName());

        ds = dsList.get(2);
        assertNotNull("POP3 DataSource should not be NULL", ds);
        assertNotNull("POP3 DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for POP3 DataSource", "b5e98a1f-5f93-4e19-a1a4-956c4c95af1b", ds.getId());
        assertTrue("DataSource should be an instance of MailPop3DataSource", ds instanceof MailPop3DataSource);
        assertEquals("wrong connectionType", ConnectionType.cleartext, ds.getConnectionType());
        assertEquals("wrong host", "pop.zimbra.com", ds.getHost());
        assertEquals("wrong datasource name", "myPop3Mail", ds.getName());
        assertTrue("wrong leaveOnServer", ((MailPop3DataSource)ds).isLeaveOnServer());

        ds = dsList.get(3);
        assertNotNull("RSS DataSource should not be NULL", ds);
        assertNotNull("RSS DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for RSS DataSource", "89bca37f-9096-419d-9471-62149a58cbdc", ds.getId());
        assertTrue("DataSource should be an instance of MailRssDataSource", ds instanceof MailRssDataSource);
        assertEquals("wrong connectionType", ConnectionType.cleartext, ds.getConnectionType());
        assertEquals("wrong host", "rss.zimbra.com", ds.getHost());
        assertEquals("wrong datasource name", "myRssFeed", ds.getName());
        assertEquals("wrong polling interval", "43200000", ds.getPollingInterval());
        assertEquals("wrong FolderId", "260", ds.getFolderId());

        ds = dsList.get(4);
        assertNotNull("Cal DataSource should not be NULL", ds);
        assertNotNull("Cal DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for Cal DataSource", "112da07b-43e3-41ab-a0b3-5c109169ee49", ds.getId());
        assertTrue("DataSource should be an instance of MailCalDataSource", ds instanceof MailCalDataSource);
        assertEquals("wrong host", "calendar.zimbra.com", ds.getHost());
        assertEquals("wrong datasource name", "GCal", ds.getName());
        assertEquals("wrong polling interval", "63100000", ds.getPollingInterval());

        ds = dsList.get(5);
        assertNotNull("GAL DataSource should not be NULL", ds);
        assertNotNull("GAL DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for GAL DataSource", "ed408f4d-f8d5-4597-bf49-563ed62b64de", ds.getId());
        assertTrue("DataSource should be an instance of MailCalDataSource", ds instanceof MailGalDataSource);
        assertEquals("wrong host", "ldap.somehost.local", ds.getHost());
        assertEquals("wrong datasource name", "corpAddressBook", ds.getName());

        ds = dsList.get(6);
        assertNotNull("Caldav DataSource should not be NULL", ds);
        assertNotNull("Caldav DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for Caldav DataSource", "95c066a8-5ad6-40fa-a094-06f8b3531878", ds.getId());
        assertTrue("DataSource should be an instance of MailCaldavDataSource", ds instanceof MailCaldavDataSource);
        assertEquals("wrong host", "dav.zimbra.com", ds.getHost());
        assertEquals("wrong datasource name", "externalDAV", ds.getName());

        //Response with multiple instances of some types of data sources
        resp = (GetDataSourcesResponse) unmarshaller.unmarshal(
                getClass().getResourceAsStream("GetManyDataSourcesResponse.xml"));
        dsList = resp.getDataSources();
        assertNotNull("datasources should not be NULL", dsList);
        assertFalse("list of datasources should not be empty", dsList.isEmpty());
        assertEquals("expecting 10 datasources in the list", 10, dsList.size());
        ds = dsList.get(0);
        assertNotNull("Generic DataSource should not be NULL", ds);
        assertNotNull("Generic DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for generic DataSource", "8d17e182-fdc6-4f6c-b83f-d478c9b04bfd", ds.getId());
        assertTrue("DataSource should be an instance of MailDataSource", ds instanceof MailDataSource);
        assertEquals("wrong refresh token", "AAbbcdd22wkBVsVdNHR.l0_jzuWvPNiAt0DBOcRm7w9zLorEM", ds.getRefreshToken());
        assertEquals("wrong host", "yahoo.com", ds.getHost());
        assertEquals("wrong import class", "com.synacor.zimbra.OAuthDataImport", ds.getImportClass());
        assertEquals("wrong datasource name", "blablah@yahoo.com", ds.getName());

        ds = dsList.get(1);
        assertNotNull("IMAP DataSource should not be NULL", ds);
        assertNotNull("IMAP DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for IMAP DataSource", "d96e9a7d-6af9-4625-ba68-37bcd73fce6d", ds.getId());
        assertTrue("DataSource should be an instance of MailImapDataSource", ds instanceof MailImapDataSource);
        assertEquals("wrong connectionType", ConnectionType.cleartext, ds.getConnectionType());
        assertEquals("wrong host", "imap.zimbra.com", ds.getHost());
        assertEquals("wrong datasource name", "myIMAPSource", ds.getName());
        assertEquals("wrong port", 143, ds.getPort().intValue());

        ds = dsList.get(2);
        assertNotNull("POP3 DataSource should not be NULL", ds);
        assertNotNull("POP3 DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for POP3 DataSource", "b5e98a1f-5f93-4e19-a1a4-956c4c95af1b", ds.getId());
        assertTrue("DataSource should be an instance of MailPop3DataSource", ds instanceof MailPop3DataSource);
        assertEquals("wrong connectionType", ConnectionType.cleartext, ds.getConnectionType());
        assertEquals("wrong host", "pop.zimbra.com", ds.getHost());
        assertEquals("wrong datasource name", "myPop3Mail", ds.getName());
        assertTrue("wrong leaveOnServer", ((MailPop3DataSource)ds).isLeaveOnServer());

        ds = dsList.get(3);
        assertNotNull("RSS DataSource should not be NULL", ds);
        assertNotNull("RSS DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for RSS DataSource", "89bca37f-9096-419d-9471-62149a58cbdc", ds.getId());
        assertTrue("DataSource should be an instance of MailRssDataSource", ds instanceof MailRssDataSource);
        assertEquals("wrong connectionType", ConnectionType.cleartext, ds.getConnectionType());
        assertEquals("wrong host", "rss.zimbra.com", ds.getHost());
        assertEquals("wrong datasource name", "myRssFeed", ds.getName());
        assertEquals("wrong polling interval", "43200000", ds.getPollingInterval());
        assertEquals("wrong FolderId", "260", ds.getFolderId());

        ds = dsList.get(4);
        assertNotNull("Cal DataSource should not be NULL", ds);
        assertNotNull("Cal DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for Cal DataSource", "112da07b-43e3-41ab-a0b3-5c109169ee49", ds.getId());
        assertTrue("DataSource should be an instance of MailCalDataSource", ds instanceof MailCalDataSource);
        assertEquals("wrong host", "calendar.zimbra.com", ds.getHost());
        assertEquals("wrong datasource name", "GCal", ds.getName());
        assertEquals("wrong polling interval", "63100000", ds.getPollingInterval());

        ds = dsList.get(5);
        assertNotNull("GAL DataSource should not be NULL", ds);
        assertNotNull("GAL DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for GAL DataSource", "ed408f4d-f8d5-4597-bf49-563ed62b64de", ds.getId());
        assertTrue("DataSource should be an instance of MailCalDataSource", ds instanceof MailGalDataSource);
        assertEquals("wrong host", "ldap.somehost.local", ds.getHost());
        assertEquals("wrong datasource name", "corpAddressBook", ds.getName());

        ds = dsList.get(6);
        assertNotNull("Caldav DataSource should not be NULL", ds);
        assertNotNull("Caldav DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for Caldav DataSource", "95c066a8-5ad6-40fa-a094-06f8b3531878", ds.getId());
        assertTrue("DataSource should be an instance of MailCaldavDataSource", ds instanceof MailCaldavDataSource);
        assertEquals("wrong host", "dav.zimbra.com", ds.getHost());
        assertEquals("wrong datasource name", "externalDAV", ds.getName());

        ds = dsList.get(7);
        assertNotNull("2d RSS DataSource should not be NULL", ds);
        assertNotNull("2d RSS DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for the 2d RSS DataSource", "f32349af-9a78-4c26-80a1-338203378930", ds.getId());
        assertTrue("DataSource should be an instance of MailRssDataSource", ds instanceof MailRssDataSource);
        assertEquals("wrong connectionType", ConnectionType.cleartext, ds.getConnectionType());
        assertEquals("wrong host", "news.yahoo.com", ds.getHost());
        assertEquals("wrong datasource name", "myYahoo", ds.getName());
        assertEquals("wrong polling interval", "43200000", ds.getPollingInterval());
        assertEquals("wrong FolderId", "261", ds.getFolderId());

        ds = dsList.get(8);
        assertNotNull("2d IMAP DataSource should not be NULL", ds);
        assertNotNull("2d IMAP DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for the 2d IMAP DataSource", "b2e929f5-e124-47a0-b1b4-a7fbcd14fb31", ds.getId());
        assertTrue("DataSource should be an instance of MailImapDataSource", ds instanceof MailImapDataSource);
        assertEquals("wrong connectionType", ConnectionType.tls_if_available, ds.getConnectionType());
        assertEquals("wrong host", "imap3.zimbra.com", ds.getHost());
        assertEquals("wrong port", 193, ds.getPort().intValue());
        assertEquals("wrong datasource name", "forgottenMail", ds.getName());

        ds = dsList.get(9);
        assertNotNull("2d Generic DataSource should not be NULL", ds);
        assertNotNull("2d Generic DataSource ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for the 2d generic DataSource", "82e3b467-5a0f-4cff-ad8d-533ed6fc4992", ds.getId());
        assertTrue("DataSource should be an instance of MailDataSource", ds instanceof MailDataSource);
        assertNull("Refresh token should be NULL",ds.getRefreshToken());
        assertEquals("wrong host", "abook.gmail.com", ds.getHost());
        assertEquals("wrong import class", "com.synacor.zimbra.OAuthDataImport", ds.getImportClass());
        assertEquals("wrong datasource name", "someone@gmail.com", ds.getName());
    }

    @Test
    public void testImportDataRequest() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(ImportDataRequest.class);
        Unmarshaller unmarshaller = jaxb.createUnmarshaller();
        ImportDataRequest resp = (ImportDataRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("UnknownImportDataRequest.xml"));
        List<DataSourceNameOrId> dsList = resp.getDataSources();
        assertNotNull("datasources should not be NULL", dsList);
        assertFalse("list of datasources should not be empty", dsList.isEmpty());
        assertEquals("expecting one datasource in the list", 1, dsList.size());
        DataSourceNameOrId ds = dsList.get(0);
        assertNotNull("Generic DataSourceNameOrId should not be NULL", ds);
        assertNotNull("Generic DataSourceNameOrId ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for generic DataSource", "8d17e182-fdc6-4f6c-b83f-d478c9b04bfd", ds.getId());
        assertTrue("DataSource should be an instance of DataSourceNameOrId", ds instanceof DataSourceNameOrId);

        resp = (ImportDataRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("ImapImportDataRequest.xml"));
        dsList = resp.getDataSources();
        assertNotNull("datasources should not be NULL", dsList);
        assertFalse("list of datasources should not be empty", dsList.isEmpty());
        assertEquals("expecting 1 datasource in the list", 1, dsList.size());
        ds = dsList.get(0);
        assertNotNull("IMAP DataSourceNameOrId should not be NULL", ds);
        assertNotNull("IMAP DataSourceNameOrId ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for IMAP DataSourceNameOrId", "d96e9a7d-6af9-4625-ba68-37bcd73fce6d", ds.getId());
        assertTrue("DataSourceNameOrId should be an instance of ImapDataSourceNameOrId", ds instanceof ImapDataSourceNameOrId);
        
        resp = (ImportDataRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("Pop3ImportDataRequest.xml"));
        dsList = resp.getDataSources();
        assertNotNull("datasources should not be NULL", dsList);
        assertFalse("list of datasources should not be empty", dsList.isEmpty());
        assertEquals("expecting 1 datasource in the list", 1, dsList.size());
        ds = dsList.get(0);
        assertNotNull("POP3 DataSourceNameOrId should not be NULL", ds);
        assertNotNull("POP3 DataSourceNameOrId ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for POP3 DataSourceNameOrId", "d96e9a7d-6af9-4625-ba68-37bcd73fce6d", ds.getId());
        assertTrue("DataSourceNameOrId should be an instance of Pop3DataSourceNameOrId", ds instanceof Pop3DataSourceNameOrId);

        resp = (ImportDataRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("RssImportDataRequest.xml"));
        dsList = resp.getDataSources();
        assertNotNull("datasources should not be NULL", dsList);
        assertFalse("list of datasources should not be empty", dsList.isEmpty());
        assertEquals("expecting 1 datasource in the list", 1, dsList.size());
        ds = dsList.get(0);
        assertNotNull("POP3 DataSourceNameOrId should not be NULL", ds);
        assertNotNull("POP3 DataSourceNameOrId ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for RSS DataSourceNameOrId", "21e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSourceNameOrId should be an instance of RssDataSourceNameOrId", ds instanceof RssDataSourceNameOrId);

        resp = (ImportDataRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("CaldavImportDataRequest.xml"));
        dsList = resp.getDataSources();
        assertNotNull("datasources should not be NULL", dsList);
        assertFalse("list of datasources should not be empty", dsList.isEmpty());
        assertEquals("expecting 1 datasource in the list", 1, dsList.size());
        ds = dsList.get(0);
        assertNotNull("Caldav DataSourceNameOrId should not be NULL", ds);
        assertNotNull("Caldav DataSourceNameOrId ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for Caldav DataSourceNameOrId", "31e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSourceNameOrId should be an instance of CaldavDataSourceNameOrId", ds instanceof CaldavDataSourceNameOrId);

        resp = (ImportDataRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("CalImportDataRequest.xml"));
        dsList = resp.getDataSources();
        assertNotNull("datasources should not be NULL", dsList);
        assertFalse("list of datasources should not be empty", dsList.isEmpty());
        assertEquals("expecting 1 datasource in the list", 1, dsList.size());
        ds = dsList.get(0);
        assertNotNull("Cal DataSourceNameOrId should not be NULL", ds);
        assertNotNull("Cal DataSourceNameOrId ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for Caldav DataSourceNameOrId", "61e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSourceNameOrId should be an instance of CalDataSourceNameOrId", ds instanceof CalDataSourceNameOrId);

        resp = (ImportDataRequest) unmarshaller.unmarshal(
                getClass().getResourceAsStream("GalImportDataRequest.xml"));
        dsList = resp.getDataSources();
        assertNotNull("datasources should not be NULL", dsList);
        assertFalse("list of datasources should not be empty", dsList.isEmpty());
        assertEquals("expecting 1 datasource in the list", 1, dsList.size());
        ds = dsList.get(0);
        assertNotNull("GAL DataSourceNameOrId should not be NULL", ds);
        assertNotNull("GAL DataSourceNameOrId ID should not be NULL", ds.getId());
        assertEquals("Wrong ID for Caldav DataSourceNameOrId", "51e1c69c-bbb3-4f5d-8903-14ef8bdacbcc", ds.getId());
        assertTrue("DataSourceNameOrId should be an instance of CalDataSourceNameOrId", ds instanceof GalDataSourceNameOrId);
    }
}
