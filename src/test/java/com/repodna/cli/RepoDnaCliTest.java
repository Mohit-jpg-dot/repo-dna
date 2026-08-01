package com.repodna.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.*;

public class RepoDnaCliTest {

    @Test
    public void shouldRegisterAllSubcommands() {
        CommandLine cmd = new CommandLine(new RepoDnaCli());
        assertThat(cmd.getSubcommands()).containsKey("init");
        assertThat(cmd.getSubcommands()).containsKey("analyze");
        assertThat(cmd.getSubcommands()).containsKey("health");
        assertThat(cmd.getSubcommands()).containsKey("explain");
        assertThat(cmd.getSubcommands()).containsKey("version");
    }

    @Test
    public void shouldPrintVersionMetadata() {
        java.io.PrintStream originalOut = System.out;
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(bos));
        try {
            CommandLine cmd = new CommandLine(new RepoDnaCli());
            int exitCode = cmd.execute("version");
            assertThat(exitCode).isEqualTo(0);
            
            String output = bos.toString();
            assertThat(output).contains("RepoDNA Version:");
            assertThat(output).contains("Java Runtime:");
            assertThat(output).contains("Operating System:");
            assertThat(output).contains("Architecture:");
        } finally {
            System.setOut(originalOut);
        }
    }
}
