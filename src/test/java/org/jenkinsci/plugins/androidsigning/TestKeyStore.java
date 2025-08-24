package org.jenkinsci.plugins.androidsigning;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;


public class TestKeyStore implements TestRule {

    public static final String KEY_STORE_RESOURCE = "/" + SignApksBuilderTest.class.getSimpleName() + ".p12";
    public static final String KEY_STORE_ID = SignApksBuilderTest.class.getSimpleName() + ".keyStore";
    public static final String KEY_ALIAS = SignApksBuilderTest.class.getSimpleName();

    public final JenkinsRule testJenkins;
    public final String resourceName;
    public final String credentialsId;
    public final String description;
    public final String password;
    public StandardCertificateCredentials credentials;

    TestKeyStore(JenkinsRule testJenkins) {
        this(testJenkins, KEY_STORE_RESOURCE, KEY_STORE_ID, "Main Test Key Store", SignApksBuilderTest.class.getSimpleName());
    }

    TestKeyStore(JenkinsRule testJenkins, String resourceName, String credentialsId, String description, String password) {
        this.testJenkins = testJenkins;
        this.resourceName = resourceName;
        this.credentialsId = credentialsId;
        this.description = description;
        this.password = password;
    }

    /**
     * Applies credentials before executing the base statement and removes them afterwards.
     * This method wraps the given base statement with credential management logic.
     * 
     * @param base The base Statement to be executed
     * @param description The Description of the test (not used in this implementation)
     * @return A new Statement that adds credential management around the base statement
     * @throws Throwable If an exception occurs during the execution of the base statement
     */
    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                addCredentials();
                try {
                    base.evaluate();
                }
                finally {
                    removeCredentials();
                }
            }
        };
    }

    /**
     * Adds credentials to the Jenkins instance for testing purposes.
     * 
     * This method reads a keystore from a resource file, encodes it in base64,
     * creates a CertificateCredentialsImpl object, and adds it to the global
     * credentials store of the Jenkins instance. If the Jenkins instance is null,
     * the method returns without doing anything.
     * 
     * @throws RuntimeException if an exception occurs during the process of
     *         reading the keystore, encoding it, or adding the credentials
     */
    void addCredentials() {
        if (testJenkins.jenkins == null) {
            return;
        }
        try {
            InputStream keyStoreIn = SignApksBuilderTest.class.getResourceAsStream(resourceName);
            byte[] keyStoreBytes = new byte[keyStoreIn.available()];
            keyStoreIn.read(keyStoreBytes);
            String keyStore = new String(Base64.getEncoder().encode(keyStoreBytes), "utf-8");
            credentials = new CertificateCredentialsImpl(
                CredentialsScope.GLOBAL, credentialsId, description, password,
                new CertificateCredentialsImpl.UploadedKeyStoreSource(keyStore));
            CredentialsStore store = CredentialsProvider.lookupStores(testJenkins.jenkins).iterator().next();
            store.addCredentials(Domain.global(), credentials);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes the stored credentials from the Jenkins credential store.
     * 
     * This method attempts to remove the credentials from the global domain of the
     * Jenkins credential store. If the Jenkins instance is not initialized, the
     * method returns without performing any action. If an IOException occurs during
     * the removal process, it is wrapped in a RuntimeException and thrown.
     * 
     * @throws RuntimeException if an IOException occurs while removing the credentials
     */
    void removeCredentials() {
        if (testJenkins.jenkins == null) {
            return;
        }
        CredentialsStore store = CredentialsProvider.lookupStores(testJenkins.jenkins).iterator().next();
        try {
            store.removeCredentials(Domain.global(), credentials);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        credentials = null;
    }
}
