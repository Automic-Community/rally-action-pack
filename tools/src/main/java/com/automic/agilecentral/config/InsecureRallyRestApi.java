package com.automic.agilecentral.config;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;

import com.automic.agilecentral.constants.Constants;
import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.rallydev.rest.RallyRestApi;

/**
 * to configure an insecure rally rest api.
 * 
 * @author shrutinambiar
 *
 */
public class InsecureRallyRestApi extends RallyRestApi {
    @SuppressWarnings("deprecation")
    public InsecureRallyRestApi(URI server, String userName, String password) throws AutomicException {
        super(server, userName, password);
        skipValidation();
    }

    public InsecureRallyRestApi(URI server, String apiKey) throws AutomicException {
        super(server, apiKey);
        skipValidation();
    }

    private void skipValidation() throws AutomicException {
        int port = CommonUtil.getEnvParameter(Constants.ENV_PORT,
                Constants.AC_PORT);
        SchemeRegistry registry = client.getConnectionManager().getSchemeRegistry();
        registry.register(new Scheme("https", port, buildSSLSocketFactory()));
    }
    
    private SSLSocketFactory buildSSLSocketFactory() throws AutomicException {
        TrustStrategy ts = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                return true;
            }
        };

        try {
            return new SSLSocketFactory(ts, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
            ConsoleWriter.writeln(e);
            throw new AutomicException(e.getMessage());
        }
    }
    

}