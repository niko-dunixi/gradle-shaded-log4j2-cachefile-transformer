package io.paulbaker.gradle.shadow.transform;

import com.github.jengelman.gradle.plugins.shadow.ShadowStats;
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

import static org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor.PLUGIN_CACHE_FILE;
import static shadow.org.apache.commons.io.output.ClosedOutputStream.CLOSED_OUTPUT_STREAM;

/**
 * Modified from the maven equivalent to work with gradle
 *
 * @author Paul Nelson Baker
 * @see <a href="https://www.linkedin.com/in/paul-n-baker/">LinkedIn</a>
 * @see <a href="https://github.com/paul-nelson-baker/">GitHub</a>
 * @see <a href="https://github.com/edwgiz/maven-shaded-log4j-transformer">edwgiz/maven-shaded-log4j-transformer</a>
 * @see <a href="https://github.com/edwgiz/maven-shaded-log4j-transformer/blob/master/src/main/java/com/github/edwgiz/mavenShadePlugin/log4j2CacheTransformer/PluginsCacheFileTransformer.java">PluginsCacheFileTransformer.java</a>
 */
public class Log4j2PluginsCacheFileTransformer implements Transformer {

    private final List<File> temporaryFiles;
    private final List<Relocator> relocators;

    public Log4j2PluginsCacheFileTransformer() {
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
        // This functionality matches the original plugin, however, I'm not clear what
        // the exact logic is. From what I can tell temporaryFiles should be never be empty
        // if anything has been performed.
        boolean transformedMultipleFiles = temporaryFiles.size() > 1;
        boolean hasMultipleFilesAndRelocators = !temporaryFiles.isEmpty() && !relocators.isEmpty();
        return transformedMultipleFiles || hasMultipleFilesAndRelocators;
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
        for (Map<String, PluginEntry> currentValue : pluginCache.getAllCategories().values()) {
            pluginEntryLoop:
            for (PluginEntry currentPluginEntry : currentValue.values()) {
                String className = currentPluginEntry.getClassName();
                RelocateClassContext relocateClassContext = new RelocateClassContext(className, new ShadowStats());
                for (Relocator currentRelocator : relocators) {
                    // If we have a relocator that can relocate our current entry...
                    boolean canRelocateClass = currentRelocator.canRelocateClass(relocateClassContext);
                    if (canRelocateClass) {
                        // Then we perform that relocation and update the plugin entry to reflect the new value.
                        String relocatedClassName = currentRelocator.relocateClass(relocateClassContext);
                        currentPluginEntry.setClassName(relocatedClassName);
                        continue pluginEntryLoop;
                    }
                }
            }
        }
    }

    private Enumeration<URL> getUrlEnumeration() {
        List<URL> urls = new ArrayList<>();
        for (File currentTemporaryFile : temporaryFiles) {
            URL url = fromFile(currentTemporaryFile);
            urls.add(url);
        }
        return Collections.enumeration(urls);
    }

    private URL fromFile(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean nonNull(Object object) {
        return object != null;
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
