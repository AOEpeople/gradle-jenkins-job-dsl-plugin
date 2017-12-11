package com.aoe.gradle.jenkinsjobdsl

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * @author Carsten Lenz, AOE
 */
class DslExtensionsSpec extends AbstractGradleProjectSpec {

    def setup() {
        def sample = new File(jobsDir, 'sample.groovy')
        sample << """

String basePath = 'example8'
String repo = 'sheehan/grails-example'

folder(basePath) {
    description 'This example shows hwp to use DSL extensions provided by other plugins.'
}

job("\$basePath/grails-example-build") {
    scm {
        git {
            remote {
                github repo
                refspec '+refs/pull/*:refs/remotes/origin/pr/*'
            }
            branch '\${sha1}'
        }
    }
    triggers {
        githubPullRequest {
            admin 'sheehan'
            triggerPhrase 'OK to test'
            onlyTriggerPhrase true
        }
    }
    steps {
        grails {
            useWrapper true
            targets(['test-app', 'war'])
        }
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
            jenkinsPlugin 'org.jenkins-ci.plugins:ghprb:1.31.4'
            jenkinsPlugin 'com.coravy.hudson.plugins.github:github:1.19.0'
            jenkinsPlugin 'org.jenkins-ci.plugins:cloudbees-folder:5.0'
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

    def "printing dependencies"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('dependencies')
                .withPluginClasspath()
                .build()

        new File('build/dependencies.txt').text = result.output

        then:
        result.task(':dependencies').outcome == SUCCESS // it really should
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
