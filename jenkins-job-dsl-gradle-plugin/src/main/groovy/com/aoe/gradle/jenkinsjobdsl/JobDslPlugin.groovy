package com.aoe.gradle.jenkinsjobdsl

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.testing.Test
import org.gradle.util.VersionNumber

/**
 * Gradle Plugin that sets up a project for authoring
 * and testing Jenkins Job DSL scripts.
 *
 * @author Carsten Lenz, AOE
 */
class JobDslPlugin implements Plugin<Project> {

    File jobDslTestsDir

    void apply(Project project) {
        jobDslTestsDir = project.file("${project.buildDir}/jobDslTests")

        project.apply plugin: 'groovy'
        project.apply plugin: 'nebula.provided-base'

        def extension = project.extensions.create('jobDsl', JobDslPluginExtension)
        extension.version Versions.jobDsl

        configureDependencies(project)

        addJobSourceSet(project)

        addTestDslTask(project)

        addDependenciesManifestationTasks(project)
    }

    void configureDependencies(Project project) {

        project.configurations {
            jenkinsPlugin
            jobDslTestSupport
            jobDslTestRuntime.extendsFrom(jobDslTestSupport)
        }

        project.dependencies {
            compileOnly "org.codehaus.groovy:groovy-all:${Versions.groovy}"
            jobDslTestSupport "com.aoe.gradle:jenkins-job-dsl-test-support:${Versions.pluginVersion}"
        }

        project.afterEvaluate { proj ->
            def extension = proj.extensions.getByType(JobDslPluginExtension)

            proj.dependencies {
                provided "org.jenkins-ci.plugins:job-dsl-core:${extension.version}"

                // Sadly because of the .hpi or .jpi pom packages we have to redundantly define the correct job-dsl deps
                jobDslTestRuntime "org.jenkins-ci.plugins:job-dsl:${extension.version}@jar"
                jobDslTestRuntime "org.jenkins-ci.plugins:job-dsl:${extension.version}"
                jobDslTestRuntime 'org.jenkins-ci.plugins:structs:1.6@jar'
                jobDslTestRuntime 'org.jenkins-ci.plugins:cloudbees-folder:6.0.4@jar'
            }

            if (extension.addRepositories) {
                proj.repositories {
                    maven { url 'http://repo.jenkins-ci.org/public' }
                    jcenter()
                }
            }
        }
    }

    def addJobSourceSet(Project project) {
        project.sourceSets {
            jobs {
                groovy {
                    compileClasspath += main.compileClasspath
                }
                compileClasspath += project.sourceSets.main.output
                runtimeClasspath += project.sourceSets.main.output
            }
        }

        project.afterEvaluate { proj ->
            def extension = proj.extensions.getByType(JobDslPluginExtension)
            proj.sourceSets {
                jobs {
                    groovy {
                        for (String dir : extension.sourceDirs) {
                            srcDir dir
                        }
                    }
                    resources {
                        for (String dir : extension.resourceDirs) {
                            srcDir dir
                        }
                    }
                }
            }
        }
    }

    void addTestDslTask(Project project) {
        Task resolveJenkinsPlugins = project.task('resolveJenkinsPlugins', type: Copy) {
            from project.configurations.jenkinsPlugin
            into project.file("${project.buildDir}/resolveJenkinsPlugins/test-dependencies")
            include '*.hpi'
            include '*.jpi'
            def mapping = [:]

            doFirst {
                project.configurations.jenkinsPlugin.resolvedConfiguration.resolvedArtifacts.each {
                    mapping[it.file.name] = "${it.name}.${it.extension}"
                }
            }
            rename { mapping[it] }

            doLast {
                def mappedName = source*.name.collect { sourceFileName ->
                    // direct file dependencies don't have mapping
                    mapping[sourceFileName] ?: sourceFileName
                }
                List<String> baseNames = mappedName.collect { it[0..it.lastIndexOf('.') - 1] }
                new File(destinationDir, 'index').setText(baseNames.join('\n'), 'UTF-8')
            }
        }

        Task unpackDslTests = project.task('unpackDslTests') {
            doLast {
                def resolvedDependencies = project.configurations.jobDslTestSupport.resolvedConfiguration.firstLevelModuleDependencies
                def jarFiles = []
                resolvedDependencies.each { ResolvedDependency dep ->
                    dep.moduleArtifacts.each { ResolvedArtifact artifact ->
                        jarFiles << artifact.file
                    }
                }

                assert jarFiles.size() == 1, "The configuration 'jobDslTestSupport' is expected to have " +
                        "exactly one artifact but has ${jarFiles.size()}. You should not modify this " +
                        "configuration. Please file a bug if you think this is an error."

                project.copy {
                    from project.zipTree(jarFiles[0])
                    into jobDslTestsDir
                }
            }
        }

        Task jobDslTest = project.task('jobDslTest', type: Test, dependsOn: [unpackDslTests, resolveJenkinsPlugins]) {
            description = 'Executes all Job DSL scripts to test for errors'
            group = 'Verification'

            classpath = project.sourceSets.main.runtimeClasspath +
                    project.configurations.jobDslTestRuntime +
                    project.files("${project.buildDir}/resolveJenkinsPlugins")
            if (isGradleFiveOrGreater(project)) {
                testClassesDirs = project.files(jobDslTestsDir)
            }
            else {
                testClassesDir = project.file(jobDslTestsDir)
            }
        }

        project.afterEvaluate { proj ->
            def extension = proj.extensions.getByType(JobDslPluginExtension)
            proj.configure(jobDslTest) {
                for (String dir in extension.allDirs) {
                    inputs.dir dir
                }
                systemProperties([
                        jobSourceDirs: extension.sourceDirs.join(File.pathSeparator),
                        jobResourceDirs: extension.resourceDirs.join(File.pathSeparator),
                        // set build directory for Jenkins test harness, JENKINS-26331
                        buildDirectory: proj.buildDir.absolutePath,
                        'jenkins.test.noSpaceInTmpDirs': 'true'
                ])
            }
        }

        project.tasks['check'].dependsOn jobDslTest
    }

    void addDependenciesManifestationTasks(Project project) {
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

    boolean isGradleFiveOrGreater(Project project) {
        println(project.gradle.gradleVersion)
        VersionNumber.parse(project.gradle.gradleVersion) >= VersionNumber.parse("5.0.0")
    }
}

