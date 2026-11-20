package com.projects.adnan.EmailProject.config;

import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

import java.sql.Types;

public class SafeSQLiteDialect extends SQLiteDialect {

    private static final IdentityColumnSupport IDENTITY_SUPPORT = new IdentityColumnSupportImpl() {
        @Override
        public boolean supportsIdentityColumns() {
            return true;
        }

        @Override
        public boolean hasDataTypeInIdentityColumn() {
            return true;
        }

        @Override
        public String getIdentityColumnString(int type) {
            return "";
        }

        @Override
        public String getIdentitySelectString(String table, String column, int type) {
            return "select last_insert_rowid()";
        }
    };

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return IDENTITY_SUPPORT;
    }

    @Override
    protected String columnType(int sqlTypeCode) {
        if (sqlTypeCode == Types.BIGINT) {
            return "integer";
        }
        return super.columnType(sqlTypeCode);
    }
}
