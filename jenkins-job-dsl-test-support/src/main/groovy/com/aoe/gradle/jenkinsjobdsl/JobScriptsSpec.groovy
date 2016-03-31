package com.aoe.gradle.jenkinsjobdsl

import groovy.io.FileType
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.MemoryJobManagement
import spock.lang.Specification
import spock.lang.Unroll


/**
 * Tests that all dsl scripts in the jobs directory will compile.
 */
class JobScriptsSpec extends Specification {

    static final String SOURCE_DIR_PROP = System.getProperty('jobSourceDirs', '')
    static final String RESOURCE_DIR_PROP = System.getProperty('jobResourceDirs', '')

    static final List<File> SOURCE_DIRS = toDirs(SOURCE_DIR_PROP)
    static final List<File> RESOURCE_DIRS = toDirs(RESOURCE_DIR_PROP)

    def allDirs = (SOURCE_DIRS + RESOURCE_DIRS)

    static List<File> toDirs(String separatedListOfDirs) {
        separatedListOfDirs
                .split(File.pathSeparator)
                .toList()
                .collect { new File(it) }
                .findAll { it.exists() }
    }

    @Unroll
    void 'test script #file.name'(File file) {
        given:
        MemoryJobManagement jm = new ExtensionAwareJobManagement()
        jm.parameters << System.getenv() // this mimics the behaviour of the plugin in jenkins

        allDirs.each { File dir ->
            dir.eachFileRecurse(FileType.FILES) {
                jm.availableFiles << [(it.path): it.text]
            }
        }

        when:
        def generatedItems = DslScriptLoader.runDslEngine file.text, jm
        generatedItems.configFiles.each { println "Generated config file ${it.name}" }
        generatedItems.jobs.each { println "Generated job ${it.jobName}" }
        generatedItems.views.each { println "Generated view ${it.name}" }

        then:
        noExceptionThrown()

        where:
        file << jobFiles
    }

    static List<File> getJobFiles() {
        List<File> files = []
        SOURCE_DIRS.each {
            it.eachFileRecurse(FileType.FILES) {
                if (it =~ /.*?\.groovy/) files << it
            }
        }
        files
    }

}

