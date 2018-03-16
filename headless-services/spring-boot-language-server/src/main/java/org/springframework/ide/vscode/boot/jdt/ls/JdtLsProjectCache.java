/*******************************************************************************
 * Copyright (c) 2018 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.jdt.ls;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ide.vscode.commons.jandex.JandexClasspath;
import org.springframework.ide.vscode.commons.java.ClasspathData;
import org.springframework.ide.vscode.commons.java.IClasspath;
import org.springframework.ide.vscode.commons.java.IJavaProject;
import org.springframework.ide.vscode.commons.java.IJavadocProvider;
import org.springframework.ide.vscode.commons.languageserver.java.JavaProjectFinder;
import org.springframework.ide.vscode.commons.languageserver.java.ProjectObserver;
import org.springframework.ide.vscode.commons.languageserver.jdt.ls.ClasspathListener;
import org.springframework.ide.vscode.commons.languageserver.util.SimpleLanguageServer;
import org.springframework.ide.vscode.commons.util.CollectorUtil;

import com.google.common.collect.ImmutableList;

import reactor.core.Disposable;

public class JdtLsProjectCache implements JavaProjectFinder, ProjectObserver {

	private SimpleLanguageServer server;
	private Map<String, JdtLsProject> table = new HashMap<String, JdtLsProject>();
	private Logger log = LoggerFactory.getLogger(JdtLsProjectCache.class);

	public JdtLsProjectCache(SimpleLanguageServer server) {
		this.server = server;
		CompletableFuture<Disposable> disposable = new CompletableFuture<Disposable>();
		this.server.onInitialized(() -> 
			disposable.complete(server.addClasspathListener(new ClasspathListener() {
				@Override
				public void changed(Event event) {
					log.info("Classpath changed: "+ event.projectUri);
				}
			}))
		);
		this.server.onShutdown(() -> 
			disposable.thenAccept(Disposable::dispose)
		);
	}

	@Override
	public void addListener(Listener listener) {
//		throw new Error("not implemented yet");

	}

	@Override
	public void removeListener(Listener listener) {
//		throw new Error("not implemented yet");

	}

	@Override
	public Optional<IJavaProject> find(TextDocumentIdentifier doc) {
		return Optional.empty();
	}
	
	private class JdtLsProject implements IJavaProject {

		private final IClasspath classpath;

		public JdtLsProject(String name, String uri, Classpath jdtClasspath) {
			this.classpath = new JdtClasspath(name, uri, jdtClasspath);
		}

		@Override
		public IClasspath getClasspath() {
			return classpath;
		}

	}
	
	private class JdtClasspath extends JandexClasspath {

		private Classpath classpath;
		private String name;
		private String projectUri;


		public JdtClasspath(String name, String uri, Classpath classpath) {
			this.name = name;
			this.projectUri = uri;
			this.classpath = classpath;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean exists() {
			return new File(projectUri).exists();
		}

		@Override
		public Path getOutputFolder() {
			return Paths.get(classpath.getDefaultOutputFolder());
		}

		@Override
		public ImmutableList<Path> getClasspathEntries() throws Exception {
			return classpath
					.getEntries()
					.stream()
					.filter(cpe -> cpe.getKind().equals(Classpath.ENTRY_KIND_BINARY))
					.map(cpe -> Paths.get(cpe.getPath()))
					.collect(CollectorUtil.toImmutableList());
		}

		@Override
		public ImmutableList<String> getClasspathResources() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ImmutableList<String> getSourceFolders() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ClasspathData createClasspathData() throws Exception {
			// We should not be needing this as we dont use DelegatingCachedClasspath
			throw new UnsupportedOperationException("Not supported for JDT classpath: ");
		}

		@Override
		public Optional<URL> sourceContainer(File classpathResource) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected IJavadocProvider createHtmlJavdocProvider(File classpathResource) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}
