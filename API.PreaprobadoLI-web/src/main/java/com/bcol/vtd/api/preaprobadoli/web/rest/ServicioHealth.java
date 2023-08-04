package com.bcol.vtd.api.preaprobadoli.web.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("health")
public class ServicioHealth {

    @GET
    @Path("status")
    public Response status() { return Response.status(Response.Status.OK).build(); }

}
