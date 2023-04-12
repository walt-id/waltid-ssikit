package id.walt.signatory.revocation

import com.beust.klaxon.Klaxon
import id.walt.common.resolveContent
import id.walt.model.credential.status.StatusList2021EntryCredentialStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

internal class StatusList2021ServiceTest : StringSpec({
    val sut = StatusList2021Service
    val rootPath = "src/test/resources/credential-status/"
    val statusLisCredential = resolveContent(rootPath + "status-list-credential.json")

    "test result" {
        forAll(
            row("vc-status-revoked.json", true),
            row("vc-status-unrevoked.json", false),
        ) { vcPath, isRevoked ->
            val vcStatus = Klaxon().parse<StatusList2021EntryCredentialStatus>(resolveContent(rootPath + vcPath))!!
            mockkStatic(::resolveContent)
            every { resolveContent(any()) } returns statusLisCredential

            val result = sut.checkRevoked(vcStatus)

            result shouldBe isRevoked

            unmockkStatic(::resolveContent)
        }
    }

    "test throwing" {
        forAll(
            row("vc-status-wrong-purpose.json", IllegalArgumentException::class, "Status purposes don't match"),
            row("vc-status-missing-index.json", IllegalArgumentException::class, "Couldn't parse status list index"),
        ) { vcPath, throwing, message ->
            val vcStatus = Klaxon().parse<StatusList2021EntryCredentialStatus>(resolveContent(rootPath + vcPath))!!
            mockkStatic(::resolveContent)
            every { resolveContent(any()) } returns statusLisCredential

            val exception = shouldThrow<Exception> {
                sut.checkRevoked(vcStatus)
            }
            exception.message shouldBe message
            exception::class shouldBe throwing

            unmockkStatic(::resolveContent)
        }
    }
})
