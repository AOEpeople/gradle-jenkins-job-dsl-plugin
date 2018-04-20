package com.aoe.gradle.jenkinsjobdsl

import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.JobManagement
import spock.lang.Unroll

/**
 * Tests that all dsl scripts in the jobs directory will compile.
 */
class DefaultJobScriptsSpec extends AbstractJobDslSpec implements SystemPropertyInputs {

    @Unroll
    void 'test DSL script #file.name'(File file) {
        given:
        JobManagement jm = createJobManagement()

        expect:
        assertValidFilename(file)

        when:
        GeneratedItems items = runScript(jm, file)
        writeItems items

        then:
        noExceptionThrown()

        where:
        file << jobFiles
    }
}

