package com.aoe.gradle.jenkinsjobdsl

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * @author Carsten Lenz, AOE
 */
class EnvironmentPassingSpec extends AbstractGradleProjectSpec {

    def setup() {
        def sample = new File(jobsDir, 'sample.groovy')
        sample << """

// will fail, if environment params do not get passed through to DSL execution
assert HAMSDI == 'bamsdi'

job("simple-job") {
    description "Job for testing"

    steps {
        shell 'echo hello world'
    }
}
"""
        buildFile << """
        plugins {
            id 'com.aoe.jenkins-job-dsl'
        }

        repositories {
            mavenLocal()
        }

        jobDsl {
            sourceDir 'src/jobs'
        }

        jobDslTest.environment(HAMSDI: 'bamsdi')

        """.stripIndent()
    }

    def "executing jobDslTest"() {
        when:
        def result = createGradleRunner()
                .withArguments('jobDslTest')
                .build()

        then:
        result.task(':jobDslTest').outcome == SUCCESS
    }

}
