package id.walt.crypto

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe

class UVarIntTest : AnnotationSpec() {

    val bytes_0x0 = byteArrayOf(0x00)
    val bytes_0x1205 = byteArrayOf(0x85.toByte(), 0x24.toByte())
    val bytes_0x01 = byteArrayOf(0x01)
    val bytes_0xed = byteArrayOf(0xed.toByte(), 0x01)

    @Test
    fun testEncode() {
        val uVarInt_0x1205 = UVarInt(0x1205u)
        uVarInt_0x1205.length shouldBe bytes_0x1205.size
        uVarInt_0x1205.bytes shouldBe bytes_0x1205

        val uVarInt_0x01 = UVarInt(0x01u)
        uVarInt_0x01.length shouldBe bytes_0x01.size
        uVarInt_0x01.bytes shouldBe bytes_0x01

        val uVarInt_0xed = UVarInt(0xedu)
        uVarInt_0xed.length shouldBe bytes_0xed.size
        uVarInt_0xed.bytes shouldBe bytes_0xed

        val uVarInt_0x0 = UVarInt(0x0u)
        uVarInt_0x0.length shouldBe bytes_0x0.size
        uVarInt_0x0.bytes shouldBe bytes_0x0
    }

    @Test
    fun testDecode() {
        val uVarInt_0x1205 = UVarInt.fromBytes(bytes_0x1205)
        uVarInt_0x1205.value shouldBe 0x1205u

        val uVarInt_0x01 = UVarInt.fromBytes(bytes_0x01)
        uVarInt_0x01.value shouldBe 0x01u

        val uVarInt_0xed = UVarInt.fromBytes(bytes_0xed)
        uVarInt_0xed.value shouldBe 0xedu

        val uVarInt_0x0 = UVarInt.fromBytes(bytes_0x0)
        uVarInt_0x0.value shouldBe 0u
    }
}
