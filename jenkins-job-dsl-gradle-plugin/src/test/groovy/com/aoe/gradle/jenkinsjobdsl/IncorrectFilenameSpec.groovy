package com.aoe.gradle.jenkinsjobdsl

import org.gradle.testkit.runner.GradleRunner

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * @author Tomas Norre Mikkelsen
 */
class IncorrectFilenameSpec extends AbstractGradleProjectSpec {

    def setup() {
        def sample = new File(jobsDir, '1sample.groovy')
        sample << """

job("1-job-with-digit") {
    
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
            jenkinsPlugin 'org.jenkins-ci.plugins:ghprb:1.31.4'
            jenkinsPlugin 'com.coravy.hudson.plugins.github:github:1.19.0'
            jenkinsPlugin 'org.jenkins-ci.plugins:cloudbees-folder:6.1.2'
        }

        jobDsl {
            sourceDir 'src/jobs'
        }
        
        jobDslTest {
            doFirst {
              classpath.each {
                println "\${it.path}"
              }
            }
         }
        """.stripIndent()
    }

    def "executing jobDslTest"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('jobDslTest')
                .withPluginClasspath()
                .buildAndFail()

        then:
        result.task(':jobDslTest').outcome == FAILED
    }
}
