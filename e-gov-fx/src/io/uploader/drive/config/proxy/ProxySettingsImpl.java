
package io.uploader.drive.config.proxy;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProxySettingsImpl implements HasProxySettings
{
	final private Logger logger = LoggerFactory.getLogger(ProxySettingsImpl.class);
	
	private volatile XMLConfiguration config = null ;
	final private int defaultPort ;
	final private String baseProperty ; 
	final private String protocol ;
	
	private String user = null ;
	private String password = null ;
	private String host = null ;
	private int port ;
	boolean isActive = false ;
	
	public ProxySettingsImpl (String baseProperty, int defaultPort, String protocol)
	{
		super () ;
		this.baseProperty = baseProperty ;
		this.defaultPort = defaultPort ;
		this.protocol = protocol ;
		
		this.port = defaultPort ;
	}
	
	public synchronized void setConfig (XMLConfiguration config) 
	{
		this.config = config ;
		if (this.config == null)
		{
			logger.info("Proxy cannot be set because the config parameter is null");
			return ;
		}
		// TODO: when the commom-configuration 2.0 will be released
		// Lock config
		// ...
		port = this.config.getInt(baseProperty + ".port", defaultPort) ;
		isActive = config.getBoolean(baseProperty + ".active", false) ; 
		user = config.getString(baseProperty + ".user") ;
		password = config.getString(baseProperty + ".password") ;
		host = config.getString(baseProperty + ".host") ;
	}
	
	public synchronized void update (Proxy newProxy) throws ConfigurationException
	{
		if (config == null)
		{
			logger.info("Proxy cannot be updated because the config parameter is null");
			return ;
		}
		if (newProxy == null)
		{
			logger.info("Proxy cannot be updated because the new proxy parameter is null");
			return ;
		}
		
		port = newProxy.getPort() ;
		isActive = newProxy.isActive() ; 
		user = newProxy.getUsername() ;
		password = newProxy.getPassword() ;
		host = newProxy.getHost() ;

		config.setProperty(baseProperty + ".port", String.valueOf(port));	
		config.setProperty(baseProperty + ".active", String.valueOf(isActive));
		config.setProperty(baseProperty + ".user", user);
		config.setProperty(baseProperty + ".password", password);
		config.setProperty(baseProperty + ".host", host);
		config.save();
	}
	
	@Override
	public synchronized boolean isActive() {
		return isActive ;
	}

	@Override
	public synchronized String getProtocol() {
		return protocol ;
	}

	@Override
	public synchronized String getUsername() {
		return user ;
	}

	@Override
	public synchronized String getPassword() {
		return password ;
	}

	@Override
	public synchronized String getHost() {
		return host ;
	}

	@Override
	public synchronized int getPort() {
		return port;
	}

	@Override
	public CredentialsProvider getCredentialsProvider() {
		if (isActive ()) {
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(
	    			new AuthScope(getHost(), getPort()), 
	    			new UsernamePasswordCredentials(getUsername(), getPassword()));  
			return credsProvider ;
		} else {
			return null;
		}
	}
}