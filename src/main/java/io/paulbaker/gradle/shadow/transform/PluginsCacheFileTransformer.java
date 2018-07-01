package io.paulbaker.gradle.shadow.transform;

import com.github.jengelman.gradle.plugins.shadow.relocation.RelocateClassContext;
import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator;
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer;
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext;
import org.apache.logging.log4j.core.config.plugins.processor.PluginCache;
import org.apache.logging.log4j.core.config.plugins.processor.PluginEntry;
import shadow.org.apache.commons.io.IOUtils;
import shadow.org.apache.commons.io.output.ProxyOutputStream;
import shadow.org.apache.tools.zip.ZipEntry;
import shadow.org.apache.tools.zip.ZipOutputStream;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor.PLUGIN_CACHE_FILE;
import static shadow.org.apache.commons.io.output.ClosedOutputStream.CLOSED_OUTPUT_STREAM;

/**
 * Modified from the maven equivalent to work with gradle
 *
 * @author Paul Nelson Baker
 * @see <a href="https://github.com/edwgiz/maven-shaded-log4j-transformer">edwgiz/maven-shaded-log4j-transformer</a>
 * @see <a href="https://github.com/edwgiz/maven-shaded-log4j-transformer/blob/master/src/main/java/com/github/edwgiz/mavenShadePlugin/log4j2CacheTransformer/PluginsCacheFileTransformer.java">PluginsCacheFileTransformer.java</a>
 */
public class PluginsCacheFileTransformer implements Transformer {

    private final List<File> temporaryFiles;
    private final List<Relocator> relocators;

    public PluginsCacheFileTransformer() {
        temporaryFiles = new ArrayList<>();
        relocators = new ArrayList<>();
    }

    @Override
    public boolean canTransformResource(org.gradle.api.file.FileTreeElement element) {
        return nonNull(element) && element.getPath().equals(PLUGIN_CACHE_FILE);
    }

    @Override
    public void transform(TransformerContext context) {
        InputStream inputStream = context.getIs();
        File tempFile = createTemporaryFile();
        temporaryFiles.add(tempFile);
        copyStreamToFile(inputStream, tempFile);
        List<Relocator> relocators = context.getRelocators();
        if (nonNull(relocators)) {
            this.relocators.addAll(relocators);
        }
    }

    private File createTemporaryFile() {
        try {
            File tempFile = File.createTempFile("Log4j2Plugins", "dat");
            tempFile.deleteOnExit();
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void copyStreamToFile(InputStream inputStream, File temporaryFile) {
        try (FileOutputStream outputStream = new FileOutputStream(temporaryFile)) {
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasTransformedResource() {
        return temporaryFiles.size() > 1 || (!temporaryFiles.isEmpty() && !relocators.isEmpty());
    }

    @Override
    public void modifyOutputStream(ZipOutputStream zipOutputStream) {
        try {
            PluginCache pluginCache = new PluginCache();
            Enumeration<URL> urlEnumeration = getUrlEnumeration();
            pluginCache.loadCacheFiles(urlEnumeration);
            relocatePlugins(pluginCache);
            zipOutputStream.putNextEntry(new ZipEntry(PLUGIN_CACHE_FILE));
            pluginCache.writeCache(new CloseShieldOutputStream(zipOutputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            temporaryFiles.clear();
        }
    }

    private void relocatePlugins(PluginCache pluginCache) {
        // Dive into PluginCache and get all the plugin entries
        List<PluginEntry> pluginEntries = pluginCache.getAllCategories().values().stream()
                .flatMap(categoryValues -> categoryValues.values().stream())
                .collect(Collectors.toList());
        // Iterate over all plugin entries
        pluginEntries.forEach(pluginEntry -> {
            String className = pluginEntry.getClassName();
            RelocateClassContext relocateClassContext = new RelocateClassContext(className);
            // If we have a relocator that can relocate our current entry...
            Optional<Relocator> validRelocator = relocators.stream()
                    .filter(relocator -> relocator.canRelocateClass(relocateClassContext))
                    .findFirst();
            // Then we perform that relocation and update the plugin entry to reflect the new value.
            validRelocator.ifPresent(relocator -> {
                String relocatedClass = relocator.relocateClass(relocateClassContext);
                pluginEntry.setClassName(relocatedClass);
            });
        });
    }

    private Enumeration<URL> getUrlEnumeration() {
        Function<File, URL> fileToURL = (File file) -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        };
        List<URL> urls = temporaryFiles.stream()
                .map(fileToURL)
                .collect(Collectors.toList());
        return Collections.enumeration(urls);
    }

    /**
     * @see <a href="https://github.com/edwgiz/maven-shaded-log4j-transformer/blob/master/src/main/java/com/github/edwgiz/mavenShadePlugin/log4j2CacheTransformer/CloseShieldOutputStream.java">CloseShieldOutputStream.java</a>
     */
    private static final class CloseShieldOutputStream extends ProxyOutputStream {

        CloseShieldOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            out.flush();
            out = CLOSED_OUTPUT_STREAM;
        }
    }
}
