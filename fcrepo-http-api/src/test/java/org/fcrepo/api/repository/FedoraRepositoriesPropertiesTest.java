package org.fcrepo.api.repository;

import org.fcrepo.FedoraObject;
import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FedoraRepositoriesPropertiesTest {


    private FedoraRepositoriesProperties testObj;
    private NodeService mockNodes;
    private Session mockSession;

    @Before
    public void setUp() throws RepositoryException {
        mockNodes = mock(NodeService.class);
        testObj = new FedoraRepositoriesProperties();
        mockSession = TestHelpers.mockSession(testObj);
        testObj.setNodeService(mockNodes);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
    }


    @Test
    public void testSparqlUpdate() throws RepositoryException, IOException {
        final FedoraObject mockObject = mock(FedoraObject.class);

        when(mockObject.getGraphProblems()).thenReturn(null);
        final InputStream mockStream =
                new ByteArrayInputStream("my-sparql-statement".getBytes());
        when(mockNodes.getObject(mockSession, "/")).thenReturn(mockObject);

        testObj.updateSparql(mockStream);

        verify(mockObject).updateGraph("my-sparql-statement");
        verify(mockSession).save();
        verify(mockSession).logout();
    }
}