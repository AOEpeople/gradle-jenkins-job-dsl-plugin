package com.aoe.gradle.jenkinsjobdsl

import org.gradle.testkit.runner.GradleRunner

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Test the Folder Support for Views
 * @author Axel Jung
 */
class ResourcesAvailabilitySpec extends AbstractGradleProjectSpec {

    def setup() {
        def job = new File(jobsDir, 'pipeline.groovy')
        job << """
        // read file from workspace
        job('example-2') {
            steps {
                shell(readFileFromWorkspace('build.sh'))
            }
        }
        """.stripIndent()
        def resource = new File(resourcesDir, 'build.sh')
        resource << """
        echo Hello World
        """.stripIndent()
        buildFile << """
        plugins {
            id 'com.aoe.jenkins-job-dsl'
        }

        repositories {
            mavenLocal()
        }

        dependencies {
            jenkinsPlugin 'org.jenkins-ci.plugins:cloudbees-folder:6.1.2'
        }

        jobDsl {
            sourceDir 'src/jobs'
            resourceDir 'src/resources'
        }
        """.stripIndent()
    }

    def "executing jobDslTest"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('jobDslTest')
                .withPluginClasspath()
                .build()

        then:
        result.task(':jobDslTest').outcome == SUCCESS
    }
}
