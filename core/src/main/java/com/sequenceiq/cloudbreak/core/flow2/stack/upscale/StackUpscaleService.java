package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.api.model.InstanceStatus.CREATED;
import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.BillingStatus;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;
import com.sequenceiq.cloudbreak.service.stack.flow.TlsSetupService;
import com.sequenceiq.cloudbreak.service.usages.UsageService;

@Service
public class StackUpscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpscaleService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private StackScalingService stackScalingService;

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private InstanceMetadataService instanceMetadataService;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private UsageService usageService;

    @Inject
    private TlsSetupService tlsSetupService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    public void startAddInstances(Stack stack, Integer scalingAdjustment) {
        String statusReason = format("Adding %s new instance(s) to the infrastructure.", scalingAdjustment);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.ADDING_NEW_INSTANCES, statusReason);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_ADDING_INSTANCES, UPDATE_IN_PROGRESS.name(), scalingAdjustment);
    }

    public Set<Resource> finishAddInstances(StackScalingFlowContext context, UpscaleStackResult payload) {
        LOGGER.info("Upscale stack result: {}", payload);
        List<CloudResourceStatus> results = payload.getResults();
        validateResourceResults(context.getCloudContext(), payload.getErrorDetails(), results);
        updateNodeCount(context.getStack().getId(), context.getCloudStack().getGroups(), results, false);
        Set<Resource> resourceSet = transformResults(results, context.getStack());
        if (resourceSet.isEmpty()) {
            throw new OperationException("Failed to upscale the cluster since all create request failed: " + results.get(0).getStatusReason());
        }
        LOGGER.debug("Adding new instances to the stack is DONE");
        return resourceSet;
    }

    public void extendingMetadata(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.EXTENDING_METADATA);
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
    }

    @Transactional
    public Set<String> finishExtendMetadata(Stack stack, String instanceGroupName, CollectMetadataResult payload) {
        List<CloudVmMetaDataStatus> coreInstanceMetaData = payload.getResults();
        int newinstances = metadataSetupService.saveInstanceMetaData(stack, coreInstanceMetaData, CREATED);
        Set<String> upscaleCandidateAddresses = new HashSet<>();
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : coreInstanceMetaData) {
            upscaleCandidateAddresses.add(cloudVmMetaDataStatus.getMetaData().getPrivateIp());
        }
        InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stack.getId(), instanceGroupName);
        int nodeCount = instanceGroup.getNodeCount() + newinstances;
        if (newinstances != 0) {
            instanceGroup.setNodeCount(nodeCount);
            instanceGroupRepository.save(instanceGroup);
        }
        clusterService.updateClusterStatusByStackIdOutOfTransaction(stack.getId(), AVAILABLE);
        eventService.fireCloudbreakEvent(stack.getId(), BillingStatus.BILLING_CHANGED.name(),
                messagesService.getMessage("stack.metadata.setup.billing.changed"));
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_METADATA_EXTEND, AVAILABLE.name());
        usageService.scaleUsagesForStack(stack.getId(), instanceGroupName, nodeCount);

        return upscaleCandidateAddresses;
    }

    public void setupTls(StackContext context, GetSSHFingerprintsResult sshFingerprints) throws CloudbreakException {
        LOGGER.info("Fingerprint has been determined: {}", sshFingerprints.getSshFingerprints());
        Stack stack = context.getStack();
        for (InstanceMetaData gwInstance : stack.getGatewayInstanceMetadata()) {
            if (CREATED.equals(gwInstance.getInstanceStatus())) {
                tlsSetupService.setupTls(stack, gwInstance, stack.getStackAuthentication().getLoginUserName(), sshFingerprints.getSshFingerprints());
            }
        }
    }

    public void removeTemporarySShKey(StackContext context, Set<String> sshFingerprints) throws CloudbreakException {
        Stack stack = context.getStack();
        for (InstanceMetaData gateway : stack.getGatewayInstanceMetadata()) {
            if (CREATED.equals(gateway.getInstanceStatus())) {
                String ipToTls = gatewayConfigService.getGatewayIp(stack, gateway);
                tlsSetupService.removeTemporarySShKey(stack, ipToTls, gateway.getSshPort(), stack.getStackAuthentication().getLoginUserName(), sshFingerprints);
            }
        }
    }

    public void bootstrappingNewNodes(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.BOOTSTRAPPING_NEW_NODES);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_BOOTSTRAP_NEW_NODES, UPDATE_IN_PROGRESS.name());
    }

    public void extendingHostMetadata(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.EXTENDING_HOST_METADATA);
    }

    public void finishExtendHostMetadata(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.UPSCALE_COMPLETED, "Stack upscale has been finished successfully.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_UPSCALE_FINISHED, AVAILABLE.name());
    }

    public void handleStackUpscaleFailure(long stackId, StackFailureEvent payload) {
        LOGGER.error("Exception during the upscale of stack", payload.getException());
        try {
            String errorReason = payload.getException().getMessage();
            stackUpdater.updateStackStatus(stackId, DetailedStackStatus.UPSCALE_FAILED, "Stack update failed. " + errorReason);
            flowMessageService.fireEventAndLog(stackId, Msg.STACK_INFRASTRUCTURE_UPDATE_FAILED, UPDATE_FAILED.name(), errorReason);
        } catch (RuntimeException e) {
            LOGGER.error("Exception during the handling of stack scaling failure: {}", e.getMessage());
        }
    }

    private Set<Resource> transformResults(List<CloudResourceStatus> cloudResourceStatuses, Stack stack) {
        Set<Resource> retSet = new HashSet<>();
        for (CloudResourceStatus cloudResourceStatus : cloudResourceStatuses) {
            if (!cloudResourceStatus.isFailed()) {
                CloudResource cloudResource = cloudResourceStatus.getCloudResource();
                Resource resource = new Resource(cloudResource.getType(), cloudResource.getName(), cloudResource.getReference(), cloudResource.getStatus(),
                        stack, null);
                retSet.add(resource);
            }
        }
        return retSet;
    }

    private void validateResourceResults(CloudContext cloudContext, Exception exception, List<CloudResourceStatus> results) {
        if (exception != null) {
            LOGGER.error(format("Failed to upscale stack: %s", cloudContext), exception);
            throw new OperationException(exception);
        }
        if (results.size() == 1 && (results.get(0).isFailed() || results.get(0).isDeleted())) {
            throw new OperationException(format("Failed to upscale the stack for %s due to: %s", cloudContext, results.get(0).getStatusReason()));
        }
    }

    private void updateNodeCount(Long stackId, List<Group> originalGroups, List<CloudResourceStatus> statuses, boolean create) {
        for (Group group : originalGroups) {
            int nodeCount = group.getInstancesSize();
            List<CloudResourceStatus> failedResources = removeFailedMetadata(stackId, statuses, group);
            if (!failedResources.isEmpty() && create) {
                int failedCount = failedResources.size();
                InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stackId, group.getName());
                instanceGroup.setNodeCount(nodeCount - failedCount);
                instanceGroupRepository.save(instanceGroup);
                flowMessageService.fireEventAndLog(stackId, Msg.STACK_INFRASTRUCTURE_ROLLBACK_MESSAGE, UPDATE_IN_PROGRESS.name(),
                        failedCount, group.getName(), failedResources.get(0).getStatusReason());
            }
        }
    }

    private List<CloudResourceStatus> removeFailedMetadata(Long stackId, List<CloudResourceStatus> statuses, Group group) {
        Map<Long, CloudResourceStatus> failedResources = new HashMap<>();
        Set<Long> groupPrivateIds = getPrivateIds(group);
        for (CloudResourceStatus status : statuses) {
            Long privateId = status.getPrivateId();
            if (privateId != null && status.isFailed() && !failedResources.containsKey(privateId) && groupPrivateIds.contains(privateId)) {
                failedResources.put(privateId, status);
                instanceMetadataService.deleteInstanceRequest(stackId, privateId);
            }
        }
        return new ArrayList<>(failedResources.values());
    }

    private Set<Long> getPrivateIds(Group group) {
        Set<Long> ids = new HashSet<>();
        for (CloudInstance cloudInstance : group.getInstances()) {
            ids.add(cloudInstance.getTemplate().getPrivateId());
        }
        return ids;
    }

    public List<CloudInstance> getNewInstances(Stack stack) {
        List<CloudInstance> cloudInstances = cloudStackConverter.buildInstances(stack);
        Iterator<CloudInstance> iterator = cloudInstances.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getTemplate().getStatus() != InstanceStatus.CREATE_REQUESTED) {
                iterator.remove();
            }
        }
        return cloudInstances;
    }
}
