package hu.bme.softarch.amoeba.game

fun main() {
    val field: MutableField = MapField(3)
    var sign: Sign = Sign.X

    while (true) {
        print("$sign>")
        val coords: List<Int> = readLine()?.split(" ")?.mapNotNull(String::toIntOrNull) ?: emptyList()

        if (coords.size != 2) {
            continue
        } else {
            try {
                field.set(Pos(coords[0], coords[1]), sign)?.let {
                    println("WIN: $it")
                }
            } catch (iae: IllegalArgumentException) {
                System.err.println("Space already occupied!")
                continue
            }
        }

        sign = if (sign == Sign.X) Sign.O else Sign.X
    }

}