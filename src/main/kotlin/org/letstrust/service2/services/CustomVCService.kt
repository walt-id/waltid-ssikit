package org.letstrust.service2.services

class CustomVCService : VcServiceDefault() {
    override fun import() = println("VC Custom import")
}
