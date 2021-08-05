package id.walt.services

import id.walt.servicematrix.BaseService

/**
 * This class extends the service-matrix BaseService in order to run the walt.id specific initialization routines on startup.
 */
abstract class WaltIdService : BaseService() {
    init {
        WaltIdServices
    }
}
