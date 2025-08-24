package org.jenkinsci.plugins.androidsigning;

import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hudson.AbortException;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;


class VerifyApkCallable extends MasterToSlaveFileCallable<VerifyApkCallable.VerifyResult> {

    private static final long serialVersionUID = 1;

    public static class VerifyResult implements Serializable {

        boolean isVerified;
        boolean isVerifiedV1Scheme;
        boolean isVerifiedV2Scheme;
        boolean isVerifiedV3Scheme;
        boolean containsErrors;
        X509Certificate[] certs;
        String[] warnings = new String[0];
        String[] errors = new String[0];

        public VerifyResult(ApkVerifier.Result result) {
            this.isVerified = result.isVerified();
            this.isVerifiedV1Scheme = result.isVerifiedUsingV1Scheme();
            this.isVerifiedV2Scheme = result.isVerifiedUsingV2Scheme();
            this.isVerifiedV3Scheme = result.isVerifiedUsingV3Scheme();
            this.certs = result.getSignerCertificates().toArray(new X509Certificate[0]);
            this.containsErrors = result.containsErrors();
            List<String> messages = new ArrayList<>();
            for (ApkVerifier.IssueWithParams issue : result.getWarnings()) {
                messages.add(issue.toString());
            }
            this.warnings = messages.toArray(warnings);
            messages.clear();
            for (ApkVerifier.IssueWithParams issue : result.getErrors()) {
                messages.add(issue.toString());
            }
            this.errors = messages.toArray(errors);
        }

    }

    private final TaskListener listener;

    VerifyApkCallable(TaskListener listener) {
        this.listener = listener;
    }

    /**
     * Invokes the APK verification process on the given input APK file.
     * 
     * This method uses the ApkVerifier to perform verification on the provided APK file.
     * It builds an ApkVerifier instance, executes the verification, and returns the result.
     * If an exception occurs during the verification process, it is logged and rethrown as an IOException.
     * 
     * @param inputApkFile The File object representing the APK file to be verified
     * @param channel The VirtualChannel used for remote execution (not used in this implementation)
     * @return VerifyResult containing the result of the APK verification process
     * @throws IOException If an I/O error occurs during the verification process or if the verification fails
     * @throws InterruptedException If the verification process is interrupted
     */
    @Override
    public VerifyResult invoke(File inputApkFile, VirtualChannel channel) throws IOException, InterruptedException {

        ApkVerifier verifier = new ApkVerifier.Builder(inputApkFile).build();
        try {
            ApkVerifier.Result result = verifier.verify();
            return new VerifyResult(result);
        }
        catch (Exception e) {
            e.printStackTrace(listener.getLogger());
            throw new IOException(e.getMessage());
        }
    }
}
