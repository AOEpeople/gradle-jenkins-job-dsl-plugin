package com.aoe.gradle.jenkinsjobdsl


import groovy.io.FileType
import hudson.model.Item
import hudson.model.View
import javaposse.jobdsl.dsl.*
import javaposse.jobdsl.plugin.JenkinsJobManagement
import jenkins.model.Jenkins
import org.apache.commons.io.FilenameUtils
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests that all dsl scripts in the jobs directory will compile.
 */
class JobScriptsSpec extends Specification {

    static final String SOURCE_DIR_PROP = System.getProperty('jobSourceDirs', '')
    static final String RESOURCE_DIR_PROP = System.getProperty('jobResourceDirs', '')

    static final List<File> SOURCE_DIRS = toDirs(SOURCE_DIR_PROP)
    static final List<File> RESOURCE_DIRS = toDirs(RESOURCE_DIR_PROP)

    static List<File> toDirs(String separatedListOfDirs) {
        separatedListOfDirs
                .split(File.pathSeparator)
                .toList()
                .collect { new File(it) }
                .findAll { it.exists() }
    }

    @Shared
    @ClassRule
    private JenkinsRule jenkinsRule = new JenkinsRule()

    @Shared
    private File outputDir = new File('./build/debug-xml')

    def setupSpec() {
        outputDir.deleteDir()
    }

    @Unroll
    void 'test DSL script #file.name'(File file) {
        given:
        JobManagement jm = new JenkinsJobManagement(System.out, [*: System.getenv()], new File('.'))

        expect:
        assertValidFilename(file)

        when:
        GeneratedItems items = new DslScriptLoader(jm).runScript(file.text)
        writeItems items

        then:
        noExceptionThrown()

        where:
        file << jobFiles
    }

    static List<File> getJobFiles() {
        List<File> files = []
        SOURCE_DIRS.each {
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
            View view = getView(viewName)
            String text = new URL(jenkins.rootUrl + view.url + 'config.xml').text
            writeFile new File(outputDir, 'views'), viewName, text
        }
    }

    private View getView(String viewName){
        viewGroup = jenkinsRule.jenkins
        viewName.split("/").dropRight(1).each { folderName ->
            viewGroup = viewGroup.getItem(folderName)
        }

        viewGroup.getView(viewName)
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

    /**
     * Check if script name is valid according to Java conventions
     *
     * @param fileName
     * @return
     */
    protected static void assertValidFilename(File fileName) {
        boolean result = true
        String normalizedName = FilenameUtils.removeExtension(fileName.getName())
        if (normalizedName.length() == 0 || !Character.isJavaIdentifierStart(normalizedName.charAt(0))) {
            result = false
        }
        for (int i = 1; i < normalizedName.length(); i += 1) {
            if (!Character.isJavaIdentifierPart(normalizedName.charAt(i))) {
                result = false
            }
        }

        assert result, "invalid script name '${fileName}; script names may only contain " +
            'letters, digits and underscores, but may not start with a digit'
    }
}

