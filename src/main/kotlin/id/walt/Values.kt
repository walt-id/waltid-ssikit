package id.walt

object Values {
    const val version = "1.13.0"
    val isSnapshot : Boolean
        get() = version.contains("SNAPSHOT")
}
