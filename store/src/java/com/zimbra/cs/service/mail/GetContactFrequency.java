package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.event.EventStore;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyGraphInterval;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyGraphTimeRange;
import com.zimbra.cs.event.analytics.contact.ContactFrequencyGraphDataPoint;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetContactFrequencyRequest;
import com.zimbra.soap.mail.message.GetContactFrequencyResponse;
import com.zimbra.soap.mail.type.ContactFrequencyData;
import com.zimbra.soap.mail.type.ContactFrequencyDataPoint;
import com.zimbra.soap.mail.type.ContactFrequencyGraphSpec;

public class GetContactFrequency extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        GetContactFrequencyRequest req = zsc.elementToJaxb(request);
        String contactEmail = req.getEmail();
        List<ContactFrequencyGraphSpec> specs = req.getGraphSpecs();
        Integer offsetInMinutes = req.getOffsetInMinutes();
        EventStore eventStore = EventStore.getFactory().getEventStore(acct.getId());
        GetContactFrequencyResponse resp = new GetContactFrequencyResponse();
        for (ContactFrequencyGraphSpec spec: specs) {
            ContactFrequencyGraphInterval interval = ContactFrequencyGraphInterval.of(spec.getInterval());
            ContactFrequencyGraphTimeRange range = new ContactFrequencyGraphTimeRange(spec.getRange());
            ContactAnalytics.ContactFrequencyGraphSpec graphSpec = new ContactAnalytics.ContactFrequencyGraphSpec(range, interval);
            List<ContactFrequencyDataPoint> dataPoints = getGraphData(contactEmail, graphSpec, offsetInMinutes, eventStore);
            ContactFrequencyData graphData = new ContactFrequencyData(spec, dataPoints);
            resp.addFrequencyGraph(graphData);
        }
        return zsc.jaxbToElement(resp);
    }

    private List<ContactFrequencyDataPoint> getGraphData(String email, ContactAnalytics.ContactFrequencyGraphSpec graphSpec, Integer offsetInMinutes, EventStore eventStore) throws ServiceException {
        List<ContactFrequencyGraphDataPoint> dataPoints;
        if(offsetInMinutes == null) {
            dataPoints = ContactAnalytics.getContactFrequencyGraph(email, graphSpec, eventStore);
        } else {
            dataPoints = ContactAnalytics.getContactFrequencyGraph(email, graphSpec, eventStore, offsetInMinutes);
        }
        List<ContactFrequencyDataPoint> soapDataPoints = dataPoints.stream().map(dp -> toSOAPDataPoint(dp)).collect(Collectors.toList());
        return soapDataPoints;
    }

    private ContactFrequencyDataPoint toSOAPDataPoint(ContactFrequencyGraphDataPoint dataPoint) {
        return new ContactFrequencyDataPoint(dataPoint.getLabel(), dataPoint.getValue());

    }
}
