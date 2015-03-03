/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.metadata.web.resources;

import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.hadoop.metadata.ITypedReferenceableInstance;
import org.apache.hadoop.metadata.Referenceable;
import org.apache.hadoop.metadata.json.Serialization$;
import org.apache.hadoop.metadata.json.TypesSerialization;
import org.apache.hadoop.metadata.types.ClassType;
import org.apache.hadoop.metadata.types.DataTypes;
import org.apache.hadoop.metadata.types.HierarchicalTypeDefinition;
import org.apache.hadoop.metadata.types.Multiplicity;
import org.apache.hadoop.metadata.types.StructTypeDefinition;
import org.apache.hadoop.metadata.types.TraitType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import scala.actors.threadpool.Arrays;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


public class MetadataDiscoveryResourceIT extends BaseResourceIT {

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

        createTypes();
        submitTypes();
        ITypedReferenceableInstance instance = createInstance();
        String instanceAsJSON = Serialization$.MODULE$.toJson(instance);
        submitEntity(instanceAsJSON);
    }

    @Test
    public void testSearchByDSL() throws Exception {
        String dslQuery = "from dsl_test_type";
        WebResource resource = service
                .path("api/metadata/discovery/search/dsl")
                .queryParam("query", dslQuery);

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        Assert.assertEquals(clientResponse.getStatus(), Response.Status.OK.getStatusCode());

        String responseAsString = clientResponse.getEntity(String.class);
        Assert.assertNotNull(responseAsString);

        JSONObject response = new JSONObject(responseAsString);
        Assert.assertNotNull(response.get("requestId"));

        Assert.assertEquals(response.getString("query"), dslQuery);
        Assert.assertEquals(response.getString("queryType"), "dsl");

        JSONObject results = response.getJSONObject("results");
        Assert.assertNotNull(results);

        JSONArray rows = results.getJSONArray("rows");
        Assert.assertEquals(rows.length(), 1);
    }

    @Test
    public void testSearchByDSLForUnknownType() throws Exception {
        String dslQuery = "from blah";
        WebResource resource = service
                .path("api/metadata/discovery/search/dsl")
                .queryParam("query", dslQuery);

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        Assert.assertEquals(clientResponse.getStatus(),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testSearchUsingGremlin() throws Exception {
         String query = "g.V.has('type', 'dsl_test_type').toList()";
        WebResource resource = service
                .path("api/metadata/discovery/search")
                .queryParam("query", query);

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        Assert.assertEquals(clientResponse.getStatus(), Response.Status.OK.getStatusCode());

        String responseAsString = clientResponse.getEntity(String.class);
        Assert.assertNotNull(responseAsString);

        JSONObject response = new JSONObject(responseAsString);
        Assert.assertNotNull(response.get("requestId"));

        Assert.assertEquals(response.getString("query"), query);
        Assert.assertEquals(response.getString("queryType"), "gremlin");
    }

    @Test
    public void testSearchUsingDSL() throws Exception {
        String query = "from dsl_test_type";
        WebResource resource = service
                .path("api/metadata/discovery/search")
                .queryParam("query", query);

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        Assert.assertEquals(clientResponse.getStatus(), Response.Status.OK.getStatusCode());

        String responseAsString = clientResponse.getEntity(String.class);
        Assert.assertNotNull(responseAsString);

        JSONObject response = new JSONObject(responseAsString);
        Assert.assertNotNull(response.get("requestId"));

        Assert.assertEquals(response.getString("query"), query);
        Assert.assertEquals(response.getString("queryType"), "dsl");
    }

    @Test
    public void testFullTextUriExists() throws Exception {
        WebResource resource = service
                .path("api/metadata/discovery/search/fulltext")
                .queryParam("depth", "0").queryParam("text", "foo").queryParam("property", "Name");

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        Assert.assertNotEquals(clientResponse.getStatus(),
                Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testGetIndexedProperties() throws Exception {
        WebResource resource = service
                .path("api/metadata/discovery/getIndexedFields");

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        Assert.assertNotEquals(clientResponse.getStatus(),
                Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testLineageUriExists() throws Exception {
        WebResource resource = service
                .path("api/metadata/discovery/search/relationships/1")
                .queryParam("depth", "1").queryParam("edgesToFollow", "bar");

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        Assert.assertNotEquals(clientResponse.getStatus(),
                Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test(dependsOnMethods = "testFullTextUriExists")
    public void testSearchForText() throws Exception {
        WebResource resource = service
                .path("api/metadata/discovery/search/fulltext")
                .queryParam("depth", "3").queryParam("text", "bar")
                .queryParam("property", "hive_table.name");

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);

        //TODO - Assure zero vertices and edges.        
        Assert.assertNotEquals(clientResponse.getStatus(),
                Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test(dependsOnMethods = "testUriExists", enabled = false)
    public void testSearchForTextNoDepth() throws Exception {
        WebResource resource = service
                .path("api/metadata/discovery/search/fulltext")
                .queryParam("depth", "0").queryParam("text", "kid").queryParam("property", "Name");

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        // TODO - Get count of expected vertices and Edges
        // Assert.assertEquals(true, true);
    }

    @Test(dependsOnMethods = "testUriExists", enabled = false)
    public void testSearchTextWithDepth() throws Exception {
        WebResource resource = service
                .path("api/metadata/discovery/search/fulltext")
                .queryParam("depth", "4").queryParam("text", "Grandad")
                .queryParam("property", "Name");

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        // TODO - Get count of expected vertices and Edges
        // Assert.assertEquals(true, true);
    }

    private void submitEntity(String instanceAsJSON) throws JSONException {
        WebResource resource = service
                .path("api/metadata/entities/submit")
                .path("dsl_test_type");

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.POST, ClientResponse.class, instanceAsJSON);
        Assert.assertEquals(clientResponse.getStatus(), Response.Status.OK.getStatusCode());

        String responseAsString = clientResponse.getEntity(String.class);
        Assert.assertNotNull(responseAsString);

        JSONObject response = new JSONObject(responseAsString);
        Assert.assertNotNull(response.get("requestId"));

        String guid = response.get("GUID").toString();
        Assert.assertNotNull(guid);
    }

    private void createTypes() throws Exception {
        HierarchicalTypeDefinition<ClassType> dslTestTypeDefinition =
                createClassTypeDef("dsl_test_type",
                        ImmutableList.<String>of(),
                        createUniqueRequiredAttrDef("name", DataTypes.STRING_TYPE),
                        createRequiredAttrDef("description", DataTypes.STRING_TYPE));

        typeSystem.defineTypes(
                ImmutableList.<StructTypeDefinition>of(),
                ImmutableList.<HierarchicalTypeDefinition<TraitType>>of(),
                ImmutableList.of(dslTestTypeDefinition));
    }

    private void submitTypes() throws Exception {
        @SuppressWarnings("unchecked")
        String typesAsJSON = TypesSerialization.toJson(typeSystem,
                Arrays.asList(new String[]{
                        "dsl_test_type",
                }));
        sumbitType(typesAsJSON, "dsl_test_type");
    }

    private ITypedReferenceableInstance createInstance() throws Exception {
        Referenceable entityInstance = new Referenceable("dsl_test_type");
        entityInstance.set("name", "foo name");
        entityInstance.set("description", "bar description");

        ClassType tableType = typeSystem.getDataType(ClassType.class, "dsl_test_type");
        return tableType.convert(entityInstance, Multiplicity.REQUIRED);
    }
}