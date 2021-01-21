import com.nimbusds.jose.util.Base64
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement

object SqlDbManager {

    val JDBC_URL = "jdbc:sqlite:test2.db"

    init {
        createDatabase()
    }

    fun createDatabase() {
        var connection: Connection? = null
        try {
            // create a database connection
            connection = getConnection()
            val statement = connection.createStatement()
            statement.queryTimeout = 30 // set timeout to 30 sec.

            // Create lt_key
            // statement.executeUpdate("drop table if exists lt_key")
            statement.executeUpdate(
                "create table if not exists lt_key(" +
                        "id integer primary key autoincrement, " +
                        "name string unique, " +
                        "priv string, " +
                        "pub string)"
            )

            // Create lt_key_meta
            // statement.executeUpdate("drop table if exists lt_key_meta")
            statement.executeUpdate(
                "create table if not exists lt_key_meta(" +
                        "id integer primary key autoincrement, " +
                        "algorithm string, " +
                        "provider string)"
            )

            // Create lt_key_alias
            // statement.executeUpdate("drop table if exists lt_key_alias")
            statement.executeUpdate(
                "create table if not exists lt_key_alias(" +
                        "id integer primary key autoincrement, " +
                        "key_id integer, " +
                        "alias string)"
            )

        } catch (e: SQLException) {
            // if the error message is "out of memory", it probably means no database file is found
            e.printStackTrace()
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                // connection close failed.
                System.err.println(e.message)
            }
        }
    }

    fun getConnection(autoCommit: Boolean = true): Connection {
        var connection = DriverManager.getConnection(JDBC_URL)
        connection.autoCommit = autoCommit
        return connection
    }

    fun getLastRowId(statement: Statement): Int {
        val rs = statement.executeQuery("SELECT last_insert_rowid() AS lastRowId")
        rs.next()
        return rs.getInt("lastRowId")
    }
}
