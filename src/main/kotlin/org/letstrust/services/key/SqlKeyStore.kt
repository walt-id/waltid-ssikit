package org.letstrust.services.key

import com.nimbusds.jose.util.Base64
import mu.KotlinLogging
import org.letstrust.common.SqlDbManager
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.sql.Statement.RETURN_GENERATED_KEYS

private val log = KotlinLogging.logger() {}

object SqlKeyStore : KeyStore {

    private val db = SqlDbManager

    init {
        SqlDbManager.start()
    }

    override fun getKeyId(alias: String): String? {
        log.trace { "Loading keyId for alias \"${alias}\"." }
        SqlDbManager.getConnection().use { con ->
            con.prepareStatement("select k.name from lt_key k, lt_key_alias a where k.id = a.key_id and a.alias = ?")
                .use { stmt ->
                    stmt.setString(1, alias)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            val name = rs.getString("name")
                            log.trace { "keyId  \"${name}\" loaded." }
                            con.commit()
                            return name
                        }
                    }
                }
        }
        return null
    }

    override fun addAlias(keyId: String, alias: String) {

        log.debug { "Adding alias \"${alias}\" for keyId \"${keyId}\"" }

        SqlDbManager.getConnection().use { con ->
            con.prepareStatement("select k.id from lt_key k where k.name = ?").use { stmt ->
                stmt.setString(1, keyId)
                stmt.executeQuery().use { rs ->
                    con.commit()
                    if (rs.next()) {
                        rs.getInt("id").let { key_id ->
                            con.prepareStatement("insert into lt_key_alias (key_id, alias) values (?, ?)").use { stmt ->
                                stmt.setInt(1, key_id)
                                stmt.setString(2, alias)
                                stmt.executeUpdate()
                                con.commit()
                                log.trace { "Alias \"${alias}\" for keyId \"${keyId}\" saved successfully." }
                            }
                        }
                    }
                }
            }
        }

    }

    override fun saveKeyPair(keys: Keys) {

        log.debug { "Saving key \"${keys.keyId}\"" }

        SqlDbManager.getConnection().use { con ->
            con.prepareStatement("insert into lt_key (name, priv, pub, algorithm, provider) values (?, ?, ?, ?, ?)", RETURN_GENERATED_KEYS)
                .use { stmt ->
                    stmt.setString(1, keys!!.keyId)

                    if (keys.isByteKey()) {
                        keys.pair?.let { stmt.setString(2, Base64.encode(it.private.encoded).toString()) }
                        keys.pair?.let { stmt.setString(3, Base64.encode(it.public.encoded).toString()) }
                    } else {
                        keys.pair?.let { stmt.setString(2, Base64.encode(PKCS8EncodedKeySpec(it.private.encoded).encoded).toString()) }
                        keys.pair?.let { stmt.setString(3, Base64.encode(X509EncodedKeySpec(it.public.encoded).encoded).toString()) }
                    }

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
                                    log.trace { "Key \"${keys.keyId}\" saved successfully." }
                                } else {
                                    log.error { "Error when saving key \"${keys.keyId}\". Rolling back transaction." }
                                    con.rollback()
                                }
                            }
                        }
                    }
                }
        }
    }

    override fun listKeys(): List<Keys> {
        val keys = ArrayList<Keys>()
        SqlDbManager.getConnection().use { con ->
            con.prepareStatement("select * from lt_key").use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        var keyId = rs.getString("name")
                        var algorithm = rs.getString("algorithm")
                        var provider = rs.getString("provider")

                        if (provider == "BC" || provider == "SunEC") {
                            val kf = KeyFactory.getInstance(algorithm, provider)

                            var pub = kf.generatePublic(X509EncodedKeySpec(Base64.from(rs.getString("pub")).decode()))
                            var priv = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.from(rs.getString("priv")).decode()))

                            keys.add(Keys(keyId, KeyPair(pub, priv), provider))

                        } else {
                            var pub = Base64.from(rs.getString("pub")).decode()
                            var priv = Base64.from(rs.getString("priv")).decode()

                            var keyPair = KeyPair(BytePublicKey(pub, algorithm), BytePrivateKey(priv, algorithm))
                            keys.add(Keys(keyId, keyPair, provider))
                        }
                    }
                }
            }
            con.commit()
        }
        return keys;
    }

    override fun loadKeyPair(keyId: String): Keys? {
        log.debug { "Loading key \"${keyId}\"." }
        SqlDbManager.getConnection().use { con ->
            con.prepareStatement("select * from lt_key where name = ?").use { stmt ->
                stmt.setString(1, keyId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        var algorithm = rs.getString("algorithm")
                        var provider = rs.getString("provider")

                        val keys = when (provider) {
                            "BC"  -> {
                                val kf = KeyFactory.getInstance(algorithm, provider)

                                var pub = kf.generatePublic(X509EncodedKeySpec(Base64.from(rs.getString("pub")).decode()))
                                var priv = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.from(rs.getString("priv")).decode()))

                                Keys(keyId, KeyPair(pub, priv), provider)
                            }
                            "SunEC"  -> {
                                val kf = KeyFactory.getInstance(algorithm)

                                var pub = kf.generatePublic(X509EncodedKeySpec(Base64.from(rs.getString("pub")).decode()))
                                var priv = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.from(rs.getString("priv")).decode()))

                                Keys(keyId, KeyPair(pub, priv), provider)
                            }
                            else -> {
                                var pub = Base64.from(rs.getString("pub")).decode()
                                var priv = Base64.from(rs.getString("priv")).decode()

                                var keyPair = KeyPair(BytePublicKey(pub, algorithm), BytePrivateKey(priv, algorithm))
                                Keys(keyId, keyPair, provider)
                            }
                        }
                        con.commit()
                        return keys
                    }
                }
            }
        }
        return null
    }

    override fun deleteKeyPair(keyId: String) {
        log.debug { "Deleting key \"${keyId}\"." }
        SqlDbManager.getConnection().use { con ->
            con.prepareStatement("delete from lt_key where name = ?")
                .use { stmt ->
                    stmt.setString(1, keyId)
                    stmt.executeUpdate()
                }
            // TODO clean up key_alias
            con.commit()
        }
    }

}
