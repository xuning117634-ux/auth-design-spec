package com.huawei.it.roma.policycenter.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PermissionPointBatchUpsertRequest(
        @NotBlank String source,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotBlank String permissionPointCode,
            @NotBlank String enterprise,
            @NotBlank String appId,
            @NotBlank String displayNameZh,
            @NotBlank String description,
            @NotEmpty List<@Valid BoundTool> boundTools,
            @NotBlank String status
    ) {
    }

    public record BoundTool(
            String toolId,
            String displayNameZh
    ) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static BoundTool fromJson(JsonNode node) {
            if (node == null || node.isNull()) {
                return new BoundTool(null, null);
            }
            if (node.isTextual()) {
                return new BoundTool(node.asText(), null);
            }
            return new BoundTool(readText(node, "toolId"), readText(node, "displayNameZh"));
        }

        private static String readText(JsonNode node, String fieldName) {
            JsonNode valueNode = node.get(fieldName);
            return valueNode == null || valueNode.isNull() ? null : valueNode.asText();
        }
    }
}
