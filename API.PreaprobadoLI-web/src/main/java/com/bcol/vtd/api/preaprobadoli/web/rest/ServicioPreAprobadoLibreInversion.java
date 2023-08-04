package com.bcol.vtd.api.preaprobadoli.web.rest;

import com.bcol.vtd.api.preaprobadoli.facade.impl.DocumentoServiceImpl;
import com.bcol.vtd.api.preaprobadoli.service.Pagina3FPLIDelegado;
import com.bcol.vtd.api.preaprobadoli.util.UtilApi;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("servicio")
public class ServicioPreAprobadoLibreInversion {

	private static final String ENCODING = "UTF-8";
	@Inject
	private Instance<Pagina3FPLIDelegado> pagina3;
	@Inject
	private Instance<DocumentoServiceImpl> documentoService;

	@POST
	@Path("pagina3aFPLI")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=" + ENCODING)
	public Response pagina3aFPLI(
			@Context HttpServletRequest requestContext,
			@CookieParam(JWT_COOKIE_KEY) String authorization,
			@CookieParam(SESSIONID_COOKIE_KEY) String idSesion,
			String json) throws InformacionUsuarioException {
		
		RespuestaServicio respuestaServicio = new RespuestaServicio();
		
		if (UtilApi.validateJsonRequest(json)) {

			try {
				ObjectMapper mapper = new ObjectMapper();
				VentaDigitalLibreInversion ventaDigitalLibreInversion = mapper.readValue(json, VentaDigitalLibreInversion.class);

				return pagina3.get().cargarPagina3A(ventaDigitalLibreInversion, idSesion, authorization, requestContext);
			
			} catch (Exception e) {
				respuestaServicio.setDescripcion(Status.INTERNAL_SERVER_ERROR.name());
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(respuestaServicio).build();
			}
		} else {
			respuestaServicio.setDescripcion(Status.BAD_REQUEST.name());
			return Response.status(Status.BAD_REQUEST).entity(respuestaServicio).build();
		}
	}

	@POST
	@Path("pagina3bFPLI")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=" + ENCODING)
	public Response pagina3bFPLI(
			@Context HttpServletRequest requestContext,
			@CookieParam(SESSIONID_COOKIE_KEY) String idSesion,
			@CookieParam(JWT_COOKIE_KEY) String authorization,
			@Valid LibreInversionRequest libreInversionRequest) {

		return pagina3.get().cargarPagina3B(libreInversionRequest, requestContext, idSesion, authorization);
	}
	
	@POST
	@Path("obtenerDocumentos")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=" + ENCODING)
	public Response obtenerDocumentos(
			@Context HttpServletRequest requestContext,
			@CookieParam(SESSIONID_COOKIE_KEY) String idsesion,
			@CookieParam(JWT_COOKIE_KEY) String authorization,
			String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			VentaDigitalLibreInversion ventaDigitalLibreInversion = mapper.readValue(json, VentaDigitalLibreInversion.class);

			return documentoService.get().obtenerCartaBienvenida(requestContext, ventaDigitalLibreInversion, idsesion, authorization);
		} catch (Exception e) {
			RespuestaServicio respuestaServicio = new RespuestaServicio();
			respuestaServicio.setDescripcion(Status.INTERNAL_SERVER_ERROR.name());

			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(respuestaServicio).build();
		}
	}

	@GET
	@Path("status")
	public Response status() { 
		return Response.status(Response.Status.OK).build();	
	}
}
