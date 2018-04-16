package com.aoe.gradle.jenkinsjobdsl

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author Carsten Lenz, AOE
 */
class JobScriptsSpecTest extends Specification {

    @Unroll
    void 'test isValidScriptName'() {
        given:
        def exceptionThrown = false

        when:
        try {
            JobScriptsSpec.assertValidFilename(filename)
        } catch (AssertionError e) {
            exceptionThrown = true
        }

        then:
        shouldThrowException == exceptionThrown

        where:
        filename                                         | shouldThrowException
        new File('/path/to/1digit-start.groovy')         | true
        new File('/path/to/with-hyphen.groovy')          | true
        new File('acceptedNameWithoutPath.groovy')       | false
        new File('/path/to/acceptedNameWithPath.groovy') | false
    }

}
