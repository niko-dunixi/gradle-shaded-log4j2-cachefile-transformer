package io.paulbaker.gradle.shadow.transform;

import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator;
import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator;
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor.PLUGIN_CACHE_FILE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Paul Nelson Baker
 * @see <a href="https://www.linkedin.com/in/paul-n-baker/">LinkedIn</a>
 * @see <a href="https://github.com/paul-nelson-baker/">GitHub</a>
 */
public class PluginsCacheFileTransformerTests {

    private final URL PLUGIN_URL = getResourceUrl(PLUGIN_CACHE_FILE);

    private static URL getResourceUrl(String resource) {
        return PluginsCacheFileTransformerTests.class.getClassLoader().getResource(resource);
    }

    private static InputStream getResourceStream(String resource) {
        return PluginsCacheFileTransformerTests.class.getClassLoader().getResourceAsStream(resource);
    }

    @Test
    public void test() {
        PluginsCacheFileTransformer transformer = new PluginsCacheFileTransformer();
        transformer.transform(new TransformerContext(PLUGIN_CACHE_FILE, getResourceStream(PLUGIN_CACHE_FILE), null));
        assertFalse(transformer.hasTransformedResource());

        List<Relocator> relocators = new ArrayList<>();
        relocators.add(new SimpleRelocator(null, null, null, null));
        transformer.transform(new TransformerContext(PLUGIN_CACHE_FILE, getResourceStream(PLUGIN_CACHE_FILE), relocators));
        assertTrue(transformer.hasTransformedResource());
    }
}
