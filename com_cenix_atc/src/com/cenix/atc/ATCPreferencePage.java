package com.cenix.atc;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.cenix.atc.prefs.ATCPreferenceInitializer;

import de.mpicbg.tds.knime.knutils.scripting.prefs.TemplateTableEditor;


public class ATCPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

    public ATCPreferencePage() {
        super(GRID);
        setPreferenceStore(ATCActivator.getDefault().getPreferenceStore());
        setDescription("ATC preferences");
    }
	
	
	@Override
	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
        addField(new BooleanFieldEditor(ATCPreferenceInitializer.REPAINT_ON_RESIZE, "Repaint on resize", parent));
        addField(new TemplateTableEditor(ATCPreferenceInitializer.ACT_TEMPLATES, "ATC template resource", parent));
	}

}
