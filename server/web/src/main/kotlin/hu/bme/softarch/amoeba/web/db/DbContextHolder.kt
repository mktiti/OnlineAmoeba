package hu.bme.softarch.amoeba.web.db

import hu.bme.softarch.amoeba.web.util.logger
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import java.math.BigInteger
import java.nio.file.Path

object DbContextHolder {

    private const val FILE_CONNECT_STRING_TEMPLATE = """jdbc:hsqldb:file:"%s"/amoeba-db/;shutdown=true"""

    private const val DB_INIT_FILE = "/sql/create-db.sql"
    private const val DB_DROP_FILE = "/sql/drop-db.sql"

    private val log by logger()

    var connection: Jdbi? = null
        private set

    fun initFileDb(path: Path) {
        log.info("Database location: ${path.toAbsolutePath()}")
        initDb(FILE_CONNECT_STRING_TEMPLATE.format(path.toAbsolutePath()))
    }

    internal fun createJdbi(connectionString: String, dropPrevious: Boolean = false): Jdbi {
        return Jdbi.create(connectionString).apply {
            fun runScript(name: String) {
                useTransaction<DatabaseException> { handle ->
                    handle.createScript(DbContextHolder::class.java.getResource(name).readText()).execute()
                }
            }

            installPlugin(KotlinPlugin())
            installPlugin(KotlinSqlObjectPlugin())
            registerColumnMapper(BigInteger::class.java) { rs, col, _ -> rs.getLong(col).toBigInteger() }

            if (dropPrevious) {
                runScript(DB_DROP_FILE)
            }
            runScript(DB_INIT_FILE)

            log.info("Database init script executed")
        }
    }

    @Synchronized
    private fun initDb(connectionString: String) {
        if (connection == null) {
            connection = createJdbi(connectionString)
        } else {
            throw IllegalStateException("Database connection is already initialized!")
        }
    }

}