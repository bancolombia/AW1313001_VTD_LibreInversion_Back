package com.bcol.vtd.api.preaprobadoli.service.connector;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class LibreIConnector {

	private Client client = null;

	@Inject
	private VentasDigitalesBusiness ventasDigitalesBusiness;

	@Inject
	private OfertaDigitalBusiness ofertaDigitalBusiness;

	@Inject
	private InformacionTransaccionBusiness infoTransaccionBussines;

	private static final Logger logger = LogManager.getLogger(LibreIConnector.class);

	public LibreIConnector() {
		if (client == null) {
			client = ClientBuilder.newClient();
		}
	}

	public void setVentaDigitalTarjetaBD(VentaDigitalBD ventaDigitalBD) throws ConectorClientException {

		ventasDigitalesBusiness.setVentaDigitalTarjeta(ventaDigitalBD);

	}

	public VentaDigital getConsultarDatosCliente(String idsesion,String pasoFuncional) throws ConectorClientException {

		return validarVentaDigital(ventasDigitalesBusiness.getClientePorIdSesionPasoFuncional(idsesion, pasoFuncional));
	}

	public VentaDigital getconsultarInformacionVisita(String idsesion) throws ConectorClientException {

		return validarVentaDigital(ventasDigitalesBusiness.getClientePorIdSesion(idsesion));
	}

	public String getSolicitudByIdSesion(String idsesion) throws ConectorClientException {

		return infoTransaccionBussines.getSolicitudByIdSesion(idsesion);
	}

	public OfertaDigital getOfertaDigitaByNumeroSolicitud(String numeroSolicitudVirtual, String paso) throws ConectorClientException, Exception {

		String ofertaRespuesta = ofertaDigitalBusiness.getOfertaDigitaByNumeroSolicitud(numeroSolicitudVirtual, paso);

		OfertaDigital ofertaDigital = new Gson().fromJson((String) ofertaRespuesta, OfertaDigital.class);

		return ofertaDigital;
	}


	public static VentaDigital validarVentaDigital(VentaDigitalBD ventaDigitalDB){

		if (null == ventaDigitalDB || null == ventaDigitalDB.getVentaDigital()){
			return null;
		}

		return ventaDigitalDB.getVentaDigital();
	}
}
