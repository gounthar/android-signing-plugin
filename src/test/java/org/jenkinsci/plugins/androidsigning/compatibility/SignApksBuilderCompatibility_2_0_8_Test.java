package org.jenkinsci.plugins.androidsigning.compatibility;

import org.jenkinsci.plugins.androidsigning.SignApksBuilder;
import org.jenkinsci.plugins.androidsigning.SignedApkMappingStrategy;
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


public class SignApksBuilderCompatibility_2_0_8_Test {

    @Rule
    public JenkinsRule testJenkins = new JenkinsRule();

    /**
     * Converts version 2.0.8 entries to builders and verifies the conversion process.
     * 
     * This test method checks if the FreeStyleProject job correctly converts old
     * version (2.0.8) entries to the new SignApksBuilder format. It verifies the
     * number of builders created and their specific properties.
     * 
     * @throws URISyntaxException if there's an issue with URI syntax
     * @throws IOException if there's an I/O error during the test
     */
    @Test
    @LocalData
    public void converts_v2_0_8_entriesToBuilders() throws URISyntaxException, IOException {

        FreeStyleProject job = (FreeStyleProject) testJenkins.jenkins.getItem(getClass().getSimpleName());
        DescribableList<Builder,?> builders = job.getBuildersList();

        assertThat(builders.size(), equalTo(3));

        SignApksBuilder builder = (SignApksBuilder) builders.get(0);
        assertThat(builder.getKeyStoreId(), equalTo("android-signing-1"));
        assertThat(builder.getKeyAlias(), equalTo("key1"));
        assertThat(builder.getApksToSign(), equalTo("build/outputs/apk/*-unsigned.apk"));
        assertThat(builder.getArchiveUnsignedApks(), is(true));
        assertThat(builder.getArchiveSignedApks(), is(true));

        builder = (SignApksBuilder) builders.get(1);
        assertThat(builder.getKeyStoreId(), equalTo("android-signing-1"));
        assertThat(builder.getKeyAlias(), equalTo("key2"));
        assertThat(builder.getApksToSign(), equalTo("SignApksBuilderTest.apk, SignApksBuilderTest-choc*.apk"));
        assertThat(builder.getArchiveUnsignedApks(), is(false));
        assertThat(builder.getArchiveSignedApks(), is(true));

        builder = (SignApksBuilder) builders.get(2);
        assertThat(builder.getKeyStoreId(), equalTo("android-signing-2"));
        assertThat(builder.getKeyAlias(), equalTo("key1"));
        assertThat(builder.getApksToSign(), equalTo("**/*.apk"));
        assertThat(builder.getArchiveUnsignedApks(), is(false));
        assertThat(builder.getArchiveSignedApks(), is(false));
    }

    /**
     * Tests that the zipalign process is not skipped for SignApksBuilder instances in a FreeStyleProject.
     * 
     * This test method verifies that for a FreeStyleProject with three SignApksBuilder instances,
     * none of them are configured to skip the zipalign process. It checks the 'skipZipalign'
     * property of each builder to ensure it is set to false.
     * 
     * @throws URISyntaxException if there's an error with URI syntax
     * @throws IOException if an I/O error occurs
     */
    @Test
    @LocalData
    public void doesNotSkipZipalignFor_v2_0_8_builders() throws URISyntaxException, IOException {

        FreeStyleProject job = (FreeStyleProject) testJenkins.jenkins.getItem(getClass().getSimpleName());
        DescribableList<Builder,?> builders = job.getBuildersList();

        assertThat(builders.size(), equalTo(3));

        SignApksBuilder builder = (SignApksBuilder) builders.get(0);
        assertThat(builder.getSkipZipalign(), is(false));

        builder = (SignApksBuilder) builders.get(1);
        assertThat(builder.getSkipZipalign(), is(false));

        builder = (SignApksBuilder) builders.get(2);
        assertThat(builder.getSkipZipalign(), is(false));
    }

    /**
     * Tests the usage of old signed APK mapping for v2.0.8 builders.
     * 
     * This method verifies that the SignApksBuilder instances in the project's
     * builder list use the UnsignedApkBuilderDirMapping strategy for signed APK mapping.
     * It checks this for three different builder instances.
     * 
     * @throws Exception if an error occurs during the test execution
     */
    @Test
    @LocalData
    public void usesOldSignedApkMappingFor_v2_0_8_builders() throws Exception {

        FreeStyleProject job = (FreeStyleProject) testJenkins.jenkins.getItem(getClass().getSimpleName());
        DescribableList<Builder,?> builders = job.getBuildersList();

        SignApksBuilder builder = (SignApksBuilder) builders.get(0);
        assertThat(builder.getSignedApkMapping(), instanceOf(SignedApkMappingStrategy.UnsignedApkBuilderDirMapping.class));

        builder = (SignApksBuilder) builders.get(1);
        assertThat(builder.getSignedApkMapping(), instanceOf(SignedApkMappingStrategy.UnsignedApkBuilderDirMapping.class));

        builder = (SignApksBuilder) builders.get(2);
        assertThat(builder.getSignedApkMapping(), instanceOf(SignedApkMappingStrategy.UnsignedApkBuilderDirMapping.class));
    }
}
