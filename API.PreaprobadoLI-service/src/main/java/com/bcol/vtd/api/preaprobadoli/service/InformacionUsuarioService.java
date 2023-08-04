package com.bcol.vtd.api.preaprobadoli.service;

import com.bcol.vtd.api.preaprobadoli.exception.InformacionUsuarioException;
import com.bcol.vtd.api.preaprobadoli.service.connector.LibreIConnector;
import com.bcol.vtd.api.preaprobadoli.util.CodigosRespuestaLibre;
import com.bcol.vtd.api.preaprobadoli.util.ConstantesLibre;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;

public class InformacionUsuarioService {

	@Inject
	LibreIConnector connector;
	
	private static final Logger logger = LogManager.getLogger(InformacionUsuarioService.class);

	public VentaDigital getDatosPersonales(String idSesion) throws InformacionUsuarioException {


		VentaDigital datosClienteVD = null;

		try {
			datosClienteVD = connector.getConsultarDatosCliente(idSesion,ConstantesLibre.pagina0bFPLI);
		}catch (ConectorClientException e){
			throw new InformacionUsuarioException(CodigosRespuestaLibre.ERROR_BD_INFO_USUARIO.getCodigo(), CodigosRespuestaLibre.ERROR_BD_INFO_USUARIO.getCodigo(), CodigosRespuestaLibre.ERROR_BD_INFO_USUARIO.getDescripcion(), e);
		}

		return datosClienteVD;
	}
	
	public VentaDigital getConsultarDatosCliente(String idSesion,String pasoFuncional) throws  InformacionUsuarioException{
		try {
			return connector.getConsultarDatosCliente(idSesion,pasoFuncional);
		}catch (ConectorClientException e){
			throw new InformacionUsuarioException(CodigosRespuestaLibre.ERROR_BD_INFO_USUARIO.getCodigo(), CodigosRespuestaLibre.ERROR_BD_INFO_USUARIO.getCodigo(), CodigosRespuestaLibre.ERROR_BD_INFO_USUARIO.getDescripcion(), e);
		}
	}

	public VentaDigital getDatosSolicitud(String idSesion) throws InformacionUsuarioException{
		try {
			String numeroSolicitud = connector.getSolicitudByIdSesion(idSesion);
			VentaDigital ventaDigital = new VentaDigital();
			Solicitud solicitud = new Solicitud ();
			ventaDigital.setSolicitud (solicitud);
			ventaDigital.getSolicitud().setNumeroSolicitudVirtual(numeroSolicitud);
			ventaDigital.setInformacionTransaccion(null);
			ventaDigital.setListaProductosPreaprobados(null);
			return ventaDigital;
		}catch (ConectorClientException e){
			throw new InformacionUsuarioException(CodigosRespuestaLibre.ERROR_BD_INFO_USUARIO.getCodigo(), CodigosRespuestaLibre.ERROR_BD_INFO_USUARIO.getCodigo(), CodigosRespuestaLibre.ERROR_BD_INFO_USUARIO.getDescripcion(), e);
		}
	}

}
