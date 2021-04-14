package org.letstrust.services.essif.mock

import org.letstrust.services.essif.UserWalletService

class UserAgent() {
    var rp = RelyingParty()

    fun requestAccess(): Boolean {
        println("2. [UA] Sign-On")
        var authReq = rp.signOn()

        println("6. [UA] Process Authentication Request: 302 openid://")
        println("7. [UA] Process Authentication Request: openid://")
        val authResp = UserWalletService.generateOidcAuthResponse(authReq)
        println("11. [UA] Authentication Response: Callback /callback 302")
        return rp.callback(authResp)
    }
}
