package id.walt.services.keystore

import id.walt.common.SqlDbManager
import id.walt.crypto.Key
import id.walt.crypto.KeyId
import id.walt.crypto.buildKey
import id.walt.crypto.toBase64
import mu.KotlinLogging
import java.sql.Connection
import java.sql.Statement.RETURN_GENERATED_KEYS

open class SqlKeyStoreService : KeyStoreService() {

    private val log = KotlinLogging.logger {}

    init {
        SqlDbManager.start()
    }

    override fun store(key: Key) {
        log.debug { "Saving key \"${key}\"..." }

        SqlDbManager.getConnection().use { connection ->
            connection.apply {
                prepareStatement(
                    "insert into lt_key (name, priv, pub, algorithm, provider) values (?, ?, ?, ?, ?)",
                    RETURN_GENERATED_KEYS
                ).use { statement ->
                    key.run {
                        listOf(
                            keyId.id,
                            keyPair!!.private?.toBase64(),
                            keyPair!!.public?.toBase64(),
                            algorithm.name,
                            cryptoProvider.name
                        ).forEachIndexed { index, str -> str?.let { statement.setString(index + 1, str) } }
                    }

                    when {
                        statement.executeUpdate() == UPDATE_SUCCESS -> {
                            commit()
                            log.trace { "Key \"${key}\" saved successfully." }
                        }

                        else -> {
                            log.error { "Error when saving key \"${key}\". Rolling back transaction." }
                            rollback()
                        }
                    }
                }
            }
        }
    }

    override fun load(alias: String, keyType: KeyType): Key {
        log.debug { "Loading key \"${alias}\"..." }
        var key: Key? = null

        val keyId = getKeyId(alias) ?: alias

        SqlDbManager.getConnection().use { connection ->
            connection.prepareStatement("select * from lt_key where name = ?").use { statement ->
                statement.setString(1, keyId)
                statement.executeQuery().use { result ->
                    if (result.next()) {
                        key = buildKey(
                            keyId,
                            result.getString("algorithm"),
                            result.getString("provider"),
                            result.getString("pub"),
                            if (keyType == KeyType.PRIVATE) result.getString("priv") else null
                        )
                    }
                }
                connection.commit()
            }
        }
        return key ?: throw IllegalArgumentException("Could not load key: $keyId")
    }


    override fun getKeyId(alias: String): String? {
        log.trace { "Loading keyId for alias \"${alias}\"..." }

        SqlDbManager.getConnection().use { con ->
            con.prepareStatement("select k.name from lt_key k, lt_key_alias a where k.id = a.key_id and a.alias = ?")
                .use { statement ->
                    statement.setString(1, alias)

                    statement.executeQuery().use { rs ->
                        if (rs.next()) {
                            val id = rs.getString("name")
                            log.trace { "keyId \"${id}\" loaded." }
                            con.commit()
                            return id
                        }
                    }
                }
        }
        return null
    }

    override fun addAlias(keyId: KeyId, alias: String) {
        log.debug { "Adding alias \"${alias}\" for keyId \"${keyId}\"..." }

        SqlDbManager.getConnection().use { con ->

            con.prepareStatement("select k.id from lt_key k where k.name = ?").use { statement ->
                statement.setString(1, keyId.id)

                statement.executeQuery().use { rs ->
                    if (rs.next()) {
                        rs.getInt("id").let { keyId ->
                            con.prepareStatement("insert into lt_key_alias (key_id, alias) values (?, ?)")
                                .use { stmt ->
                                    stmt.setInt(1, keyId)
                                    stmt.setString(2, alias)
                                    stmt.executeUpdate()
                                    log.trace { "Alias \"${alias}\" for keyId \"${keyId}\" saved successfully." }
                                }
                        }
                    }
                }
            }
            con.commit()
        }
    }

    override fun listKeys(): List<Key> {
        val keys = ArrayList<Key>()
        SqlDbManager.getConnection().use { con ->
            con.prepareStatement("select * from lt_key").use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        keys.add(
                            buildKey(
                                rs.getString("name"),
                                rs.getString("algorithm"),
                                rs.getString("provider"),
                                rs.getString("pub"),
                                rs.getString("priv")
                            )
                        )
                    }
                }
            }
            con.commit()
        }
        return keys
    }

    private fun deleteKeyAndAliases(keyName: String, con: Connection) {
        con.prepareStatement("select id from lt_key where name = ?").use { stmt ->
            stmt.setString(1, keyName)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    con.prepareStatement("delete from lt_key_alias where key_id = ?").use { stmt ->
                        stmt.setString(1, rs.getString("id"))
                        stmt.executeUpdate()
                    }
                }
            }
        }

        con.prepareStatement("delete from lt_key where name = ?")
            .use { stmt ->
                stmt.setString(1, keyName)
                stmt.executeUpdate()
            }
    }

    private fun deleteKeyByAliases(alias: String, con: Connection) {
        con.prepareStatement("select key_id from lt_key_alias where alias = ?").use { stmt ->
            stmt.setString(1, alias)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    con.prepareStatement("delete from lt_key where id = ?").use { stmt ->
                        stmt.setString(1, rs.getString("key_id"))
                        stmt.executeUpdate()
                    }
                    con.prepareStatement("delete from lt_key_alias where key_id = ?").use { stmt ->
                        stmt.setString(1, rs.getString("key_id"))
                        stmt.executeUpdate()
                    }
                }
            }
        }
    }

    override fun delete(alias: String) {
        log.debug { "Deleting key \"${alias}\"." }

        SqlDbManager.getConnection().use { con ->

            deleteKeyAndAliases(alias, con)

            deleteKeyByAliases(alias, con)

            con.commit()
        }
    }

    companion object {
        private const val UPDATE_SUCCESS = 1
    }

}
