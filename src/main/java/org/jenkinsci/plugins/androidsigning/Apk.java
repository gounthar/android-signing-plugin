package org.jenkinsci.plugins.androidsigning;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;


public final class Apk extends AbstractDescribableImpl<Apk> {
    private String keyStore;
    private String alias;
    private String apksToSign;
    private boolean archiveSignedApks = true;
    private boolean archiveUnsignedApks = false;

    // renamed fields
    transient private String selection;

    /**
     *
     * @param keyStore an ID of a {@link com.cloudbees.plugins.credentials.common.StandardCertificateCredentials}
     * @param alias the alias of the signing key in the key store
     * @param apksToSign an Ant-style glob pattern; multiple globs separated by commas are allowed
     */
    @DataBoundConstructor
    public Apk(String keyStore, String alias, String apksToSign) {
        this.keyStore = keyStore;
        this.alias = alias;
        this.apksToSign = apksToSign;
    }

    @DataBoundSetter
    public void setArchiveSignedApks(boolean x) {
        archiveSignedApks = x;
    }

    @DataBoundSetter
    public void setArchiveUnsignedApks(boolean x) {
        archiveUnsignedApks = x;
    }

    public Apk archiveSignedApks(boolean x) {
        setArchiveSignedApks(x);
        return this;
    }

    public Apk archiveUnsignedApk(boolean x) {
        setArchiveUnsignedApks(x);
        return this;
    }

    @SuppressWarnings("unused")
    protected Object readResolve() {
        if (apksToSign == null) {
            apksToSign = selection;
        }
        return this;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Apk> {
        @Override @Nonnull
        public String getDisplayName() {
            return "APK Signing Entry";
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillKeyStoreItems(@AncestorInPath ItemGroup<?> parent) {
            if (parent == null) {
                parent = Jenkins.getInstance();
            }
            ListBoxModel items = new ListBoxModel();
            List<StandardCertificateCredentials> keys = CredentialsProvider.lookupCredentials(
                StandardCertificateCredentials.class, parent, ACL.SYSTEM, SignApksBuilder.NO_REQUIREMENTS);
            for (StandardCertificateCredentials key : keys) {
                items.add(key.getDescription(), key.getId());
            }
            return items;
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckAlias(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FormValidation.validateRequired(value);
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckApksToSign(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException, InterruptedException {
            if (project == null) {
                return FormValidation.warning(Messages.validation_noProject());
            }
            FilePath someWorkspace = project.getSomeWorkspace();
            if (someWorkspace != null) {
                String msg = someWorkspace.validateAntFileMask(value, FilePath.VALIDATE_ANT_FILE_MASK_BOUND);
                if (msg != null) {
                    return FormValidation.error(msg);
                }
                return FormValidation.ok();
            }
            else {
                return FormValidation.warning(Messages.validation_noWorkspace());
            }
        }
    }

    public String getApksToSign() {
        return apksToSign;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public String getAlias() {
        return alias;
    }

    public boolean getArchiveUnsignedApks() {
        return archiveUnsignedApks;
    }

    public boolean getArchiveSignedApks() {
        return archiveSignedApks;
    }

    public String[] getSelectionGlobs() {
        String[] globs = getApksToSign().split("\\s*,\\s*");
        List<String> cleanGlobs = new ArrayList<>(globs.length);
        for (String glob : globs) {
            glob = glob.trim();
            if (glob.length() > 0) {
                cleanGlobs.add(glob);
            }
        }
        return cleanGlobs.toArray(new String[cleanGlobs.size()]);
    }
}
