package com.bcol.vtd.api.preaprobadoli.service;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bcol.vtd.api.preaprobadoli.service.connector.LibreIConnector;

public class PersistenciaService {
	
	@Inject
    private LibreIConnector connector;
	
	private static final Logger logger = LogManager.getLogger(PersistenciaService.class);

	/**
	 * Método encargado de realizar la persistencia del objeto Ventas Digitales, el cual contiene los campos que
	 * componen la aplicación.
	 *
	 * @param excepcionServicio
	 * @param ventaDigitalLibreInversion
	 * @param traeSolicitud
	 * @param paso
	 * @param estadoSolicitud
	 * @param respuestaServicio
	 **/

	public void persistirVentasDigitales(ExcepcionServicio excepcionServicio, RespuestaServicio respuestaServicio,
										 VentaDigital ventaDigitalLibreInversion, boolean traeSolicitud, String paso,
										 String estadoSolicitud, boolean encriptar) {
		// ALMACENAR EN BASE DE DATOS
		String idProducto = null;
		
		try {
			VentaDigitalBD ventaDigitalBD = new VentaDigitalBD();

			if (paso != null)
				ventaDigitalLibreInversion.getInformacionTransaccion().setPasoFuncional(paso);

			if (estadoSolicitud != null)
				ventaDigitalLibreInversion.getSolicitud().setEstadoId(estadoSolicitud);

			ventaDigitalBD.setVentaDigital(ventaDigitalLibreInversion);
			idProducto = ventaDigitalLibreInversion.getProducto().getCodigoProducto();
			if (encriptar)
				ventaDigitalBD = HashUtil.encriptarInformacionSensible(ventaDigitalBD);

			connector.setVentaDigitalTarjetaBD(ventaDigitalBD);

		} catch (Exception e) {
			respuestaServicio = new RespuestaServicio();
			respuestaServicio.setCodigo(idProducto + "-" + CodigosRespuestaServicios.BD001.getCodigo());
		}
	}

}
