package com.automic.agilecentral.actions;

import java.net.URI;
import java.net.URISyntaxException;
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
import com.automic.agilecentral.constants.ExceptionConstants;
import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;
import com.automic.agilecentral.util.ConsoleWriter;
import com.automic.agilecentral.validator.AgileCentralValidator;
import com.rallydev.rest.RallyRestApi;

/**
 * This class defines the execution of any action.It provides some initializations and validations on common inputs .The
 * child actions will implement its executeSpecific() method as per their own need.
 */
public abstract class AbstractHttpAction extends AbstractAction {

    /**
     * apiKey to make the request for all the actions
     */
    protected RallyRestApi rallyRestTarget;

    public AbstractHttpAction() {
        addOption(Constants.BASE_URL, true, "CA Agile Central URL");
        addOption(Constants.USERNAME, false, "Username for Login into CA Agile Central");
        addOption(Constants.BASIC_AUTH, true, "Basic authentication");
        addOption(Constants.SKIP_CERT_VALIDATION, false, "Skip SSL Validation");
    }

    /**
     * This method initializes the arguments and calls the execute method.
     *
     * @throws AutomicException
     *             exception while executing an action
     */
    public final void execute() throws AutomicException {
        prepareCommonInputs();
        executeSpecific();
    }

    @SuppressWarnings("deprecation")
    private void prepareCommonInputs() throws AutomicException {

        URI baseUrl = null; // base url

        String temp = getOptionValue(Constants.BASE_URL);
        try {
            baseUrl = new URI(temp);
        } catch (URISyntaxException e) {
            ConsoleWriter.writeln(e);
            String msg = String.format(ExceptionConstants.INVALID_INPUT_PARAMETER, "URL", temp);
            throw new AutomicException(msg);
        }

        // check if rally authentication is using API token or basic auth
        String tempAuth = getOptionValue(Constants.BASIC_AUTH);
        AgileCentralValidator.checkNotEmpty(tempAuth, "Basic authentication");
        boolean basicAuth = CommonUtil.convert2Bool(tempAuth);

        // api token or password to username
        String password = System.getenv(Constants.ENV_PASSWORD);

        // for performing all the CRUD and query operations
        if (basicAuth) {
            rallyRestTarget = new RallyRestApi(baseUrl, getOptionValue(Constants.USERNAME), password);
        } else {
            rallyRestTarget = new RallyRestApi(baseUrl, password);
        }

        String apiVersion = CommonUtil.getEnvParameter(Constants.ENV_API_VERSION, Constants.AC_API_VERSION);
        rallyRestTarget.setWsapiVersion(apiVersion);

        // if cert validation needs to be skipped
        if (CommonUtil.convert2Bool(getOptionValue(Constants.SKIP_CERT_VALIDATION))) {
            int port = CommonUtil.getEnvParameter(Constants.ENV_PORT, Constants.AC_PORT);
            SchemeRegistry registry = rallyRestTarget.getClient().getConnectionManager().getSchemeRegistry();
            registry.register(new Scheme("https", port, buildSSLSocketFactory()));
        }

    }

    /**
     * to configure an insecure rally rest api.
     * 
     * @return
     * @throws AutomicException
     */
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

    /**
     * Method to execute the action.
     *
     * @throws AutomicException
     */
    protected abstract void executeSpecific() throws AutomicException;

}