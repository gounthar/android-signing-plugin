package org.jenkinsci.plugins.androidsigning;

import hudson.remoting.Callable;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class GetPathSeparator implements Callable<String, IOException>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public String call() throws IOException {
        return File.pathSeparator;
    }

    @Override
    public void checkRoles(RoleChecker roleChecker) throws SecurityException {
        // This callable does not require any specific roles, so we can leave this method empty.
    }
}
