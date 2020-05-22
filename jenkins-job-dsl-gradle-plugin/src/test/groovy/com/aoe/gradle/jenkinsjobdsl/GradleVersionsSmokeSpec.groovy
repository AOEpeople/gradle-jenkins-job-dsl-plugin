package com.aoe.gradle.jenkinsjobdsl

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class GradleVersionsSmokeSpec extends AbstractGradleProjectSpec {

    def setup() {
        def sample = new File(jobsDir, 'sample.groovy')
        sample << """
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

        """.stripIndent()
    }

    @Unroll
    def "runs with Gradle version #gradleVersion"() {
        when:
        def result = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
                .withArguments('jobDslTest')
                .withPluginClasspath()
                .build()

        then:
        result.task(':jobDslTest').outcome == SUCCESS

        where:
        gradleVersion << ['4.10.3', '5.6.4', '6.0.1', '6.3']
    }
}
