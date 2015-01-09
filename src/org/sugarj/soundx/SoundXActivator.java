package org.sugarj.soundx;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.sugarj.BaseLanguageRegistry;

public class SoundXActivator extends AbstractUIPlugin {

	private BaseLanguageDefinition bld;

	private static SoundXActivator plugin;

	public SoundXActivator(String bldFilename) {
		bld = new BaseLanguageDefinition(bldFilename);
		BaseLanguageRegistry.getInstance().registerBaseLanguage(
				bld.getAbstractBaseLanguage());
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
}
