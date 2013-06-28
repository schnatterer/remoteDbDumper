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

package info.schnatterer.remoteDbDumper.dump.util;

import info.schnatterer.remoteDbDumper.dump.DbDumpException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.attachment.Attachment;
import com.gargoylesoftware.htmlunit.attachment.CollectingAttachmentHandler;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

/**
 * Helpers to facilitate working with HtmlUnit.
 * 
 * @author schnatterer
 * 
 */
public final class HtmlUnitUtils {
	private HtmlUnitUtils() {
	}

	private static Logger logger = Logger.getLogger(HtmlUnitUtils.class);

	public static Page getButtonAndClick(String buttonDescription,
			String buttonName, HtmlForm form) throws DbDumpException {
		HtmlInput input = form.getInputByName(buttonName);
		if (input == null) {
			throw new DbDumpException("Unable to find " + buttonDescription
					+ " button with id \"" + buttonName + "\"", form);
		}
		if (!(input instanceof HtmlSubmitInput)) {
			throw new DbDumpException(buttonDescription
					+ " button is not a submit " + "  name \"" + buttonName
					+ "\"", form);
		}
		final HtmlSubmitInput button = (HtmlSubmitInput) input;

		try {
			logger.debug("Clicking " + buttonDescription + " button on form: "
					+ form);
			// Page newPage = button.click();
			// if (!(newPage instanceof HtmlPage)) {
			// throw new DbDumpException("Unexpected response on clicking "
			// + buttonDescription + " button. Code "
			// + newPage.getWebResponse().getStatusCode() + ". Type "
			// + newPage.getClass().getSimpleName(), form);
			// }
			// return (HtmlPage) newPage;
			return button.click();
		} catch (IOException e) {
			throw new DbDumpException("I/O Error clicking download button", e);
		}
	}

	public static WebClient createWebClient(List<Attachment> attachments) {
		WebClient webClient = new WebClient();
		// webClient.getOptions().setJavaScriptEnabled(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setPopupBlockerEnabled(true);
		// webClient.getOptions().setPrintContentOnFailingStatusCode(false);
		// webClient.getOptions().setRedirectEnabled(true);
		// webClient.setStatusHandler(new StatusHandler() {
		//
		// public void statusMessageChanged(Page page, String message) {
		// logger.debug("status: " + message);
		// }
		// });
		//
		// webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {
		//
		// public void timeoutError(HtmlPage htmlPage, long allowedTime,
		// long executionTime) {
		// logger.trace("timeoutError on page " + htmlPage);
		// }
		//
		// public void scriptException(HtmlPage htmlPage,
		// ScriptException scriptException) {
		// logger.trace("scriptException: " + scriptException);
		//
		// }
		//
		// public void malformedScriptURL(HtmlPage htmlPage, String url,
		// MalformedURLException malformedURLException) {
		// logger.trace("malformedURLException: " + malformedURLException);
		//
		// }
		//
		// public void loadScriptError(HtmlPage htmlPage, URL scriptUrl,
		// Exception exception) {
		// logger.trace("loadScriptError: " + exception);
		// }
		// });
		//
		// webClient.setAttachmentHandler(new AttachmentHandler() {
		// public void handleAttachment(Page arg0) {
		// logger.trace(arg0.getUrl());
		//
		// }
		// });
		//
		// webClient.setCssErrorHandler(new ErrorHandler() {
		//
		// public void warning(CSSParseException exception)
		// throws CSSException {
		// logger.trace("css warning: " + exception);
		// }
		//
		// public void fatalError(CSSParseException exception)
		// throws CSSException {
		// logger.trace("css fatalError: " + exception);
		// }
		//
		// public void error(CSSParseException exception) throws CSSException {
		// logger.trace("css error: " + exception);
		// }
		// });

		webClient.setAttachmentHandler(new CollectingAttachmentHandler(
				attachments));
		return webClient;
	}

	public static List<String> downloadAttachments(
			List<Attachment> attachments, File targetDir)
			throws DbDumpException {
		List<String> successfullyDownloaded = new LinkedList<String>();
		if (attachments.size() < 1) {
			throw new DbDumpException(
					"Clicking the button did not offer a file to download");
		}
		if (attachments.size() > 1) {
			logger.info("More than one attachment returned, trying to download all");
		}

		for (Attachment attachment : attachments) {
			String fileName = attachment.getSuggestedFilename();
			logger.debug("Downloading file " + fileName);

			InputStream is = null;
			OutputStream out = null;
			try {
				is = attachment.getPage().getWebResponse().getContentAsStream();
				File newFile = null;
				try {

					newFile = new File(targetDir, fileName);
					out = new FileOutputStream(newFile);

					int read = 0;
					byte[] bytes = new byte[1024];

					while ((read = is.read(bytes)) != -1) {
						out.write(bytes, 0, read);
					}
					successfullyDownloaded.add(fileName);
				} catch (IOException e) {
					throw new DbDumpException("Unable to write to file "
							+ targetDir.getAbsolutePath() + fileName, e);
				}
			} catch (IOException e1) {
				throw new DbDumpException(
						"Unable to open stream to attachment " + fileName, e1);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						logger.error("Unable to close input stream to file"
								+ fileName);
					}
				}
				if (out != null) {
					try {
						out.flush();
						out.close();
					} catch (IOException e) {
						logger.error("Unable to flush and close stream to target file"
								+ targetDir.getAbsolutePath() + fileName);
					}
				}
			}
		}
		return successfullyDownloaded;
	}
}
