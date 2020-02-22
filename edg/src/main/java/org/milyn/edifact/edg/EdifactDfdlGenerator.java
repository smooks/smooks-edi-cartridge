package org.milyn.edifact.edg;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.apache.commons.lang.WordUtils;
import org.milyn.edifact.ect.formats.unedifact.UnEdifactSpecificationReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.zip.ZipInputStream;

public class EdifactDfdlGenerator {

    public void xx() throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/d03b.zip");
        UnEdifactSpecificationReader unEdifactSpecificationReader = new UnEdifactSpecificationReader(new ZipInputStream(resourceAsStream), true, true);

        Mustache mustache = new DefaultMustacheFactory().compile("EDIFACT-SupplyChain-D03B/EDIFACT-SupplyChain-Segments-D.03B.xsd.mustache");
        StringWriter stringWriter = new StringWriter();
        unEdifactSpecificationReader.getDefinitionModel().getSegments().getSegments().stream().forEach(s -> s.setName(WordUtils.capitalizeFully(s.getName()).replace(" ", "")));
        mustache.execute(stringWriter, new HashMap() {{
            this.put("segments", unEdifactSpecificationReader.getDefinitionModel().getSegments().getSegments());
        }});
        stringWriter = stringWriter;
    }

}
