package id.walt.common

import com.nimbusds.jose.util.Base64
import id.walt.crypto.KeyAlgorithm
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class SQLiteTest : AnnotationSpec() {
    val keyService = KeyService.getService()

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    @Test
    fun createKeyStoreDb() {
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val key = keyService.load(keyId.id, KeyType.PRIVATE)
        val database = SqlDbManager
        val pubKeyStr = Base64.encode(key.getPublicKey().encoded).toString()


        database.getConnection().use { con ->
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
                        val keyStr = rs.getString("key")
                        println("id = " + rs.getInt("id"))
                        pubKeyStr shouldBe keyStr
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
