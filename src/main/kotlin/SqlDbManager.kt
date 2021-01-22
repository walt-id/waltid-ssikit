import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.SQLException




object SqlDbManager {

    val JDBC_URL = "jdbc:sqlite:test3.db"

    private val config: HikariConfig = HikariConfig()
    private var ds: HikariDataSource? = null

    init {
        config.setJdbcUrl("jdbc:sqlite:test3.db") //jdbc:sqlite::memory:
        config.maximumPoolSize = 15
//        config.setUsername("user")
//        config.setPassword("password")
//        config.addDataSourceProperty("cachePrepStmts", "true")
//        config.addDataSourceProperty("prepStmtCacheSize", "250")
//        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        ds = HikariDataSource(config)

        createDatabase()
    }

    fun getConnection(): Connection {
        return ds!!.connection!!
    }


    fun createDatabase() {
        this.getConnection().use { con ->
            con!!.createStatement().use { stmt ->

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

        }
    }

    fun getConnection(autoCommit: Boolean = true): Connection {
        var connection = DriverManager.getConnection(JDBC_URL)
        connection.autoCommit = autoCommit
        return connection
    }

    fun getLastRowId(statement: Statement): Int {
        val rs = statement.executeQuery("select last_insert_rowid() AS lastRowId")
        rs.next()
        return rs.getInt("lastRowId")
    }
}
