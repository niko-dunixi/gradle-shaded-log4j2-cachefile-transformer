package io.paulbaker.gradle.shadow.transform;

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Paul Nelson Baker
 * @see <a href="https://www.linkedin.com/in/paul-n-baker/">LinkedIn</a>
 * @see <a href="https://github.com/paul-nelson-baker/">GitHub</a>
 */
public class NaiveIncludeLog4j2TransformerPlugin implements Plugin<Project> {

    private final Logger log;

    public NaiveIncludeLog4j2TransformerPlugin() {
        log = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public void apply(Project project) {
        log.info("Looking at project: " + project.getName());
        Set<ShadowJar> shadowJarTasks = getShadowJarTasks(project);
        for (ShadowJar currentTask : shadowJarTasks) {
            List<Transformer> transformers = currentTask.getTransformers();
            boolean hasLog4j2Transformer = hasLog4j2Transformer(transformers);
            if (!hasLog4j2Transformer) {
                log.info("Appending new Lo4j2PluginsCacheFileTransformer to shadowJar task");
                transformers.add(new Log4j2PluginsCacheFileTransformer());
            }
        }
    }

    /**
     * @param project the target project to retrieve from
     * @return a set of all ShadowJar tasks, includes tasks from sub-projects.
     */
    private Set<ShadowJar> getShadowJarTasks(Project project) {
        Set<Task> untypedTasks = project.getTasksByName("shadowJar", true);
        Set<ShadowJar> shadowJarTasks = new LinkedHashSet<>();
        for (Task currentTask : untypedTasks) {
            if (currentTask instanceof ShadowJar) {
                shadowJarTasks.add((ShadowJar) currentTask);
            }
        }
        log.info(String.format("Found [%d] shadowJar tasks", shadowJarTasks.size()));
        return shadowJarTasks;
    }

    /**
     * @param transformers the transformers to check
     * @return will return true if there is an instance of Log4j2PluginsCacheFileTransformer within the list.
     */
    private boolean hasLog4j2Transformer(List<Transformer> transformers) {
        for (Transformer currentTransformer : transformers) {
            if (currentTransformer instanceof Log4j2PluginsCacheFileTransformer) {
                return true;
            }
        }
        return false;
    }
}
