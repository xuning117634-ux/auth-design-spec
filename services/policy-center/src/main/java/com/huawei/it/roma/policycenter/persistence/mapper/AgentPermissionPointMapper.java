package com.huawei.it.roma.policycenter.persistence.mapper;

import com.huawei.it.roma.policycenter.persistence.model.AgentPermissionPointRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentPermissionPointMapper {

    @Update("""
            UPDATE pc_agent_permission_point
            SET status = #{status},
                updated_at = CURRENT_TIMESTAMP
            WHERE agent_id = #{agentId}
              AND enterprise = #{enterprise}
              AND permission_point_code = #{permissionPointCode}
            """)
    int update(AgentPermissionPointRow row);

    @Insert("""
            INSERT INTO pc_agent_permission_point (
                agent_id,
                enterprise,
                permission_point_code,
                status,
                created_at,
                updated_at
            )
            VALUES (
                #{agentId},
                #{enterprise},
                #{permissionPointCode},
                #{status},
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            """)
    int insert(AgentPermissionPointRow row);
}
