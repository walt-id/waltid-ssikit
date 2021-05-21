package org.letstrust.service2

abstract class LetstrustService {
    protected open val implementation: LetstrustService get() = throw NotImplementedError()
}
