package hu.bme.softarch.amoeba.web

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.ServletContextHandler
import org.glassfish.jersey.servlet.ServletContainer

object Server {

    private val log by logger()

    fun startServer(wsPort: Int = 8080, webPort: Int = 8080, clientDir: String? = null) {
        log.info("Starting server: Web port: $webPort, WebSocket port: $wsPort, client hosted from: $clientDir")

        val jettyServer = Server(webPort)

        val staticContext = ServletContextHandler(ServletContextHandler.NO_SESSIONS).apply {
            contextPath = "/static/"

            if (clientDir != null) {
                resourceBase = clientDir
                log.info("Serving static files from $baseResource")
            }

            addServlet(DefaultServlet::class.java, "/")
        }

        val restContext = ServletContextHandler(ServletContextHandler.NO_SESSIONS).apply {
            with (addServlet(ServletContainer::class.java, "/api/*")) {
                initOrder = 0
                setInitParameter(
                    "jersey.config.server.provider.packages",
                    "hu.bme.softarch.amoeba.web, org.codehaus.jackson.jaxrs, org.glassfish.jersey.examples.multipart"
                )
                setInitParameter(
                    "jersey.config.server.provider.classnames",
                    "org.glassfish.jersey.jackson.JacksonFeature, org.glassfish.jersey.media.multipart.MultiPartFeature"
                )
            }
        }

        jettyServer.handler = ContextHandlerCollection().apply {
           handlers = arrayOf(staticContext, restContext)
        }

        jettyServer.start()
        jettyServer.join()
    }

}