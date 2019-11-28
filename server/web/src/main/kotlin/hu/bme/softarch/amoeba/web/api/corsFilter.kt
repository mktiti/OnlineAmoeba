package hu.bme.softarch.amoeba.web.api

import javax.inject.Singleton
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.ext.Provider

@Provider
@Singleton
class CorsFilter : ContainerResponseFilter {

    override fun filter(request: ContainerRequestContext, response: ContainerResponseContext) {
        response.headers.apply {
            add("Access-Control-Allow-Origin", "*")
            add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
            add("Access-Control-Allow-Credentials", "true")
            add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH")
        }
    }

}