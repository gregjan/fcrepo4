/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration.kernel.services;

import static com.google.common.io.Files.createTempDir;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.update.UpdateAction.parseExecute;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_PREFIX;
import static org.jgroups.util.Util.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.inject.Inject;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.fcrepo.integration.kernel.AbstractIT;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.RepositoryService;
import org.junit.Test;
import org.modeshape.jcr.api.Problems;
import org.springframework.test.context.ContextConfiguration;

import com.hp.hpl.jena.query.Dataset;

@ContextConfiguration({"/spring-test/repo.xml"})
public class RepositoryServiceImplIT extends AbstractIT {

    @Inject
    private Repository repository;

    @Inject
    RepositoryService repositoryService;

    @Inject
    DatastreamService datastreamService;

    private static final IdentifierTranslator idTranslator = new DefaultIdentifierTranslator();

    @Test
    public void testGetAllObjectsDatastreamSize() throws Exception {
        Session session = repository.login();

        final double originalSize = repositoryService.getRepositorySize();

        datastreamService.createDatastream(session,
                "testObjectServiceNode", "application/octet-stream",
                null, new ByteArrayInputStream("asdf".getBytes()));
        session.save();
        session.logout();

        session = repository.login();

        final double afterSize = repositoryService.getRepositorySize();

        assertEquals(4.0, afterSize - originalSize);

        session.logout();
    }

    @Test
    public void testGetNamespaceRegistryGraph() throws Exception {
        final Session session = repository.login();

        final Dataset registryGraph = repositoryService.getNamespaceRegistryDataset(session, idTranslator);

        final NamespaceRegistry namespaceRegistry =
            session.getWorkspace().getNamespaceRegistry();

        logger.info(namespaceRegistry.toString());
        logger.info(registryGraph.toString());
        for (final String s : namespaceRegistry.getPrefixes()) {
            if (s.isEmpty() || s.equals("xmlns") || s.equals("jcr")) {
                continue;
            }
            final String uri = namespaceRegistry.getURI(s);
            assertTrue("expected to find JCR namespaces " + s + " in graph", registryGraph.asDatasetGraph().contains(
                    ANY, createResource(uri).asNode(), HAS_NAMESPACE_PREFIX.asNode(), createPlainLiteral(s).asNode()));
        }
        session.logout();
    }

    @Test
    public void testUpdateNamespaceRegistryGraph() throws Exception {
        final Session session = repository.login();

        final Dataset registryGraph = repositoryService.getNamespaceRegistryDataset(session, idTranslator);
        final NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();

        parseExecute("INSERT { <info:abc> <" + HAS_NAMESPACE_PREFIX.toString() + "> \"abc\" } WHERE { }", registryGraph);

        assertEquals("abc", namespaceRegistry.getPrefix("info:abc"));
        session.logout();
    }

    @Test
    public void testBackupRepository() throws Exception {
        final Session session = repository.login();

        datastreamService.createDatastream(session, "testObjectServiceNode0", "application/octet-stream", null,
                new ByteArrayInputStream("asdfx".getBytes()));
        session.save();

        final File backupDirectory = createTempDir();

        final Problems problems = repositoryService.backupRepository(session, backupDirectory);

        assertFalse(problems.hasProblems());
        session.logout();
    }

    @Test
    public void testRestoreRepository() throws Exception {
        final Session session = repository.login();

        datastreamService.createDatastream(session, "testObjectServiceNode1", "application/octet-stream", null,
                new ByteArrayInputStream("asdfy".getBytes()));
        session.save();

        final File backupDirectory = createTempDir();

        repositoryService.backupRepository(session, backupDirectory);

        final Problems problems = repositoryService.restoreRepository(session, backupDirectory);

        assertFalse(problems.hasProblems());
        session.logout();
    }
}
