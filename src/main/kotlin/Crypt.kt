import io.ipfs.multibase.Base58
import io.ipfs.multibase.Multibase


fun ByteArray.encodeBase58 (): String = Base58.encode(this)

fun String.decodeBase58 (): ByteArray = Base58.decode(this)

fun ByteArray.encodeMultiBase58 (): String = Multibase.encode(Multibase.Base.Base58BTC, this)

fun String.decodeMultiBase58 (): ByteArray  = Multibase.decode(this)
