package com.bcol.vtd.api.preaprobadoli.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;

public class UtilApi extends ServiciosVentasDigitales {
	
	private static final Logger logger = LogManager.getLogger(UtilApi.class);

	public static boolean validateJsonRequest(String json) {

		boolean response = false;

		if (json != null) {

			try {

				ObjectMapper mapper = new ObjectMapper();
				VentaDigitalLibreInversion request = mapper.readValue(json, VentaDigitalLibreInversion.class);
				response = true;
			} catch (Exception e) {
				response = false;
			}

		}

		return response;
	}

	
	public static NewCookie createCookieJWTDefault(String value) {
		if (value != null && !value.contains(ConstantesLibre.CONCAT_PREFIX)) {
			value = ConstantesLibre.CONCAT_PREFIX + value;
		}

		return createCookie(JWT_COOKIE_KEY, value, COOKIE_MAX_AGE);
	}
    
    public static NewCookie createCookie(String key, String value, Integer age) {
    	int maxAge = -1;
    	
    	if (age != null) {
            maxAge = (age * 60) + 5;
    	}

		return new NewCookie(key, value, "/", null, Cookie.DEFAULT_VERSION, null, maxAge, null, true, true);
    }
}
