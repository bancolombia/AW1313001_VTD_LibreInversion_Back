package com.bcol.vtd.api.preaprobadoli.facade.impl;

import com.bcol.vtd.api.preaprobadoli.security.ISecurity;
import com.bcol.vtd.api.preaprobadoli.service.InformacionUsuarioService;
import com.bcol.vtd.api.preaprobadoli.service.ParameterService;
import com.bcol.vtd.api.preaprobadoli.service.PersistenciaService;
import com.bcol.vtd.api.preaprobadoli.util.ConstantesLibre;
import com.bcol.vtd.api.preaprobadoli.util.PlantillasUtil;
import com.bcol.vtd.api.preaprobadoli.util.UtilApi;
import com.bcol.vtd.api.preaprobadoli.util.VentaDigitalUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class DocumentoServiceImpl{

    VentaDigitalUtil ventaDigitalUtil;
    InformacionUsuarioService infousuario;
    UtilApi utilApi;
    ParameterService parameterService;
    PersistenciaService persistenciaService;
    ISecurity security;

    @Inject
    public DocumentoServiceImpl(VentaDigitalUtil ventaDigitalUtil,
                                InformacionUsuarioService infousuario, UtilApi utilApi, ParameterService parameterService,
                                PersistenciaService persistenciaService, 
                                ISecurity security) {
        this.ventaDigitalUtil = ventaDigitalUtil;
        this.infousuario = infousuario;
        this.utilApi = utilApi;
        this.parameterService = parameterService;
        this.persistenciaService = persistenciaService;
        this.security = security;
    }

    private static final Logger logger = LogManager.getLogger(DocumentoServiceImpl.class);

    public Response obtenerCartaBienvenida(HttpServletRequest requestContext, VentaDigitalLibreInversion ventaDigitalLibreInversion, String idSesion,
                                           String authorization) {

        VentaDigital ventaDigitalBD = null;
        ExcepcionServicio excepcionServicio = null;
        RespuestaServicio respuestaServicio = new RespuestaServicio();
        Boolean cartaSeguro = false;
        List<Parametro> datosCartaSeguro = null;
        File archivoFinal = null;
        String fileExtention = "pdf";
        String tipoCarta;

        try {
            ventaDigitalBD = infousuario.getDatosPersonales(idSesion);

            idProducto = ventaDigitalBD.getProducto().getCodigoProducto();

            ventaDigitalLibreInversion.setInformacionTransaccion(informacionTransaccion);
            ventaDigitalLibreInversion.setDatosPersonales(ventaDigitalBD.getDatosPersonales());

            tipoCarta = ventaDigitalLibreInversion.getInformacionCredito().getIdPlan().equals(ConstantesLibre.CODIGOS_PLANES[ConstantesLibre.CONST_UNO])  ||
                    ventaDigitalLibreInversion.getInformacionCredito().getIdPlan().equals(ConstantesLibre.CODIGOS_PLANES[ConstantesLibre.CONST_TRES])
                    ? ConstantesLibre.TIPO_CARTA_BIENVENIDA_SEGURO : ConstantesLibre.TIPO_CARTA_BIENVENIDA;
            List<Parametro> datosCartaBienvenida = obtenerDatosDescargaDocumento(ventaDigitalLibreInversion, ventaDigitalBD, idSesion, tipoCarta);

            cartaSeguro = Boolean.TRUE;
            datosCartaSeguro = ventaDigitalUtil.obtenerDatosCartaSeguro(ventaDigitalLibreInversion, idSesion);

            try{
                if(datosCartaBienvenida != null && (cartaSeguro.equals(Boolean.TRUE) ? (datosCartaSeguro != null ? true : false) : true)) {
                    File cartaBienvenida = getCartaBienvenida(datosCartaBienvenida, ConstantesLibre.TIPO_CARTA_BIENVENIDA);

                    String documento = "";
                    documento = ventaDigitalLibreInversion.getSolicitud().getOfertaDigital().getDocumentos().get(0)
                            .getDocumento();

                    List<File> listFiles = new ArrayList<File>();

                    listFiles.add(cartaBienvenida);

                    listFiles.add(ventaDigitalUtil.transformStringB64ToFile(documento, fileExtention));

                    if (cartaSeguro.equals(Boolean.TRUE)) {
                        File polizaSeguro = new File(parameterService.obtenerPropiedad(ConstantesLibre.FWK_ENVIAR_CORREO_RUTA_PLANTILLA_LIBREINVERSION),
                                parameterService.obtenerPropiedad(ConstantesLibre.NOMBRE_SOLICITUD_SEGURO_DESEMPLEO));
                        listFiles.add(polizaSeguro);
                    }

                    archivoFinal = ventaDigitalUtil.mergePDFFiles(listFiles, fileExtention);

                    byte[] fileContent = FileUtils.readFileToByteArray(archivoFinal);
                    String base64Document = java.util.Base64.getEncoder().encodeToString(fileContent);

                    ResponseService res = new ResponseService();
                    res.setStatus(StatusResponse.SUCCESS.getName());
                    res.setOutput(base64Document);
                    return Response.ok(res).build();
                } else{
                    throw new Exception("Error consiguiendo datos de inserción en la(s) carta(s) de bienvenida.");
                }

            } catch (Exception ex){
                codeExcepcionDescarga = ConstantesLibre.errorInternoDescargaDocs;
                persistenciaService.persistirVentasDigitales(excepcionServicio, respuestaServicio, ventaDigitalLibreInversion, false,
                        Constantes.pagina3bFPLI, Constantes.ESTADO_SOLICITUD_EN_PROCESO_ENTREGA, true);
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(respuestaServicio).build();

        } catch (Exception e) {
            try {
                ventaDigitalLibreInversion = (VentaDigitalLibreInversion) HashUtil.desEncriptarInformacionSensible(ventaDigitalLibreInversion);
            } catch (Exception ex) {
                logger.error(" - error interno Obteniendo documento: " + ex.getMessage());
            }

            logger.error("Obteniendo: " + ConstantesLibre.CONSTANTE_DESCARGA_DOCUMENTO + ". Excepción no controlada - " + EnumLogVentasDigitales.ERROR_API_VENTAS_DIGITALES_501.getValue() + e.getMessage());
            respuestaServicio.setCodigo(CodigosRespuestaServicios.SRV_001.getCodigo());
            respuestaServicio.setDescripcion(null);
            excepcionServicio = new ExcepcionServicio(CodigosRespuestaServicios.SRV_001.getCodigo(), (e.getMessage().length() > 100) ? e.getMessage().substring(0, 99) : e.getMessage());

            ventaDigitalLibreInversion.getInformacionTransaccion().setExcepcionServicio(excepcionServicio);

            persistenciaService.persistirVentasDigitales(null, respuestaServicio, ventaDigitalLibreInversion,
                    false, Constantes.pagina3bFPLI, Constantes.ESTADO_SOLICITUD_EN_PROCESO_ENTREGA, true);

            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(respuestaServicio).build();
        }
    }

    public File getCartaBienvenida(List<Parametro> datosCartaBienvenida, String tipoCarta){

        File resultFile = null;
        RespuestaServicioEnvioCorreo respuestaDocumento =  getDocumento(datosCartaBienvenida, tipoCarta);
        if(respuestaDocumento.getEstadoRespuesta()) {
            resultFile = (File) respuestaDocumento.getObjetoRespuesta();
        }

        return resultFile;
    }

    public RespuestaServicioEnvioCorreo getDocumento(List<Parametro> listaParametros, String tipoCarta) {

        RespuestaServicioEnvioCorreo respuestaServicioEnvioCorreo = null;

        PlantillasUtil plantillasUtil = new PlantillasUtil(listaParametros, parameterService);

        plantillasUtil.createFile(tipoCarta);
        
        logger.info("Respuesta envio correo "+plantillasUtil.getRespuestaServicioEnvioCorreo());
        respuestaServicioEnvioCorreo = plantillasUtil.getRespuestaServicioEnvioCorreo();

        if (respuestaServicioEnvioCorreo.getEstadoRespuesta()) {

            respuestaServicioEnvioCorreo.setObjetoRespuesta(plantillasUtil.getFilePdf());
        }
        return respuestaServicioEnvioCorreo;
    }

    public List<Parametro> obtenerDatosDescargaDocumento(VentaDigitalLibreInversion ventaDigitalLibreInversion,
                                                         VentaDigital infoVisitaVD, String idSesion, String tipoCarta) throws IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(3);
        List<Parametro> listaParametros = null;

        String cuentaDesembolso = ventaDigitalLibreInversion.getInformacionCredito().getNumeroCuentaDesembolsoTxt();

        try{
            VentaDigital infoSolicitudVD = infousuario.getDatosSolicitud(idSesion);
            String numeroCredito = ventaDigitalUtil.obtenerNumeroCredito(infoSolicitudVD);
            if(numeroCredito != null) {
                listaParametros = new ArrayList<Parametro>();
                listaParametros.add(new Parametro(ConstantesEnviadorCorreo.CREDITO_NUMERO.getValue(), numeroCredito));
                listaParametros.add(new Parametro(ConstantesEnviadorCorreo.EMAIL.getValue(), ""));
                listaParametros.add(new Parametro(ConstantesEnviadorCorreo.NOMBRE_CLIENTE.getValue(), ventaDigitalLibreInversion.getDatosPersonales().getNombreLargoCliente()));
                listaParametros.add(new Parametro(ConstantesLibre.CUENTA_DESEMBOLSO, cuentaDesembolso));
                listaParametros.add(new Parametro(ConstantesLibre.FWK_ENVIAR_CORREO_RUTA_PLANTILLA, ConstantesLibre.FWK_ENVIAR_CORREO_RUTA_PLANTILLA_LIBREINVERSION));
                listaParametros.add(new Parametro(ConstantesLibre.FWK_DESCARGAR_PDF_NOMBRE_PLANTILLA, ConstantesLibre.FWK_DESCARGAR_PDF_NOMBRE_PLANTILLA));
            }

        } catch (Exception e){
            logger.error("Error obteniendo parámetros para la descarga de los documentos: " + e.getMessage());
        }

        return listaParametros;
    }

}
