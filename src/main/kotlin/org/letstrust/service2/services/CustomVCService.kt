package org.letstrust.service2.services

class CustomVCService : LetstrustVCService() {
    override fun import() = println("VC Custom import")
}
