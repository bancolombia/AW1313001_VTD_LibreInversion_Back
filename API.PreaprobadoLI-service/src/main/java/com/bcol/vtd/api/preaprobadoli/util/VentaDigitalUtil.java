package com.bcol.vtd.api.preaprobadoli.util;

import bancolombia.poc.vd.security.model.JWTClaims;
import com.bcol.vtd.api.preaprobadoli.dto.request.LibreInversionRequest;
import com.bcol.vtd.api.preaprobadoli.security.ISecurity;
import com.bcol.vtd.api.preaprobadoli.service.InformacionUsuarioService;
import com.bcol.vtd.api.preaprobadoli.service.PersistenciaService;
import com.bcol.vtd.api.preaprobadoli.service.connector.LibreIConnector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.*;

public class VentaDigitalUtil {

    private static final Logger logger = LogManager.getLogger(VentaDigitalUtil.class);

    @Inject
    private LibreIConnector connector;

    @Inject
    private ISecurity security;

    @Inject
    private PersistenciaService persistenciaService;

	@Inject
	InformacionUsuarioService informacionUsuarioService;

	public InformacionTransaccion obtenerInformacionTransaccion(String ipcliente, String idSesion, String tokenApp, String canal,
                                                                String pasoFuncional) throws LibreInversionException {

        InformacionTransaccion informacionTransaccion = new InformacionTransaccion();
        informacionTransaccion.setIpCliente(ipcliente);
        informacionTransaccion.setTokenApp(tokenApp);
        informacionTransaccion.setPasoFuncional(pasoFuncional);
        informacionTransaccion.setCanal(canal);
        informacionTransaccion.setFechaHoraTransaccion(new Date());

        return informacionTransaccion;

    }
    private String getFechaActualLetra() {
        Calendar c1 = new GregorianCalendar(new Locale("es", "ES"));
        String mes = new SimpleDateFormat("MMMM", new Locale("es", "ES")).format(c1.getTime());
        String mesActual = mes.substring(0, 1).toUpperCase() + mes.substring(1);
        return c1.get(Calendar.DAY_OF_MONTH) + " de " + mesActual + " de " + c1.get(Calendar.YEAR);
    }

    public List<Parametro> obtenerDatosCartaSeguro(VentaDigitalLibreInversion ventaDigitalLibreInversion, String idSesion) {

        List<Parametro> parametros = null;

        parametros = new ArrayList<>();
        parametros.add(new Parametro(ConstantesLibre.CIUDAD, ventaDigitalLibreInversion.getInformacionVivienda().getCodigoCiudad()));
        parametros.add(new Parametro(ConstantesLibre.FECHA_ACTUAL, getFechaActualLetra()));
        parametros.add(new Parametro(ConstantesLibre.NOMBRE_COMPLETO, ventaDigitalLibreInversion.getDatosPersonales().getNombreLargoCliente()));
        parametros.add(new Parametro(ConstantesLibre.CREDITO_NUMERO, obtenerNumeroCredito(infoSolicitudVD)));
        parametros.add(new Parametro(ConstantesLibre.FWK_ENVIAR_CORREO_RUTA_PLANTILLA, ConstantesLibre.FWK_ENVIAR_CORREO_RUTA_PLANTILLA_LIBREINVERSION));
        parametros.add(new Parametro(ConstantesLibre.IMAGENES, ConstantesLibre.IMAGENES_CARTA_BIENVENIDA_SEGURO));

        return parametros;
    }


    public String obtenerNumeroCredito(VentaDigital infoSolicitudVD) {
        String numeroCredito = null;
        try {
            OfertaDigital oferta = connector.getOfertaDigitaByNumeroSolicitud(infoSolicitudVD.getSolicitud().getNumeroSolicitudVirtual(), Constantes.paso3);
            numeroCredito = oferta.getConfirmacionTransaccion().getDescripcion();

        } catch (Exception e) {
            logger.error("Error obteniendo oferta digital paso 3: " + e.getMessage());
        }
        return numeroCredito;
    }

