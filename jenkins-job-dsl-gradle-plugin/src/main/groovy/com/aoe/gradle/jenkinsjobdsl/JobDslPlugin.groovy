package com.aoe.gradle.jenkinsjobdsl

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.testing.Test

/**
 * @author Carsten Lenz, AOE
 */
class JobDslPlugin implements Plugin<Project> {

    void apply(Project project) {

        project.apply plugin: 'groovy'
        project.apply plugin: 'nebula.provided-base'

        def extension = project.extensions.create('jobDsl', JobDslPluginExtension)
        extension.sourceDir 'src/jobs'
        extension.version Versions.jobDsl()

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
            //TODO: Must this be the Jenkins Groovy version?
            provided 'org.codehaus.groovy:groovy-all:2.4.3'
        }

        project.afterEvaluate { proj ->
            proj.dependencies {
                def extension = proj.extensions.getByType(JobDslPluginExtension)
                provided "org.jenkins-ci.plugins:job-dsl-core:${extension.version}"
                jobDslTest "com.aoe.gradle:jenkins-job-dsl-test-support:${Versions.pluginVersion()}"

                // This is a hack because Gradle ignores the <type>jar</type> in the pom.xml of our test-support
                jobDslRuntime "org.jenkins-ci.plugins:job-dsl:${extension.version}@jar"
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
                for (String sourceDir in extension.sourceDirs) {
                    inputs.dir sourceDir
                }
                systemProperties([jobSourceDirs: extension.sourceDirs.join(File.pathSeparator)])
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
                proj.copy {
                    from("${project.projectDir}") {
                        for (String sourceDir in extension.sourceDirs) {
                            include "${sourceDir}/**"
                        }
                    }
                    into project.workspaceDir
                }
            }
        }

        project.task('run', type: JavaExec, dependsOn: workspace) {
            description = 'Run a specific DSL script given by -PjobFile=src/jobs/... and output generated config XMLs'
            group = 'Execution'

            classpath = project.sourceSets.main.runtimeClasspath +
                    project.configurations.jobDslRuntime

            main = 'com.aoe.fraport.jenkins.Runner'
            workingDir = project.workspaceDir

            args = [prop(project, 'jobFile')]

            doFirst {
                if (!prop(project, 'jobFile')) {
                    project.logger.error 'Please provide the project parameter "jobFile" like "gradle run -PjobFile=src/jobs/bla.groovy"'
                    throw new StopExecutionException()
                }
            }
        }

        project.task('runAll', type: JavaExec, dependsOn: workspace) {
            description = 'Run all DSL scripts and output generated config XMLs'
            group = 'Execution'

            classpath = project.sourceSets.main.runtimeClasspath +
                    project.configurations.jobDslRuntime

            main = 'com.aoe.fraport.jenkins.Runner'
            workingDir = project.workspaceDir

            def allJobFiles = project.fileTree('src/jobs') {
                include '**/*.groovy'
            }
            args = allJobFiles.files*.path.toList()
        }
    }

    public void addDependenciesManifestationTasks(Project project) {
        Task libs = project.task('libs', type: Copy) {
            description = 'Copies all compile dependencies into a local folder (\'lib\' by default)'
            from project.configurations.compile
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

