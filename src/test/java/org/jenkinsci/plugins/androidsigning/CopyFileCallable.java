package org.jenkinsci.plugins.androidsigning;

import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import jenkins.MasterToSlaveFileCallable;

class CopyFileCallable extends MasterToSlaveFileCallable<Void> {

    private static final long serialVersionUID = 1L;

    private final String destPath;

    CopyFileCallable(String destPath) {
        this.destPath = destPath;
    }

    @Override
    public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        long fileSize = f.length();
        FileChannel inChannel = FileChannel.open(f.toPath(), StandardOpenOption.READ);
        FileChannel outChannel = FileChannel.open(
                new File(destPath).toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.SYNC);
        inChannel.transferTo(0, fileSize, outChannel);
        outChannel.close();
        inChannel.close();
        System.out.printf("%s copied %s to %s", getClass().getSimpleName(), f.getAbsolutePath(), destPath);
        return null;
    }
}
