package com.cenix.atc;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import de.mpicbg.tds.knime.knutils.scripting.AbstractTableScriptingNodeModel;
import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplate;


public class ATCNodeModel extends AbstractTableScriptingNodeModel {
	
	private static CompositeConfiguration config=null;
	
	private ArrayList<String> colNames;
	private ArrayList<DataType> colTypes;
	private ArrayList<String> colValues;
	private HashMap<String, Class<? extends DataCell> > variableTypeMap;
	private HashMap<String, HashMap<String, String> > variableItemIdMap; 
	
	private final static String KNIME_DATA_TYPE_ATTRIBUTE = "knime-data-type";

    public ATCNodeModel(int numInputs, int numOutputs) {
        super(numInputs, numOutputs);
        if (config == null) {
            try {
                config = new CompositeConfiguration();
                // Config file mapping supported tags to KNIME data cell types.
                config.addConfiguration(
                        new PropertiesConfiguration(
                        		ATCNodeModel.class.getResource(
                        				"/com/cenix/atc/config/rggtags.properties")));
                // Config file mapping supported values of the knime-data-type attribute
                // to KNIME data cell types.
                config.addConfiguration(
                        new PropertiesConfiguration(
                        		ATCNodeModel.class.getResource(
                        				"/com/cenix/atc/config/rggknimedatatype.properties")));
            } catch (ConfigurationException ex) {
                Logger.getLogger(ATCNodeModel.class.getName()).severe(ex.toString());
            }
        }        
        // Arrays collecting the name, type, and value for each scripted column.
        this.colNames = new ArrayList<String>();
        this.colTypes = new ArrayList<DataType>();
        this.colValues = new ArrayList<String>();
        this.variableTypeMap = new HashMap<String, Class<? extends DataCell> >();
        this.variableItemIdMap = new HashMap<String, HashMap<String, String> >();
    }
	
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) 
    		throws InvalidSettingsException {
    	this.colNames.clear();
    	this.colTypes.clear();
    	this.colValues.clear();
        String rScript = script.getStringValue();
        Pattern pat = Pattern.compile("(?<name>[\\w\\.#]+) ?(=|<-) ?(?<value>.+)");
        // Build a map of variable names to data cell types.
        this.buildMaps();
        if (rScript != null) {
        	// Go through the generated Script. Only lines matching a variable
        	// assignment regex (allowing = or <- as assignment operator) are 
        	// considered.
        	String[] lines = rScript.split("\n");
        	for (int i=0;i<lines.length;i++) {
        		String line = lines[i];
        		Matcher mat = pat.matcher(line);
        		if (mat.matches()) {
        			// Extract and record variable name, type, and value. 
        			// Enclosing quotes are stripped from the value.
            		String varName = mat.group("name");
            		String varValue = mat.group("value");
            		this.colNames.add(varName);
            		Class<? extends DataCell> defaultDataCellClass = null; 
            		if (this.variableTypeMap.containsKey(varName)) {
            			defaultDataCellClass = this.variableTypeMap.get(varName);
            		} else {
            			// For variable declarations without a tag, we use the
            			// StringCell class.
            			defaultDataCellClass = StringCell.class;
            		}
            		this.colTypes.add(DataType.getType(defaultDataCellClass));
            		if (varValue.startsWith("\"") && varValue.endsWith("\"")) {
            			varValue = varValue.substring(1, varValue.length() -1);
            		}
            		if (this.variableItemIdMap.containsKey(varName)) {
            			// If this was a combobox or listbox variable, we have 
            			// to look up the ID for the selected item.
            			HashMap<String, String> idsMap = this.variableItemIdMap.get(varName);
            			this.colValues.add(idsMap.get(varValue));
            		} else {
            			this.colValues.add(varValue);
            		}
        		}
        	}        	
        }
    	// Create output column and table specs.
    	DataColumnSpec[] colSpecs = new DataColumnSpec[colNames.size()];
    	for (int i=0; i<colSpecs.length; i++) {
    		colSpecs[i] = new DataColumnSpecCreator(colNames.get(i), colTypes.get(i)).createSpec();
    	}
        return new DataTableSpec[]{new DataTableSpec(colSpecs)};
    }
    
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
                                          final ExecutionContext exec) throws Exception {
    	// Create data cells.
        final DataTableSpec outSpecs = configure(new DataTableSpec[]{null})[0];
    	int numCols = outSpecs.getNumColumns();
    	DataCell[][] stringCells = new DataCell[1][numCols];
    	// Build a table of string values.
    	DataColumnSpec[] stringColSpecs = new DataColumnSpec[numCols];
    	for (int i=0; i<numCols; i++) {
    		StringCell sCell = new StringCell(this.colValues.get(i));
    		stringCells[0][i] = sCell;
    		stringColSpecs[i] = new DataColumnSpecCreator(colNames.get(i), StringCell.TYPE).createSpec();
    	}
    	DataRow row = new DefaultRow(new RowKey("0"), stringCells[0]);
    	BufferedDataContainer cnt = exec.createDataContainer(new DataTableSpec(stringColSpecs));
    	cnt.addRowToTable(row);
    	cnt.close();
    	BufferedDataTable bufTbl = exec.createBufferedDataTable(cnt.getTable(), exec);
    	// Now, convert to the proper column types as configured by the input columns.
    	BufferedDataTable out = exec.createSpecReplacerTable(bufTbl, outSpecs);
    	return new BufferedDataTable[]{out};
    }

    private void buildMaps() {    	
        String serializedTmpl = template.getStringValue();
        if (serializedTmpl != "") {
        	// We need to pass the class loader to the XStream instance so the ScriptTemplate class
        	// tag can be looked up.
            XStream xstream = new XStream(null,
            							  new DomDriver(),
            							  ATCNodeModel.class.getClassLoader());
            ScriptTemplate tmpl = (ScriptTemplate) xstream.fromXML(serializedTmpl);
            String rggTmpl = tmpl.getTemplate();
            InputStream xmlStream = new BufferedInputStream(
            							new ByteArrayInputStream(
            									rggTmpl.getBytes(Charset.forName("UTF-8"))));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
    	        DocumentBuilder builder = factory.newDocumentBuilder();
    	        Document document = builder.parse(xmlStream);
    	        Element rootElement = null;
    	        for (int i = 0; i < document.getChildNodes().getLength(); i++) {
    	            if (document.getChildNodes().item(i).getNodeType() == Element.ELEMENT_NODE) {
    	            	rootElement = (Element) document.getChildNodes().item(i);
    	            }
    	        }
    	        traverseElement(rootElement);
            } catch (Exception e) {
    			Logger.getLogger(getClass().getName()).severe(e.toString());
    		}        	
        }
    }
    
    private void traverseElement(Element el) {
		for (int i = 0; i < el.getChildNodes().getLength(); i++) {
			if (el.getChildNodes().item(i).getNodeType() == Element.ELEMENT_NODE) {
	        	Element childNode = (Element) el.getChildNodes().item(i);
	        	handleElement(childNode);    				
			}
    	}	
    }
    
	private void handleElement(Element el) {    	
    	String elTag = el.getNodeName();
		Node txtNode = el.getPreviousSibling();
    	if (config.containsKey(elTag) && txtNode != null) {
    		// We support defining the variable name in the "var" attribute or in a text node
    		// immediately preceding the element definition, e.g.:
    		//		my_text = <textfield label="My text:"/>
    		String varName = null;
    		varName = el.getAttribute("var");
    		if (StringUtils.isBlank(varName)) {
    			varName = txtNode.getTextContent().split("=")[0].trim();
    		}
    		if (varName == null) {
    			throw new IllegalArgumentException(
    					"Need to define variable name (either as var attribute or in a text node).");
    		}
    		// Process the "knime-data-type" attribute. 
    		String dataTypeName = el.getAttribute(KNIME_DATA_TYPE_ATTRIBUTE);
    		if (! StringUtils.isBlank(dataTypeName)) {
    			loadTypeFromConfig(dataTypeName, varName);
    		} else {
        		loadTypeFromConfig(elTag, varName);
    		}
    		if (elTag == "combobox" || elTag == "listbox") {
    			// For comboboxes and listboxex, we support a "item-ids" attribute.
        		String itemIdsString = null;
        		itemIdsString = el.getAttribute("item-ids");
        		if (!StringUtils.isBlank(itemIdsString)) {
        			loadItemIds(varName, el.getAttribute("items"), itemIdsString);
        		}
    		}
    	} else if (elTag == "group") {
    		traverseElement(el);
    	}
	}
	
	@SuppressWarnings("unchecked")
	private void loadTypeFromConfig(String configName, String varName) {
		if (config.containsKey(configName)) {
			String clsName = config.getString(configName);
			try {
	    		@SuppressWarnings("rawtypes")
				Class defaultCellTypeClass = Class.forName(clsName);
	    		this.variableTypeMap.put((String) varName, defaultCellTypeClass);
			} catch (ClassNotFoundException ex) {
				Logger.getLogger(getClass().getName()).warning(
											"Invalid data type definition " + clsName);
			}    			
		} else {
			Logger.getLogger(getClass().getName()).warning("No data type configured for XML value: " + configName);
		}
	}
		
	private void loadItemIds(String varName, String itemsString, String itemIdsString) {
		HashMap <String, String> itemIdMap = new HashMap <String, String> ();
		String[] itemStrings = itemsString.split(",");
		String[] itemIdStrings = itemIdsString.split(",");
		if (itemStrings.length - 1 != itemIdStrings.length) {
			throw new IllegalArgumentException("The number of values in the items and the item-ids attributes differ.");
		}
		for (int i=0; i<itemStrings.length-1; i++) {
			String itemString = itemStrings[i+1].trim();
			String itemIdString = itemIdStrings[i].trim();
			itemIdMap.put(itemString, itemIdString);
		}
		this.variableItemIdMap.put(varName, itemIdMap);
	}

}
