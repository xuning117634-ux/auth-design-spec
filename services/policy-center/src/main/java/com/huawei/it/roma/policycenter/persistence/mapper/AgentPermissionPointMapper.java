package com.huawei.it.roma.policycenter.persistence.mapper;

import com.huawei.it.roma.policycenter.persistence.model.AgentPermissionPointRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentPermissionPointMapper {

    @Update("""
            UPDATE pc_agent_permission_point
            SET permission_point_codes = #{permissionPointCodes},
                last_sync_source = #{lastSyncSource},
                updated_at = CURRENT_TIMESTAMP
            WHERE agent_id = #{agentId}
              AND enterprise = #{enterprise}
            """)
    int update(AgentPermissionPointRow row);

    @Insert("""
            INSERT INTO pc_agent_permission_point (
                agent_id,
                enterprise,
                permission_point_codes,
                last_sync_source,
                created_at,
                updated_at
            )
            VALUES (
                #{agentId},
                #{enterprise},
                #{permissionPointCodes},
                #{lastSyncSource},
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            """)
    int insert(AgentPermissionPointRow row);
}
