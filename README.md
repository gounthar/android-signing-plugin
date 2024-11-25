Here is my proposed update to the README.md based on the code:

# Jenkins Android Signing Plugin

## Summary and Purpose

The Android Signing plugin provides a build step for signing Android APK files in Jenkins. Key benefits:

- Centrally manage and protect Android release signing certificates in Jenkins
- No need to distribute private keys and passwords to developers 
- Works well in multi-node/cloud environments without copying keystores to every node
- Keeps keystore passwords encrypted rather than in plain text
- Signs APKs programmatically without exposing passwords in shell commands
- Supports APK Signature Scheme v2 and v3 via Android `apksig` library

## Requirements

- Jenkins with the [Credentials Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Credentials+Plugin) installed
- JDK 1.8+ (required for Android `apksig` library)
- Android SDK with `zipalign` utility installed on the build node

## Installation

1. Install the Credentials Plugin in Jenkins
2. Copy `android-signing.hpi` to `$JENKINS_HOME/plugins/`
3. Restart Jenkins

Note: This plugin is not yet available in the Jenkins Update Center.

## Usage

### Setup

1. Configure a PKCS12 keystore credential in Jenkins Credentials Plugin
   - Must be password-protected (non-empty password)
   - Password protects both keystore and key entry

2. Ensure Android SDK with `zipalign` is available on build nodes

### Configure Build Step

Add a "Sign Android APKs" build step to your job with:

1. Select the keystore credential
2. Specify key alias (optional if only one key entry exists) 
3. Specify APK files to sign using Ant-style glob patterns (e.g. `**/*-unsigned.apk`)

### Output Options

Two choices for signed APK output location:

1. **Output to unsigned APK sibling** (default)
   - Writes signed APK next to input APK
   - Example: `app-unsigned.apk` -> `app.apk`
   - Useful for post-processing signed APKs

2. **Output to separate directory**  
   - Writes to `SignApksBuilder-out/<apk-name>/`
   - Avoids collisions between multiple signing steps
   
### Pipeline Support

Example pipeline syntax:

```groovy
node {
    signAndroidApks(
        keyStoreId: "app.keystore", 
        keyAlias: "release",
        apksToSign: "**/*-unsigned.apk",
        // Optional:
        // signedApkMapping: [$class: 'UnsignedApkBuilderDirMapping']
        // androidHome: env.ANDROID_HOME
        // zipalignPath: env.ANDROID_ZIPALIGN 
    )
}
```

### Job DSL Support

Example Job DSL:

```groovy
freeStyleJob('android-app') {
    steps {
        signAndroidApks('**/*-unsigned.apk') {
            keyStoreId 'app.keystore'
            keyAlias 'release'
            // Optional:
            // signedApkMapping unsignedApkSibling()
            archiveSignedApks true
            androidHome '/opt/android-sdk'
        }
    }
}
```

## zipalign Configuration

The plugin will find `zipalign` in this order:

1. _Zipalign Path_ form input (supports env vars)
2. _ANDROID_HOME Override_ form input (supports env vars) 
3. `ANDROID_ZIPALIGN` build variable
4. `ANDROID_ZIPALIGN` environment variable
5. `ANDROID_HOME` build variable
6. `ANDROID_HOME` environment variable
7. `PATH` environment variable
   - Directory containing `zipalign`
   - Android SDK home containing tools

## Support 

Please submit issues to [Jenkins JIRA](https://issues.jenkins-ci.org/issues/?jql=project%3DJENKINS%20AND%20component%3Dandroid-signing-plugin).

## License

See LICENSE and NOTICE files for copyright and license details.

## References

- [Android App Signing](https://developer.android.com/studio/publish/app-signing.html#signing-manually)
- [APK Signature Scheme v2](https://source.android.com/security/apksigning/v2.html)
- [APK Signature Scheme v3](https://source.android.com/security/apksigning/v3)
