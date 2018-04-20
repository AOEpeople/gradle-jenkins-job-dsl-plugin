package com.aoe.gradle.jenkinsjobdsl

import groovy.transform.Canonical

/**
 * The inputs for parameterizing the job dsl test.
 */
@Canonical
class InputParams {
    List<File> sourceDirs
    List<File> resourceDirs
    File outputDir
}