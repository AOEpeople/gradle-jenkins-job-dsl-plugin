package com.aoe.gradle.jenkinsjobdsl

import org.gradle.testkit.runner.GradleRunner
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * @author Carsten Lenz, AOE
 */
class AbstractGradleProjectSpec extends Specification {

    final TemporaryFolder testProjectDir = new TemporaryFolder(new File('build'))

    File buildFile
    File jobsDir

    def setup() {
        testProjectDir.create()
        buildFile = testProjectDir.newFile('build.gradle')
        jobsDir = testProjectDir.newFolder('src', 'jobs')
    }

    def cleanup() {

    }

    GradleRunner createGradleRunner() {
        GradleRunner.create()
                .withGradleVersion('4.0')
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
    }
}
