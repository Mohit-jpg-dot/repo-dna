package com.repodna.commands;

import com.repodna.version.VersionInfo;
import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

/**
 * Command to output version details.
 */
@Command(
    name = "version",
    description = "Display version and build metadata",
    mixinStandardHelpOptions = true
)
public class VersionCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("RepoDNA Version:  " + VersionInfo.getVersion());
        System.out.println("Java Runtime:     " + VersionInfo.getJavaVersion());
        System.out.println("Operating System: " + VersionInfo.getOsName());
        System.out.println("Architecture:     " + VersionInfo.getOsArch());
        System.out.println("Build Date:       " + VersionInfo.getBuildDate());
        System.out.println("Git Commit:       " + VersionInfo.getGitCommit());
        return 0;
    }
}
