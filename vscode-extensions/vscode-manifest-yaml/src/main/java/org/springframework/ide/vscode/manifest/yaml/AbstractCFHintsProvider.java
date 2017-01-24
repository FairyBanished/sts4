/*******************************************************************************
 * Copyright (c) 2017 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.manifest.yaml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.ide.vscode.commons.cloudfoundry.client.cftarget.CFTarget;
import org.springframework.ide.vscode.commons.cloudfoundry.client.cftarget.CFTargetCache;
import org.springframework.ide.vscode.commons.cloudfoundry.client.cftarget.NoTargetsException;
import org.springframework.ide.vscode.commons.util.Assert;
import org.springframework.ide.vscode.commons.util.ExceptionUtil;
import org.springframework.ide.vscode.commons.util.ValueParseException;
import org.springframework.ide.vscode.commons.yaml.schema.YValueHint;

public abstract class AbstractCFHintsProvider implements Callable<Collection<YValueHint>> {

	public static final String EMPTY_VALUE = "";
	protected final CFTargetCache targetCache;

	private static final Logger logger = Logger.getLogger(AbstractCFHintsProvider.class.getName());

	public AbstractCFHintsProvider(CFTargetCache targetCache) {
		Assert.isNotNull(targetCache);
		this.targetCache = targetCache;
	}

	@Override
	public Collection<YValueHint> call() throws Exception {
		Collection<YValueHint> hints = new ArrayList<>();
		try {
			List<CFTarget> targets = targetCache.getOrCreate();
			Collection<YValueHint> resolvedHints = getHints(targets);
			if (resolvedHints != null) {
				hints.addAll(resolvedHints);
			}
		} catch (Throwable e) {
			// Convert any error into something readable to the user as it may
			// appear in the content assist
			// UI. Do NOT wrap the original exception as the framework may look
			// for the deepest cause when
			// resolving the error message. Instead, log the full error, and
			// only throw a
			// new exception with a "nicer" message
			Throwable noTargetsError = ExceptionUtil.getThrowable(e, NoTargetsException.class);
			if (noTargetsError != null) {
				// Do not log the no-targets exception as it may be encountered
				// frequently
				// if a user does not have a CF client installed
				throw new ValueParseException(ExceptionUtil.getMessageNoAppendedInformation(noTargetsError));
			} else {
				// Log any other error
				logger.log(Level.SEVERE, ExceptionUtil.getMessage(e), e);

				if (ExceptionUtil.getThrowable(e, IOException.class) != null) {
					throw new ValueParseException(
							"Connection failure to Cloud Foundry. Please check the log for more details.");

				} else {
					throw new ValueParseException(
							"Failed to fetch values from Cloud Foundry. Please check the log for more details.");
				}
			}
		}
		return hints;
	}

	/**
	 * 
	 * @return non-null list of hints. Return empty if no hints available
	 */
	abstract protected Collection<YValueHint> getHints(List<CFTarget> targets) throws Exception;

}
