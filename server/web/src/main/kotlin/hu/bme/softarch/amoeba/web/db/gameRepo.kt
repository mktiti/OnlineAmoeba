package hu.bme.softarch.amoeba.web.db

import hu.bme.softarch.amoeba.web.api.FullGame

interface GameRepo {

    fun fetchArchived(id: Long): FullGame?

    fun archive(game: FullGame)

}