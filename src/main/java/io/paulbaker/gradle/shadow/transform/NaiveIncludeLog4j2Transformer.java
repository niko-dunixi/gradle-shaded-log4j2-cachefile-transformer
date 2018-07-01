package io.paulbaker.gradle.shadow.transform;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.Set;

/**
 * @author Paul Nelson Baker
 * @see <a href="https://www.linkedin.com/in/paul-n-baker/">LinkedIn</a>
 * @see <a href="https://github.com/paul-nelson-baker/">GitHub</a>
 */
public class NaiveIncludeLog4j2Transformer implements Plugin<Project> {

    @Override
    public void apply(Project target) {
        Set<Task> shadowJarTasks = target.getTasksByName("shadowJar", true);
        for (Task currentTask : shadowJarTasks) {
            currentTask.setProperty("transform", PluginsCacheFileTransformer.class);
        }
    }
}
