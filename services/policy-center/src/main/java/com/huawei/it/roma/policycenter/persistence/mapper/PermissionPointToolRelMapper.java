package com.huawei.it.roma.policycenter.persistence.mapper;

import com.huawei.it.roma.policycenter.persistence.model.PermissionPointToolRelRow;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PermissionPointToolRelMapper {

    @Delete("""
            DELETE FROM pc_permission_point_tool_rel
            WHERE permission_point_code = #{permissionPointCode}
            """)
    int deleteByPermissionPointCode(@Param("permissionPointCode") String permissionPointCode);

    @Insert({
            "<script>",
            "INSERT INTO pc_permission_point_tool_rel (permission_point_code, tool_id, created_at)",
            "VALUES",
            "<foreach collection='relations' item='relation' separator=','>",
            "(#{relation.permissionPointCode}, #{relation.toolId}, CURRENT_TIMESTAMP)",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("relations") List<PermissionPointToolRelRow> relations);

    @Select({
            "<script>",
            "SELECT rel.permission_point_code, rel.tool_id",
            "FROM pc_permission_point_tool_rel rel",
            "JOIN pc_permission_point pp ON pp.permission_point_code = rel.permission_point_code",
            "WHERE pp.status = 'ACTIVE'",
            "AND rel.tool_id IN",
            "<foreach collection='toolIds' item='toolId' open='(' separator=',' close=')'>",
            "#{toolId}",
            "</foreach>",
            "ORDER BY rel.tool_id, rel.permission_point_code",
            "</script>"
    })
    List<PermissionPointToolRelRow> findActiveByToolIds(@Param("toolIds") List<String> toolIds);

    @Select({
            "<script>",
            "SELECT rel.permission_point_code, rel.tool_id",
            "FROM pc_permission_point_tool_rel rel",
            "JOIN pc_permission_point pp ON pp.permission_point_code = rel.permission_point_code",
            "WHERE pp.status = 'ACTIVE'",
            "AND rel.permission_point_code IN",
            "<foreach collection='permissionPointCodes' item='code' open='(' separator=',' close=')'>",
            "#{code}",
            "</foreach>",
            "ORDER BY rel.permission_point_code, rel.tool_id",
            "</script>"
    })
    List<PermissionPointToolRelRow> findActiveByPermissionPointCodes(
            @Param("permissionPointCodes") List<String> permissionPointCodes
    );

    @Select({
            "<script>",
            "SELECT permission_point_code, tool_id",
            "FROM pc_permission_point_tool_rel",
            "WHERE permission_point_code IN",
            "<foreach collection='permissionPointCodes' item='code' open='(' separator=',' close=')'>",
            "#{code}",
            "</foreach>",
            "ORDER BY permission_point_code, tool_id",
            "</script>"
    })
    List<PermissionPointToolRelRow> findByPermissionPointCodes(
            @Param("permissionPointCodes") List<String> permissionPointCodes
    );
}
