package com.huawei.it.roma.policycenter.persistence.mapper;

import com.huawei.it.roma.policycenter.persistence.model.StrategyConditionValueRow;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StrategyConditionValueMapper {

    @Delete("""
            DELETE FROM pc_agent_strategy_condition_value
            WHERE strategy_id = #{strategyId}
            """)
    int deleteByStrategyId(@Param("strategyId") String strategyId);

    @Insert({
            "<script>",
            "INSERT INTO pc_agent_strategy_condition_value (strategy_id, value_order, condition_value)",
            "VALUES",
            "<foreach collection='values' item='value' separator=','>",
            "(#{value.strategyId}, #{value.valueOrder}, #{value.conditionValue})",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("values") List<StrategyConditionValueRow> values);

    @Select({
            "<script>",
            "SELECT strategy_id, value_order, condition_value",
            "FROM pc_agent_strategy_condition_value",
            "WHERE strategy_id IN",
            "<foreach collection='strategyIds' item='strategyId' open='(' separator=',' close=')'>",
            "#{strategyId}",
            "</foreach>",
            "ORDER BY strategy_id, value_order",
            "</script>"
    })
    List<StrategyConditionValueRow> findByStrategyIds(@Param("strategyIds") List<String> strategyIds);
}
