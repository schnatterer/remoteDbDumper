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

package info.schnatterer.remoteDbDumper.dump.drupal;

import static info.schnatterer.remoteDbDumper.dump.util.HtmlUnitUtils.createWebClient;
import static info.schnatterer.remoteDbDumper.dump.util.HtmlUnitUtils.downloadAttachments;
import static info.schnatterer.remoteDbDumper.dump.util.HtmlUnitUtils.getButtonAndClick;
import info.schnatterer.remoteDbDumper.dump.DbDump;
import info.schnatterer.remoteDbDumper.dump.DbDumpException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.attachment.Attachment;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

/**
 * Implementation of dumping a database by using the drupal plugin <a
 * href="https://drupal.org/project/backup_migrate">Backup and Migrate</a>.
 * 
 * @author schnatterer
 * 
 */
public class DrupalDbDump implements DbDump {
	private static final String NAME_INPUT_LOGIN_PASS = "pass";
	private static final String NAME_INPUT_LOGIN_NAME = "name";
	private static final String XPATH_FORM_LOGIN = "//form[@id='user-login-form']";
	private static final String DESCR_BUTTON_LOGIN = "login";
	private static final String DESCR_BUTTON_BACKUP = "backup";
	private static final String NAME_BUTTON_BACKUP = "op";

	private static final String NAME_BUTTON_LOGIN = "op";

	private static final String XPATH_FORM_ID_BACKUP_MIGRATE = "//form[@id='backup-migrate-ui-manual-quick-backup-form']";
	private static final String ID_SELECT_PROFILE = "profile_id";
	private static final String ID_SELECT_DESTINATION = "destination_id";
	private static final String ID_SELECT_SOURCE = "source_id";
	private static final String VALUE_SELECT_PROFILE = "default";
	private static final String VALUE_SELECT_DESTINATION = "download";
	private static final String VALUE_SELECT_SOURCE = "db";

	private Logger logger = Logger.getLogger(this.getClass());

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * info.schnatterer.remoteDbDump.backup.drupal.DbDump#download(java.lang
	 * .String, java.lang.String, java.lang.String, java.io.File)
	 */
	public List<String> download(String url, String user, String password,
			File file) throws DbDumpException {
		final List<Attachment> attachments = new ArrayList<Attachment>();
		final WebClient webClient = createWebClient(attachments);

		logger.info("Connecting to " + url);
		HtmlPage page;
		try {
			page = webClient.getPage(url);
			int statusCode = page.getWebResponse().getStatusCode();
			if (statusCode == 403) {
				WebWindow currentWindow = webClient.getCurrentWindow()
						.getTopWindow();

				// Page pageFocusAfterLogin = login(page, user, password);
				// statusCode =
				// pageFocusAfterLogin.getWebResponse().getStatusCode();

				/*
				 * Login might return a page other then the one we want (e.g. ad
				 * popup). So make sure to continue with the page contained in
				 * the original window.
				 */
				login(page, user, password);
				Page loggedInPage = currentWindow.getEnclosedPage();
				if (!(loggedInPage instanceof HtmlPage)) {
					throw new DbDumpException(
							"Login returned unexpected page format. Expected HtmlPage. Page: "
									+ page);
				}
				page = (HtmlPage) loggedInPage;
				statusCode = loggedInPage.getWebResponse().getStatusCode();
				if (statusCode != 200) {
					throw new DbDumpException("Login failed - status code "
							+ statusCode, page);
				}
				logger.info("Successfully logged in to \"" + page.getUrl()
						+ "\" as \"" + user + "\"");
			} else if (statusCode != 200) {
				throw new DbDumpException("Connection failed with code "
						+ statusCode, page);
			} else {
				logger.info("No log in required, trying to continue without logging in");
			}

			clickDownloadButton(page);
			return downloadAttachments(attachments, file);
		} catch (FailingHttpStatusCodeException e) {
			throw new DbDumpException("Error connecting, HTTP status code: "
					+ e.getStatusCode(), e);
		} catch (MalformedURLException e) {
			throw new DbDumpException(
					"Error connecting, malformed URL: " + url, e);
		} catch (IOException e) {
			throw new DbDumpException("Error connecting, IO Exception.", e);
		} finally {
			webClient.closeAllWindows();
		}
	}

	private Page clickDownloadButton(HtmlPage page) throws DbDumpException {
		// POST params
		// source_id=db
		// destination_id=download
		// profile_id=default
		// op=Backup+now
		// form_id=backup_migrate_ui_manual_quick_backup_form
		// form_build_id=<generated id>
		// form_token=<generated token>

		// Get the form that we are dealing with and within that form,
		// find the submit button and the field that we want to change.

		final HtmlForm backupForm = page
				.getFirstByXPath(XPATH_FORM_ID_BACKUP_MIGRATE);

		if (backupForm == null) {
			throw new DbDumpException("Unable to find backup form", page);
		}
		final HtmlSelect selectSource = backupForm
				.getSelectByName(ID_SELECT_SOURCE);
		if (selectSource == null) {
			throw new DbDumpException("Unable to find source select in form",
					page);
		}
		selectSource.setSelectedAttribute(VALUE_SELECT_SOURCE, true);

		final HtmlSelect selectDest = backupForm
				.getSelectByName(ID_SELECT_DESTINATION);
		if (selectDest == null) {
			throw new DbDumpException(
					"Unable to find destination select in form", page);
		}
		selectDest.setSelectedAttribute(VALUE_SELECT_DESTINATION, true);

		final HtmlSelect selectProfile = backupForm
				.getSelectByName(ID_SELECT_PROFILE);
		if (selectProfile == null) {
			throw new DbDumpException("Unable to find profile select in form",
					page);
		}
		selectProfile.setSelectedAttribute(VALUE_SELECT_PROFILE, true);

		// Now submit the form by clicking the button and get back the second
		// page.
		// final HtmlSubmitInput button = (HtmlSubmitInput)
		// form.getInputByName("edit-submit");
		return getButtonAndClick(DESCR_BUTTON_BACKUP, NAME_BUTTON_BACKUP,
				backupForm);
	}

	private Page login(HtmlPage page, String user, String password)
			throws DbDumpException {
		// POST params:
		// name=<user>
		// pass=<password>
		// op=...
		// form_id=user_login_block
		// form_build_id=<generated id>

		// Get the form that we are dealing with and within that form,
		// find the submit button and the field that we want to change.
		// final HtmlForm form = page.getFormByName("user-login-form");
		final HtmlForm form = page.getFirstByXPath(XPATH_FORM_LOGIN);
		if (form == null) {
			throw new DbDumpException("Unable to find login form", page);
		}

		final HtmlTextInput inputName = form
				.getInputByName(NAME_INPUT_LOGIN_NAME);
		if (inputName == null) {
			throw new DbDumpException("Unable to find input for login name",
					page);
		}
		inputName.setValueAttribute(user);

		final HtmlPasswordInput inputPass = form
				.getInputByName(NAME_INPUT_LOGIN_PASS);
		if (inputPass == null) {
			throw new DbDumpException(
					"Unable to find input for login password", page);
		}
		inputPass.setValueAttribute(password);

		return getButtonAndClick(DESCR_BUTTON_LOGIN, NAME_BUTTON_LOGIN, form);
	}
}
