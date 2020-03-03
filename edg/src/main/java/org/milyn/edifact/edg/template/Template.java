package org.milyn.edifact.edg.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.DecoratedCollection;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class Template {
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final DefaultMustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory() {
        @Override
        public void encode(String value, Writer writer) {
            try {
                writer.append(value);
            } catch (IOException e) {
                throw new MustacheException(e);
            }
        }
    };

    static {
        MUSTACHE_FACTORY.setObjectHandler(new ReflectionObjectHandler() {
            @Override
            public Object coerce(Object object) {
                if (object instanceof Collection) {
                    return new DecoratedCollection((Collection) object);
                }
                return super.coerce(object);
            }
        });
    }

    protected final String version;

    protected Map<String, Object> scope = new HashMap<>();

    public Template(final String version) {
        this.version = version;
        scope.put("version", version);
    }

    public Map<String, Object> getScope() {
        return scope;
    }

    public abstract String getName();

    public String materialise() {
        final Mustache segmentsMustache = MUSTACHE_FACTORY.compile(getName());
        final StringWriter stringWriter = new StringWriter();
        segmentsMustache.execute(stringWriter, getScope());

        return stringWriter.toString();
    }
}
