package com.huawei.it.roma.policycenter.persistence.mapper;

import com.huawei.it.roma.policycenter.persistence.model.AgentStrategyRow;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentStrategyMapper {

    @Update("""
            UPDATE pc_agent_strategy
            SET agent_id = #{agentId},
                permission_point_code = #{permissionPointCode},
                condition_field = #{conditionField},
                condition_operator = #{conditionOperator},
                effect = #{effect},
                status = #{status},
                updated_at = CURRENT_TIMESTAMP
            WHERE strategy_id = #{strategyId}
            """)
    int update(AgentStrategyRow row);

    @Insert("""
            INSERT INTO pc_agent_strategy (
                strategy_id,
                agent_id,
                permission_point_code,
                condition_field,
                condition_operator,
                effect,
                status,
                created_at,
                updated_at
            )
            VALUES (
                #{strategyId},
                #{agentId},
                #{permissionPointCode},
                #{conditionField},
                #{conditionOperator},
                #{effect},
                #{status},
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            """)
    int insert(AgentStrategyRow row);

    @Select({
            "<script>",
            "SELECT strategy_id, agent_id, permission_point_code, condition_field, condition_operator, effect, status",
            "FROM pc_agent_strategy",
            "WHERE strategy_id IN",
            "<foreach collection='strategyIds' item='strategyId' open='(' separator=',' close=')'>",
            "#{strategyId}",
            "</foreach>",
            "</script>"
    })
    List<AgentStrategyRow> findByStrategyIds(@Param("strategyIds") List<String> strategyIds);

    @Select({
            "<script>",
            "SELECT s.strategy_id, s.agent_id, s.permission_point_code, s.condition_field,",
            "s.condition_operator, s.effect, s.status",
            "FROM pc_agent_strategy s",
            "JOIN pc_permission_point pp ON pp.permission_point_code = s.permission_point_code",
            "WHERE s.agent_id = #{agentId}",
            "AND s.status = 'ACTIVE'",
            "AND pp.status = 'ACTIVE'",
            "AND s.permission_point_code IN",
            "<foreach collection='permissionPointCodes' item='code' open='(' separator=',' close=')'>",
            "#{code}",
            "</foreach>",
            "ORDER BY s.permission_point_code, s.strategy_id",
            "</script>"
    })
    List<AgentStrategyRow> findActiveByAgentIdAndPermissionPointCodes(
            @Param("agentId") String agentId,
            @Param("permissionPointCodes") List<String> permissionPointCodes
    );
}
