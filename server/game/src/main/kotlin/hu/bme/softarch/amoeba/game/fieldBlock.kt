package hu.bme.softarch.amoeba.game

internal interface FieldBlock {

    val range: FieldRange

    operator fun get(pos: LocalPos): Sign?

}

internal fun FieldBlock.toLocal(pos: Pos): LocalPos
        = LocalPos((pos.x - range.bottomLeft.x).toInt(), (pos.y - range.bottomLeft.y).toInt())

internal operator fun FieldBlock.contains(pos: Pos) = range.contains(pos)

internal operator fun FieldBlock.get(pos: Pos): Sign? = get(toLocal(pos))

internal class EmptyBlock(
    override val range: FieldRange
) : FieldBlock {

    override fun get(pos: LocalPos): Sign? = null

}

internal interface MutableFieldBlock : FieldBlock {

    operator fun set(pos: LocalPos, sign: Sign)

}

internal operator fun MutableFieldBlock.set(pos: Pos, sign: Sign) {
    set(toLocal(pos), sign)
}

internal class MapFieldBlock(
    override val range: FieldRange
) : MutableFieldBlock {

    private val data = mutableMapOf<LocalPos, Sign>()

    override fun get(pos: LocalPos): Sign? = data[pos]

    override fun set(pos: LocalPos, sign: Sign) {
        data[pos] = sign
    }

}