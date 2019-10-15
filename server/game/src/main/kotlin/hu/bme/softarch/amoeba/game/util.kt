package hu.bme.softarch.amoeba.game

import java.util.*

tailrec fun chain(start: Pos, length: Int, delta: LocalPos, rev: Boolean, acc: MutableList<Pos> = LinkedList()): List<Pos> = if (length > 0) {
    if (rev) {
        acc.add(0, start)
    } else {
        acc += start
    }
    chain(start + delta, length - 1, delta, rev, acc)
} else {
    acc
}