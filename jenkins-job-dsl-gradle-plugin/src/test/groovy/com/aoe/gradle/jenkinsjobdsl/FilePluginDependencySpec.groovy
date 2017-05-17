package com.aoe.gradle.jenkinsjobdsl

import org.gradle.testkit.runner.GradleRunner

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * @author Carsten Lenz, AOE
 */
class FilePluginDependencySpec extends AbstractGradleProjectSpec {

    File pluginsDir

    def setup() {
        pluginsDir = testProjectDir.newFolder('plugins')
        new File(pluginsDir, 'test.hpi').text = ""
        buildFile << """
        plugins {
            id 'com.aoe.jenkins-job-dsl'
        }
        
        dependencies {
            jenkinsPlugin 'org.jenkins-ci.plugins:nested-view:1.14'
            jenkinsPlugin files('plugins/test.hpi')
        }

        repositories {
            mavenLocal()
            mavenCentral()
            jcenter()
        }

        jobDsl {
            sourceDir 'src/jobs'
        }

        """.stripIndent()
    }

    def "resolve file plugin dependencies"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('resolveJenkinsPlugins')
                .withPluginClasspath()
                .build()

        def depFolder = new File(testProjectDir.root, 'build/resolveJenkinsPlugins/test-dependencies')
        def files = depFolder.list().toList()

        then:
        result.task(':resolveJenkinsPlugins').outcome == SUCCESS
        files.size() == 3 // 2 hpi 1 index
        "test.hpi" in files
        "nested-view.hpi" in files
        "index" in files
    }
}
