/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.jbpm.springboot.samples;

import static org.appformer.maven.integration.MavenRepository.getMavenRepository;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.appformer.maven.integration.MavenRepository;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.runtime.manager.impl.jpa.EntityManagerFactoryManager;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.admin.MigrationReport;
import org.jbpm.services.api.admin.ProcessInstanceMigrationService;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.process.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.aexp.isap.ots.bpm.mcservice.JBPMApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {JBPMApplication.class, TestAutoConfiguration.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations="classpath:application-test.properties")
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class MigrationProcessTest {
    
    static final String ARTIFACT_ID = "evaluation";
    static final String GROUP_ID = "org.jbpm.test";
    static final String VERSION = "1.0.0";
    
    static final String VERSION_2 = "1.0.1";

    private KModuleDeploymentUnit unit = null;
    
    private KModuleDeploymentUnit unitV2 = null;
    
    @Autowired
    private ProcessService processService;
    
    @Autowired
    private DeploymentService deploymentService;
    
    @Autowired
    private ProcessInstanceMigrationService processInstanceMigrationService;
    
    @Autowired
    private RuntimeDataService runtimeDataService;
    
    @BeforeClass
    public static void generalSetup() {
        KieServices ks = KieServices.Factory.get();
        ReleaseId releaseId = ks.newReleaseId(GROUP_ID, ARTIFACT_ID, VERSION);
        File kjar = new File("../kjars/evaluation/jbpm-module.jar");
        File pom = new File("../kjars/evaluation/pom.xml");
        MavenRepository repository = getMavenRepository();
        repository.installArtifact(releaseId, kjar, pom);
        
        ReleaseId releaseId2 = ks.newReleaseId(GROUP_ID, ARTIFACT_ID, VERSION_2);
        File kjar2 = new File("../kjars/evaluation/jbpm-module.jar");
        File pom2 = new File("../kjars/evaluation/pom2.xml");
        
        repository.installArtifact(releaseId2, kjar2, pom2);

        EntityManagerFactoryManager.get().clear();
    }
    
    
    @Before
    public void setup() {
        unit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION);
        deploymentService.deploy(unit);
        
        unitV2 = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION_2);
        deploymentService.deploy(unitV2);
    }
    
    @After
    public void cleanup() {

        deploymentService.undeploy(unit);
        deploymentService.undeploy(unitV2);
    }
 
    @Test
    public void testProcessStartAndAbort() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("employee", "john");
        parameters.put("reason", "SpringBoot jBPM evaluation");
        long processInstanceId = processService.startProcess(unit.getIdentifier(), "evaluation");
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId > 0);
        
        try {
            ProcessInstanceDesc piLog = runtimeDataService.getProcessInstanceById(processInstanceId);
            assertNotNull(piLog);
            assertEquals(unit.getIdentifier(), piLog.getDeploymentId());
            
            MigrationReport report = processInstanceMigrationService.migrate(unit.getIdentifier(), processInstanceId, unitV2.getIdentifier(), "evaluation");
            assertTrue(report.isSuccessful());
            
            piLog = runtimeDataService.getProcessInstanceById(processInstanceId);
            assertNotNull(piLog);
            assertEquals(unitV2.getIdentifier(), piLog.getDeploymentId());
        } finally {
            processService.abortProcessInstance(processInstanceId);
            
            ProcessInstance pi = processService.getProcessInstance(processInstanceId);
            assertNull(pi);
        }
    }
    
    
}

