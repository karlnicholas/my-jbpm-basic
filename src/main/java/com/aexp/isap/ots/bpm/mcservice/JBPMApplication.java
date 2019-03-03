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

package com.aexp.isap.ots.bpm.mcservice;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.model.ProcessDefinition;
import org.jbpm.services.api.model.UserTaskInstanceDesc;
import org.kie.api.runtime.query.QueryContext;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class JBPMApplication {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JBPMApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(JBPMApplication.class, args);
    }

    @Bean
    CommandLineRunner deployAndValidate() {
        return new CommandLineRunner() {
            
            @Autowired            
            private DeploymentService deploymentService;
            
            @Autowired
            private RuntimeDataService runtimeDataService;
            
            @Autowired
            private ProcessService processService;

            @Autowired
            private UserTaskService userTaskService;
            
            @Override
            public void run(String... strings) throws Exception {                
                KModuleDeploymentUnit unit = null;
                if (strings.length > 1) {
                    String arg = strings[1];
                    LOGGER.info("About to deploy : {}", arg);
                    
                    String[] gav = arg.split(":");
                    
                    unit = new KModuleDeploymentUnit(gav[0], gav[1], gav[2]);
                    deploymentService.deploy(unit);
                    LOGGER.info("{} successfully deployed", arg);
                }
                LOGGER.info("Available processes:");
                Collection<ProcessDefinition> processes = runtimeDataService.getProcesses(new QueryContext());
                for (ProcessDefinition def : processes) {
                    LOGGER.info("\t{} (with id '{})", def.getName(), def.getId());
                }
                
                if (unit != null && !processes.isEmpty()) {
                    String processId = processes.iterator().next().getId();
                    LOGGER.info("About to start process with id {}", processId);
                    long processInstanceId = processService.startProcess(unit.getIdentifier(), processId);
                    LOGGER.info("Started instance of {} process with id {}", processId, processInstanceId);
                    
//                    processService.abortProcessInstance(processInstanceId);
//                    LOGGER.info("Aborted instance with id {}", processInstanceId);
                    UserTaskInstanceDesc task = runtimeDataService.getTaskById(1L);
                    LOGGER.info("getTaskById {}", task);
                    LOGGER.info("task ActualOwner {}", task.getActualOwner());
                    LOGGER.info("task CreatedBy {}", task.getCreatedBy());
                    LOGGER.info("getTaskById {}", task);
                    List<TaskSummary> tasks = runtimeDataService.getTasksAssignedAsPotentialOwner("New Actor", new QueryFilter());
                    LOGGER.info("getTasksAssignedAsPotentialOwner {}", tasks);
                    if ( tasks.size() == 1 ) {

	                    userTaskService.start(tasks.get(0).getId(), "New Actor");
	                    LOGGER.info("start {}", tasks);
	
	                    userTaskService.complete(tasks.get(0).getId(), "New Actor", Collections.emptyMap());
	                    LOGGER.info("complete {}", tasks);
                    } else {
	                    LOGGER.info("no tasks found for user");
                    }
                }
                LOGGER.info("========= Verification completed successfully =========");
            }
        };
    }
}
