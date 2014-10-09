package com.cenix.atc.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.cenix.atc.ATCActivator;

public class ATCPreferenceInitializer extends AbstractPreferenceInitializer {
	
    public static final String REPAINT_ON_RESIZE = "repaint.on.resize";
    public static final String ACT_TEMPLATES = "act.templates";


    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = ATCActivator.getDefault().getPreferenceStore();
        store.setDefault(REPAINT_ON_RESIZE, false);
        store.setDefault(ACT_TEMPLATES, ""); 
    }

}
	
