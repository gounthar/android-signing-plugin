package org.jenkinsci.plugins.androidsigning;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.PretendSlave;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.slaves.EnvironmentVariablesNodeProperty;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


public class SignApksStepTest {

    private JenkinsRule testJenkins = new JenkinsRule();
    private TestKeyStore testKeyStore = new TestKeyStore(testJenkins);

    @Rule
    public RuleChain jenkinsChain = RuleChain.outerRule(testJenkins).around(testKeyStore);

    private String androidHome;
    private PretendSlave slave;
    private FakeZipalign zipalign;

    /**
     * Sets up the test environment for Android-related Jenkins tests.
     * 
     * This method prepares the environment by:
     * 1. Setting the ANDROID_HOME environment variable
     * 2. Adding the ANDROID_HOME to Jenkins global node properties
     * 3. Creating a fake Zipalign tool
     * 4. Setting up a slave node with the necessary environment
     * 
     * @throws Exception If there's an error setting up the environment
     */
    @Before
    public void setupEnvironment() throws Exception {
        URL androidHomeUrl = getClass().getResource("/android");
        androidHome = new File(androidHomeUrl.toURI()).getAbsolutePath();
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars envVars = prop.getEnvVars();
        envVars.put("ANDROID_HOME", androidHome);
        testJenkins.jenkins.getGlobalNodeProperties().add(prop);
        zipalign = new FakeZipalign();
        slave = testJenkins.createPretendSlave(zipalign);
        slave.getComputer().getEnvironment().put("ANDROID_HOME", androidHome);
        slave.setLabelString(slave.getLabelString() + " " + getClass().getSimpleName());
    }

    /**
     * Tests the functionality of the DSL (Domain-Specific Language) for signing Android APKs.
     * 
     * This method creates a WorkflowJob, sets up a CpsFlowDefinition with a node that wraps
     * a CopyTestWorkspace and calls the signAndroidApks step. It then builds the job,
     * asserts its success, and verifies the expected artifacts are created and properly named.
     * 
     * @throws Exception If any error occurs during the test execution
     * @return void This method doesn't return anything
     */
    @Test
    public void dslWorks() throws Exception {
        WorkflowJob job = testJenkins.jenkins.createProject(WorkflowJob.class, getClass().getSimpleName());
        job.setDefinition(new CpsFlowDefinition(String.format(
            "node('%s') {%n" +
            "  wrap($class: 'CopyTestWorkspace') {%n" +
            "    signAndroidApks(" +
            "      keyStoreId: '%s',%n" +
            "      keyAlias: '%s',%n" +
            "      apksToSign: '*-unsigned.apk, **/*-release-unsigned.apk',%n" +
            "      archiveSignedApks: true,%n" +
            "      archiveUnsignedApks: true,%n" +
            "      androidHome: env.ANDROID_HOME%n" +
            "    )%n" +
            "  }%n" +
            "}", getClass().getSimpleName(), TestKeyStore.KEY_STORE_ID, TestKeyStore.KEY_ALIAS)));

        WorkflowRun build = testJenkins.buildAndAssertSuccess(job);
        List<String> artifactNames = build.getArtifacts().stream().map(Run.Artifact::getFileName).collect(Collectors.toList());

        assertThat(artifactNames.size(), equalTo(4));
        assertThat(artifactNames, hasItem(endsWith("SignApksBuilderTest-unsigned.apk")));
        assertThat(artifactNames, hasItem(endsWith("SignApksBuilderTest.apk")));
        assertThat(artifactNames, hasItem(endsWith("app-release-unsigned.apk")));
        assertThat(artifactNames, hasItem(endsWith("app-release.apk")));
        assertThat(zipalign.lastProc.cmds().get(0), startsWith(androidHome));
    }

