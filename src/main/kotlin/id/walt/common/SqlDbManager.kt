package id.walt.common

import com.zaxxer.hikari.HikariDataSource
import id.walt.services.WaltIdServices
import mu.KotlinLogging
import java.sql.Connection
import java.sql.Statement

object SqlDbManager {
    private val log = KotlinLogging.logger {}

//    val JDBC_URL = "jdbc:sqlite:data/walt.db"
//    val JDBC_URL = "jdbc:sqlite::memory:"

    //  private val config: HikariConfig = HikariConfig()
    private var dataSource: HikariDataSource? = WaltIdServices.loadHikariDataSource()

    // TODO: Should be configurable
    val recreateDb = false

    fun start() {
//        config.jdbcUrl = JDBC_URL
//        config.maximumPoolSize = 1
//        config.isAutoCommit = false
//        config.setUsername("user")
//        config.setPassword("password")
//        config.addDataSourceProperty("cachePrepStmts", "true")
//        config.addDataSourceProperty("prepStmtCacheSize", "250")
//        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        // Logger.getLogger("").level = ALL

        //  ds = HikariDataSource(config)
        dataSource = WaltIdServices.loadHikariDataSource()
        createDatabase()
    }

    fun stop() {
        dataSource?.close()
    }

    private fun createDatabase() {
        getConnection().use { con ->
            con.createStatement().use { stmt ->

                if (recreateDb) {
                    log.debug { "Recreating database" }
                    stmt.executeUpdate("drop table if exists lt_key")
                    stmt.executeUpdate("drop table if exists lt_key_alias")
                }

                // Create lt_key
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

    fun getConnection(): Connection {
        // var connection = DriverManager.getConnection(JDBC_URL)
        return dataSource!!.connection!!
    }

    fun getLastRowId(statement: Statement): Int {
        val rs = statement.executeQuery("select last_insert_rowid() AS lastRowId")
        rs.next()
        return rs.getInt("lastRowId")
    }
}
