package com.bcol.vtd.api.preaprobadoli.security;

import bancolombia.poc.vd.security.model.JWTClaims;
import com.bcol.vtd.api.preaprobadoli.exception.LibreInversionException;


public interface ISecurity {


    /**
     * @param publicKey
     * @param jwt
     * @return claims from jwt
     * @throws Exception
     */
    JWTClaims validateJwToken(String publicKey, String jwt) throws LibreInversionException;

    /**
     * @param claims
     * @return
     * @throws Exception
     */
    String generateJwToken(JWTClaims claims)throws Exception;

    /**
     * @param ventaDigital
     * @param jwt
     * @return idSesion from jwt
     */
    String getIdSesion(VentaDigital ventaDigital, String jwt) throws Exception;

    /**
     * @param path
     * @return publicKey from certificate file
     */
    String getPublicKeyFile(String path);

    /**
     * @param path
     * @return privateKey from certificate file
     */
    String getPrivateKey(String path);

    /**
     * @param jwt
     * @return idSesion from jwt
     */
    public JWTClaims getJWTClaims(String jwt) throws Exception;
}
