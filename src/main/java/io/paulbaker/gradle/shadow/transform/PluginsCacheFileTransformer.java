package io.paulbaker.gradle.shadow.transform;

import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer;
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext;
import shadow.org.apache.tools.zip.ZipOutputStream;
import shadow.org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;
import static org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor.PLUGIN_CACHE_FILE;

/**
 * Modified from the maven equivalent to work with gradle
 *
 * @author Paul Nelson Baker
 * @see <a href="https://github.com/edwgiz/maven-shaded-log4j-transformer/blob/master/src/main/java/com/github/edwgiz/mavenShadePlugin/log4j2CacheTransformer/PluginsCacheFileTransformer.java">PluginsCacheFileTransformer</a>
 */
public class PluginsCacheFileTransformer implements Transformer {

    private final List<File> temporaryFiles;

    public PluginsCacheFileTransformer() {
        temporaryFiles = new ArrayList<>();
    }

    @Override
    public boolean canTransformResource(org.gradle.api.file.FileTreeElement element) {
        return nonNull(element) && element.getPath().endsWith(PLUGIN_CACHE_FILE);
    }

    @Override
    public void transform(TransformerContext context) {
        File tempFile = File.createTempFile("Log4j2Plugins", "dat");
        IOUtil.copy();
    }

    @Override
    public boolean hasTransformedResource() {
        return false;
    }

    @Override
    public void modifyOutputStream(ZipOutputStream zipOutputStream) {

    }
}
