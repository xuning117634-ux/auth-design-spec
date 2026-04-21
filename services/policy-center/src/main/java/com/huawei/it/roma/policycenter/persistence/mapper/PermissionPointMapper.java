package com.huawei.it.roma.policycenter.persistence.mapper;

import com.huawei.it.roma.policycenter.persistence.model.PermissionPointRow;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PermissionPointMapper {

    @Update("""
            UPDATE pc_permission_point
            SET display_name_zh = #{displayNameZh},
                description = #{description},
                status = #{status},
                last_sync_source = #{lastSyncSource},
                updated_at = CURRENT_TIMESTAMP
            WHERE permission_point_code = #{permissionPointCode}
            """)
    int update(PermissionPointRow row);

    @Insert("""
            INSERT INTO pc_permission_point (
                permission_point_code,
                display_name_zh,
                description,
                status,
                last_sync_source,
                created_at,
                updated_at
            )
            VALUES (
                #{permissionPointCode},
                #{displayNameZh},
                #{description},
                #{status},
                #{lastSyncSource},
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            """)
    int insert(PermissionPointRow row);

    @Select({
            "<script>",
            "SELECT permission_point_code, display_name_zh, description, status, last_sync_source",
            "FROM pc_permission_point",
            "WHERE permission_point_code IN",
            "<foreach collection='permissionPointCodes' item='code' open='(' separator=',' close=')'>",
            "#{code}",
            "</foreach>",
            "</script>"
    })
    List<PermissionPointRow> findByCodes(@Param("permissionPointCodes") List<String> permissionPointCodes);

    @Select({
            "<script>",
            "SELECT permission_point_code, display_name_zh, description, status, last_sync_source",
            "FROM pc_permission_point",
            "WHERE status = 'ACTIVE'",
            "AND permission_point_code IN",
            "<foreach collection='permissionPointCodes' item='code' open='(' separator=',' close=')'>",
            "#{code}",
            "</foreach>",
            "ORDER BY permission_point_code",
            "</script>"
    })
    List<PermissionPointRow> findActiveByCodes(@Param("permissionPointCodes") List<String> permissionPointCodes);
}