    public File transformStringB64ToFile(String encodeStringB64, String fileExtention) {

        byte[] byteArrayFile = Base64.getDecoder().decode(encodeStringB64);

        File tempFile = null;
        Path file=null;
        try {
        	FileAttribute<Set<PosixFilePermission>> attributes
		      = PosixFilePermissions.asFileAttribute(new HashSet<>(
		          Arrays.asList(PosixFilePermission.OWNER_WRITE,
		                        PosixFilePermission.OWNER_READ)));
        	
            file = Files.createTempFile("tmp", ".".concat(fileExtention), attributes);
            tempFile=file.toFile();
        } catch (IOException e) {
            logger.info(VentaDigitalUtil.class +
                    "transformStringB64ToFile: " + e.getMessage(), e);
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tempFile);
        } catch (FileNotFoundException e) {
            logger.info(VentaDigitalUtil.class +
                    "Error iniciando FileOutputStream: " + e.getMessage(), e);
        }
        try {
            fos.write(byteArrayFile);
            fos.close();
        } catch (IOException e) {
            logger.info(VentaDigitalUtil.class +
                    "Error escribiendo en el archivo: " + e.getMessage(), e);
        }
        return tempFile;
    }

    public File mergePDFFiles(List<File> listFiles, String fileExtention) {

        try {

        	Path fileTemp=null;
        	 File mergedFile=null;
            //Instantiating PDFMergerUtility class
            PDFMergerUtility PDFmerger = new PDFMergerUtility();

            FileAttribute<Set<PosixFilePermission>> attributes
		      = PosixFilePermissions.asFileAttribute(new HashSet<>(
		          Arrays.asList(PosixFilePermission.OWNER_WRITE,
		                        PosixFilePermission.OWNER_READ)));
            
            //Setting the destination file
            fileTemp = Files.createTempFile("merged", ".".concat(fileExtention), attributes);
            mergedFile=fileTemp.toFile();
            PDFmerger.setDestinationFileName(mergedFile.getAbsolutePath());

            for (File file : listFiles) {

                PDFmerger.addSource(file);
            }

            PDFmerger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());

            return mergedFile;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            logger.info(VentaDigitalUtil.class +
                    "Error Uniendo archivos: " + e.getMessage(), e);
        }

        return null;
    }

    public String retornarJwt(JWTClaims claims) {

        String jwt = null;
        try {
            jwt = security.generateJwToken(claims);
        } catch (Exception e) {
            logger.error("Error retornando jwt -> " + e.getMessage());
        }

        return jwt;
    }

	public VentaDigitalLibreInversion requestToventaDigitalLibreInversion(LibreInversionRequest libreInversionRequest)
			throws LibreInversionException {
		
		Gson gson = new Gson();
		ObjectMapper mapper = new ObjectMapper();
		SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
		VentaDigitalLibreInversion ventaDigitalLibreInversion = new VentaDigitalLibreInversion();
		String json = null;
		
		try {
			json  = gson.toJson(libreInversionRequest);
			mapper.setDateFormat(formato);
			ventaDigitalLibreInversion = mapper.readValue(json, VentaDigitalLibreInversion.class);

		} catch (Exception e) {
			throw new LibreInversionException(CodigosRespuestaLibre.ERROR_LIBRE_REQUEST_VALID.getCodigo(),
					CodigosRespuestaLibre.ERROR_LIBRE_REQUEST_VALID.getDescripcion(), e);
		}
		return ventaDigitalLibreInversion;
	}

	public String getAttributeCustomClaims(
            Map<String, Object> customClaims,
            String key,
            CodigosRespuestaLibre error) throws ValidacionException {
    	
        if (!customClaims.containsKey(key) || null == customClaims.get(key)) {
            throw new ValidacionException(error.getCodigo(), null, error.getDescripcion());
        }
        return (String) customClaims.get(key);
    }

	public RespuestaServicio persistenciaSessionService (
            String code,
            String descripcion,
            String idSesion,
            String pasoFuncional,
            boolean encriptar,
            VentaDigitalLibreInversion ventaDigitalLibreInversion) {
		
		RespuestaServicio respuestaServicio = null;
		VentaDigital ventaDigital = null;
		String codProducto = null;
		
		try{
			if (ventaDigitalLibreInversion == null) {
				ventaDigital = informacionUsuarioService
						.getConsultarDatosCliente (idSesion, ConstantesLibre.pagina0cFPLI);
				ventaDigitalLibreInversion = organizarVentaDigital(ventaDigital, pasoFuncional);
			}	
			codProducto = ventaDigitalLibreInversion.getProducto().getCodigoProducto();

			persistenciaService.persistirVentasDigitales (null, respuestaServicio, ventaDigitalLibreInversion, false,
					pasoFuncional, null, encriptar);
			
			if (null == respuestaServicio) {
				respuestaServicio = new RespuestaServicio(codProducto.concat("-").concat(code));
			}

		} catch(Exception ex){
			respuestaServicio = new RespuestaServicio(CodigosRespuestaLibre.ERROR_BD_INFO_USUARIO.getCodigo());
		}
		
		return respuestaServicio;
	}

	public VentaDigitalLibreInversion organizarVentaDigital(VentaDigital ventaDigital, String pasoFuncional) {

		VentaDigitalLibreInversion ventaDigitalLibreInversion = new VentaDigitalLibreInversion ();

		ventaDigitalLibreInversion.setSolicitud(ventaDigital.getSolicitud());
		ventaDigitalLibreInversion.setProducto(ventaDigital.getProducto());
		ventaDigitalLibreInversion.setInformacionTransaccion(ventaDigital.getInformacionTransaccion ());
		ventaDigitalLibreInversion.setDatosPersonales(ventaDigital.getDatosPersonales());

		return ventaDigitalLibreInversion;
	}

}
