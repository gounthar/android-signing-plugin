package org.jenkinsci.plugins.androidsigning;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.util.ArgumentListBuilder;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;


public class ZipalignToolTest {

    FilePath workspace;
    FilePath androidHome;
    FilePath androidHomeZipalign;
    FilePath altZipalign;

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    /**
     * Initializes the test environment by creating temporary directories and copying necessary files.
     * 
     * This method sets up the workspace, Android SDK, and alternative zipalign tool for testing purposes.
     * It creates temporary directories and copies the required files from resources to these directories.
     * 
     * @throws Exception if there's an error during file operations or URI conversions
     */
    @Before
    public void copyWorkspace() throws Exception {
        FilePath tempDirPath = new FilePath(tempDir.getRoot());

        URL workspaceUrl = getClass().getResource("/workspace");
        FilePath workspace = new FilePath(new File(workspaceUrl.toURI()));
        this.workspace = tempDirPath.child("workspace");
        workspace.copyRecursiveTo(this.workspace);

        URL androidHomeUrl = getClass().getResource("/android");
        FilePath androidHome = new FilePath(new File(androidHomeUrl.toURI()));
        this.androidHome = tempDirPath.child("android-sdk");
        androidHome.copyRecursiveTo(this.androidHome);
        androidHomeZipalign = this.androidHome.child("build-tools").child("1.0").child("zipalign");

        URL altZipalignUrl = getClass().getResource("/alt-zipalign");
        FilePath altZipalign = new FilePath(new File(altZipalignUrl.toURI()));
        this.altZipalign = tempDirPath.child("alt-zipalign");
        altZipalign.copyRecursiveTo(this.altZipalign);
        this.altZipalign = this.altZipalign.child("zipalign");
    }

