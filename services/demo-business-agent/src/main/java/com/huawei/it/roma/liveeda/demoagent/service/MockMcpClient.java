package com.huawei.it.roma.liveeda.demoagent.service;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MockMcpClient {

    public String invoke(String message, Set<String> requiredTools, String accessToken) {
        if (requiredTools.contains("mcp:financial-report-server/query_monthly_report")) {
            return """
                    我已经根据财报工具返回的模拟数据完成整理。
                    结果摘要：
                    1. 本月营业收入保持稳定，整体趋势正常。
                    2. 成本项目略有上升，建议继续关注费用波动。
                    3. 如果你愿意，我可以继续细化到环比变化和重点科目分析。
                    原始问题：%s
                    """.formatted(message).trim();
        }
        if (requiredTools.contains("mcp:contract-server/get_contract")) {
            return """
                    我已经根据合同工具返回的模拟数据完成查询整理。
                    查询结果摘要：
                    1. 已定位到目标合同的基础信息、签约主体和当前状态。
                    2. 当前合同仍在有效期内，关键条款未发现异常变更。
                    3. 如果你愿意，我可以继续帮你整理合同金额、到期时间和审批流摘要。
                    原始问题：%s
                    """.formatted(message).trim();
        }
        if (requiredTools.contains("mcp:invoice-server/query_invoices")) {
            return """
                    我已经根据发票工具返回的模拟数据完成查询。
                    查询结果摘要：
                    1. 已检索到本次条件对应的发票记录。
                    2. 当前可以继续查看发票明细、状态和时间范围。
                    3. 如果你愿意，我还可以继续帮你按月份或状态筛选。
                    原始问题：%s
                    """.formatted(message).trim();
        }
        return """
                我已经完成模拟工具调用，并拿到了可用结果。
                当前可以继续围绕这个问题做更细的分析或追问。
                原始问题：%s
                """.formatted(message).trim();
    }
}
