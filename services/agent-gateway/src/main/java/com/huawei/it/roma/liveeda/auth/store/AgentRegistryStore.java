package com.huawei.it.roma.liveeda.auth.store;

import com.huawei.it.roma.liveeda.auth.config.AgentRegistryProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AgentRegistryStore {

    private final Map<String, AgentRegistryEntry> entriesById;

    public AgentRegistryStore(AgentRegistryProperties properties) {
        this.entriesById = properties.getEntries().stream()
                .map(entry -> new AgentRegistryEntry(
                        entry.getAgentId(),
                        entry.getAgentName(),
                        entry.getAgentServiceAccount(),
                        entry.getPrincipal(),
                        entry.getSubscribedTools().stream().collect(Collectors.toSet()),
                        entry.getAllowedReturnHosts()
                ))
                .collect(Collectors.toMap(AgentRegistryEntry::agentId, Function.identity()));
    }

    public AgentRegistryEntry require(String agentId) {
        return find(agentId)
                .orElseThrow(() -> new GatewayException(HttpStatus.BAD_REQUEST, "Unknown agent_id: " + agentId));
    }

    public Optional<AgentRegistryEntry> find(String agentId) {
        return Optional.ofNullable(entriesById.get(agentId));
    }
}