    @Test
    public void findsZipalignInAndroidHomeEnvVar() throws Exception {
        EnvVars envVars = new EnvVars();
        envVars.put(ZipalignTool.ENV_ANDROID_HOME, androidHome.getRemote());
        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        ArgumentListBuilder cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(androidHomeZipalign.getRemote()));
    }

    @Test
    public void findsZipalignInAndroidZipalignEnvVar() throws Exception {
        EnvVars envVars = new EnvVars();
        envVars.put(ZipalignTool.ENV_ZIPALIGN_PATH, altZipalign.getRemote());
        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        ArgumentListBuilder cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(altZipalign.getRemote()));
    }

    /**
     * Tests the functionality of finding the zipalign tool in the PATH environment variable
     * when the tools directory is present.
     * 
     * This method verifies that the ZipalignTool can correctly locate the zipalign executable
     * in various PATH configurations, including when the Android SDK tools directory is
     * present in different positions within the PATH.
     * 
     * @throws Exception if any file operations or assertions fail
     */
    @Test
    public void findsZipalignInPathEnvVarWithToolsDir() throws Exception {
        FilePath toolsDir = androidHome.child("tools");
        toolsDir.mkdirs();
        FilePath androidTool = toolsDir.child("android");
        androidTool.write(getClass().getSimpleName(), "utf-8");

        EnvVars envVars = new EnvVars();

        String path = String.join(File.pathSeparator, toolsDir.getRemote(), "/other/tools", "/other/bin");
        envVars.put(ZipalignTool.ENV_PATH, path);
        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        ArgumentListBuilder cmd = zipalign.commandFor("path-test.apk", "path-test-aligned.apk");

        assertThat(cmd.toString(), startsWith(androidHomeZipalign.getRemote()));

        path = String.join(File.pathSeparator, "/other/tools", toolsDir.getRemote(), "/other/bin");
        envVars.put(ZipalignTool.ENV_PATH, path);
        zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        cmd = zipalign.commandFor("path-test.apk", "path-test-aligned.apk");

        assertThat(cmd.toString(), startsWith(androidHomeZipalign.getRemote()));

        path = String.join(File.pathSeparator, "/other/tools", "/other/bin", toolsDir.getRemote());
        envVars.put(ZipalignTool.ENV_PATH, path);
        zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        cmd = zipalign.commandFor("path-test.apk", "path-test-aligned.apk");

        assertThat(cmd.toString(), startsWith(androidHomeZipalign.getRemote()));

        path = String.join(File.pathSeparator, toolsDir.getRemote());
        envVars.put(ZipalignTool.ENV_PATH, path);
        zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        cmd = zipalign.commandFor("path-test.apk", "path-test-aligned.apk");

        assertThat(cmd.toString(), startsWith(androidHomeZipalign.getRemote()));
    }

    /**
     * Tests the ZipalignTool's ability to find the zipalign executable in the PATH environment variable,
     * specifically when the tools/bin directory is included in various positions within the PATH.
     * 
     * This test method creates a mock Android SDK directory structure, sets up different PATH
     * configurations, and verifies that the ZipalignTool correctly locates and uses the zipalign
     * executable from the expected location.
     * 
     * @throws Exception if any I/O or filesystem operations fail during the test setup or execution
     */
    @Test
    public void findsZipalignInPathEnvVarWithToolsBinDir() throws Exception {
        FilePath toolsBinDir = androidHome.child("tools").child("bin");
        toolsBinDir.mkdirs();
        FilePath sdkmanagerTool = toolsBinDir.child("sdkmanager");
        sdkmanagerTool.write(getClass().getSimpleName(), "utf-8");

        EnvVars envVars = new EnvVars();

        String path = String.join(File.pathSeparator, toolsBinDir.getRemote(), "/other/tools", "/other/bin");
        envVars.put(ZipalignTool.ENV_PATH, path);
        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        ArgumentListBuilder cmd = zipalign.commandFor("path-test.apk", "path-test-aligned.apk");

        assertThat(cmd.toString(), startsWith(androidHomeZipalign.getRemote()));

        path = String.join(File.pathSeparator, "/other/tools", toolsBinDir.getRemote(), "/other/bin");
        envVars.put(ZipalignTool.ENV_PATH, path);
        zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        cmd = zipalign.commandFor("path-test.apk", "path-test-aligned.apk");

        assertThat(cmd.toString(), startsWith(androidHomeZipalign.getRemote()));

        path = String.join(File.pathSeparator, "/other/tools", "/other/bin", toolsBinDir.getRemote());
        envVars.put(ZipalignTool.ENV_PATH, path);
        zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        cmd = zipalign.commandFor("path-test.apk", "path-test-aligned.apk");

        assertThat(cmd.toString(), startsWith(androidHomeZipalign.getRemote()));

        path = String.join(File.pathSeparator, toolsBinDir.getRemote());
        envVars.put(ZipalignTool.ENV_PATH, path);
        zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        cmd = zipalign.commandFor("path-test.apk", "path-test-aligned.apk");

        assertThat(cmd.toString(), startsWith(androidHomeZipalign.getRemote()));
    }
    
    /**
     * Tests the ability of ZipalignTool to find the zipalign executable in various PATH configurations.
     * 
     * This test method verifies that the ZipalignTool can correctly locate the zipalign executable
     * when it's present in different positions within the PATH environment variable. It tests
     * scenarios where the zipalign directory is at the beginning, middle, and end of the PATH,
     * as well as when it's the only directory in the PATH.
     * 
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void findsZipalignInPathEnvVarWithZipalignParentDir() throws Exception {
        FilePath zipalignDir = altZipalign.getParent();

        EnvVars envVars = new EnvVars();

        String path = String.join(File.pathSeparator, zipalignDir.getRemote(), "/other/tools", "/other/bin");
        envVars.put(ZipalignTool.ENV_PATH, path);
        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        ArgumentListBuilder cmd = zipalign.commandFor("path-test.apk", "path-test-aligned.apk");

        assertThat(cmd.toString(), startsWith(altZipalign.getRemote()));

        path = String.join(File.pathSeparator, "/other/tools", zipalignDir.getRemote(), "/other/bin");
        envVars.put(ZipalignTool.ENV_PATH, path);
        zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        cmd = zipalign.commandFor("path-test.apk", "path-test-aligned.apk");

        assertThat(cmd.toString(), startsWith(altZipalign.getRemote()));

        path = String.join(File.pathSeparator, "/other/tools", "/other/bin", zipalignDir.getRemote());
        envVars.put(ZipalignTool.ENV_PATH, path);
        zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        cmd = zipalign.commandFor("path-test.apk", "path-test-aligned.apk");

        assertThat(cmd.toString(), startsWith(altZipalign.getRemote()));

        path = String.join(File.pathSeparator, zipalignDir.getRemote());
        envVars.put(ZipalignTool.ENV_PATH, path);
        zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        cmd = zipalign.commandFor("path-test.apk", "path-test-aligned.apk");

        assertThat(cmd.toString(), startsWith(altZipalign.getRemote()));
    }

    /**
     * Tests that the ZipalignTool correctly overrides the Android Home environment variable.
     * 
     * This test method verifies that when both ANDROID_HOME and ZIPALIGN_PATH environment variables
     * are set, the ZipalignTool prioritizes the ZIPALIGN_PATH over ANDROID_HOME when constructing
     * the command for zipalign operation.
     * 
     * @throws Exception if an error occurs during the test execution
     */
    @Test
    public void androidZiplignOverridesAndroidHome() throws Exception {
        EnvVars envVars = new EnvVars();
        envVars.put(ZipalignTool.ENV_ANDROID_HOME, androidHomeZipalign.getRemote());
        envVars.put(ZipalignTool.ENV_ZIPALIGN_PATH, altZipalign.getRemote());
        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        ArgumentListBuilder cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(altZipalign.getRemote()));
    }

    /**
     * Tests if the ZipalignTool uses the latest zipalign executable from the Android SDK.
     * 
     * This method creates a mock Android SDK environment with a newer build tools version,
     * sets up the necessary environment variables, and verifies that the ZipalignTool
     * correctly selects the latest zipalign executable.
     * 
     * @throws IOException If there's an error in file operations
     * @throws InterruptedException If the thread is interrupted during execution
     */
    @Test
    public void usesLatestZipalignFromAndroidHome() throws IOException, InterruptedException {
        FilePath newerBuildTools = androidHome.child("build-tools").child("1.1");
        newerBuildTools.mkdirs();
        FilePath newerZipalign = newerBuildTools.child("zipalign");
        newerZipalign.write("# fake zipalign", "utf-8");

        EnvVars envVars = new EnvVars();
        envVars.put(ZipalignTool.ENV_ANDROID_HOME, androidHome.getRemote());
        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        ArgumentListBuilder cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(newerZipalign.getRemote()));

        newerBuildTools.deleteRecursive();
    }

    /**
     * Tests if an explicitly set Android home directory overrides environment variables.
     * 
     * This method verifies that when an explicit Android home directory is provided,
     * it takes precedence over the directories specified in environment variables
     * for locating the zipalign tool.
     * 
     * @throws Exception if any I/O or environment setup operations fail
     */
    @Test
    public void explicitAndroidHomeOverridesEnvVars() throws Exception {
        EnvVars envVars = new EnvVars();
        envVars.put(ZipalignTool.ENV_ANDROID_HOME, androidHomeZipalign.getRemote());
        envVars.put(ZipalignTool.ENV_ZIPALIGN_PATH, altZipalign.getRemote());

        FilePath explicitAndroidHome = workspace.createTempDir("my-android-home", "");
        androidHome.copyRecursiveTo(explicitAndroidHome);

        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, explicitAndroidHome.getRemote(), null);
        ArgumentListBuilder cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(explicitAndroidHome.getRemote()));

        explicitAndroidHome.deleteRecursive();
    }

    /**
     * Tests if an explicitly provided zipalign tool overrides the zipalign paths set in environment variables.
     * 
     * This test method creates a mock environment with various zipalign paths set, then creates a temporary
     * explicit zipalign file. It verifies that when a ZipalignTool is created with this explicit path,
     * it takes precedence over the environment variable paths when generating the command.
     * 
     * @throws IOException If there's an error in file operations
     * @throws InterruptedException If the thread is interrupted during execution
     */
    @Test
    public void explicitZipalignOverridesEnvZipaligns() throws IOException, InterruptedException {
        EnvVars envVars = new EnvVars();
        envVars.put(ZipalignTool.ENV_ANDROID_HOME, androidHomeZipalign.getRemote());
        envVars.put(ZipalignTool.ENV_ZIPALIGN_PATH, altZipalign.getRemote());

        FilePath explicitZipalign = workspace.createTempDir("my-zipalign", "").child("zipalign");
        explicitZipalign.write("# fake zipalign", "utf-8");
        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, null, explicitZipalign.getRemote());
        ArgumentListBuilder cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(explicitZipalign.getRemote()));

        explicitZipalign.getParent().deleteRecursive();
    }

    /**
     * Tests if an explicitly provided zipalign tool overrides all other zipalign configurations.
     * 
     * This method sets up various zipalign configurations through environment variables and
     * explicit file paths, then verifies that the explicitly provided zipalign tool takes
     * precedence over all other configurations when constructing the zipalign command.
     * 
     * @throws Exception if any I/O error occurs during the test setup or execution
     */
    @Test
    public void explicitZipalignOverridesEverything() throws Exception {
        EnvVars envVars = new EnvVars();
        envVars.put(ZipalignTool.ENV_ANDROID_HOME, androidHomeZipalign.getRemote());
        envVars.put(ZipalignTool.ENV_ZIPALIGN_PATH, altZipalign.getRemote());

        FilePath explicitAndroidHome = workspace.createTempDir("my-android-home", "");
        androidHome.copyRecursiveTo(explicitAndroidHome);

        FilePath explicitZipalign = workspace.createTempDir("my-zipalign", "").child("zipalign");
        explicitZipalign.write("# fake zipalign", "utf-8");

        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, explicitAndroidHome.getRemote(), explicitZipalign.getRemote());
        ArgumentListBuilder cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(explicitZipalign.getRemote()));

        explicitZipalign.getParent().deleteRecursive();
        explicitAndroidHome.deleteRecursive();
    }

    /**
     * Tests the behavior of ZipalignTool when the zipalign executable is not found in the ANDROID_HOME environment variable path.
     * This test case specifically checks if the tool falls back to using the Windows executable (zipalign.exe) when the environment
     * variable does not contain the expected zipalign tool.
     * 
     * @throws IOException If an I/O error occurs during the test
     * @throws InterruptedException If the test thread is interrupted
     * @throws URISyntaxException If there's an error in URI syntax when creating file paths
     */
    @Test
    public void triesWindowsExeIfEnvAndroidHomeZipalignDoesNotExist() throws IOException, InterruptedException, URISyntaxException {
        URL androidHomeUrl = getClass().getResource("/win-android");
        FilePath winAndroidHome = new FilePath(new File(androidHomeUrl.toURI()));
        FilePath winAndroidHomeZipalign = winAndroidHome.child("build-tools").child("1.0").child("zipalign.exe");

        EnvVars envVars = new EnvVars();
        envVars.put(ZipalignTool.ENV_ANDROID_HOME, winAndroidHome.getRemote());
        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        ArgumentListBuilder cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(winAndroidHomeZipalign.getRemote()));
    }

    /**
     * Tests the ZipalignTool's behavior when the environment variable for zipalign path doesn't exist.
     * This test verifies that the tool attempts to use a Windows executable (.exe) version of zipalign
     * if the specified path in the environment variable does not exist.
     * 
     * @throws Exception If any error occurs during the test execution
     */
    @Test
    public void triesWindowsExeIfEnvZipalignDoesNotExist() throws Exception {
        URL androidHomeUrl = getClass().getResource("/win-android");
        FilePath winAndroidHome = new FilePath(new File(androidHomeUrl.toURI()));
        FilePath suffixedZipalign = winAndroidHome.child("build-tools").child("1.0").child("zipalign.exe");
        FilePath unsuffixedZipalign = suffixedZipalign.getParent().child("zipalign");

        EnvVars envVars = new EnvVars();
        envVars.put(ZipalignTool.ENV_ZIPALIGN_PATH, unsuffixedZipalign.getRemote());
        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, null, null);
        ArgumentListBuilder cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(suffixedZipalign.getRemote()));
    }

    /**
     * Tests the behavior of ZipalignTool when an explicit Android home is provided but zipalign.exe does not exist.
     * This test verifies that the tool falls back to using the Windows executable in the specified path.
     * 
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void triesWindowsExeIfExplicitAndroidHomeZipalignDoesNotExist() throws Exception {
        URL androidHomeUrl = getClass().getResource("/win-android");
        FilePath winAndroidHome = new FilePath(new File(androidHomeUrl.toURI()));
        FilePath winAndroidHomeZipalign = winAndroidHome.child("build-tools").child("1.0").child("zipalign.exe");

        EnvVars envVars = new EnvVars();
        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, winAndroidHome.getRemote(), null);
        ArgumentListBuilder cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(winAndroidHomeZipalign.getRemote()));
    }

    /**
     * Tests if the ZipalignTool correctly uses the Windows executable (.exe) version of zipalign
     * when the explicitly specified zipalign does not exist.
     * 
     * This test method sets up a mock Android SDK environment on Windows, creates a ZipalignTool
     * instance with a non-existent zipalign path, and verifies that the tool falls back to using
     * the Windows-specific zipalign.exe file.
     * 
     * @throws Exception if there's an error in file operations or URI conversions
     */
    @Test
    public void triesWindowsExeIfExplicitZipalignDoesNotExist() throws Exception {
        URL androidHomeUrl = getClass().getResource("/win-android");
        FilePath winAndroidHome = new FilePath(new File(androidHomeUrl.toURI()));
        FilePath suffixedZipalign = winAndroidHome.child("build-tools").child("1.0").child("zipalign.exe");
        FilePath unsuffixedZipalign = suffixedZipalign.getParent().child("zipalign");

        EnvVars envVars = new EnvVars();
        ZipalignTool zipalign = new ZipalignTool(envVars, workspace, System.out, null, unsuffixedZipalign.getRemote());
        ArgumentListBuilder cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(suffixedZipalign.getRemote()));
    }

    /**
     * Tests the resolution of variable references in explicit parameters for the ZipalignTool.
     * 
     * This method verifies that the ZipalignTool correctly resolves environment variables
     * when constructing command-line arguments. It tests two scenarios:
     * 1. Using an alternate zipalign executable path
     * 2. Using an alternate Android home directory
     * 
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void resolvesVariableReferencesInExplicitParameters() throws Exception {
        EnvVars env = new EnvVars();
        env.put("ALT_ZIPALIGN", altZipalign.getRemote());
        ZipalignTool zipalign = new ZipalignTool(env, workspace, System.out, null, "${ALT_ZIPALIGN}");
        ArgumentListBuilder cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(altZipalign.getRemote()));

        env.clear();
        env.put("ALT_ANDROID_HOME", androidHome.getRemote());
        zipalign = new ZipalignTool(env, workspace, System.out, "${ALT_ANDROID_HOME}", null);
        cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(androidHomeZipalign.getRemote()));
    }

    /**
     * Tests if the ZipalignTool can find the Windows zipalign executable from the PATH environment variable.
     * 
     * This test method sets up a mock environment with a specific PATH, creates a ZipalignTool instance,
     * and verifies that the tool correctly constructs a command using the zipalign executable from the given PATH.
     * 
     * @throws Exception if there's an error in accessing resources or creating file paths
     */
    @Test
    public void findsWindowsZipalignFromEnvPath() throws Exception {
        URL url = getClass().getResource("/win-android");
        FilePath winAndroidHome = new FilePath(new File(url.toURI()));
        EnvVars env = new EnvVars();
        env.put("PATH", winAndroidHome.getRemote());

        ZipalignTool zipalign = new ZipalignTool(env, workspace, System.out, null, null);
        ArgumentListBuilder cmd = zipalign.commandFor("test.apk", "test-aligned.apk");

        assertThat(cmd.toString(), startsWith(winAndroidHome.getRemote()));
    }
}
