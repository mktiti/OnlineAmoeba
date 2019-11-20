package hu.bme.softarch.amoeba.web.util

object CodeGenerator {

    val digitAlphabet = ('0' .. '9').toList()

    val defaultAlphabet = ('a' .. 'z') + ('A' .. 'Z') + digitAlphabet

    fun generate(length: Int, alphabet: List<Char> = defaultAlphabet)
            = (1 .. length).joinToString(separator = "") { alphabet.random().toString() }

}