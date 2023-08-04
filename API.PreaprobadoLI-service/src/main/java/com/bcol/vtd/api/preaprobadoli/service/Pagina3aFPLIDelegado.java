package com.bcol.vtd.api.preaprobadoli.service;

import bancolombia.poc.vd.security.model.JWTClaims;
import com.bcol.vtd.api.preaprobadoli.exception.LibreInversionException;
import com.bcol.vtd.api.preaprobadoli.service.connector.LibreIConnector;
import com.bcol.vtd.api.preaprobadoli.util.CodigosRespuestaLibre;
import com.bcol.vtd.api.preaprobadoli.util.ConstantesLibre;
import com.bcol.vtd.api.preaprobadoli.util.PlantillasUtil;
import com.bcol.vtd.api.preaprobadoli.util.VentaDigitalUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Pagina3aFPLIDelegado extends ServiciosVentasDigitales {

	private static final Logger logger = LogManager.getLogger(Pagina3aFPLIDelegado.class);

	private LibreIConnector connector;
	private InformacionUsuarioService infousuario;
	private VentaDigitalUtil ventaDigitalUtil;
	private ParameterService parameterService;

	@Inject
	public Pagina3aFPLIDelegado(LibreIConnector connector, InformacionUsuarioService infousuario, VentaDigitalUtil ventaDigitalUtil, ParameterService parameterService){
		this.connector = connector;
		this.infousuario = infousuario;
		this.ventaDigitalUtil = ventaDigitalUtil;
		this.parameterService = parameterService;
	}

	public RespuestaPreaprobado pagina3aFPLI(VentaDigitalLibreInversion ventaDigitalLibreInversion,
			HttpServletRequest requestContext,
			String tokenUsuario,
			String idSesion,
			JWTClaims claims) {
		
		String canal = null;
		String correoCliente = null;
		VentaDigital datosClienteVD = null;
		VentaDigital infoSolicitudVD = null;
		ExcepcionServicio excepcionServicio = null;
		InformacionTransaccion informacionTransaccion = null;
		RespuestaPreaprobado respuestaPreaprobado = null;

		try {

			datosClienteVD = connector.getConsultarDatosCliente(idSesion,ConstantesLibre.pagina0cFPLI);

			canal = String.valueOf(ConstantesLibre.CANAL);

			informacionTransaccion = ventaDigitalUtil.obtenerInformacionTransaccion(
					datosClienteVD.getInformacionTransaccion().getIpCliente(), idSesion, tokenUsuario, canal,
					Constantes.pagina3aFPLI);
			infoSolicitudVD = infousuario.getDatosSolicitud(idSesion);

			ventaDigitalLibreInversion.setDatosPersonales(datosClienteVD.getDatosPersonales());
			ventaDigitalLibreInversion.setInformacionTransaccion(informacionTransaccion);

			idProducto = datosClienteVD.getProducto().getCodigoProducto();
			correoCliente = ventaDigitalLibreInversion.getDatosPersonales().getCorreoElectronico();

			if(correoCliente != null && !correoCliente.isEmpty()) {

				EnviadorCorreoProxy correoProxy = new EnviadorCorreoProxy(configuracion.getPropiedades(),idSesion);
				Map<String, Object> datosCorreo = prepararEnvioCorreo(ventaDigitalLibreInversion, infoSolicitudVD, idSesion, correoCliente);
				correoProxy.prepararEnvioCorreo(datosCorreo);
			}
		} catch (Exception e) {
			excepcionServicio = new ExcepcionServicio(CodigosRespuestaLibre.ERROR_PAGINA3A_ENVIO_CORREO.getCodigo(), (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage());
			informacionTransaccion.setExcepcionServicio(excepcionServicio);
		}

        return respuestaPreaprobado;

	}

	public Map<String, Object> prepararEnvioCorreo(VentaDigitalLibreInversion ventaDigitalLibreInversion, VentaDigital infoSolicitudVD,
												   String idSesion, String correoCliente) throws Exception, LibreInversionException {
		String documento = null;

		Map<String, Object> datosCorreo = null;

		VentaDigital ventaDigital = null;
		ventaDigital = infousuario.getDatosPersonales(idSesion);

		try {
			documento = obtenerArchivosAdjuntos(ventaDigitalLibreInversion, idSesion, ConstantesLibre.TIPO_CARTA_BIENVENIDA);
		} catch (LibreInversionException e){
			throw e;
		}

		String nombre = ventaDigital.getDatosPersonales().getNombreLargoCliente();
		String numeroCredito = ventaDigitalUtil.obtenerNumeroCredito(infoSolicitudVD);
		String montoSolicitado = ventaDigitalLibreInversion.getInformacionCredito().getMontoSolicitado();
		String cuentaDebito = ventaDigitalLibreInversion.getInformacionCredito().getDebitoAutomatico().equalsIgnoreCase("true") ? ventaDigitalLibreInversion.getInformacionCredito().getNumeroCuentaDebitarTxt() : null;

		NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setMaximumFractionDigits(3);
		DecimalFormat formatNum = new DecimalFormat("#,###");

		datosCorreo = new java.util.HashMap<String, Object>();
		datosCorreo.put(ConstantesLibre.IMAGENES, ConstantesLibre.IMAGENES_CORREO_LIBREINVERSION);
		datosCorreo.put(ConstantesEnviadorCorreo.DOCUMENTO_PDF.getValue(), documento);
		datosCorreo.put(ConstantesEnviadorCorreo.EMAIL.getValue(), correoCliente);
		datosCorreo.put(ConstantesEnviadorCorreo.NOMBRE_CLIENTE.getValue(), nombre);
		datosCorreo.put(ConstantesEnviadorCorreo.VALOR_DESEMBOLSO.getValue(), formatNum.format(Integer.parseInt(montoSolicitado)).replace(',', '.'));
		datosCorreo.put(ConstantesEnviadorCorreo.CREDITO_NUMERO.getValue(), numeroCredito);
		if (cuentaDebito != null)
			datosCorreo.put(ConstantesEnviadorCorreo.CUENTA_DEBITO.getValue(), cuentaDebito);
		datosCorreo.put(ConstantesLibre.FWK_ENVIAR_CORREO_RUTA_PLANTILLA, ConstantesLibre.FWK_ENVIAR_CORREO_RUTA_PLANTILLA_LIBREINVERSION);
		datosCorreo.put(ConstantesLibre.FWK_ENVIAR_CORREO_ASUNTO, ConstantesLibre.FWK_ENVIAR_CORREO_ASUNTO_LIBREINVERSION);
		datosCorreo.put(ConstantesLibre.ID_SESION, idSesion);

		return datosCorreo;
	}

	private String obtenerArchivosAdjuntos(VentaDigitalLibreInversion ventaDigitalLibreInversion, String idSesion, String tipoCarta) throws LibreInversionException {

		String documento = "";
		String response = null;
		String fileExtention = "pdf";
		List<Parametro> listaParametros = new ArrayList<>();
		byte[] fileContent = null;
		List<File> listFiles = null;
		File cartaBienvenidaSeguro = null;
		File archivoFinal = null;

		if (null != ventaDigitalLibreInversion.getSolicitud().getOfertaDigital().getDocumentos()) {
			documento = ventaDigitalLibreInversion.getSolicitud().getOfertaDigital().getDocumentos().get(0).getDocumento();
			listFiles = new ArrayList<>();
			listFiles.add(ventaDigitalUtil.transformStringB64ToFile(documento, fileExtention));
		} else{
			throw new LibreInversionException(CodigosRespuestaLibre.ERROR_DOCS_NULL_VENTA_DIGITAL.getCodigo(), CodigosRespuestaLibre.ERROR_DOCS_NULL_VENTA_DIGITAL.getDescripcion());
		}

		if (tipoCarta.equals(ConstantesLibre.TIPO_CARTA_BIENVENIDA_SEGURO)) {

			RespuestaServicioEnvioCorreo respuestaServicioEnvioCorreo = null;
			listaParametros = ventaDigitalUtil.obtenerDatosCartaSeguro(ventaDigitalLibreInversion, idSesion);

			PlantillasUtil plantillasUtil = new PlantillasUtil(listaParametros, parameterService);
			plantillasUtil.createFile(ConstantesLibre.TIPO_CARTA_BIENVENIDA_SEGURO);
			respuestaServicioEnvioCorreo = plantillasUtil.getRespuestaServicioEnvioCorreo();
			if (respuestaServicioEnvioCorreo.getEstadoRespuesta()) {
				cartaBienvenidaSeguro = plantillasUtil.getFilePdf();
			} else{
				throw new LibreInversionException(CodigosRespuestaLibre.ERROR_DOCS_CREATE_CARTA_SEGURO.getCodigo(), CodigosRespuestaLibre.ERROR_DOCS_CREATE_CARTA_SEGURO.getDescripcion());
			}

			listFiles.add(cartaBienvenidaSeguro);

		}

		archivoFinal = ventaDigitalUtil.mergePDFFiles(listFiles, fileExtention);

		try {
			fileContent = FileUtils.readFileToByteArray(archivoFinal);
		} catch (IOException e) {
			logger.error("Error creando documentos de bienvenida adjuntos" + e.getMessage(), e);
		} finally {
			if (null != archivoFinal) {
				try {
					Files.delete(archivoFinal.toPath());
				} catch (IOException e) {
					logger.error("Error eliminando documento final" + e.getMessage(), e);
				}
			}
		}

		response = java.util.Base64.getEncoder().encodeToString(fileContent);
		return response;

	}
}
