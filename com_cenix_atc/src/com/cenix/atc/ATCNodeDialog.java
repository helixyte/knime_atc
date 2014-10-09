package com.cenix.atc;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.knime.core.node.NodeLogger;

import com.cenix.atc.prefs.ATCPreferenceInitializer;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.mpicbg.tds.knime.knutils.scripting.ScriptingNodeDialog;
import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplate;
import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplateWizard;


public class ATCNodeDialog extends ScriptingNodeDialog {
	
	private static final String urlPat = "https?://[\\-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[\\-a-zA-Z0-9+&@#/%=~_|]";
	private static final String idPat =  "[\\w]+";
	// The URL "macro" has the form {{{URL(<url>,<id field>,<label field>)}}}
	// Both the id and the label field are optional (if none is given, the 
	// attribute "id" is used; if only the id field is given, the IDs are 
	// shown as labels).
	private static final Pattern rggPat = 
			Pattern.compile("[$]{3}URL[(](?<url>" + urlPat + ")( *, *(?<idfield>" + idPat + ")( *, *(?<labelfield>" + idPat + "))?)?[)][$]{3}");
	private static final NodeLogger logger = NodeLogger.getLogger(ATCNodeDialog.class);
	
    /**
     * Node settings pane for ATC nodes.
     */
    public ATCNodeDialog() {
    	// We don't need the options the super class provides.
        super("", null, true, true);
        // Remove the script output tab (we build the output specs ourselves depending on the input
        // fields).
        this.removeTab("Script Output");
        // Hide the "Provides User-Interface" label and check box.
        ScriptTemplateWizard tmplTab = (ScriptTemplateWizard) this.getTab("Templates");
        for (Component tcmpt : getAllComponents(tmplTab)) {
        	if (tcmpt instanceof JLabel && ((JLabel) tcmpt).getText().startsWith("Provides") ) {
                tcmpt.getParent().setVisible(false);
                break;
        	}
        }
        // Hide the "Unlink from Template" and "Edit Template" buttons. Ideally, we would like to
        // rename the "Script Editor" tab to something else, but the SCRIPT_TAB_NAME constant is final.
        JPanel scriptTab = (JPanel) this.getTab(SCRIPT_TAB_NAME);
        for (Component scmpt : getAllComponents(scriptTab)) {
        	if (scmpt instanceof JButton && ((JButton) scmpt).getText().startsWith("Unlink") ) {
                scmpt.getParent().setVisible(false);
                break;
        	}
        }
    }
    
    /**
     * Obtains the templates from the preferences tab settings.
     */
    @Override
    public String getTemplatesFromPreferences() {
        return ATCActivator.getDefault().getPreferenceStore().getString(
        												ATCPreferenceInitializer.ACT_TEMPLATES);
    }

    /**
     * Overridden so we can process the $$$URL()$$$ "macro" before the template is used.
     */
    @Override
    public void useTemplate(ScriptTemplate template) {
    	String processedTemplateString = this.preprocessTemplateString(template.getTemplate());
    	template.setTemplate(processedTemplateString);
    	super.useTemplate(template);
    }
    
    private String preprocessTemplateString(String tmplString) {
    	Matcher rggMatcher = ATCNodeDialog.rggPat.matcher(tmplString);
    	while (rggMatcher.find()) {
    		//
    		String url = rggMatcher.group("url");
    		String idFieldName = rggMatcher.group("idfield");
    		String labelFieldName = rggMatcher.group("labelfield");
    		if (StringUtils.isBlank(idFieldName)) {
    			idFieldName = "id";
    		}
    		boolean hasLabels = ! StringUtils.isBlank(labelFieldName);
    		Client restClient = Client.create();
    		WebResource rc = restClient.resource(url);
    		try {
    			logger.info("Calling REST service at " + url + "to obtain RGG template data.");
        		ClientResponse response = rc.accept(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        		if (response.getStatus() != 200) {
        			String msg = "Error preprocessing template because call to REST service at " + 
        						 url + "" + " failed (HTTP status " + response.getStatus() + ").";
        			logger.error(msg);
        		}    			
        		String jsonResponse = response.getEntity(String.class);
        		Object jsonItemsObj = JSONValue.parse(jsonResponse);
        		JSONArray jsonItems = (JSONArray) jsonItemsObj;
        		ArrayList<String> itemIds = new ArrayList<String>();
        		ArrayList<String> itemLabels = new ArrayList<String>();
        		for (Object jsonItemObj : jsonItems) {
        			JSONObject jsonItem = (JSONObject) jsonItemObj;
        			itemIds.add((String) jsonItem.get(idFieldName).toString());
        			if (hasLabels) {
        				itemLabels.add((String) jsonItem.get(labelFieldName));
        			}
        		}
        		if (itemIds.size() == 0) {
        			String msg = "Error preprocessing template because call to REST service at " + 
   						 		 url + "" + " did not return any results.";
        			logger.error(msg);
        		}
        		String itemsString = null;
        		String itemIdsString = null;
        		if (hasLabels) {
        			itemsString = itemLabels.toString();
        			itemIdsString = itemIds.toString();
        		} else {
        			itemsString = itemIds.toString();
        		}
      			String replString = "Please Select...," + itemsString.substring(1, itemsString.length()-1);
      			if (hasLabels) {
      				// We store the item IDs fetched in the REST calls as a custom "item-ids" 
      				// attribute in the XML element. 
      				replString += "'" + System.getProperty("line.separator") + "item-ids='" + itemIdsString.substring(1, itemIdsString.length()-1);
      			}
        		tmplString = tmplString.replace(tmplString.substring(rggMatcher.start(), rggMatcher.end()), replString);
        		rggMatcher.reset(tmplString);
    		} catch (ClientHandlerException exc) {
    			String msg = "Error preprocessing template because call to REST service at " + 
    					     url + "" + " failed (" + exc + ").";
    			logger.error(msg);
			} 
    	}
    	return tmplString;
    }
	
    private List<Component> getAllComponents(Container cnt) {
    	// Recursively traverse and collect the given container's children.
        Component[] comps = cnt.getComponents();
        List<Component> compList = new ArrayList<Component>();
        for (Component comp : comps) {
        	compList.add(comp);
        	if (comp instanceof Container) {
        		compList.addAll(getAllComponents((Container) comp));
        	}
        }
    	return compList;
    }    
    
}