    /**
     * Tests if the Android home directory is set from environment variables when not specified in the script.
     * 
     * This test method creates a WorkflowJob, sets up a pipeline definition using CpsFlowDefinition,
     * and executes the job. It then verifies if the APK signing process is successful and if the
     * Android home directory is correctly set from environment variables.
     * 
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void setsAndroidHomeFromEnvVarsIfNotSpecifiedInScript() throws Exception {
        WorkflowJob job = testJenkins.jenkins.createProject(WorkflowJob.class, getClass().getSimpleName());
        job.setDefinition(new CpsFlowDefinition(String.format(
            "node('%s') {%n" +
            "  wrap($class: 'CopyTestWorkspace') {%n" +
            "    signAndroidApks(" +
            "      keyStoreId: '%s',%n" +
            "      keyAlias: '%s',%n" +
            "      apksToSign: '*-unsigned.apk, **/*-release-unsigned.apk'%n" +
            "    )%n" +
            "  }%n" +
            "}", getClass().getSimpleName(), TestKeyStore.KEY_STORE_ID, TestKeyStore.KEY_ALIAS)));

        WorkflowRun build = testJenkins.buildAndAssertSuccess(job);
        List<String> artifactNames = build.getArtifacts().stream().map(Run.Artifact::getFileName).collect(Collectors.toList());

        assertThat(artifactNames.size(), equalTo(2));
        assertThat(artifactNames, hasItem(endsWith("SignApksBuilderTest.apk")));
        assertThat(artifactNames, hasItem(endsWith("app-release.apk")));
        assertThat(zipalign.lastProc.cmds().get(0), startsWith(androidHome));
    }

    /**
     * Tests if the Android zipalign tool is set from environment variables when not specified in the script.
     * 
     * This test method verifies that the SignAndroidApks step correctly uses the ANDROID_ZIPALIGN
     * environment variable to set the zipalign tool path when it's not explicitly specified in the
     * pipeline script. It sets up a mock environment, creates a test job, and checks the resulting
     * artifacts and zipalign command.
     * 
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void setsAndroidZipalignFromEnvVarsIfNotSpecifiedInScript() throws Exception {
        URL altZipalignUrl = getClass().getResource("/alt-zipalign/zipalign");
        String altZipalign = new File(altZipalignUrl.toURI()).getAbsolutePath();
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars envVars = prop.getEnvVars();
        envVars.put("ANDROID_ZIPALIGN", altZipalign);

        WorkflowJob job = testJenkins.jenkins.createProject(WorkflowJob.class, getClass().getSimpleName());
        job.setDefinition(new CpsFlowDefinition(String.format(
            "node('%s') {%n" +
            "  wrap($class: 'CopyTestWorkspace') {%n" +
            "    signAndroidApks(" +
            "      keyStoreId: '%s',%n" +
            "      keyAlias: '%s',%n" +
            "      apksToSign: '**/*-unsigned.apk'%n" +
            "    )%n" +
            "  }%n" +
            "}", getClass().getSimpleName(), TestKeyStore.KEY_STORE_ID, TestKeyStore.KEY_ALIAS)));

        WorkflowRun build = testJenkins.buildAndAssertSuccess(job);
        List<String> artifactNames = build.getArtifacts().stream().map(Run.Artifact::getFileName).collect(Collectors.toList());

        assertThat(artifactNames.size(), equalTo(3));
        assertThat(artifactNames, hasItem(endsWith("SignApksBuilderTest.apk")));
        assertThat(artifactNames, hasItem(endsWith("app-release.apk")));
        assertThat(artifactNames, hasItem(endsWith("app-debug.apk")));
        assertThat(zipalign.lastProc.cmds().get(0), startsWith(androidHome));
    }

    /**
     * Tests that the signAndroidApks step does not use environment variables when the script specifies
     * ANDROID_HOME or zipalign path explicitly.
     * 
     * This method sets up two test scenarios:
     * 1. Specifies an alternative ANDROID_HOME directory
     * 2. Specifies an alternative zipalign path
     * 
     * In both cases, it verifies that the specified paths are used instead of the environment variables.
     * 
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void doesNotUseEnvVarsIfScriptSpecifiesAndroidHomeOrZipalign() throws Exception {
        URL altAndroidHomeUrl = getClass().getResource("/win-android");
        String altAndroidHome = new File(altAndroidHomeUrl.toURI()).getAbsolutePath();

        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars envVars = prop.getEnvVars();
        envVars.put("ANDROID_ZIPALIGN", "/fail/zipalign");
        testJenkins.jenkins.getGlobalNodeProperties().add(prop);

        WorkflowJob job = testJenkins.jenkins.createProject(WorkflowJob.class, getClass().getSimpleName());
        job.setDefinition(new CpsFlowDefinition(String.format(
            "node('%s') {%n" +
            "  wrap($class: 'CopyTestWorkspace') {%n" +
            "    signAndroidApks(" +
            "      keyStoreId: '%s',%n" +
            "      keyAlias: '%s',%n" +
            "      apksToSign: '**/*-unsigned.apk',%n" +
            "      androidHome: '%s'%n" +
            "    )%n" +
            "  }%n" +
            "}", getClass().getSimpleName(), TestKeyStore.KEY_STORE_ID, TestKeyStore.KEY_ALIAS, altAndroidHome)));

        testJenkins.buildAndAssertSuccess(job);

        assertThat(zipalign.lastProc.cmds().get(0), startsWith(altAndroidHome));

        URL altZipalignUrl = getClass().getResource("/alt-zipalign/zipalign");
        String altZipalign = new File(altZipalignUrl.toURI()).getAbsolutePath();

        job.setDefinition(new CpsFlowDefinition(String.format(
            "node('%s') {%n" +
            "  wrap($class: 'CopyTestWorkspace') {%n" +
            "    signAndroidApks(" +
            "      keyStoreId: '%s',%n" +
            "      keyAlias: '%s',%n" +
            "      apksToSign: '**/*-unsigned.apk',%n" +
            "      zipalignPath: '%s'%n" +
            "    )%n" +
            "  }%n" +
            "}", getClass().getSimpleName(), TestKeyStore.KEY_STORE_ID, TestKeyStore.KEY_ALIAS, altZipalign)));

        testJenkins.buildAndAssertSuccess(job);

        assertThat(zipalign.lastProc.cmds().get(0), startsWith(altZipalign));
    }

    /**
     * Tests that the signAndroidApks step skips the zipalign process when skipZipalign is set to true.
     * 
     * This test method creates a Jenkins workflow job with a pipeline script that uses the signAndroidApks step.
     * It sets the skipZipalign parameter to true and verifies that the zipalign process is not executed.
     * 
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void skipsZipalign() throws Exception {
        WorkflowJob job = testJenkins.jenkins.createProject(WorkflowJob.class, getClass().getSimpleName());
        job.setDefinition(new CpsFlowDefinition(String.format(
            "node('%s') {%n" +
                "  wrap($class: 'CopyTestWorkspace') {%n" +
                "    signAndroidApks(" +
                "      keyStoreId: '%s',%n" +
                "      keyAlias: '%s',%n" +
                "      apksToSign: '**/*-unsigned.apk',%n" +
                "      skipZipalign: true%n" +
                "    )%n" +
                "  }%n" +
                "}", getClass().getSimpleName(), TestKeyStore.KEY_STORE_ID, TestKeyStore.KEY_ALIAS)));

        testJenkins.buildAndAssertSuccess(job);

        assertThat(zipalign.lastProc, nullValue());
    }

    /**
     * Tests that the signed APK mapping defaults to the unsigned APK sibling.
     * 
     * This method creates a Jenkins pipeline job that signs an Android APK using the signAndroidApks step,
     * and then verifies that the signed APK is correctly archived with the expected name.
     * 
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void signedApkMappingDefaultsToUnsignedApkSibling() throws Exception {
        WorkflowJob job = testJenkins.jenkins.createProject(WorkflowJob.class, getClass().getSimpleName());
        job.setDefinition(new CpsFlowDefinition(String.format(
            "node('%s') {%n" +
            "  wrap($class: 'CopyTestWorkspace') {%n" +
            "    signAndroidApks(" +
            "      keyStoreId: '%s',%n" +
            "      keyAlias: '%s',%n" +
            "      apksToSign: 'SignApksBuilderTest-unsigned.apk',%n" +
            "      archiveSignedApks: false%n" +
            "    )%n" +
            "    archive includes: 'SignApksBuilderTest.apk'%n" +
            "  }%n" +
            "}", getClass().getSimpleName(), TestKeyStore.KEY_STORE_ID, TestKeyStore.KEY_ALIAS)));

        WorkflowRun run = testJenkins.buildAndAssertSuccess(job);
        List<WorkflowRun.Artifact> artifacts = run.getArtifacts();

        assertThat(artifacts.size(), equalTo(1));
        assertThat(artifacts.get(0).getFileName(), equalTo("SignApksBuilderTest.apk"));
    }

    /**
     * Tests the usage of a specified signed APK mapping in the signAndroidApks step.
     * 
     * This method creates a Jenkins workflow job, sets up a pipeline definition
     * that uses the signAndroidApks step with a custom signed APK mapping,
     * executes the job, and verifies the output artifacts.
     * 
     * @throws Exception if any error occurs during the test execution
     * @return void
     */
    @Test
    public void usesSpecifiedSignedApkMapping() throws Exception {
        WorkflowJob job = testJenkins.jenkins.createProject(WorkflowJob.class, getClass().getSimpleName());
        job.setDefinition(new CpsFlowDefinition(String.format(
            "node('%s') {%n" +
                "  wrap($class: 'CopyTestWorkspace') {%n" +
                "    signAndroidApks(" +
                "      keyStoreId: '%s',%n" +
                "      keyAlias: '%s',%n" +
                "      apksToSign: 'SignApksBuilderTest-unsigned.apk',%n" +
                "      archiveSignedApks: false,%n" +
                "      signedApkMapping: [$class: 'TestSignedApkMapping']%n" +
                "    )%n" +
                "    archive includes: 'TestSignedApkMapping-SignApksBuilderTest-unsigned.apk'%n" +
                "  }%n" +
                "}", getClass().getSimpleName(), TestKeyStore.KEY_STORE_ID, TestKeyStore.KEY_ALIAS)));

        WorkflowRun run = testJenkins.buildAndAssertSuccess(job);
        List<WorkflowRun.Artifact> artifacts = run.getArtifacts();

        assertThat(artifacts.size(), equalTo(1));
        assertThat(artifacts.get(0).getFileName(), equalTo("TestSignedApkMapping-SignApksBuilderTest-unsigned.apk"));
    }
}
