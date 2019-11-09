package hu.bme.softarch.amoeba.web.db

import hu.bme.softarch.amoeba.web.FullGame

interface GameRepo {

    fun fetchArchived(joinCode: String): FullGame

    fun archive()

}