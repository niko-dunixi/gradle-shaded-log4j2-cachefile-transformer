package io.paulbaker.gradle.shadow.transform;

import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator;
import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator;
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import shadow.org.apache.tools.zip.ZipOutputStream;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.Collections.singletonList;
import static org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor.PLUGIN_CACHE_FILE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Paul Nelson Baker
 * @see <a href="https://www.linkedin.com/in/paul-n-baker/">LinkedIn</a>
 * @see <a href="https://github.com/paul-nelson-baker/">GitHub</a>
 */
public class Log4j2PluginsCacheFileTransformerTests {

    private final URL PLUGIN_URL = getResourceUrl(PLUGIN_CACHE_FILE);
    private Log4j2PluginsCacheFileTransformer transformer;

    private static URL getResourceUrl(String resource) {
        return Log4j2PluginsCacheFileTransformerTests.class.getClassLoader().getResource(resource);
    }

    private static InputStream getResourceStream(String resource) {
        return Log4j2PluginsCacheFileTransformerTests.class.getClassLoader().getResourceAsStream(resource);
    }

    @BeforeEach
    public void setupPluginCacheFileTransformer() {
        transformer = new Log4j2PluginsCacheFileTransformer();
    }

    @Test
    public void testShouldNotTransform() {
        transformer.transform(new TransformerContext(PLUGIN_CACHE_FILE, getResourceStream(PLUGIN_CACHE_FILE), null));
        assertFalse(transformer.hasTransformedResource());
    }

    @Test
    public void testShouldTransform() {
        List<Relocator> relocators = new ArrayList<>();
        relocators.add(new SimpleRelocator(null, null, null, null));
        transformer.transform(new TransformerContext(PLUGIN_CACHE_FILE, getResourceStream(PLUGIN_CACHE_FILE), relocators));
        assertTrue(transformer.hasTransformedResource());
    }

    @ParameterizedTest
    @CsvSource({
            "org.apache.logging, new.location.org.apache.logging, new.location.org.apache.logging",
            "org.apache.logging, new.location.org.apache.logging, org.apache.logging",
    })
    public void testRelocate(String source, String pattern, String target) throws IOException {
        List<Relocator> relocators = singletonList((Relocator) new SimpleRelocator(source, pattern, null, null));
        transformer.transform(new TransformerContext(PLUGIN_CACHE_FILE, getResourceStream(PLUGIN_CACHE_FILE), relocators));
        assertTrue(transformer.hasTransformedResource(), "Transformer didn't transform resources");
        // Write out to a fake jar file
        File testableZipFile = File.createTempFile("testable-zip-file-", ".jar");
        try (FileOutputStream fileOutputStream = new FileOutputStream(testableZipFile)) {
            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {
                try (ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream)) {
                    transformer.modifyOutputStream(zipOutputStream);
                }
            }
        }
        // Pull the data back out and make sure it was transformed
        ZipFile zipFile = new ZipFile(testableZipFile);
        ZipEntry zipFileEntry = zipFile.getEntry(PLUGIN_CACHE_FILE);
        InputStream inputStream = zipFile.getInputStream(zipFileEntry);
        try (Scanner scanner = new Scanner(inputStream)) {
            boolean hasAtLeastOneTransform = false;
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine();
                if (nextLine.contains(source)) {
                    hasAtLeastOneTransform = true;
                    assertTrue(nextLine.contains(target), "Target wasn't included in transform");
                }
            }
            assertTrue(hasAtLeastOneTransform, "There were no transformations inside the file");
        }
    }
}
