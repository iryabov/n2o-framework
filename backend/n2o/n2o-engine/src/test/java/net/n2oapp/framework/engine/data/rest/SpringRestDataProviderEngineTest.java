package net.n2oapp.framework.engine.data.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.metadata.dataprovider.N2oRestDataProvider;
import net.n2oapp.properties.test.TestStaticProperties;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SpringRestDataProviderEngineTest {
    @Test
    public void testSimple() {
        Properties properties = new Properties();
        properties.put("n2o.engine.mapper", "spel");
        new TestStaticProperties().setProperties(properties);

        //самый простой случай
        TestRestTemplate restTemplate = new TestRestTemplate();
        SpringRestDataProviderEngine actionEngine = new SpringRestDataProviderEngine(restTemplate, new ObjectMapper());
        N2oRestDataProvider dataProvider = new N2oRestDataProvider();
        dataProvider.setQuery("http://www.someUrl.org/{id}");
        dataProvider.setMethod(N2oRestDataProvider.Method.POST);
        Map<String, Object> request = new HashMap<>();
        request.put("id", 1);
        actionEngine.invoke(dataProvider, request);
        assertThat(restTemplate.getQuery(), is("http://www.someUrl.org/1"));

        //случай с повторением параметра
        restTemplate = new TestRestTemplate();
        actionEngine = new SpringRestDataProviderEngine(restTemplate, new ObjectMapper());
        dataProvider = new N2oRestDataProvider();
        dataProvider.setQuery("http://www.someUrl.org/{id}/{id}");
        dataProvider.setMethod(N2oRestDataProvider.Method.POST);
        actionEngine.invoke(dataProvider, request);
        assertThat(restTemplate.getQuery(), is("http://www.someUrl.org/1/1"));

    }

    @Test
    public void testDatasetMapper() {
        Properties properties = new Properties();
        properties.put("n2o.engine.mapper", "dataset");
        new TestStaticProperties().setProperties(properties);
        TestRestTemplate restTemplate = new TestRestTemplate();
        SpringRestDataProviderEngine actionEngine = new SpringRestDataProviderEngine(restTemplate, new ObjectMapper());
        N2oRestDataProvider invocation = new N2oRestDataProvider();
        invocation.setQuery("http://www.someUrl.org/{id}");
        invocation.setMethod(N2oRestDataProvider.Method.POST);
        Map<String, Object> request = new HashMap<>();
        request.put("id", 1);
        actionEngine.invoke(invocation, request);
        assertThat(restTemplate.getQuery(), is("http://www.someUrl.org/1"));
        assertThat(restTemplate.getResponseType().equals(DataSet.class), is(true));
    }

    @Test
    public void testReplacePlaceholders() {
        TestRestTemplate restTemplate = new TestRestTemplate();
        SpringRestDataProviderEngine actionEngine = new SpringRestDataProviderEngine(restTemplate, new ObjectMapper());
        N2oRestDataProvider dataProvider = new N2oRestDataProvider();
        dataProvider.setMethod(N2oRestDataProvider.Method.POST);
        dataProvider.setFiltersSeparator("&");
        dataProvider.setJoinSeparator(";");
        dataProvider.setSelectSeparator(";");
        dataProvider.setSortingSeparator("&");
        dataProvider.setQuery("http://www.someUrl.org/findAll;{select};{join}?{filters}&amp;{sorting}&amp;offset={offset}&amp;limit={limit}&amp;count={count}&amp;page={page}");

        Map<String, Object> request = new HashMap<>();
        request.put("select", Arrays.asList("id", "name"));
        request.put("join", Arrays.asList("join=table2", "join=table3"));
        request.put("filters", Arrays.asList("id={id}", "name={name}"));
        request.put("sorting", Arrays.asList("sort=id,{idSortDir}", "sort=name,{nameSortDir}"));
        request.put("limit", 1);
        request.put("offset", 2);
        request.put("count", 3);
        request.put("id", 123);
        request.put("name", "test");
        request.put("idSortDir", "ASC");
        request.put("nameSortDir", "DESC");
        request.put("page", 1);
        actionEngine.invoke(dataProvider, request);
        assertThat(restTemplate.getQuery(), is("http://www.someUrl.org/findAll;id;name;join=table2;join=table3?id=123&name=test&sort=id,ASC&sort=name,DESC&offset=2&limit=1&count=3&page=1"));
        Map<String, Object> body = (Map<String, Object>) restTemplate.getRequestEntity().getBody();
        assertThat(body.get("id"), is(123));
        assertThat(body.get("name"), is("test"));

        restTemplate = new TestRestTemplate();
        actionEngine = new SpringRestDataProviderEngine(restTemplate, new ObjectMapper());
        dataProvider = new N2oRestDataProvider();
        dataProvider.setMethod(N2oRestDataProvider.Method.POST);
        dataProvider.setQuery("http://www.someUrl.org/findAll?{filters}");
        dataProvider.setFiltersSeparator("&");
        request = new HashMap<>();
        request.put("select", Arrays.asList("id", "name"));
        request.put("join", Arrays.asList("join=table2", "join=table3"));
        request.put("filters", Arrays.asList("id={id}", "name={name}"));
        request.put("sorting", Arrays.asList("sort=id,{idSortDir}", "sort=name,{nameSortDir}"));
        request.put("limit", 1);
        request.put("offset", 2);
        request.put("count", 3);
        request.put("id", 123);
        request.put("name", "test");
        request.put("idSortDir", "ASC");
        request.put("nameSortDir", "DESC");
        request.put("page", 1);

        actionEngine.invoke(dataProvider, request);
        assertThat(restTemplate.getQuery(), is("http://www.someUrl.org/findAll?id=123&name=test"));

        body = (Map<String, Object>) restTemplate.getRequestEntity().getBody();
        assertThat(body.get("offset"), is(2));
        assertThat(body.get("idSortDir"), is("ASC"));
        assertThat(body.get("limit"), is(1));
        assertThat(body.get("count"), is(3));
        assertThat(body.get("name"), is("test"));
        assertThat(body.get("nameSortDir"), is("DESC"));
        assertThat(body.get("id"), is(123));
        assertThat(body.get("page"), is(1));
    }

    @Test
    public void testBaseUrl() {
        DataSet res = new DataSet();
        res.put("id", 1);
        res.put("name", "test");
        TestRestTemplate restTemplate = new TestRestTemplate();
        SpringRestDataProviderEngine actionEngine = new SpringRestDataProviderEngine(restTemplate, new ObjectMapper());
        actionEngine.setBaseRestUrl("http://localhost:8080");
        N2oRestDataProvider dataProvider = new N2oRestDataProvider();
        dataProvider.setMethod(N2oRestDataProvider.Method.POST);
        dataProvider.setQuery("/findAll");
        Map<String, Object> request = new HashMap<>();
        actionEngine.invoke(dataProvider, request);
        assertThat(restTemplate.getQuery(), is("http://localhost:8080/findAll"));

        //случай без / в url
        restTemplate = new TestRestTemplate();
        actionEngine = new SpringRestDataProviderEngine(restTemplate, new ObjectMapper());
        dataProvider = new N2oRestDataProvider();
        actionEngine.setBaseRestUrl("http://localhost:8080");
        dataProvider.setMethod(N2oRestDataProvider.Method.POST);
        dataProvider.setQuery("findAll");
        request = new HashMap<>();
        actionEngine.invoke(dataProvider, request);
        assertThat(restTemplate.getQuery(), is("http://localhost:8080/findAll"));
    }

    @Test
    public void testNoMethodSet() {
        DataSet req = new DataSet();
        req.put("id", 1);
        req.put("name", "test");
        TestRestTemplate restTemplate = new TestRestTemplate();
        SpringRestDataProviderEngine actionEngine = new SpringRestDataProviderEngine(restTemplate, new ObjectMapper());
        N2oRestDataProvider dataProvider = new N2oRestDataProvider();
        dataProvider.setQuery("http://www.someUrl.org/{id}");

        Map<String, Object> request = new HashMap<>();
        request.put("id", 1);
        actionEngine.invoke(dataProvider, request);
        assertThat(restTemplate.getQuery(), is("http://www.someUrl.org/1"));
    }

    @Test
    public void testDateSerializing() {
        TestRestTemplate restClient = new TestRestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
        SpringRestDataProviderEngine actionEngine = new SpringRestDataProviderEngine(restClient, objectMapper);
        actionEngine.setBaseRestUrl("http://localhost:8080");
        N2oRestDataProvider dataProvider = new N2oRestDataProvider();
        dataProvider.setQuery("test/path?{filters}");
        dataProvider.setFiltersSeparator("&");

        Map<String, Object> request = new HashMap<>();
        request.put("date.begin", new Date(0));
        request.put("date.end", new Date(86400000));
        request.put("filters", Arrays.asList("date_begin={date.begin}", "date_end={date.end}"));

        actionEngine.invoke(dataProvider, request);

        assertThat(restClient.getQuery(), is("http://localhost:8080/test/path?date_begin=1970-01-01T03%3A00%3A00&date_end=1970-01-02T03%3A00%3A00"));
    }

    @Test
    public void testListParameters() {
        Properties properties = new Properties();
        properties.put("n2o.engine.mapper", "spel");
        new TestStaticProperties().setProperties(properties);
        TestRestTemplate restClient = new TestRestTemplate();
        SpringRestDataProviderEngine actionEngine = new SpringRestDataProviderEngine(restClient, new ObjectMapper());
        N2oRestDataProvider dataProvider = new N2oRestDataProvider();
        dataProvider.setFiltersSeparator("&");
        dataProvider.setQuery("http://www.someUrl.org/path?{filters}");
        dataProvider.setMethod(N2oRestDataProvider.Method.GET);
        Map<String, Object> request = new HashMap<>();
        request.put("filters", new ArrayList<>());
        request.put("filter1*.id", Arrays.asList("1", "2"));
        request.put("filter2*.name", Arrays.asList("a", "b"));
        request.put("filter3*.value", "testValue");
        ((List) request.get("filters")).add("filter1={filter1*.id}");
        ((List) request.get("filters")).add("filter2={filter2*.name}");
        ((List) request.get("filters")).add("filter3={filter3*.value}");

        actionEngine.invoke(dataProvider, request);

        assertThat(restClient.getQuery(), is("http://www.someUrl.org/path?filter1=1&filter1=2&filter2=a&filter2=b&filter3=testValue"));
    }
}