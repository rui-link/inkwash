/*
 * This file is part of Inkwash.
 * Copyright (C) 2026 ruilink team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.cmd;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * inkwash CMS License Management CLI. Subcommands: keygen, generate, validate,
 * machine-code.
 */
@Command(name = "inkwash-cmd", mixinStandardHelpOptions = true, version = "1.0.0", description = "inkwash CMS License Management Tool", subcommands = {
		KeyGenCommand.class, GenerateCommand.class, ValidateCommand.class, MachineCodeCommand.class })
/**
 * Inkwash license management CLI entry point.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class LicenseCommand implements Callable<Integer> {

	public static void main(String[] args) {
		int exitCode = new CommandLine(new LicenseCommand()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() {
		// No subcommand given: print usage and exit non-zero.
		CommandLine.usage(this, System.out);
		return 2;
	}
}
