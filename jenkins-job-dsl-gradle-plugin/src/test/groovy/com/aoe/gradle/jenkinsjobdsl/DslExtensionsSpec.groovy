package com.aoe.gradle.jenkinsjobdsl

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * @author Carsten Lenz, AOE
 */
class DslExtensionsSpec extends Specification {
    //@Rule //todo
    final TemporaryFolder testProjectDir = new TemporaryFolder(new File('build'))

    File buildFile
    File jobsDir

    def setup() {
        testProjectDir.create()
        buildFile = testProjectDir.newFile('build.gradle')
        jobsDir = testProjectDir.newFolder('src', 'jobs')
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
            mavenCentral()
            jcenter()
        }
        
        dependencies {
            jobDslRuntime 'org.jenkins-ci.plugins:structs:1.2@jar'
            jobDslRuntime 'org.jenkins-ci.plugins:cloudbees-folder:5.0@jar'
            jobDslExtension 'org.jenkins-ci.plugins:ghprb:1.31.4'
            jobDslExtension 'com.coravy.hudson.plugins.github:github:1.19.0'
            jobDslExtension 'org.jenkins-ci.plugins:cloudbees-folder:5.0'
        }

        jobDsl {
            sourceDir 'src/jobs'
        }
        
        testDsl {
            doFirst {
              classpath.each {
                println "\${it.path}"
              }
            }
         }
        """.stripIndent()
    }

    def "executing testDsl"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('testDsl')
                .withPluginClasspath()
                .build()

        then:
//        result.output.contains('')
        result.task(':testDsl').outcome == SUCCESS
    }
}
