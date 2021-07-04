package org.letstrust.services.essif.mock

import org.letstrust.services.essif.UserWalletService

class UserAgent {

    fun requestAccess(): Boolean {
        println("2. [UA] Sign-On")
        var authReq = RelyingParty.signOn()

        println("6. [UA] Process Authentication Request: 302 openid://")
        println("7. [UA] Process Authentication Request: openid://")
        return UserWalletService.oidcAuthResponse(authReq)
    }
}
