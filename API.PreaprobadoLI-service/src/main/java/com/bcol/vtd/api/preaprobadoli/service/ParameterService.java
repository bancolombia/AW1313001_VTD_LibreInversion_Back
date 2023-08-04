package com.bcol.vtd.api.preaprobadoli.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class ParameterService {

    private static final Logger LOGGER = LogManager.getLogger();

    private final static String PATH_PREAPROBADO_LIBRE_INVERSION= "properties.json";

    Map<String, String> parameters;

    public ParameterService() {
        try {
            parameters =  CargadorPropiedades.readJsonFile(PATH_PREAPROBADO_LIBRE_INVERSION);
        } catch (Exception e) {
            LOGGER.error("Error cargando archivos de propiedades: " + e.getMessage(), e);
        }
    }

    public String obtenerPropiedad(String key) {
        return parameters.get(key);
    }
}
