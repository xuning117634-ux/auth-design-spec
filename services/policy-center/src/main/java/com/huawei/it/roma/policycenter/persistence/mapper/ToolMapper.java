package com.huawei.it.roma.policycenter.persistence.mapper;

import com.huawei.it.roma.policycenter.persistence.model.ToolRow;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ToolMapper {

    @Update("""
            UPDATE pc_tool
            SET display_name_zh = #{displayNameZh},
                updated_at = CURRENT_TIMESTAMP
            WHERE tool_id = #{toolId}
            """)
    int update(ToolRow row);

    @Insert("""
            INSERT INTO pc_tool (
                tool_id,
                display_name_zh,
                created_at,
                updated_at
            )
            VALUES (
                #{toolId},
                #{displayNameZh},
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            """)
    int insert(ToolRow row);

    @Select({
            "<script>",
            "SELECT tool_id",
            "FROM pc_tool",
            "WHERE tool_id IN",
            "<foreach collection='toolIds' item='toolId' open='(' separator=',' close=')'>",
            "#{toolId}",
            "</foreach>",
            "</script>"
    })
    List<String> findExistingToolIds(@Param("toolIds") List<String> toolIds);

    @Select({
            "<script>",
            "SELECT tool_id, display_name_zh",
            "FROM pc_tool",
            "WHERE tool_id IN",
            "<foreach collection='toolIds' item='toolId' open='(' separator=',' close=')'>",
            "#{toolId}",
            "</foreach>",
            "</script>"
    })
    List<ToolRow> findByToolIds(@Param("toolIds") List<String> toolIds);

    @Select({
            "<script>",
            "SELECT DISTINCT t.tool_id, t.display_name_zh",
            "FROM pc_tool t",
            "JOIN pc_permission_point_tool_rel rel ON rel.tool_id = t.tool_id",
            "JOIN pc_permission_point pp ON pp.permission_point_code = rel.permission_point_code",
            "WHERE pp.status = 'ACTIVE'",
            "AND rel.permission_point_code IN",
            "<foreach collection='permissionPointCodes' item='code' open='(' separator=',' close=')'>",
            "#{code}",
            "</foreach>",
            "ORDER BY t.tool_id",
            "</script>"
    })
    List<ToolRow> findActiveToolsByPermissionPointCodes(@Param("permissionPointCodes") List<String> permissionPointCodes);
}
