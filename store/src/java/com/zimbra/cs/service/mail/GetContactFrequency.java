package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.event.EventStore;
import com.zimbra.cs.event.analytics.contact.ContactFrequencyGraph;
import com.zimbra.cs.event.analytics.contact.ContactFrequencyGraph.TimeRange;
import com.zimbra.cs.event.analytics.contact.ContactFrequencyGraphDataPoint;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetContactFrequencyRequest;
import com.zimbra.soap.mail.message.GetContactFrequencyResponse;
import com.zimbra.soap.mail.type.ContactFrequencyData;
import com.zimbra.soap.mail.type.ContactFrequencyDataPoint;

public class GetContactFrequency extends MailDocumentHandler {

    private static final Map<String, TimeRange> timeRangeMap = new HashMap<>();
    static {
        timeRangeMap.put("d", TimeRange.CURRENT_MONTH);
        timeRangeMap.put("w", TimeRange.LAST_SIX_MONTHS);
        timeRangeMap.put("m", TimeRange.CURRENT_YEAR);
    }

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        GetContactFrequencyRequest req = zsc.elementToJaxb(request);
        String contactEmail = req.getEmail();
        String freqByCSV = req.getFrequencyBy();
        EventStore eventStore = EventStore.getFactory().getEventStore(acct.getId());
        GetContactFrequencyResponse resp = new GetContactFrequencyResponse();
        for (Pair<String, TimeRange> timeRange: parseTimeRange(freqByCSV)) {
            String freqBy = timeRange.getFirst();
            TimeRange range = timeRange.getSecond();
            List<ContactFrequencyDataPoint> dataPoints = getGraphData(contactEmail, range, eventStore);
            ContactFrequencyData graphData = new ContactFrequencyData(freqBy, dataPoints);
            resp.addFrequencyGraph(graphData);
        }
        return zsc.jaxbToElement(resp);
    }

    private List<ContactFrequencyDataPoint> getGraphData(String email, TimeRange timeRange, EventStore eventStore) throws ServiceException {
        List<ContactFrequencyGraphDataPoint> dataPoints = ContactFrequencyGraph.getContactFrequencyGraph(email, timeRange, eventStore);
        List<ContactFrequencyDataPoint> soapDataPoints = dataPoints.stream().map(dp -> toSOAPDataPoint(dp)).collect(Collectors.toList());
        return soapDataPoints;
    }

    private ContactFrequencyDataPoint toSOAPDataPoint(ContactFrequencyGraphDataPoint dataPoint) {
        return new ContactFrequencyDataPoint(dataPoint.getLabel(), dataPoint.getValue());

    }
    private List<Pair<String, TimeRange>> parseTimeRange(String csv) throws ServiceException {
        List<Pair<String, TimeRange>> requestedRanges = new ArrayList<>();
        for (String token: csv.split(",")) {
            TimeRange tr = timeRangeMap.get(token);
            if (tr != null) {
                requestedRanges.add(new Pair<String, TimeRange>(token, tr));
            } else {
                throw ServiceException.INVALID_REQUEST(token + " is not a valid time range; accepted values are {d,w,m}", null);
            }
        }
        return requestedRanges;
    }
}
