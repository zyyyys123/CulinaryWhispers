package com.zyyyys.culinarywhispers.common.db.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SchemaPatchMapper {
    String currentDatabase();

    Integer countTable(@Param("schema") String schema, @Param("table") String table);

    Integer countColumn(@Param("schema") String schema, @Param("table") String table, @Param("column") String column);

    Integer countIndex(@Param("schema") String schema, @Param("table") String table, @Param("index") String index);

    void exec(@Param("sql") String sql);
}
