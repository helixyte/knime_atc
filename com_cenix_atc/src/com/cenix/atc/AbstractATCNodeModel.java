package com.cenix.atc;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;

import de.mpicbg.tds.knime.knutils.AbstractNodeModel;
import de.mpicbg.tds.knime.knutils.scripting.ScriptingNodeDialog;


public abstract class AbstractATCNodeModel extends AbstractNodeModel {

    protected final SettingsModelString template;

   
    public AbstractATCNodeModel(PortType[] inPorts, PortType[] outPorts) {
        super(inPorts, outPorts);
        template = createTemplateProperty();
    }
    
    
    protected AbstractATCNodeModel(int numInPorts, int numOutPorts, int... optionalInputs) {
        this(createPorts(numInPorts, optionalInputs), createPorts(numOutPorts));
    }
	
    
    public static SettingsModelString createTemplateProperty() {
        return new SettingsModelString(ScriptingNodeDialog.SCRIPT_TEMPLATE, ScriptingNodeDialog.SCRIPT_TEMPLATE_DEFAULT);
    }

    
    @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
    	throw new AbstractMethodError("Abstract method.");
    }

    @Override
    protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
    	throw new AbstractMethodError("Abstract method.");
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        template.saveSettingsTo(settings);
    }


    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        // It can be safely assumed that the settings are valided by the
        // method below.
        try {
            template.loadSettingsFrom(settings);
        } catch (Throwable t) {
            throw new RuntimeException("Could not unpersist template.");
        }
    }


    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
    }
    
}
