package com.huawei.it.roma.liveeda.demoagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.it.roma.liveeda.demoagent.client.PolicyCenterClient;
import com.huawei.it.roma.liveeda.demoagent.client.PolicyCenterClient.ResolveByCodesResult;
import com.huawei.it.roma.liveeda.demoagent.config.DemoAgentProperties;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "demo-agent.mcp", name = "mode", havingValue = "real")
public class RealMcpGatewayClient implements McpGatewayClient {

    private final RestClient.Builder restClientBuilder;
    private final DemoAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final PolicyCenterClient policyCenterClient;

    @Override
    public Set<String> extractAuthorizedPermissionPointCodes(String trToken) {
        return new LinkedHashSet<>(decodeTrToken(trToken).authorizedPermissionPointCodes());
    }

    @Override
    public Set<String> resolveCoveredTools(String trToken) {
        TrTokenParser.DecodedTrContext trContext = decodeTrToken(trToken);
        ResolveByCodesResult resolution = policyCenterClient.resolveByCodes(trContext.authorizedPermissionPointCodes());
        if (resolution == null || resolution.tools() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Policy center returned empty tools for TR");
        }
        return resolution.tools().stream()
                .map(PolicyCenterClient.ToolView::toolId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String invoke(String agentId, String trToken, Set<String> requiredTools, String message) {
        TrTokenParser.DecodedTrContext trContext = decodeTrToken(trToken);
        if (!agentId.equals(trContext.agentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "TR agent does not match request agent");
        }
        RestClient restClient = restClientBuilder.baseUrl(properties.getMcp().getGatewayBaseUrl()).build();
        InvokeResponse response = post(restClient)
                .body(new InvokeRequest(agentId, trToken, requiredTools.stream().sorted().toList(), message))
                .retrieve()
                .body(InvokeResponse.class);
        if (response == null || !"SUCCESS".equalsIgnoreCase(response.status()) || isBlank(response.answer())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Real MCP gateway returned empty result");
        }
        return response.answer();
    }

    private RequestBodySpec post(RestClient restClient) {
        RequestBodySpec request = restClient.post().uri(properties.getMcp().getInvokePath());
        for (Map.Entry<String, String> entry : properties.getMcp().getHeaders().entrySet()) {
            if (!isBlank(entry.getKey()) && !isBlank(entry.getValue())) {
                request.header(entry.getKey(), entry.getValue());
            }
        }
        return request;
    }

    private TrTokenParser.DecodedTrContext decodeTrToken(String trToken) {
        try {
            return new TrTokenParser(objectMapper).decode(trToken);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "TR cannot be parsed for real MCP gateway invocation", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record InvokeRequest(
            String agentId,
            String tr,
            List<String> tools,
            String message
    ) {
    }

    private record InvokeResponse(
            String status,
            String answer
    ) {
    }
}
