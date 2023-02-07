package id.walt.crypto

import com.nimbusds.jwt.JWTParser
import java.time.Instant
import java.time.ZoneId

object JwtUtils {

    fun isJwtExpired(jwt: String) = JWTParser.parse(jwt).jwtClaimsSet.expirationTime.toInstant().isBefore(Instant.now())
    fun getJwtExpirationIfExpired(jwt: String): Instant? = JWTParser.parse(jwt).jwtClaimsSet.expirationTime.toInstant().let {
        if (it.isBefore(Instant.now())) it else null
    }

    fun getJwtExpirationMessageIfExpired(jwt: String): String? = getJwtExpirationIfExpired(jwt).let {
        if (it == null) null else
            "EBSI Bearer token expired at $it UTC - it is ${Instant.now()} UTC " +
                    "(expired ${it.atZone(ZoneId.systemDefault())} system time zone)."
    }


}
