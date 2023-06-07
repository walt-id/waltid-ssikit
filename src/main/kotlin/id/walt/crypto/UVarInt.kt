package id.walt.crypto

/**
 * Unsigned variable-length integer
 * https://github.com/multiformats/unsigned-varint
 * Used for multicodec: https://github.com/multiformats/multicodec
 */
class UVarInt(val value: UInt) {
    val bytes: ByteArray = bytesFromUInt(value)
    val length
        get() = bytes.size

    private fun bytesFromUInt(num: UInt): ByteArray {
        val varInt = mutableListOf<Byte>()
        var rest = num
        while ((rest and MSBALL) != 0u) {
            varInt.add(((rest and 0xFFu) or MSB).toByte())
            rest = rest.shr(7)
        }
        varInt.add(rest.toByte())
        return varInt.toByteArray()
    }

    override fun toString(): String {
        return "0x${value.toString(16)}"
    }

    companion object {
        val MSB = 0x80u
        val LSB = 0x7Fu
        val MSBALL = 0xFFFFFF80u

        fun fromBytes(bytes: ByteArray): UVarInt {
            if (bytes.isEmpty())
                throw IllegalArgumentException("Empty byte array")

            var idx = 0
            var value = (bytes[idx].toUInt() and LSB)
            while (idx + 1 < bytes.size && (bytes[idx].toUInt() and MSB) != 0u) {
                idx++
                value = value or (bytes[idx].toUInt() and LSB).shl(idx * 7)
            }
            return UVarInt(value)
        }
    }
}
