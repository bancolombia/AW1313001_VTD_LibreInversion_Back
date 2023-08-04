package com.bcol.vtd.api.preaprobadoli.util;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;


/***
 * Clase de utilidades para la api de envio de correo
 */
public class PlantillasUtil {


    private List<Parametro> listaParametros = new ArrayList<Parametro>();
    private static final Logger logger = LogManager.getLogger(PlantillasUtil.class);

    private String rutaplantilla = null;
    private String plantilla = null;

    private String asuntoCorreo = null;
    private String deCorreo = null;
    private String paraCorreo = null;

    private List<String> imagen_tarjeta = null;

    private String numeroIdentificacion = null;

    private VelocityEngine velocityEngine = null;
    private VelocityContext context = null;
    private Template template = null;
    private StringWriter writer = null;

    private String contenidoArchivo1 = null;

    private String usuarioSL = null;
    private String claveSL = null;

    private String imagenes[] = null;

    private File filePdf;

    private RespuestaServicioEnvioCorreo respuestaServicioEnvioCorreo;

    @Inject
    ParameterService parameterService;

    public PlantillasUtil(List<Parametro> listaParametros, ParameterService parameterService) {

        this.listaParametros = listaParametros;
        this.parameterService = parameterService;
        respuestaServicioEnvioCorreo = new RespuestaServicioEnvioCorreo();
        respuestaServicioEnvioCorreo.setEstadoRespuesta(true);

    }


    /***
     * Procedimiento que mapea los parametros recibidos y crea el cuerpo html del correo
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean createBodyHtml() {

        writer = new StringWriter();

        try {

            plantilla = parameterService.obtenerPropiedad(ConstantesGeneracionPDF.NOMBRE_PLANTILLA_CORREO) != null ?
                    parameterService.obtenerPropiedad(ConstantesGeneracionPDF.NOMBRE_PLANTILLA_CORREO).toString() : "";

            usuarioSL = parameterService.obtenerPropiedad(ConstantesGeneracionPDF.USUARIO_CORREO) != null ?
                    parameterService.obtenerPropiedad(ConstantesGeneracionPDF.USUARIO_CORREO).toString() : "";

            claveSL = parameterService.obtenerPropiedad(ConstantesGeneracionPDF.CLAVE_CORREO) != null ?
                    parameterService.obtenerPropiedad(ConstantesGeneracionPDF.CLAVE_CORREO).toString() : "";

            deCorreo = parameterService.obtenerPropiedad(ConstantesGeneracionPDF.DE_CORREO) != null ?
                    parameterService.obtenerPropiedad(ConstantesGeneracionPDF.DE_CORREO).toString() : "";

            context = new VelocityContext();

            for (Parametro param : listaParametros) {

                context.put(param.getClave(), param.getValor());

                if (param.getObject() != null) {
                    context.put(param.getClave(), param.getObject());
                }
                if (param.getClave().equals(ConstantesGeneracionPDF.PARA)) {
                    paraCorreo = param.getObject().toString();
                }
                if (param.getClave().equals(ConstantesGeneracionPDF.ARCHIVO1)) {
                    contenidoArchivo1 = param.getObject().toString();
                }
                if (param.getClave().equals(ConstantesGeneracionPDF.DOCUMENTO)) {
                    numeroIdentificacion = param.getObject().toString();
                }
                if (param.getClave().equals(ConstantesGeneracionPDF.IMAGENES)) {
                    imagenes = param.getValor().split(",");
                }
                if (param.getClave().equals(ConstantesGeneracionPDF.CODIGO_IMAGEN)) {
                    imagen_tarjeta = ((ArrayList<String>) param.getObject());
                }
                if (param.getClave().equals(ConstantesGeneracionPDF.ASUNTO_CORREO)) {
                    asuntoCorreo = parameterService.obtenerPropiedad(param.getValor());
                }
                if (param.getClave().equals(ConstantesGeneracionPDF.RUTA_PLANTILLA_CORREO)) {
                    rutaplantilla = parameterService.obtenerPropiedad(param.getValor());
                    context.put("RUTA_IMG", rutaplantilla);
                }
                if (param.getClave().equals(ConstantesGeneracionPDF.NOMBRE_PLANTILLA_PDF)) {
                    plantilla = "pdf_" + plantilla;
                }

            }

            velocityEngine = new VelocityEngine();
            velocityEngine.setProperty("input.encoding", StandardCharsets.UTF_8.name());
            velocityEngine.setProperty("file.resource.loader.description", "Velocity File Resource Loader");
            velocityEngine.setProperty("file.resource.loader.class",
                    "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
            velocityEngine.setProperty("file.resource.loader.path", rutaplantilla);
            velocityEngine.setProperty("file.resource.loader.cache", "false");
            velocityEngine.setProperty("file.resource.loader.modificationCheckInterval", "2");
            velocityEngine.init();

            try {
                template = velocityEngine.getTemplate(plantilla);
            } catch (Exception e) {
                logger.error("Error exception: " + e.getMessage(), e);
                respuestaServicioEnvioCorreo.setError(CodigosRespuestaServicios.DDOC_002.getCodigo(),
                        CodigosRespuestaServicios.DDOC_002.getDescripcion() + plantilla);
                return false;
            }

            template.merge(context, writer);

            return true;

        } catch (Exception e) {
            logger.error("Error  exception: " + e.getMessage(), e);
            respuestaServicioEnvioCorreo.setError(CodigosRespuestaServicios.DDOC_005.getCodigo(),
                    plantilla + " -> " + CodigosRespuestaServicios.DDOC_005.getDescripcion());

            return false;
        }

    }


    /***
     * procedimiento que se encarga de convertir la plantilla html en un archivo PDF
     *
     * @param tipoCarta String que identifica el tipo de carta a crear.
     *
     */
    public void createFile(String tipoCarta) {
        logger.info("entramos a createFile");
        filePdf = null;
        Path file = null;

        try{

        	FileAttribute<Set<PosixFilePermission>> attributes
		      = PosixFilePermissions.asFileAttribute(new HashSet<>(
		          Arrays.asList(PosixFilePermission.OWNER_WRITE,
		                        PosixFilePermission.OWNER_READ)));
            file = Files.createTempFile("tmp", ".".concat("pdf"), attributes);
            filePdf=file.toFile();

            Document document = new Document();

            document.setPageSize(PageSize.LETTER);

            PdfWriter pdfWriter = null;

            pdfWriter = PdfWriter.getInstance(document,new FileOutputStream(filePdf.getPath()));

            document.open();

            createBodyHtml();

            if (respuestaServicioEnvioCorreo.getEstadoRespuesta()){
                logger.info("ningún error en respuestaServicioEnvioCorreo");
                String content = this.writer.toString();

                XMLWorkerHelper.getInstance().parseXHtml(pdfWriter, document,new StringReader(content));

                document.close();

                pdfWriter.close();
                logger.info("PDF generated successfully");

                System.out.println("PDF generated successfully");

                System.out.println(filePdf.getPath());
            }



        } catch (Exception e){
            logger.error("Error  exception: " + e.getMessage(),e);
            respuestaServicioEnvioCorreo.setError(CodigosRespuestaServicios.DDOC_003.getCodigo(),
                    CodigosRespuestaServicios.DDOC_003.getDescripcion());
        }

    }

    public File getFilePdf() {
        return this.filePdf;
    }

    public RespuestaServicioEnvioCorreo getRespuestaServicioEnvioCorreo() {
        return this.respuestaServicioEnvioCorreo;
    }
}
