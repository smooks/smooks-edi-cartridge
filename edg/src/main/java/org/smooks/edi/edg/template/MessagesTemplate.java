package org.smooks.edi.edg.template;

import com.github.mustachejava.TemplateFunction;
import org.apache.commons.jxpath.JXPathContext;
import org.smooks.edi.ect.formats.unedifact.UnEdifactSpecificationReader;
import org.smooks.edi.edisax.model.internal.Edimap;
import org.smooks.edi.edisax.util.EDIUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

public class MessagesTemplate extends Template {
    private static final String SEQUENCE_XML_START_TAG = "<xsd:sequence dfdl:initiatedContent=\"yes\">\n";
    private static final String SEQUENCE_XML_END_TAG = "</xsd:sequence>\n";
    private static final String MESSAGE_SEGMENT_MUSTACHE_PARTIAL = "{{> MessageSegment.xsd.mustache}}\n";
    private static final String MESSAGE_SEGMENT_GROUP_MUSTACHE_PARTIAL = "{{> MessageSegmentGroup.xsd.mustache}}\n";

    private final List<String> messageTypes;

    public MessagesTemplate(final String version, final UnEdifactSpecificationReader unEdifactSpecificationReader) throws IOException {
        super(version);
        final List<Map<String, Object>> messageDefinitions = new ArrayList<>();
        messageTypes = unEdifactSpecificationReader.getMessageNames().stream().filter(m -> !m.equals(EDIUtils.MODEL_SET_DEFINITIONS_DESCRIPTION.getName())).collect(Collectors.toList());
        for (String messageType : messageTypes) {
            final Edimap edimap = unEdifactSpecificationReader.getMappingModel(messageType);
            final Map<String, Object> messageDefinition = OBJECT_MAPPER.convertValue(edimap, Map.class);
            final List<Map<String, Object>> segments = (List<Map<String, Object>>) JXPathContext.newContext(messageDefinition).getValue("/segments/segments", List.class);
            templatizeSegments(segments);

            messageDefinitions.add(messageDefinition);
        }
        scope.put("messages", messageDefinitions);
    }

    private void templatizeSegments(final List<Map<String, Object>> segments) {
        templatizeSegments(segments.get(0), tail(segments), true, 0);
    }

    private int templatizeSegments(final Map<String, Object> headSegment, final List<Map<String, Object>> tailSegments, final boolean isFirstSegment, final int segmentGroupCounter) {
        final int segmentGroupIndex;
        final StringWriter stringWriter = new StringWriter();
        final List<Map<String, Object>> nestedSegments = (List<Map<String, Object>>) headSegment.get("segments");
        if (isEmpty(nestedSegments)) {
            segmentGroupIndex = segmentGroupCounter;
            writeSegment(tailSegments, stringWriter, isFirstSegment);
        } else {
            final int nestedSegmentGroupIndex = segmentGroupCounter + 1;
            writeSegmentGroup(headSegment, tailSegments, stringWriter, nestedSegmentGroupIndex);
            segmentGroupIndex = templatizeSegments(nestedSegments.get(0), tail(nestedSegments), true, nestedSegmentGroupIndex);
        }

        if (headSegment.get("segcode").equals("UNS") || headSegment.get("segcode").equals("UGH") || headSegment.get("segcode").equals("UGT")) {
            headSegment.put("namespacePrefix", "srv");
        } else {
            headSegment.put("namespacePrefix", version);
        }
        headSegment.put("render", (TemplateFunction) s -> stringWriter.toString());

        if (!tailSegments.isEmpty()) {
            return templatizeSegments(tailSegments.get(0), tail(tailSegments), false, segmentGroupIndex);
        } else {
            return segmentGroupIndex;
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

    private void writeSegmentGroup(final Map<String, Object> headSegment, final List<Map<String, Object>> tailSegments, final StringWriter stringWriter, final int segmentGroupIndex) {
        headSegment.put("xmltag", "SegGrp-" + segmentGroupIndex);
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

    public List<String> getMessageTypes() {
        return messageTypes;
    }
}
