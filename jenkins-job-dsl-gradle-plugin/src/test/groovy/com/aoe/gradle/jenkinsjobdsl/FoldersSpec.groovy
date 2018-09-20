package com.aoe.gradle.jenkinsjobdsl

import org.gradle.testkit.runner.GradleRunner

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Test the Folder Support for Views
 * @author Axel Jung
 */
class FoldersSpec extends AbstractGradleProjectSpec {

    def setup() {
        def sample = new File(jobsDir, 'views.groovy')
        sample << """

folder("hamsdi")

listView("hamsdi/bamsdi") {
    jobs {
        regex(".*tests.*")
    }
    columns {
        status()
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
        
        dependencies {
            jenkinsPlugin 'org.jenkins-ci.plugins:cloudbees-folder:6.1.2'
        }

        jobDsl {
            sourceDir 'src/jobs'
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
