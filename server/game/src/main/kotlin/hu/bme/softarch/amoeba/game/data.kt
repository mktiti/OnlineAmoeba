package hu.bme.softarch.amoeba.game

import java.math.BigInteger

enum class Sign { X, O }

data class Pos(
    val x: BigInteger,
    val y: BigInteger
) {

    constructor(x: Int, y: Int) : this(x.toBigInteger(), y.toBigInteger())

    operator fun plus(offset: LocalPos): Pos = Pos(x + offset.x.toBigInteger(), y + offset.y.toBigInteger())

    override fun toString() = "($x, $y)"

}

data class LocalPos(
    val x: Int,
    val y: Int
) {

    fun plus(offset: LocalPos, xLimit: Int, yLimit: Int): LocalPos? {
        val res = LocalPos(x + offset.x, y + offset.y)
        return if (res.x < xLimit && res.y < yLimit) {
            res
        } else {
            null
        }
    }

    operator fun unaryMinus() = LocalPos(-x, -y)

}

data class FieldRange(
    val bottomLeft: Pos,
    val topRight: Pos
) {

    operator fun contains(pos: Pos)
            = bottomLeft.x <= pos.x && bottomLeft.y <= pos.y &&
              topRight.x >= pos.x && topRight.y >= pos.y

}