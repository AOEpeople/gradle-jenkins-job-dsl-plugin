package com.aoe.gradle.jenkinsjobdsl

import groovy.io.FileType
import hudson.model.Item
import hudson.model.View
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.GeneratedJob
import javaposse.jobdsl.dsl.GeneratedView
import javaposse.jobdsl.plugin.JenkinsJobManagement
import jenkins.model.Jenkins
import org.apache.commons.io.FilenameUtils
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification

abstract class AbstractJobDslSpec extends Specification {

    @Shared
    @ClassRule
    protected JenkinsRule jenkinsRule = new JenkinsRule()

    @Shared
    protected List<File> sourceDirs

    @Shared
    protected List<File> resourceDirs

    @Shared
    protected File outputDir

    static GeneratedItems runScript(JenkinsJobManagement jm, File file) {
        new DslScriptLoader(jm).runScript(file.text)
    }

    static JenkinsJobManagement createJobManagement(Map<String, ?> envVars = System.getenv()) {
        new JenkinsJobManagement(System.out, [*: envVars], new File('.'))
    }

    /**
     * Check if script name is valid according to Java conventions.
     *
     * @param scriptFile the script file to check
     * @throws AssertionError when filename is invalid
     */
    static void assertValidFilename(File scriptFile) {
        boolean result = true
        String normalizedName = FilenameUtils.removeExtension(scriptFile.getName())
        if (normalizedName.length() == 0 || !Character.isJavaIdentifierStart(normalizedName.charAt(0))) {
            result = false
        }
        for (int i = 1; i < normalizedName.length(); i += 1) {
            if (!Character.isJavaIdentifierPart(normalizedName.charAt(i))) {
                result = false
            }
        }

        assert result, "invalid script name '${scriptFile}; script names may only contain " +
                'letters, digits and underscores, but may not start with a digit'
    }

    def setupSpec() {
        InputParams inputParams = getInputParams()

        sourceDirs = inputParams.sourceDirs
        resourceDirs = inputParams.resourceDirs
        outputDir = inputParams.outputDir

        outputDir.deleteDir()
    }

    abstract InputParams getInputParams()

    List<File> getJobFiles() {
        List<File> files = []
        sourceDirs.each {
            it.eachFileRecurse(FileType.FILES) {
                if (it =~ /.*?\.groovy/) files << it
            }
        }
        files
    }

    /**
     * Write the config.xml for each generated job and view to the build dir.
     */
    void writeItems(GeneratedItems items) {
        Jenkins jenkins = jenkinsRule.jenkins

        items.jobs.each { GeneratedJob generatedJob ->
            String jobName = generatedJob.jobName
            Item item = jenkins.getItemByFullName(jobName)
            String text = new URL(jenkins.rootUrl + item.url + 'config.xml').text
            writeFile new File(outputDir, 'jobs'), jobName, text
        }

        items.views.each { GeneratedView generatedView ->
            String viewName = generatedView.name
            View view = jenkins.getView(viewName)
            String text = new URL(jenkins.rootUrl + view.url + 'config.xml').text
            writeFile new File(outputDir, 'views'), viewName, text
        }
    }

    /**
     * Write a single XML file, creating any nested dirs.
     */
    void writeFile(File dir, String name, String xml) {
        List tokens = name.split('/')
        File folderDir = tokens[0..<-1].inject(dir) { File tokenDir, String token ->
            new File(tokenDir, token)
        }
        folderDir.mkdirs()

        File xmlFile = new File(folderDir, "${tokens[-1]}.xml")
        xmlFile.text = xml
    }

}
