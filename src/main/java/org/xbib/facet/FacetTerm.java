/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.xbib.facet;

/**
 * Facet term
 *
 * @author <a href="mailto:joergprante@gmail.com">J&ouml;rg Prante</a>
 */
public class FacetTerm {

    private String actualTerm;

    private String query;

    private String requestUrl;

    private Long count;

    public FacetTerm(String actualTerm, long count, String query, String requestUrl) {
        this.actualTerm = actualTerm;
        this.query = query;
        this.requestUrl = requestUrl;
        this.count = count;
    }

    public String getActualTerm() {
        return actualTerm;
    }

    public String getQuery() {
        return query;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public Long getCount() {
        return count;
    }

}
