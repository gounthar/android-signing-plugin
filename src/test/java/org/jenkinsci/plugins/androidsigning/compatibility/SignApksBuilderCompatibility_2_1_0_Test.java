package org.jenkinsci.plugins.androidsigning.compatibility;

import org.jenkinsci.plugins.androidsigning.SignApksBuilder;
import org.jenkinsci.plugins.androidsigning.SignedApkMappingStrategy;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.IOException;
import java.net.URISyntaxException;

import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import hudson.util.DescribableList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class SignApksBuilderCompatibility_2_1_0_Test {

    @Rule
    public JenkinsRule testJenkins = new JenkinsRule();

    /**
     * Verifies that the SignApksBuilder does not skip zipalign for v2.1.0 builders.
     * 
     * This test method checks the configuration of two SignApksBuilder instances
     * in a FreeStyleProject's builder list. It ensures that both builders have
     * the correct settings, including keystore IDs, key aliases, APK signing patterns,
     * archiving options, and that zipalign is not skipped.
     * 
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    @LocalData
    public void doesNotSkipZipalignFor_v2_1_0_builders() throws Exception {

        FreeStyleProject job = (FreeStyleProject) testJenkins.jenkins.getItem(getClass().getSimpleName());
        DescribableList<Builder,?> builders = job.getBuildersList();

        assertThat(builders.size(), equalTo(2));

        SignApksBuilder builder = (SignApksBuilder) builders.get(0);
        assertThat(builder.getKeyStoreId(), equalTo("android-team-1"));
        assertThat(builder.getKeyAlias(), equalTo("android-team-1"));
        assertThat(builder.getApksToSign(), equalTo("build/outputs/apk/*-unsigned.apk"));
        assertThat(builder.getArchiveSignedApks(), is(true));
        assertThat(builder.getArchiveUnsignedApks(), is(true));
        assertThat(builder.getSkipZipalign(), is(false));

        builder = (SignApksBuilder) builders.get(1);
        assertThat(builder.getKeyStoreId(), equalTo("android-team-2"));
        assertThat(builder.getKeyAlias(), equalTo("android-team-2"));
        assertThat(builder.getApksToSign(), equalTo("**/*-unsigned.apk"));
        assertThat(builder.getArchiveSignedApks(), is(true));
        assertThat(builder.getArchiveUnsignedApks(), is(false));
        assertThat(builder.getSkipZipalign(), is(false));
    }

    /**
     * Tests the usage of old signed APK mapping for v2.1.0 builders.
     * 
     * This method verifies that the SignApksBuilder instances in a FreeStyleProject
     * are using the UnsignedApkBuilderDirMapping strategy for signed APK mapping.
     * It checks two builders to ensure they both use the correct mapping strategy.
     * 
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    @LocalData
    public void usesOldSignedApkMappingFor_v2_1_0_builders() throws Exception {

        FreeStyleProject job = (FreeStyleProject) testJenkins.jenkins.getItem(getClass().getSimpleName());
        DescribableList<Builder,?> builders = job.getBuildersList();

        assertThat(builders.size(), equalTo(2));

        SignApksBuilder builder = (SignApksBuilder) builders.get(0);
        assertThat(builder.getSignedApkMapping(), instanceOf(SignedApkMappingStrategy.UnsignedApkBuilderDirMapping.class));

        builder = (SignApksBuilder) builders.get(1);
        assertThat(builder.getSignedApkMapping(), instanceOf(SignedApkMappingStrategy.UnsignedApkBuilderDirMapping.class));
    }

}
