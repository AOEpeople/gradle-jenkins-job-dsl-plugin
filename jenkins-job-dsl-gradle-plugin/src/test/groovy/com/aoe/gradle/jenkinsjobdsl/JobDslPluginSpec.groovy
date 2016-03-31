package com.aoe.gradle.jenkinsjobdsl

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * @author Carsten Lenz, AOE
 */
class JobDslPluginSpec extends Specification {
    @Rule
    def final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile
    File jobsDir
    List<File> pluginClasspath

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        jobsDir = testProjectDir.newFolder('src', 'jobs')
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
        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
    }

    def "executing testDsl"() {
        given:
        buildFile << """
        plugins {
            id 'com.aoe.jenkins-job-dsl'
        }

        repositories {
            mavenLocal()
            mavenCentral()
            jcenter()
        }

        jobDsl {
            sourceDir 'src/jobs'
        }

        testDsl {
            environment(HAMSDI: 'bamsdi')
        }
        """.stripIndent()

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('testDsl')
                .withPluginClasspath(pluginClasspath)
                .build()

        then:
//        result.output.contains('')
        result.task(':testDsl').outcome == SUCCESS
    }
}
