package com.bcol.vtd.api.preaprobadoli.service;

import bancolombia.poc.vd.security.model.JWTClaims;
import com.bcol.vtd.api.preaprobadoli.dto.request.LibreInversionRequest;
import com.bcol.vtd.api.preaprobadoli.exception.InformacionUsuarioException;
import com.bcol.vtd.api.preaprobadoli.exception.LibreInversionException;
import com.bcol.vtd.api.preaprobadoli.security.ISecurity;
import com.bcol.vtd.api.preaprobadoli.service.connector.LibreIConnector;
import com.bcol.vtd.api.preaprobadoli.util.CodigosRespuestaLibre;
import com.bcol.vtd.api.preaprobadoli.util.ConstantesLibre;
import com.bcol.vtd.api.preaprobadoli.util.VentaDigitalUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static com.bcol.vtd.api.preaprobadoli.util.UtilApi.createCookieJWTDefault;

public class Pagina3FPLIDelegado extends ServiciosVentasDigitales {
	
	private static final Logger logger = LogManager.getLogger(Pagina3FPLIDelegado.class);

	LibreIConnector connector;
	InformacionUsuarioService infousuario;
	VentaDigitalUtil ventaDigitalUtil;
	PersistenciaService persistenciaService;
	Pagina3aFPLIDelegado pagina3aFPLIDelegado;
	ParameterService parameterService;
	ISecurity security;

	@Inject
	public Pagina3FPLIDelegado(LibreIConnector connector, InformacionUsuarioService infousuario, VentaDigitalUtil ventaDigitalUtil,
								PersistenciaService persistenciaService,
							   Pagina3aFPLIDelegado pagina3aFPLIDelegado, ParameterService parameterService, ISecurity security) {

		this.connector = connector;
		this.infousuario = infousuario;
		this.ventaDigitalUtil = ventaDigitalUtil;
		this.persistenciaService = persistenciaService;
		this.pagina3aFPLIDelegado = pagina3aFPLIDelegado;
		this.parameterService = parameterService;
		this.security = security;
	}

	public Response cargarPagina3A(
			VentaDigitalLibreInversion ventaDigitalLibreInversion,
			String idSesion,
			String authorization,
			HttpServletRequest requestContext)throws InformacionUsuarioException {

		String jwt = "";
		JWTClaims claims = null;
		String tokenUsuario = null;
		String pathPublicKey = null;
		VentaDigital ventaDigital = null;
		RespuestaServicio respuestaServicio = null;
		ExcepcionServicio excepcionServicio = null;
		RespuestaPreaprobado respuestaPreaprobado = null;

		ventaDigital = infousuario.getDatosPersonales(idSesion);
		ventaDigitalLibreInversion.setDatosPersonales(ventaDigital.getDatosPersonales());
		pathPublicKey = parameterService.obtenerPropiedad(ConstantesLibre.PATH_PUBLIC_KEY);

		 try {
			claims = security.validateJwToken(security.getPublicKeyFile(pathPublicKey), authorization.split(ConstantesLibre.SPLIT_PREFIX)[1]);
			tokenUsuario = ventaDigitalUtil.getAttributeCustomClaims(claims.getCustomClaims(),
	    			ConstantesLibre.TOKEN_USUARIO_KEY, CodigosRespuestaLibre.ERROR_TOKEN_USUARIO_JWT);

	 	} catch (LibreInversionException e) {
			 persistenciaService.persistirVentasDigitales(excepcionServicio, respuestaServicio, ventaDigitalLibreInversion, false,
					 ConstantesLibre.pagina3aFPLI, null, true);
		} catch (Exception ex) {
			 logger.error(ex, ex);
		}

		respuestaPreaprobado = pagina3aFPLIDelegado.pagina3aFPLI(ventaDigitalLibreInversion, requestContext,
				tokenUsuario, idSesion, claims);

		jwt = ventaDigitalUtil.retornarJwt(claims);
		
		return Response.status(Status.OK).cookie(createCookieJWTDefault(jwt))
				.entity(respuestaPreaprobado.getVentaDigital())
				.build();
	}

	public Response cargarPagina3B(
			LibreInversionRequest libreInversionRequest,
			HttpServletRequest requestContext,
			String idSesion,
			String authorization) {

		String jwt = "";
		JWTClaims claims = null;
		String pathPublicKey = null;
		VentaDigital datosClienteVD = null;
		RespuestaServicio respuestaServicio = null;
		ExcepcionServicio excepcionServicio = null;
		VentaDigitalLibreInversion ventaDigitalLibreInversion = null;

		try {
			ventaDigitalLibreInversion = ventaDigitalUtil.requestToventaDigitalLibreInversion(libreInversionRequest);

			datosClienteVD = connector.getConsultarDatosCliente(idSesion,ConstantesLibre.pagina0cFPLI);
			claims = security.validateJwToken(security.getPublicKeyFile(pathPublicKey), authorization.split(ConstantesLibre.SPLIT_PREFIX)[1]);
			ventaDigitalLibreInversion.setDatosPersonales(datosClienteVD.getDatosPersonales());
			
		}  catch (ConsumerSessionException cs) {
	           respuestaServicio = ventaDigitalUtil.persistenciaSessionService (cs.getCode(), cs.getDescription(),
	        		   idSesion, ConstantesLibre.pagina3bFPLI, true, ventaDigitalLibreInversion);

		} catch (Exception e) {
			excepcionServicio = new ExcepcionServicio();
			persistenciaService.persistirVentasDigitales(excepcionServicio, respuestaServicio, ventaDigitalLibreInversion, false,
					ConstantesLibre.pagina3aFPLI, null, true);
		}

		persistenciaService.persistirVentasDigitales(null, respuestaServicio, ventaDigitalLibreInversion,
				false, Constantes.pagina3bFPLI, Constantes.ESTADO_SOLICITUD_EN_PROCESO_ENTREGA, false);

		jwt = ventaDigitalUtil.retornarJwt(claims);

		return Response.status(Status.OK).cookie(createCookieJWTDefault(jwt))
				.entity(ventaDigitalLibreInversion)
				.build();
	}
}
