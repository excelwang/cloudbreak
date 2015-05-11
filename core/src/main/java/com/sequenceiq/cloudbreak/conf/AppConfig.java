package com.sequenceiq.cloudbreak.conf;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_CONTAINER_THREADPOOL_CAPACITY_SIZE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_CONTAINER_THREADPOOL_CORE_SIZE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_INTERMEDIATE_THREADPOOL_CAPACITY_SIZE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_INTERMEDIATE_THREADPOOL_CORE_SIZE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_THREADPOOL_CAPACITY_SIZE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_THREADPOOL_CORE_SIZE;
import static com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorTool.SWARM;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptorMapFactory;
import com.sequenceiq.cloudbreak.core.flow.ExecutorBasedParallelContainerRunner;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorTool;
import com.sequenceiq.cloudbreak.orchestrator.ParallelContainerRunner;
import com.sequenceiq.cloudbreak.orchestrator.swarm.SwarmContainerOrchestrator;
import com.sequenceiq.cloudbreak.service.credential.CredentialHandler;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.aws.TemplateReader;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Configuration
public class AppConfig {

    @Value("${cb.threadpool.core.size:" + CB_THREADPOOL_CORE_SIZE + "}")
    private int corePoolSize;
    @Value("${cb.threadpool.capacity.size:" + CB_THREADPOOL_CAPACITY_SIZE + "}")
    private int queueCapacity;

    @Value("${cb.intermediate.threadpool.core.size:" + CB_INTERMEDIATE_THREADPOOL_CORE_SIZE + "}")
    private int intermediateCorePoolSize;
    @Value("${cb.intermediate.threadpool.capacity.size:" + CB_INTERMEDIATE_THREADPOOL_CAPACITY_SIZE + "}")
    private int intermediateQueueCapacity;

    @Value("${cb.container.threadpool.core.size:" + CB_CONTAINER_THREADPOOL_CORE_SIZE + "}")
    private int containerCorePoolSize;
    @Value("${cb.container.threadpool.capacity.size:" + CB_CONTAINER_THREADPOOL_CAPACITY_SIZE + "}")
    private int containerteQueueCapacity;

    @Autowired
    private TemplateReader templateReader;

    @Autowired
    private List<CloudPlatformConnector> cloudPlatformConnectorList;

    @Autowired
    private List<ProvisionSetup> provisionSetups;

    @Autowired
    private List<MetadataSetup> metadataSetups;

    @Autowired
    private List<CredentialHandler<? extends Credential>> credentialHandlers;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Map<ContainerOrchestratorTool, ContainerOrchestrator> containerOrchestrators() {
        Map<ContainerOrchestratorTool, ContainerOrchestrator> map = new HashMap<>();
        map.put(SWARM, new SwarmContainerOrchestrator(simpleParalellContainerRunnerExecutor()));
        return map;
    }

    @Bean
    public ParallelContainerRunner simpleParalellContainerRunnerExecutor() {
        return new ExecutorBasedParallelContainerRunner(containerBootstrapBuilderExecutor());
    }

    @Bean
    public Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors() {
        Map<CloudPlatform, CloudPlatformConnector> map = new HashMap<>();
        for (CloudPlatformConnector provisionService : cloudPlatformConnectorList) {
            map.put(provisionService.getCloudPlatform(), provisionService);
        }
        return map;
    }

    @Bean
    public Map<CloudPlatform, ProvisionSetup> provisionSetups() {
        Map<CloudPlatform, ProvisionSetup> map = new HashMap<>();
        for (ProvisionSetup provisionSetup : provisionSetups) {
            map.put(provisionSetup.getCloudPlatform(), provisionSetup);
        }
        return map;
    }

    @Bean
    public Map<CloudPlatform, MetadataSetup> metadataSetups() {
        Map<CloudPlatform, MetadataSetup> map = new HashMap<>();
        for (MetadataSetup metadataSetup : metadataSetups) {
            map.put(metadataSetup.getCloudPlatform(), metadataSetup);
        }
        return map;
    }

    @Bean
    public Map<CloudPlatform, CredentialHandler> credentialHandlers() {
        Map<CloudPlatform, CredentialHandler> map = new HashMap<>();
        for (CredentialHandler credentialHandler : credentialHandlers) {
            map.put(credentialHandler.getCloudPlatform(), credentialHandler);
        }
        return map;
    }

    @Bean
    public AsyncTaskExecutor intermediateBuilderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(intermediateCorePoolSize);
        executor.setQueueCapacity(intermediateQueueCapacity);
        executor.setThreadNamePrefix("intermediateBuilderExecutor-");
        executor.initialize();
        return executor;
    }

    @Bean
    public AsyncTaskExecutor resourceBuilderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("resourceBuilderExecutor-");
        executor.initialize();
        return executor;
    }

    @Bean
    public AsyncTaskExecutor containerBootstrapBuilderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(containerCorePoolSize);
        executor.setQueueCapacity(containerteQueueCapacity);
        executor.setThreadNamePrefix("containerBootstrapBuilderExecutor-");
        executor.initialize();
        return executor;
    }

    @Bean
    public RestOperations restTemplate() {
        return new RestTemplate();
    }

    @Bean(name = "autoSSLAcceptorRestTemplate")
    public RestOperations autoSSLAcceptorRestTemplate() throws Exception {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        sslContextBuilder.loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                return true;
            }
        });
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build());
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
        requestFactory.setHttpClient(httpClient);
        return new RestTemplate(requestFactory);
    }

    @Bean
    public StackServiceComponentDescriptorMapFactory stackServiceComponentDescriptorMapFactory() throws IOException {
        Map<String, Integer> maxCardinalityReps = Maps.newHashMap();
        maxCardinalityReps.put("1", 1);
        maxCardinalityReps.put("0-1", 1);
        maxCardinalityReps.put("1-2", 1);
        maxCardinalityReps.put("0+", Integer.MAX_VALUE);
        maxCardinalityReps.put("1+", Integer.MAX_VALUE);
        maxCardinalityReps.put("ALL", Integer.MAX_VALUE);
        String stackServiceComponentsJson = FileReaderUtils.readFileFromClasspath("hdp/hdp-services.json");
        return new StackServiceComponentDescriptorMapFactory(stackServiceComponentsJson, maxCardinalityReps, new ObjectMapper());
    }

}
