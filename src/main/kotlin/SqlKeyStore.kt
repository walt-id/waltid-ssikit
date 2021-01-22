import com.nimbusds.jose.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.sql.Statement.RETURN_GENERATED_KEYS


object SqlKeyStore : KeyStore {

    private val db = SqlDbManager

    init {
        db.createDatabase()
    }

    override fun getKeyId(keyId: String): String? {
        db.getConnection().use { con ->
            con.prepareStatement("select k.name from lt_key k, lt_key_alias a where k.id = a.key_id and a.alias = ?")
                .use { stmt ->
                    stmt.setString(1, keyId)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            return rs.getString("name")
                        }
                    }
                }
        }
        return null
    }

    override fun addAlias(keyId: String, identifier: String) {
        db.getConnection().use { con ->
            con.prepareStatement("select k.id from lt_key k where k.name = ?").use { stmt ->
                stmt.setString(1, keyId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        rs.getInt("id").let { key_id ->
                            print("keyid: " + key_id)
                            con.prepareStatement("insert into lt_key_alias (key_id, alias) values (?, ?)").use { stmt ->
                                stmt.setInt(1, key_id)
                                stmt.setString(2, identifier)
                                stmt.executeUpdate()
                            }
                        }
                    }
                }
            }
        }

    }

    override fun saveKeyPair(keys: Keys) {

        db.getConnection(false).use { con ->
            con.prepareStatement("insert into lt_key (name, priv, pub, algorithm, provider) values (?, ?, ?, ?, ?)", RETURN_GENERATED_KEYS)
                .use { stmt ->
                    stmt.setString(1, keys!!.keyId)

                    keys.pair?.let { stmt.setString(2, Base64.encode(X509EncodedKeySpec(it.private.encoded).encoded).toString()) }
                    keys.pair?.let { stmt.setString(3, Base64.encode(X509EncodedKeySpec(it.public.encoded).encoded).toString()) }

                    keys.privateKey?.let { stmt.setString(2, Base64.encode(it).toString()) }
                    keys.publicKey?.let { stmt.setString(3, Base64.encode(keys!!.publicKey).toString()) }

                    keys.algorithm?.let { stmt.setString(4, it) }
                    keys.provider?.let { stmt.setString(5, it) }

                    stmt.executeUpdate()

                    stmt.generatedKeys.use { generatedKeys ->
                        if (generatedKeys.next()) {
                            val key_id = generatedKeys.getInt(1)
                            con.prepareStatement("insert into lt_key_alias (key_id, alias) values (?, ?)").use { stmt ->
                                stmt.setInt(1, key_id)
                                stmt.setString(2, keys!!.keyId)
                                if (stmt.executeUpdate() == 1) {
                                    con.commit()
                                    println("key ${keys.keyId} saved successfully")
                                } else {
                                    println("key ${keys.keyId} not saved successfully")
                                }
                            }
                        }
                    }
                }
        }
    }

    override fun loadKeyPair(keyId: String): Keys? {
        db.getConnection().use { con ->
            con.prepareStatement("select * from lt_key where name = ?").use { stmt ->
                stmt.setString(1, keyId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        var algorithm = rs.getString("algorithm")
                        var provider = rs.getString("provider")

                        if (provider == "BC") {
                            val kf = KeyFactory.getInstance(algorithm, provider)

                            var pub = kf.generatePublic(X509EncodedKeySpec(Base64.from(rs.getString("pub")).decode()))
                            var priv = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.from(rs.getString("priv")).decode()))

                            return Keys(keyId, KeyPair(pub, priv), algorithm, provider)

                        } else {
                            var pub = Base64.from(rs.getString("pub")).decode()
                            var priv = Base64.from(rs.getString("priv")).decode()

                            return Keys(keyId, priv, pub, algorithm, provider)
                        }
                    }
                }
            }
        }
        return null
    }

    override fun deleteKeyPair(keyId: String) {
        db.getConnection().use { con ->
            con.prepareStatement("delete from lt_key where name = ?")
                .use { stmt ->
                    stmt.setString(1, keyId)
                    stmt.executeUpdate()
                }
            // TODO clean up key_alias
        }
    }

}
