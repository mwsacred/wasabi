package com.intuit.wasabi.repository.cassandra.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.PrioritiesAccessor;

public class CassandraPrioritiesRepositoryITest {

    PrioritiesAccessor accessor;
    ExperimentAccessor experimentAccessor;
    
    CassandraPrioritiesRepository repository;
    
    Application.Name applicationName;
    
    List<Experiment.ID> priorityIds;

	private Session session;

	private MappingManager manager;

	private Mapper<com.intuit.wasabi.repository.cassandra.pojo.Application> mapper;
    static UUID experimentId1 = UUID.randomUUID();
    static UUID experimentId2 = UUID.randomUUID();
    static Date date1 = new Date();
    static Date date2 = new Date();

    
    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new CassandraRepositoryModule());
        injector.getInstance(Key.get(String.class, Names.named("CassandraInstanceName")));

        session = injector.getInstance(CassandraDriver.class).getSession();
        manager = new MappingManager(session);
        mapper = manager.mapper(com.intuit.wasabi.repository.cassandra.pojo.Application.class);
    	accessor = manager.createAccessor(PrioritiesAccessor.class);
    	experimentAccessor = manager.createAccessor(ExperimentAccessor.class);
    	repository = new CassandraPrioritiesRepository(accessor,experimentAccessor);
    	applicationName = Application.Name.valueOf("TestApplicationName" + System.currentTimeMillis());
    	priorityIds = new ArrayList<>();
    }
    
	@Test
	public void testZeroGetPrioritiesSuccess() {
				
		PrioritizedExperimentList result = repository.getPriorities(applicationName);
		assertEquals("Value should be equal", 0, result.getPrioritizedExperiments().size());
	}

	@Test
	public void testTwoGetPrioritiesSuccess() {
		
    	experimentAccessor.insertExperiment(experimentId1, 
    			"d1", "", 1.0, date1, date2, 
    			com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l1", 
    			applicationName.toString(), date1, date2, true, 
    			"m1", "v1", true, 5000, "c1");

    	experimentAccessor.insertExperiment(experimentId2, 
    			"d2", "", 1.0, date1, date2, 
    			com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l2", 
    			applicationName.toString(), date1, date2, true, 
    			"m2", "v2", true, 5000, "c2");
		
    	List<UUID> priorityIds = new ArrayList<>();
    	priorityIds.add(experimentId1);
    	priorityIds.add(experimentId2);
    	
    	accessor.updatePriorities(priorityIds, applicationName.toString());
    	
		PrioritizedExperimentList result = repository.getPriorities(applicationName);
		assertEquals("Value should be equal", 2, result.getPrioritizedExperiments().size());
		
		assertEquals("Value should be equal", experimentId1, result.getPrioritizedExperiments()
				.get(0).getID().getRawID());
		assertEquals("Value should be equal", experimentId2, result
				.getPrioritizedExperiments().get(1).getID().getRawID());
	}
	@Test
	public void testCreatePrioritiesOneIdSuccess() {
		priorityIds.add(Experiment.ID.newInstance());
		repository.createPriorities(applicationName, priorityIds);
		List<ID> priorityIdsList = repository.getPriorityList(applicationName);
		
		assertEquals("Size should be same", 1, priorityIdsList.size());
		assertEquals("Values should be same", priorityIdsList.get(0), priorityIds.get(0));
		
		int length = repository.getPriorityListLength(applicationName);
		assertEquals("Size should be same", 1, length);
	}

	@Test
	public void testCreatePrioritiesTwoIdsSuccess() {
		priorityIds.add(Experiment.ID.newInstance());
		priorityIds.add(Experiment.ID.newInstance());
		
		repository.createPriorities(applicationName, priorityIds);
		List<ID> priorityIdsList = repository.getPriorityList(applicationName);
		
		assertEquals("Size should be same", 2, priorityIdsList.size());
		assertEquals("Values should be same", priorityIdsList.get(0), 
				priorityIds.get(0));
		assertEquals("Values should be same", priorityIdsList.get(1), 
				priorityIds.get(1));
		int length = repository.getPriorityListLength(applicationName);
		assertEquals("Size should be same", 2, length);
	}

	@Test
	public void testCreatePrioritiesTwoIdsAndThenEmptyIdsSuccess() {
		priorityIds.add(Experiment.ID.newInstance());
		priorityIds.add(Experiment.ID.newInstance());
		
		repository.createPriorities(applicationName, priorityIds);
		List<ID> priorityIdsList = repository.getPriorityList(applicationName);
		
		assertEquals("Size should be same", 2, priorityIdsList.size());
		assertEquals("Values should be same", priorityIdsList.get(0),
				priorityIds.get(0));
		assertEquals("Values should be same", priorityIdsList.get(1), 
				priorityIds.get(1));
		int length = repository.getPriorityListLength(applicationName);
		assertEquals("Size should be same", 2, length);
		
		// Now empty list and create
		priorityIds.clear();
		repository.createPriorities(applicationName, priorityIds);
		priorityIdsList = repository.getPriorityList(applicationName);
		
		assertEquals("Size should be same", 0, priorityIdsList.size());
		length = repository.getPriorityListLength(applicationName);
		assertEquals("Size should be same", 0, length);		
	}
}
