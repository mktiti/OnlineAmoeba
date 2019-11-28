package hu.bme.softarch.amoeba.game

import java.math.BigInteger

enum class Sign {
    X, O;

    operator fun not() = when (this) {
        X -> O
        O -> X
    }

}

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

enum class ContainType {
    CONTAIN, INTERSECT, NONE;

    infix fun and(other: ContainType) = when (this) {
        CONTAIN -> other
        INTERSECT -> if (other == NONE) NONE else INTERSECT
        NONE -> NONE
    }

}

data class FieldRange(
    val bottomLeft: Pos,
    val topRight: Pos
) {

    private companion object {
        private fun contain(aMin: BigInteger, aMax: BigInteger, bMin: BigInteger, bMax: BigInteger): ContainType = when {
            aMin <= bMin && aMax >= bMax -> ContainType.CONTAIN
            bMin in (aMin..aMax) || bMax in (aMin..aMax) -> ContainType.INTERSECT
            else -> ContainType.NONE
        }
    }

    operator fun contains(pos: Pos)
            = bottomLeft.x <= pos.x && bottomLeft.y <= pos.y &&
              topRight.x >= pos.x && topRight.y >= pos.y

    fun contain(other: FieldRange): ContainType =
            contain(bottomLeft.x, topRight.x, other.bottomLeft.x, other.topRight.x) and
            contain(bottomLeft.y, topRight.y, other.bottomLeft.y, other.topRight.y)

}
