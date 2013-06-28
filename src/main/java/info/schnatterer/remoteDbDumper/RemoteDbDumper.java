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

package info.schnatterer.remoteDbDumper;

import info.schnatterer.remoteDbDumper.cli.RemoteDbDumperCli;
import info.schnatterer.remoteDbDumper.dump.DbDumpException;
import info.schnatterer.remoteDbDumper.dump.drupal.DrupalDbDump;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.varia.NullAppender;

import com.beust.jcommander.ParameterException;

/**
 * Entry point.
 * 
 * @author schnatterer
 * 
 */
public class RemoteDbDumper {
	private static final String LOG_FILE_PATTERN = "%d{ISO8601} %-5p %m%n";
	private static Logger logger = Logger.getLogger(RemoteDbDumper.class);

	/**
	 * @param args
	 * @throws IOException
	 * @throws JaxenException
	 */
	public static void main(String[] args) throws IOException {
		/* Parse command line arguments/parameter (command line interface) */
		RemoteDbDumperCli cliParams = null;
		try {
			cliParams = RemoteDbDumperCli.readParams(args,
					RemoteDbDumperCli.class.getSimpleName());
		} catch (ParameterException e) {
			// Already logged
			System.exit(1);
		} catch (Throwable e) {
			logger.error("Fatal error occurred during initialization", e);
			System.exit(1);
		}

		if (cliParams != null) {
			/*
			 * Successfully read command line params
			 */
			try {
				File file = new File(cliParams.getOutputDir());
				if (!file.isDirectory()) {
					logger.error("Path is not a directory or does not exist: "
							+ file.getAbsolutePath());
					System.exit(1);
				} else {
					if (cliParams.isQuiet()) {
						switchOffLogging();
					} else {
						// In addition to console output write log to target
						// directory
						setUpFileLogging(file);
					}
					List<String> downloadedFiles = new DrupalDbDump().download(
							cliParams.getUrl(), cliParams.getUser(),
							cliParams.getPassword(), file);
					logger.info("Successfully downloaded "
							+ downloadedFiles.size() + " file(s) "
							+ downloadedFiles.toString() + " to "
							+ file.getAbsolutePath());
				}
			} catch (Throwable t) {
				if (!(t instanceof DbDumpException)) {
					logger.error("Error downloading drupal database: "
							+ t.getMessage());
				} else {
					logger.error(t.getMessage(), t);
				}
				System.exit(1);
			}
		} else {
			logger.error("Unable to read command line arguments");
			System.exit(1);
		}
	}

	private static void setUpFileLogging(File file) {
		FileAppender fa = new FileAppender();
		fa.setName("FileLogger");
		fa.setFile(new File(file, RemoteDbDumper.class.getSimpleName() + ".log")
				.getAbsolutePath());
		fa.setLayout(new PatternLayout(LOG_FILE_PATTERN));
		fa.setThreshold(Level.INFO);
		fa.setAppend(true);
		fa.activateOptions();

		Logger.getRootLogger().addAppender(fa);
	}

	private static void switchOffLogging() {
		Logger.getRootLogger().setLevel(Level.OFF);
		Logger.getRootLogger().removeAllAppenders();
		Logger.getRootLogger().addAppender(new NullAppender());
	}
}
