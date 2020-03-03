package org.milyn.edifact.edg.template;

import com.github.mustachejava.TemplateFunction;
import org.apache.commons.jxpath.JXPathContext;
import org.milyn.edifact.ect.formats.unedifact.UnEdifactSpecificationReader;
import org.milyn.edifact.edisax.model.internal.Edimap;
import org.milyn.edifact.edisax.util.EDIUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

public class MessagesTemplate extends Template {
    private static final String SEQUENCE_XML_START_TAG = "<xsd:sequence dfdl:initiatedContent=\"yes\">\n";
    private static final String SEQUENCE_XML_END_TAG = "</xsd:sequence>\n";
    private static final String MESSAGE_SEGMENT_MUSTACHE_PARTIAL = "{{> MessageSegment.xsd.mustache}}\n";
    private static final String MESSAGE_SEGMENT_GROUP_MUSTACHE_PARTIAL = "{{> MessageSegmentGroup.xsd.mustache}}\n";

    public MessagesTemplate(final String version, final UnEdifactSpecificationReader unEdifactSpecificationReader) throws IOException {
        super(version);
        final List<Map<String, Object>> messages = new ArrayList<>();
        final Set<String> messageNames = unEdifactSpecificationReader.getMessageNames().stream().filter(m -> !m.equals(EDIUtils.MODEL_SET_DEFINITIONS_DESCRIPTION.getName())).collect(Collectors.toSet());
        for (String messageName : messageNames) {
            final Edimap edimap = unEdifactSpecificationReader.getMappingModel(messageName);
            final Map<String, Object> message = OBJECT_MAPPER.convertValue(edimap, Map.class);
            final List<Map<String, Object>> segments = (List<Map<String, Object>>) JXPathContext.newContext(message).getValue("/segments/segments", List.class);
            templatizeSegments(segments);

            messages.add(message);
        }
        scope.put("messages", messages);
    }

    private void templatizeSegments(final List<Map<String, Object>> segments) {
        templatizeSegments(segments.get(0), tail(segments), true);
    }

    private void templatizeSegments(final Map<String, Object> headSegment, final List<Map<String, Object>> tailSegments, final boolean isFirstSegment) {
        final StringWriter stringWriter = new StringWriter();
        final List<Map<String, Object>> nestedSegments = (List<Map<String, Object>>) headSegment.get("segments");
        if (isEmpty(nestedSegments)) {
            writeSegment(tailSegments, stringWriter, isFirstSegment);
        } else {
            writeSegmentGroup(headSegment, tailSegments, stringWriter);
            templatizeSegments(nestedSegments.get(0), tail(nestedSegments), true);
        }

        if (headSegment.get("segcode").equals("UNS")) {
            headSegment.put("namespacePrefix", "srv");
        } else {
            headSegment.put("namespacePrefix", version);
        }
        headSegment.put("render", (TemplateFunction) s -> stringWriter.toString());

        if (!tailSegments.isEmpty()) {
            templatizeSegments(tailSegments.get(0), tail(tailSegments), false);
        }
    }

    private void writeSegment(final List<Map<String, Object>> tailSegments, final StringWriter stringWriter, final boolean isFirstSegment) {
        if (isFirstSegment) {
            stringWriter.write(SEQUENCE_XML_START_TAG);
        }
        stringWriter.write(MESSAGE_SEGMENT_MUSTACHE_PARTIAL);
        if (tailSegments.isEmpty() || !isEmpty((Collection) tailSegments.get(0).get("segments"))) {
            stringWriter.write(SEQUENCE_XML_END_TAG);
        }
    }

    private void writeSegmentGroup(final Map<String, Object> headSegment, final List<Map<String, Object>> tailSegments, final StringWriter stringWriter) {
        final String xmltag = (String) headSegment.get("xmltag");
        headSegment.put("xmltag", "SegGrp-" + xmltag.substring(xmltag.lastIndexOf("_") + 1));
        stringWriter.write(MESSAGE_SEGMENT_GROUP_MUSTACHE_PARTIAL);
        if (!tailSegments.isEmpty() && isEmpty((Collection) tailSegments.get(0).get("segments"))) {
            stringWriter.write(SEQUENCE_XML_START_TAG);
        }
    }

    private List tail(final List list) {
        if (list.isEmpty() || list.size() < 2) {
            return Collections.EMPTY_LIST;
        } else {
            return list.subList(1, list.size());
        }
    }

    private boolean isEmpty(final Collection collection) {
        return (collection == null || collection.isEmpty());
    }

    @Override
    public String getName() {
        return "EDIFACT-Templates/EDIFACT-Messages.dfdl.xsd.mustache";
    }
}
