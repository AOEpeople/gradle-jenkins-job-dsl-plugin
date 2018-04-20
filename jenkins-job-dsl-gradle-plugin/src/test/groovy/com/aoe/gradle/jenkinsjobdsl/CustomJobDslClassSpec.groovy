package com.aoe.gradle.jenkinsjobdsl

import org.gradle.testkit.runner.GradleRunner

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * @author Carsten Lenz, AOE
 */
class CustomJobDslClassSpec extends AbstractGradleProjectSpec {

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
            testClass 'com.testing.CustomSpec'
        }
        
        jobDslTest {
            doFirst {
              classpath.each {
                println "\${it.path}"
              }
            }
         }
        """.stripIndent()


        def customSpecFolder = testProjectDir.newFolder('src', 'jobDslTest', 'groovy', 'com', 'testing')
        def customSpec = new File(customSpecFolder, 'CustomSpec.groovy')
        customSpec << """
        package com.testing
        
        import com.aoe.gradle.jenkinsjobdsl.*
        
        class CustomSpec extends AbstractJobDslSpec implements SystemPropertyInputs {
            def "all is good"() {
                when:
                def nothing = ""
                then:
                noExceptionThrown()
            }
        }
        """.stripIndent()
    }

    def "executing jobDslTest"() {
        when:
        def result = createGradleRunner()
                .withArguments('jobDslTest', '--stacktrace')
                .build()

        then:
        result.task(':jobDslTest').outcome == SUCCESS
    }
}
