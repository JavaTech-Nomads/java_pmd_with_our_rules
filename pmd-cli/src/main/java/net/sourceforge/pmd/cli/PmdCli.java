/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.cli;

import net.sourceforge.pmd.cli.commands.internal.PmdRootCommand;

import picocli.CommandLine;

public final class PmdCli {

    private PmdCli() { }

    public static void main(String[] args) {
        final CommandLine cli = new CommandLine(new PmdRootCommand())
                .setCaseInsensitiveEnumValuesAllowed(true);
        
        System.exit(cli.execute(args));
    }
}
