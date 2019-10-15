package hu.bme.softarch.amoeba.game

import java.math.BigInteger.ONE

interface Field {

    operator fun get(pos: Pos): Sign?

}

interface MutableField : Field {

    operator fun set(pos: Pos, sign: Sign): List<Pos>?

}

class MapField(
    private val toWin: Int,
    blockSize: Int = 512
) : MutableField {

    companion object {
        private val halfDirs = listOf(
            LocalPos(0, 1),
            LocalPos(1, 1),
            LocalPos(1, 0),
            LocalPos(1, -1)
        )
    }

    private val blockSize = blockSize.toBigInteger()

    private val blocks = mutableListOf<MutableFieldBlock>()

    private fun blockOf(pos: Pos): MutableFieldBlock? = blocks.find { it.contains(pos) }

    private fun createBlock(pos: Pos): MutableFieldBlock {
        val bottomLeft = Pos(pos.x % blockSize, pos.y % blockSize)
        val topRight = Pos(
            bottomLeft.x + blockSize - ONE,
            bottomLeft.y + blockSize - ONE
        )

        return MapFieldBlock(FieldRange(bottomLeft, topRight)).apply {
            blocks += this
        }
    }

    private tailrec fun lineLength(sign: Sign, pos: Pos, delta: LocalPos, acc: Int = 0): Int = if (get(pos) == sign) {
        lineLength(sign, pos + delta, delta, acc + 1)
    } else {
        acc
    }

    private fun isWon(pos: Pos, sign: Sign, toWin: Int): List<Pos>? {
        for (dir in halfDirs) {
            val topLength = lineLength(sign, pos, dir)
            val bottomLength = lineLength(sign, pos, -dir)

            if (topLength + bottomLength - 1 >= toWin) {
                return chain(pos, topLength, dir, true) + chain(pos, bottomLength, -dir, false).drop(1)
            }
        }

        return null
    }

    override fun get(pos: Pos): Sign? = blockOf(pos)?.get(pos)

    override fun set(pos: Pos, sign: Sign): List<Pos>? {
        get(pos)?.let {
            throw IllegalArgumentException("Pos $pos is already occupied!")
        }

        val block = blockOf(pos) ?: createBlock(pos)
        block[pos] = sign
        return isWon(pos, sign, toWin)
    }
}