package org.letstrust.common

//fun readEssif(fileName: String) = File("src/test/resources/essif/${fileName}.json").readText(Charsets.UTF_8)

fun readEssif(fileName: String) = ClassLoader.getSystemResource("essif/${fileName}.json").readText(Charsets.UTF_8)
