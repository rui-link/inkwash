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

import picocli.CommandLine.Command;

import top.ruilink.inkwash.cmd.license.HardwareFingerprinter;

/**
 * Prints the machine code computed by the shared hardware fingerprint
 * ({@link HardwareFingerprinter}), the exact value the inkwash-api backend
 * verifies for machine-bound licenses. The value is continuous 32-hex (no
 * hyphens). 'info' is an alias.
 */
@Command(name = "machine-code", aliases = {
		"info" }, mixinStandardHelpOptions = true, description = "Print this machine's machine code (continuous 32-hex)")
/**
 * Machine code reporting command for the current host.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class MachineCodeCommand implements Callable<Integer> {

	@Override
	public Integer call() {
		try {
			String machineCode = HardwareFingerprinter.machineCode();
			System.out.println();
			System.out.println("======================================");
			System.out.println("  Machine Code Detected");
			System.out.println("======================================");
			System.out.println();
			System.out.println("Machine Code: " + machineCode);
			System.out.println();
			return 0;
		} catch (Exception e) {
			System.err.println("Error computing machine code: " + e.getMessage());
			return 1;
		}
	}
}
