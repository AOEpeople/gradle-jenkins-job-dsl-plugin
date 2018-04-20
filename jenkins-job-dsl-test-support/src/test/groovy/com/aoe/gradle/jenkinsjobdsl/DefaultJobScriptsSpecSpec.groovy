package com.aoe.gradle.jenkinsjobdsl

class DefaultJobScriptsSpecSpec extends DefaultJobScriptsSpec {
    @Override
    InputParams getInputParams() {
        return new InputParams(
                sourceDirs: [new File('src/testjobs')],
                resourceDirs: [],
                outputDir: new File('./build/test-debug-xml')
        )
    }
}

