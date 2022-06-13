package id.walt.common

import com.beust.klaxon.JsonObject

fun JsonObject.deepMerge(source: JsonObject): JsonObject {
  for (key in source.keys) {
    val value: Any? = source[key]
    if (!this.contains(key)) {
      // new value for "key":
      this.put(key, value)
    } else {
      // existing value for "key" - recursively deep merge:
      if (value is JsonObject) {
        val valueJson: JsonObject = value
        (this[key] as JsonObject).deepMerge(valueJson)
      } else {
        this[key] = value
      }
    }
  }
  return this
}
