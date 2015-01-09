package org.sugarj.soundx;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.sugarj.BaseLanguageRegistry;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;

public class SoundXActivator extends AbstractUIPlugin {

	private static SoundXActivator plugin;

	public SoundXActivator(String bldFilename) {
		Path pluginDirectory = getPluginDirectory();
		SoundXAbstractBaseLanguage instance = SoundXAbstractBaseLanguage.getInstance();
		instance.processBaseLanguageDefinition(bldFilename, pluginDirectory);
		BaseLanguageRegistry.getInstance().registerBaseLanguage(instance);
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static SoundXActivator getDefault() {
		return plugin;
	}

	private final Path getPluginDirectory() {
		String thisClassPath = this.getClass().getName().replace(".", "/")
				+ ".class";
		URL thisClassURL = this.getClass().getClassLoader()
				.getResource(thisClassPath);

		if (thisClassURL.getProtocol().equals("bundleresource"))
			try {
				thisClassURL = FileLocator.resolve(thisClassURL);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		String classPath;
		try {
			classPath = new File(thisClassURL.toURI()).getAbsolutePath();
		} catch (URISyntaxException e) {
			classPath = new File(thisClassURL.getPath()).getAbsolutePath();
		}

		String binPath = classPath.substring(0, classPath.length()
				- thisClassPath.length());

		String binSuffix = binPath.substring(binPath.length()-4, binPath.length());
		if(! binSuffix.equals("bin/"))
			throw new RuntimeException("binPath does not end with bin/");

		String pluginPath = binPath.substring(0, binPath.length()-4);

		return new AbsolutePath(pluginPath);
	}
}
