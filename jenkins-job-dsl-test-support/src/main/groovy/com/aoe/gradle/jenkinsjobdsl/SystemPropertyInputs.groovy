package com.aoe.gradle.jenkinsjobdsl

/**
 * Builds InputParams from system properties. This is the default
 * parameterization method when running tests from Gradle.
 */
trait SystemPropertyInputs {
    static final String SOURCE_DIR_PROP = System.getProperty('jobSourceDirs', '')
    static final String RESOURCE_DIR_PROP = System.getProperty('jobResourceDirs', '')

    static final String OUTPUT_DIR = System.getProperty('outputDir', './build/debug-xml')

    InputParams getInputParams() {
        new InputParams(
                sourceDirs: toDirs(SOURCE_DIR_PROP),
                resourceDirs: toDirs(RESOURCE_DIR_PROP),
                outputDir: new File(OUTPUT_DIR)
        )
    }

    static List<File> toDirs(String separatedListOfDirs) {
        separatedListOfDirs
                .split(File.pathSeparator)
                .toList()
                .collect { new File(it) }
                .findAll { it.exists() }
    }
}