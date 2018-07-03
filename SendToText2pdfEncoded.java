package com.skytizens.alfresco.actions;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ClassManager;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.interfaces.CustomActionExecuter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.commons.io.IOUtils;

public class SendToText2pdfEncoded extends ActionExecuterAbstractBase implements CustomActionExecuter {

	private static String MODULE_NAME = "Media Converter";
	private static String MODULE_CODE = "0";
	private static String MODULE_ENCODED_CODE = "0";

	private static final Log logger = LogFactory.getLog(SendToText2pdfEncoded.class);

	private boolean useModule = false;

	private ServiceRegistry serviceRegistry;
	private NodeService nodeService;
	private RetryingTransactionHelper retryingTransactionHelper;
	private ContentService contentService;

	private String convertPath;

	public final Random r = new Random();

	/**
	 * @param serviceRegistry
	 *            the serviceRegistry to set
	 */
	@Override
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = serviceRegistry.getNodeService();
		this.retryingTransactionHelper = serviceRegistry.getRetryingTransactionHelper();
		this.contentService = serviceRegistry.getContentService();
	}

	@Override
	public void setClassManager(ClassManager classManager) {
		useModule = MODULE_CODE.equals(classManager.getVerifyCode(MODULE_ENCODED_CODE, this));
	}

	@Override
	public void setParameters(Map<String, String> parameters) {

		this.convertPath = parameters.containsKey("convertPath") ? parameters.get("convertPath") : null;
	}

	@Override
	protected void executeImpl(final Action action, final NodeRef nodeRef) {

		if (useModule) {
			try {
				String currentUser = AuthenticationUtil.getRunAsUser();
				String fName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
				File path = getTempFile(nodeRef);
				Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
				props.put(ContentModel.PROP_NAME, fName);

				File targetFile = null;
				File tempDir = TempFileProvider.getTempDir();
				String sourcePath = tempDir.getAbsolutePath() + "/" + getClass().getSimpleName() + "_sign_target_"
						+ nodeRef.getId() + ".pdf";

				NodeRef saveNodeRules = (NodeRef) action.getParameterValue(SendToText2pdf.PARAM_SAVENODERULES_VALUE);
				String saveNode = (String) action.getParameterValue(SendToText2pdf.PARAM_SAVENODE_VALUE);
				String textfont = (String) action.getParameterValue(SendToText2pdf.PARAM_FONT_NAME);
				String fontsize = (String) action.getParameterValue(SendToText2pdf.PARAM_FONT_SIZE);
				String spaceline = (String) action.getParameterValue(SendToText2pdf.PARAM_EXTRA_SPACELINES);
				String spacecharacters = (String) action.getParameterValue(SendToText2pdf.PARAM_EXTRA_SPACECHARACTERS);
				String papersize = (String) action.getParameterValue(SendToText2pdf.PARAM_PAPER_SIZE);
				String pageOrientation = (String) action.getParameterValue(SendToText2pdf.PARAM_PAGE_ORIENTATION);
				String title = (String) action.getParameterValue(SendToText2pdf.PARAM_TITLE_NAME);
				String encoding = (String) action.getParameterValue(SendToText2pdf.PARAM_ENCODING_VALUE);
				String author = (String) action.getParameterValue(SendToText2pdf.PARAM_AUTHOR_NAME);
				String marginleft = (String) action.getParameterValue(SendToText2pdf.PARAM_MARGIN_LEFT);
				String marginright = (String) action.getParameterValue(SendToText2pdf.PARAM_MARGIN_RIGHT);
				String margintop = (String) action.getParameterValue(SendToText2pdf.PARAM_MARGIN_TOP);
				String marginbottom = (String) action.getParameterValue(SendToText2pdf.PARAM_MARGIN_BOTTOM);
				Boolean brakeOnblanks = (Boolean) action.getParameterValue(SendToText2pdf.PARAM_BRAKE_ONBLANKS);
				Boolean pagenumber = (Boolean) action.getParameterValue(SendToText2pdf.PARAM_PAGE_NUMBER);
				Boolean rownumber = (Boolean) action.getParameterValue(SendToText2pdf.PARAM_ROW_NUMBER);
				String regex = (String) action.getParameterValue(SendToText2pdf.PARAM_BRAKE_REGEX);

				NodeRef saveNodeRef = null;
				if (saveNode != null && !saveNode.isEmpty()) {
					saveNodeRef = new NodeRef(saveNode);
				} else if (saveNodeRules != null) {
					saveNodeRef = saveNodeRules;
				} else {
					saveNodeRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
				}
				StringBuilder ccmd = new StringBuilder();

				ccmd.append(this.convertPath);
				ccmd.append(" " + path.getAbsolutePath());
				ccmd.append(" --output " + sourcePath);
				if (checkString(textfont)) {
					ccmd.append(" --font \"" + textfont + "\"");
				}
				if (checkString(fontsize)) {
					ccmd.append(" --font-size " + fontsize);
				}
				if (checkString(spaceline)) {
					ccmd.append(" --extra-vertical-space \"" + spaceline + "\"");
				}
				if (checkString(spacecharacters)) {
					ccmd.append(" --kerning \"" + spacecharacters + "\"");
				}
				if (checkString(papersize)) {
					ccmd.append(" --media " + papersize);
				}
				if (pageOrientation == "landscape") {
					ccmd.append(" --landscape");

				}
				if (checkString(title)) {
					ccmd.append(" --title \"" + title + "\"");
				}
				if (checkString(author)) {
					ccmd.append(" --author \"" + author + "\"");
				}
				if (checkString(marginleft)) {
					ccmd.append(" --margin-left \"" + marginleft + "\"");
				}
				if (checkString(marginright)) {
					ccmd.append(" --margin-right \"" + marginright + "\"");
				}
				if (checkString(margintop)) {
					ccmd.append(" --margin-top \"" + margintop + "\"");
				}
				if (checkString(marginbottom)) {
					ccmd.append(" --margin-bottom \"" + marginbottom + "\"");
				}

				if (checkString(encoding) && !encoding.equals("Auto")) {
					ccmd.append(" --encoding " + encoding);
				}

				if (brakeOnblanks) {
					ccmd.append(" --break-on-blanks");
				}
				if (pagenumber) {
					ccmd.append(" --page-number");
				}
				if (rownumber) {
					ccmd.append(" --linenumber");
				}
				if(checkString(regex)){
					ccmd.append(" --break-on-regexp "+regex);
				}
				Process pr = Runtime.getRuntime().exec(ccmd.toString());

				if (pr.waitFor() == 0) {
					targetFile = new File(sourcePath);
					if (targetFile.exists()) {
						SendToText2pdf.generateImageToAlfresco(this, retryingTransactionHelper, serviceRegistry,
								nodeService, contentService, saveNodeRef, targetFile, path, fName, currentUser);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setObjects(Map<String, Object> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {
		// TODO Auto-generated method stub

	}

	public synchronized File getTempFile(NodeRef nodeRef) throws Exception {
		String name = getClass().getSimpleName() + "_txt2pdf_source_" + nodeRef.getId() + ".txt";
		File tempDir = TempFileProvider.getTempDir();
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}
		File tempFile = new File(tempDir, name);
		if (!tempFile.exists()) {
			ContentReader contentReader = serviceRegistry.getContentService().getReader(nodeRef,
					ContentModel.PROP_CONTENT);
			OutputStream out = new FileOutputStream(tempFile);
			IOUtils.copy(contentReader.getContentInputStream(), out);
			out.flush();
			out.close();
		}
		return tempFile;
	}

	private Boolean checkString(String str) {
		return (str != null) && (!str.isEmpty());
	}

}
