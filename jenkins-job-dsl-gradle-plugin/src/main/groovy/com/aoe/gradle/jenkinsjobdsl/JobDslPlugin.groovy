package com.aoe.gradle.jenkinsjobdsl

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.testing.Test

/**
 * Gradle Plugin that sets up a project for authoring
 * and testing Jenkins Job DSL scripts.
 *
 * @author Carsten Lenz, AOE
 */
class JobDslPlugin implements Plugin<Project> {

    void apply(Project project) {

        project.apply plugin: 'groovy'
        project.apply plugin: 'nebula.provided-base'

        def extension = project.extensions.create('jobDsl', JobDslPluginExtension)
        extension.version Versions.jobDsl

        configureDependencies(project)

        addTestDslTask(project)

        addRunJobConfigurationTasks(project)

        addDependenciesManifestationTasks(project)
    }

    public void configureDependencies(Project project) {

        project.configurations {
            jobDslExtension
            jobDslTest
            jobDslRuntime.extendsFrom(jobDslExtension, jobDslTest)
        }

        project.dependencies {
            provided "org.codehaus.groovy:groovy-all:${Versions.groovy}"
            jobDslTest "com.aoe.gradle:jenkins-job-dsl-test-support:${Versions.pluginVersion}"
        }

        project.afterEvaluate { proj ->
            def extension = proj.extensions.getByType(JobDslPluginExtension)

            proj.dependencies {
                provided "org.jenkins-ci.plugins:job-dsl-core:${extension.version}"

                // This is a hack because Gradle ignores the <type>jar</type> in the pom.xml of our test-support
                jobDslRuntime "org.jenkins-ci.plugins:job-dsl:${extension.version}@jar"
            }
            if (extension.addRepositories) {
                proj.repositories {
                    jcenter()
                    maven { url 'http://repo.jenkins-ci.org/releases/' }
                }
            }
        }

    }

    void addTestDslTask(Project project) {
        def jobDslTestsDir = "${project.buildDir}/tmp/jobDslTests"

        Task unpackDslTests = project.task('unpackDslTests') {
            doLast {
                def resolvedDependencies = project.configurations.jobDslTest.resolvedConfiguration.firstLevelModuleDependencies
                def jarFiles = []
                resolvedDependencies.each { it.moduleArtifacts.each { jarFiles << it.file } }
                assert jarFiles.size() == 1
                project.copy {
                    from project.zipTree(jarFiles[0])
                    into jobDslTestsDir
                }
            }
        }

        Task testDsl = project.task('testDsl', type: Test, dependsOn: unpackDslTests) {
            description = 'Executes all Job DSL scripts to test for errors'
            group = 'Verification'

            classpath = project.sourceSets.main.runtimeClasspath +
                    project.configurations.jobDslRuntime

            testClassesDir = project.file(jobDslTestsDir)
        }
        project.afterEvaluate { proj ->
            def extension = proj.extensions.getByType(JobDslPluginExtension)
            proj.configure(testDsl) {
                for (String dir in extension.allDirs) {
                    inputs.dir dir
                }
                systemProperties([
                        jobSourceDirs: extension.sourceDirs.join(File.pathSeparator),
                        jobResourceDirs: extension.resourceDirs.join(File.pathSeparator)
                ])
            }
        }

        project.tasks['check'].dependsOn testDsl
    }

    void addRunJobConfigurationTasks(Project project) {
        project.ext.workspaceDir = project.file('build/workspace')

        Task workspace = project.task('workspace')
        workspace.description = 'Prepare a workspace directory for local Job DSL execution'

        project.afterEvaluate { proj ->
            def extension = project.extensions.getByType(JobDslPluginExtension)
            proj.configure(workspace) {
                doLast {
                    proj.workspaceDir.mkdirs()
                    proj.copy {
                        from("${project.projectDir}") {
                            for (String sourceDir in extension.allDirs) {
                                include "${sourceDir}/**"
                            }
                        }
                        into project.workspaceDir
                    }
                }
            }
        }

        project.task('run', type: JavaExec, dependsOn: workspace) {
            description = 'Run a specific DSL script given by -PjobFile=src/jobs/... and output generated config XMLs'
            group = 'Execution'

            classpath = project.sourceSets.main.runtimeClasspath +
                    project.configurations.jobDslRuntime

            main = 'com.aoe.gradle.jenkinsjobdsl.Runner'
            workingDir = project.workspaceDir

            args = [prop(project, 'jobFile')]

            doFirst {
                if (!prop(project, 'jobFile')) {
                    project.logger.error 'Please provide the project parameter "jobFile" like "gradle run -PjobFile=src/jobs/bla.groovy"'
                    throw new StopExecutionException()
                }
            }
        }

        Task runAll = project.task('runAll', type: JavaExec, dependsOn: workspace) {
            description = 'Run all DSL scripts and output generated config XMLs'
            group = 'Execution'

            classpath = project.sourceSets.main.runtimeClasspath +
                    project.configurations.jobDslRuntime

            main = 'com.aoe.gradle.jenkinsjobdsl.Runner'
            workingDir = project.workspaceDir
        }

        project.afterEvaluate { proj ->
            proj.configure(runAll) {
                doFirst {
                    def extension = project.extensions.getByType(JobDslPluginExtension)
                    def includeGroovyFiles = project.fileTree(project.workspaceDir) {
                        include '**/*.groovy'
                    }
                    def allJobFiles = extension.sourceDirs.collectMany {
                        includeGroovyFiles.from("${project.workspaceDir}/${it}").files.toList()
                    }
                    args = allJobFiles.collect { it.toURI().toURL() }
                }
            }
        }
    }

    public void addDependenciesManifestationTasks(Project project) {
        Task libs = project.task('libs', type: Copy) {
            description = 'Copies all compile dependencies into a local folder (\'lib\' by default)'
            from(project.configurations.compile - project.configurations.provided)
            into 'lib'
        }

        project.tasks['build'].dependsOn libs

        project.configure(project.tasks['clean']) {
            delete 'lib'
        }
    }

    String prop(Project project, String property, String defaultValue = '') {
        project.hasProperty(property) ? project.getProperty(property) : defaultValue
    }
}

