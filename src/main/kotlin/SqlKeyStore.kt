import com.nimbusds.jose.util.Base64
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
            con.prepareStatement("insert into lt_key (name, priv, pub) values (?, ?, ?)", RETURN_GENERATED_KEYS)
                .use { stmt ->
                    stmt.setString(1, keys!!.keyId)
                    stmt.setString(2, Base64.encode(keys!!.privateKey).toString())
                    stmt.setString(3, Base64.encode(keys!!.publicKey).toString())
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
                        var pub = Base64.from(rs.getString("pub")).decode()
                        var priv = Base64.from(rs.getString("priv")).decode()
                        var algorithm = rs.getString("algorithm")
                        var provider = rs.getString("provider")
                        return Keys(keyId, priv, pub, algorithm, provider)
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
