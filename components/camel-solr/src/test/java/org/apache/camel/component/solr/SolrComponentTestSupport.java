/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.solr;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.apache.camel.builder.RouteBuilder;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SolrComponentTestSupport extends SolrTestSupport {
    protected static final String TEST_ID = "test1";
    protected static final String TEST_ID2 = "test2";
   
    private SolrFixtures solrFixtures;
   

    protected void solrInsertTestEntry() {
        solrInsertTestEntry(TEST_ID);
    }
    
    protected static Collection secureOrNot() {
    	return Arrays.asList(new Object[][] {{true}, {false}});
    }
    
    public SolrComponentTestSupport(Boolean useHttps) {
    	this.solrFixtures = new SolrFixtures(useHttps);
    }
    
    String solrRouteUri() {
    	return solrFixtures.solrRouteUri();
    }

    protected void solrInsertTestEntry(String id) {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(SolrConstants.OPERATION, SolrConstants.OPERATION_INSERT);
        headers.put("SolrField.id", id);
        template.sendBodyAndHeaders("direct:start", "", headers);
    }

    protected void solrCommit() {
        template.sendBodyAndHeader("direct:start", "", SolrConstants.OPERATION, SolrConstants.OPERATION_COMMIT);
    }

    protected QueryResponse executeSolrQuery(String query) throws SolrServerException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        SolrServer solrServer = solrFixtures.getServer();
        return solrServer.query(solrQuery);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        SolrFixtures.createSolrFixtures();
    }
 
    @AfterClass
    public static void afterClass() throws Exception {
    	SolrFixtures.teardownSolrFixtures();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to(solrRouteUri());
                from("direct:splitThenCommit")
                    .split(body())
                        .to(solrRouteUri())
                    .end()
                    .setHeader(SolrConstants.OPERATION, constant(SolrConstants.OPERATION_COMMIT))
                    .to(solrRouteUri());
            }
        };
    }
    
    @Parameters
    public static Collection<Object[]> serverTypes() {
    	Object[][] serverTypes = {{true}, {false}};
    	return Arrays.asList(serverTypes);
    }

    @Before
    public void clearIndex() throws Exception {
    	solrFixtures.clearIndex();	
    }
}
