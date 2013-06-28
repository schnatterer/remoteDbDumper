/**
 * Copyright (C) 2013 Johannes Schnatterer
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.schnatterer.remoteDbDumper.cli;

import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * Command Line Interface.
 * 
 */
public class RemoteDbDumperCli {
	private static final String EOL = System.getProperty("line.separator");

	/** Description for parameter - main parameter (URL). */
	private static final String DESC_MAIN = "[URI to try downloading the database dump from]";
	private static final String DESC_PASSWORD = "password to enter on 403 responses";
	private static final String DESC_USER = "user name to enter on 403 responses";
	private static final String DESC_OUTPUT = "dump file to a specific directory, instead of the current working directory";
	private static final String DESC_HELP = "show this message";
	private static final String DESC_QUIET = "don't write log file to output directory";

	/**
	 * Using the {@link JCommander} framework to parse parameters.
	 * 
	 * See http://jcommander.org/
	 */
	private JCommander commander = null;

	/** Definition of parameter - main parameter (destination folder). */
	@Parameter(description = DESC_MAIN, required = true)
	private List<String> mainParams;

	/** Description for parameter - password */
	@Parameter(names = { "-p", "--password" }, description = DESC_PASSWORD)
	private String password = "";

	/** Description for parameter - user name */
	@Parameter(names = { "-u", "--user" }, description = DESC_USER)
	private String user = "";

	/** Description for parameter - output directory */
	@Parameter(names = { "-o", "--output" }, description = DESC_OUTPUT)
	private String output = ".";

	/** Description for parameter - quiet */
	@Parameter(names = { "-q", "--quiet" }, description = DESC_QUIET)
	private boolean isQuiet = false;

	/** Description for parameter - help */
	@Parameter(names = "--help", help = true, description = DESC_HELP)
	private boolean help;

	/** @return the value of the destination path parameter. */
	public String getUrl() {
		return mainParams.get(0);
	}

	/** @return the value of the "output" paramter. */
	public String getOutputDir() {
		return output;

	}

	/** @return the value of the "user" paramter. */
	public String getUser() {
		return user;
	}

	/** @return the value of the "password" paramter. */
	public String getPassword() {
		return password;
	}

	/** @return the value of the "quiet" paramter. */
	public boolean isQuiet() {
		return isQuiet;
	}

	/**
	 * Don't instantiate. Use {@link #readParams(String[], String)} instead.
	 */
	private RemoteDbDumperCli() {
	}

	/**
	 * Reads the command line parameters and prints error messages when
	 * something went wrong
	 * 
	 * @param argv
	 * @return an instance of {@link MiToCliParams} when everything went ok, or
	 *         <code>null</code> if "-- help" was called.
	 * 
	 * @throws ParameterException
	 *             when something went wrong
	 */
	public static RemoteDbDumperCli readParams(String[] argv,
			String programmName) throws ParameterException {
		RemoteDbDumperCli cliParams = new RemoteDbDumperCli();
		try {
			cliParams.commander = new JCommander(cliParams);
			cliParams.commander.setProgramName(programmName);
			cliParams.commander.parse(argv);
		} catch (ParameterException e) {
			// Print err
			StringBuilder errStr = new StringBuilder(e.getMessage() + EOL);
			if (cliParams.commander != null) {
				cliParams.commander.usage(errStr, "  ");
			}
			System.err.println(errStr.toString());
			// Rethrow, so the main application knows something went wrong
			throw e;
		}

		if (cliParams.help == true) {
			cliParams.commander.usage();
			cliParams = null;
		}

		return cliParams;
	}
}
