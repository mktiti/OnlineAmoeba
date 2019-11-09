package hu.bme.softarch.amoeba.web

object CodeGenerator {

    private val defaultAlphabet = ('a' .. 'z') + ('A' .. 'Z') + ('0' .. '9')

    fun generate(length: Int, alphabet: List<Char> = defaultAlphabet)
            = (1 .. length).joinToString { defaultAlphabet.random().toString() }

}