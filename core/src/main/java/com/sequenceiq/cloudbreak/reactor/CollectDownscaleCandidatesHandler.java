package com.sequenceiq.cloudbreak.reactor;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariDecommissioner;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CollectDownscaleCandidatesHandler implements ReactorEventHandler<CollectDownscaleCandidatesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private AmbariDecommissioner ambariDecommissioner;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CollectDownscaleCandidatesRequest.class);
    }

    @Override
    public void accept(Event<CollectDownscaleCandidatesRequest> event) {
        CollectDownscaleCandidatesRequest request = event.getData();
        CollectDownscaleCandidatesResult result;
        try {
            Stack stack = stackService.getByIdWithLists(request.getStackId());
            Set<String> hostNames;
            if (request.getHostNames() == null) {
                hostNames = ambariDecommissioner.collectDownscaleCandidates(stack, request.getHostGroupName(), request.getScalingAdjustment());
            } else {
                hostNames = request.getHostNames();
            }
            result = new CollectDownscaleCandidatesResult(request, hostNames);
        } catch (Exception e) {
            result = new CollectDownscaleCandidatesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
