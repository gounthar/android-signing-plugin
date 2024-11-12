package org.jenkinsci.plugins.androidsigning;

import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import jenkins.MasterToSlaveFileCallable;

public class GetPathSeparator extends MasterToSlaveFileCallable<String> {
    private static final long serialVersionUID = 1;

    @Override
    public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        return File.pathSeparator;
    }
}
