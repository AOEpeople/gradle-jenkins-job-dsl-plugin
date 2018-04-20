package com.aoe.gradle.jenkinsjobdsl

/**
 * DSL to configure the project for the JobDslPlugin
 *
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
     * The source dirs containing the DSL scripts.
     * Default: []
     */
    List sourceDirs = []

    /**
     * Additional resources that must be available when the DSL scripts execute but
     * are not DSL scripts themselves (e.g. configuration, files for jobs)
     * Default: []
     */
    List resourceDirs = []

    /**
     * Should default repositories be added?
     * Currently jcenter() and jenkins-release repo are added which should
     * be sufficient for resolving all dependencies that are needed for the
     * features of this plugin. Set to false to add own repos or proxies.
     */
    Boolean addRepositories = true

    /**
     * The test class to be executed when running `jobDslTest`
     * Default: 'com.aoe.gradle.jenkinsjobdsl.DefaultJobScriptsSpec'
     */
    String testClass = 'com.aoe.gradle.jenkinsjobdsl.DefaultJobScriptsSpec'

    void version(String version) {
        this.version = version
    }

    void sourceDir(String dir) {
        sourceDirs << dir
    }

    void resourceDir(String dir) {
        resourceDirs << dir
    }

    List<String> getAllDirs() {
        sourceDirs + resourceDirs
    }
}

