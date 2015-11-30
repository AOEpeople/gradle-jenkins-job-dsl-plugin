package com.aoe.gradle.jenkinsjobdsl

/**
 * @author Carsten Lenz, AOE
 */
class JobDslPluginExtension {

    /**
     * Version of the Job DSL core library to use - should be the same as the version of the
     * Job DSL plugin installed in Jenkins for running the DSL scripts.
     * Default is the version the plugin was built with.
     */
    String version

    /**
     * The source dirs to include in the classpath for running the DSL scripts.
     * Default with JobDslPlugin : 'src/jobs'
     */
    List sourceDirs = []

    void sourceDir(String dir) {
        sourceDirs << dir
    }
}

