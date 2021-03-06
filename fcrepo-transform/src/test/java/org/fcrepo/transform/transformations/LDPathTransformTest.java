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
package org.fcrepo.transform.transformations;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.util.Symbol;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.WebApplicationException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import static org.fcrepo.transform.transformations.LDPathTransform.CONFIGURATION_FOLDER;
import static org.fcrepo.transform.transformations.LDPathTransform.getNodeTypeTransform;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class LDPathTransformTest {

    @Mock
    Node mockNode;

    @Mock
    Session mockSession;

    LDPathTransform testObj;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        when(mockNode.getSession()).thenReturn(mockSession);
    }

    @Test(expected=WebApplicationException.class)
    public void testGetNodeTypeSpecificLdpathProgramForMissingProgram() throws RepositoryException {
        final Node mockConfigNode = mock(Node.class);
        when(mockSession.getNode(CONFIGURATION_FOLDER + "some-program")).thenReturn(mockConfigNode);

        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        final NodeType mockNodeType = mock(NodeType.class);
        final NodeType mockNtBase = mock(NodeType.class);
        when(mockNodeType.getSupertypes()).thenReturn(new NodeType[] { mockNtBase });
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        getNodeTypeTransform(mockNode, "some-program");
    }

    @Test
    public void testGetNodeTypeSpecificLdpathProgramForNodeTypeProgram() throws RepositoryException {
        final Node mockConfigNode = mock(Node.class);
        final Node mockTypeConfigNode = mock(Node.class, RETURNS_DEEP_STUBS);
        when(mockSession.getNode(CONFIGURATION_FOLDER + "some-program")).thenReturn(mockConfigNode);

        final NodeType mockNodeType = mock(NodeType.class);
        final NodeType mockNtBase = mock(NodeType.class);
        when(mockNodeType.getSupertypes()).thenReturn(new NodeType[] { mockNtBase });
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        when(mockNodeType.toString()).thenReturn("custom:type");
        when(mockConfigNode.hasNode("custom:type")).thenReturn(true);
        when(mockConfigNode.getNode("custom:type")).thenReturn(mockTypeConfigNode);
        final InputStream mockInputStream = mock(InputStream.class);
        when(mockTypeConfigNode.getNode("jcr:content").getProperty("jcr:data").getBinary().getStream()).thenReturn(mockInputStream);
        final LDPathTransform nodeTypeSpecificLdpathProgramStream = getNodeTypeTransform(mockNode, "some-program");

        assertEquals(new LDPathTransform(mockInputStream), nodeTypeSpecificLdpathProgramStream);
    }

    @Test
    public void testGetNodeTypeSpecificLdpathProgramForSupertypeProgram() throws RepositoryException {
        final Node mockConfigNode = mock(Node.class);
        final Node mockTypeConfigNode = mock(Node.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockSession.getNode(LDPathTransform.CONFIGURATION_FOLDER + "some-program")).thenReturn(mockConfigNode);

        final NodeType mockNodeType = mock(NodeType.class);
        final NodeType mockNtBase = mock(NodeType.class);
        when(mockNodeType.getSupertypes()).thenReturn(new NodeType[] { mockNtBase });
        when(mockNodeType.toString()).thenReturn("custom:type");
        when(mockNtBase.toString()).thenReturn("nt:base");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(mockConfigNode.hasNode("custom:type")).thenReturn(false);

        when(mockConfigNode.hasNode("nt:base")).thenReturn(true);
        when(mockConfigNode.getNode("nt:base")).thenReturn(mockTypeConfigNode);

        final InputStream mockInputStream = mock(InputStream.class);
        when(mockTypeConfigNode.getNode("jcr:content").getProperty("jcr:data").getBinary().getStream()).thenReturn(mockInputStream);

        final LDPathTransform nodeTypeSpecificLdpathProgramStream = LDPathTransform.getNodeTypeTransform(mockNode, "some-program");

        assertEquals(new LDPathTransform(mockInputStream), nodeTypeSpecificLdpathProgramStream);
    }

    @Test
    public void testProgramQuery() {

        final Model model = ModelFactory.createDefaultModel();
        model.add(model.createResource("abc"), model.createProperty("http://purl.org/dc/elements/1.1/title"), model.createLiteral("some-title"));
        final Dataset testDataset = DatasetFactory.create(model);
        testDataset.getContext().set(Symbol.create("uri"), "abc");
        final InputStream testReader = new ByteArrayInputStream("title = dc:title :: xsd:string ;".getBytes());

        testObj = new LDPathTransform(testReader);
        final Map<String,Collection<Object>> stringCollectionMap = testObj.apply(testDataset);

        assert(stringCollectionMap != null);
        assertEquals(1, stringCollectionMap.size());
        assertEquals(1, stringCollectionMap.get("title").size());
        assertTrue(stringCollectionMap.get("title").contains("some-title"));
    }
}
