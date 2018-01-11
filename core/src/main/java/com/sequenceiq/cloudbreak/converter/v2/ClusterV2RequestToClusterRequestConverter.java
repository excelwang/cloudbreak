package com.sequenceiq.cloudbreak.converter.v2;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class ClusterV2RequestToClusterRequestConverter extends AbstractConversionServiceAwareConverter<ClusterV2Request, ClusterRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterV2RequestToClusterRequestConverter.class);

    @Override
    public ClusterRequest convert(ClusterV2Request source) {
        ClusterRequest cluster = new ClusterRequest();
        cluster.setExecutorType(source.getExecutorType());
        cluster.setEmailNeeded(source.getEmailNeeded());
        cluster.setEmailTo(source.getEmailTo());
        cluster.setFileSystem(source.getFileSystem());
        cluster.setName(source.getName());
        if (source.getRdsConfig() != null) {
            cluster.setRdsConfigIds(source.getRdsConfig().getRdsConfigIds());
            cluster.setRdsConfigJsons(source.getRdsConfig().getRdsConfigs());
        }
        cluster.setLdapConfigName(source.getLdapConfigName());
        if (source.getAmbari() != null) {
            cluster.setAmbariDatabaseDetails(source.getAmbari().getAmbariDatabaseDetails());
            cluster.setAmbariRepoDetailsJson(source.getAmbari().getAmbariRepoDetailsJson());
            cluster.setAmbariStackDetails(source.getAmbari().getAmbariStackDetails());
            cluster.setBlueprintCustomPropertiesAsString(source.getAmbari().getBlueprintCustomProperties());
            cluster.setBlueprintId(source.getAmbari().getBlueprintId());
            cluster.setBlueprintName(source.getAmbari().getBlueprintName());
            cluster.setBlueprintInputs(source.getAmbari().getBlueprintInputs());
            cluster.setConfigStrategy(source.getAmbari().getConfigStrategy());
            cluster.setConnectedCluster(source.getAmbari().getConnectedCluster());
            cluster.setEnableSecurity(source.getAmbari().getEnableSecurity());
            cluster.setGateway(source.getAmbari().getGateway());
            cluster.setKerberos(source.getAmbari().getKerberos());
            cluster.setPassword(source.getAmbari().getPassword());
            cluster.setUserName(source.getAmbari().getUserName());
            cluster.setValidateBlueprint(source.getAmbari().getValidateBlueprint());
        }
        cluster.setHostGroups(new HashSet<>());
        return cluster;
    }
}
