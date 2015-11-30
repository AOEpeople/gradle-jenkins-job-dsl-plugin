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

    @Unroll
    void 'test script #file.name'(File file) {
        given:
        MemoryJobManagement jm = new ExtensionAwareJobManagement()

        def sourceDirsProp = System.getProperty('jobSourceDirs', '')

        def sourceDirs = sourceDirsProp.split(File.pathSeparator).toList()

        sourceDirs.each { String dir ->
            def dirPath = new File(dir)
            if (dirPath.exists()) {
                dirPath.eachFileRecurse(FileType.FILES) {
                    jm.availableFiles << [(it.path): it.text]
                }
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
        new File('src/jobs').eachFileRecurse(FileType.FILES) {
            if (it =~ /.*?\.groovy/) files << it
        }
        files
    }

}

