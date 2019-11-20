package hu.bme.softarch.amoeba.web

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer
import org.glassfish.jersey.servlet.ServletContainer

object Server {

    private val log by logger()

    fun startServer(wsPort: Int = 8080, webPort: Int = 8080, clientDir: String? = null) {
        log.info("Starting server: Web port: $webPort, WebSocket port: $wsPort, client hosted from: $clientDir")

        val jettyServer = Server(webPort)

        ServletContextHandler(ServletContextHandler.NO_SESSIONS).apply {
            contextPath = "/"
            jettyServer.handler = this

            if (clientDir != null) {
                ServletHolder("static-client", DefaultServlet::class.java).apply {
                    resourceBase = clientDir
                    setInitParameter("pathInfoOnly", "true")

                    addServlet(this, "/static/*")
                }

                log.info("Serving static files from $baseResource")
            }

            with(addServlet(ServletContainer::class.java, "/api/*")) {
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

            WebSocketServerContainerInitializer.configureContext(this).addEndpoint(MatchClientController::class.java)
            addServlet(ServletHolder("default", DefaultServlet()), "/")
        }

        jettyServer.start()
        jettyServer.join()
    }

}