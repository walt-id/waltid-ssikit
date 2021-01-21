import com.nimbusds.jose.util.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import java.security.Security
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement
import kotlin.test.assertEquals
import java.sql.ResultSet

import java.sql.PreparedStatement
import java.util.ArrayList


class SQLiteTest {
    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun createKeyStoreDb() {

        val kms = KeyManagementService
        val keyId = kms.generateSecp256k1KeyPair()
        val keys = kms.loadKeys(keyId)
        val db = SqlDbManager
        var pubKeyStr = Base64.encode(keys!!.publicKey).toString()

        db.getConnection().use { con ->
            con.createStatement().use { stmt ->
                stmt.executeUpdate("drop table if exists lt_keystore")
                stmt.executeUpdate(
                    "create table if not exists lt_keystore(" +
                            "id integer primary key autoincrement, " +
                            "name string unique, " +
                            "key string)"
                )
                stmt.executeQuery("select * from lt_keystore").use { rs ->
                    while (rs.next()) {
                        // read the result set
                        println("name = " + rs.getString("name"))
                        println("key = " + rs.getString("key"))
                        var keyStr = rs.getString("key")
                        println("id = " + rs.getInt("id"))
                        assertEquals(pubKeyStr, keyStr)
                        var key = Base64.from(keyStr).decode()
                    }
                }
                stmt.executeQuery("SELECT last_insert_rowid() AS lastRowId").use { rs ->
                    println(rs.next())
                    println("lastRowId: " + rs.getInt("lastRowId"))
                }
            }
        }
    }


    @Test
    fun insertDataTest() {
        var c: Connection? = null
        var stmt: Statement? = null

        try {
            Class.forName("org.sqlite.JDBC")
            c = DriverManager.getConnection("jdbc:sqlite:test.db")
            c.autoCommit = false
            println("Opened database successfully")
            stmt = c.createStatement()
            var sql = "INSERT INTO COMPANY (ID,NAME,AGE,ADDRESS,SALARY) " +
                    "VALUES (1, 'Paul', 32, 'California', 20000.00 );"
            stmt.executeUpdate(sql)
            sql = "INSERT INTO COMPANY (ID,NAME,AGE,ADDRESS,SALARY) " +
                    "VALUES (2, 'Allen', 25, 'Texas', 15000.00 );"
            stmt.executeUpdate(sql)
            sql = "INSERT INTO COMPANY (ID,NAME,AGE,ADDRESS,SALARY) " +
                    "VALUES (3, 'Teddy', 23, 'Norway', 20000.00 );"
            stmt.executeUpdate(sql)
            sql = "INSERT INTO COMPANY (ID,NAME,AGE,ADDRESS,SALARY) " +
                    "VALUES (4, 'Mark', 25, 'Rich-Mond ', 65000.00 );"
            stmt.executeUpdate(sql)
            stmt.close()
            c.commit()
            c.close()
        } catch (e: Exception) {
            System.err.println(e.javaClass.name + ": " + e.message)
            System.exit(0)
        }
        println("Records created successfully")
    }

}
