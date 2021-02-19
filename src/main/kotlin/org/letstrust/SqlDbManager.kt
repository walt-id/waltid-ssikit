package org.letstrust

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.Statement


object SqlDbManager {

    // TODO Should be configurable
    val JDBC_URL = "jdbc:sqlite:letstrust.db"
    //val JDBC_URL = "jdbc:sqlite::memory:"

    private val config: HikariConfig = HikariConfig()
    private var ds: HikariDataSource? = null

    init {
        config.jdbcUrl = JDBC_URL
        config.maximumPoolSize = 15
        config.isAutoCommit = false
//        config.setUsername("user")
//        config.setPassword("password")
//        config.addDataSourceProperty("cachePrepStmts", "true")
//        config.addDataSourceProperty("prepStmtCacheSize", "250")
//        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        // Logger.getLogger("").level = ALL

        ds = HikariDataSource(config)

        createDatabase()
    }

    fun createDatabase() {
        getConnection().use { con ->
            con.createStatement().use { stmt ->

                // Create lt_key
                stmt.executeUpdate("drop table if exists lt_key")
                stmt.executeUpdate(
                    "create table if not exists lt_key(" +
                            "id integer primary key autoincrement, " +
                            "name string unique, " +
                            "algorithm string, " +
                            "provider string," +
                            "priv string, " +
                            "pub string)"
                )

                // Create lt_key_alias
                stmt.executeUpdate("drop table if exists lt_key_alias")
                stmt.executeUpdate(
                    "create table if not exists lt_key_alias(" +
                            "id integer primary key autoincrement, " +
                            "key_id integer, " +
                            "alias string unique)"
                )
            }
            con.commit()
        }
    }

//    fun getConnection(autoCommit: Boolean = true): Connection {
//        return ds!!.connection!!
//    }

    fun getConnection(): Connection {
        // var connection = DriverManager.getConnection(JDBC_URL)
        return ds!!.connection!!
    }

    fun getLastRowId(statement: Statement): Int {
        val rs = statement.executeQuery("select last_insert_rowid() AS lastRowId")
        rs.next()
        return rs.getInt("lastRowId")
    }
}
