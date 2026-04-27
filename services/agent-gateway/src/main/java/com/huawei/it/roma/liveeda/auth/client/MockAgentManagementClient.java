package com.huawei.it.roma.liveeda.auth.client;

import com.huawei.it.roma.liveeda.auth.config.MockAgentManagementProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Profile("mock")
public class MockAgentManagementClient implements AgentManagementClient {

    private final Map<String, AgentRegistryEntry> entriesById;

    public MockAgentManagementClient(MockAgentManagementProperties properties) {
        this.entriesById = properties.getEntries().stream()
                .map(entry -> new AgentRegistryEntry(
                        entry.getAgentId(),
                        entry.getAgentName(),
                        entry.getEnterprise(),
                        entry.getAppId(),
                        entry.getAllowedReturnHosts(),
                        entry.getSubscribedPermissionPointCodes()
                ))
                .collect(Collectors.toMap(AgentRegistryEntry::agentId, Function.identity()));
    }

    @Override
    public AgentRegistryEntry getGatewayProfile(String agentId) {
        AgentRegistryEntry entry = entriesById.get(agentId);
        if (entry == null) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "Unknown agent_id: " + agentId);
        }
        return entry;
    }
}
