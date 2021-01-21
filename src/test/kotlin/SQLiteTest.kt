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
}
