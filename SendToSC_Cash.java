package com.skytizens.alfresco.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.RuntimeActionService;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionDefinition;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ClassManager;
import org.alfresco.util.CodeTransformer;
import org.alfresco.util.interfaces.CustomActionExecuter;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;

public class SendToSC_Cash extends ActionExecuterAbstractBase implements CustomActionExecuter {
	
public static final String NAME = "sc-cash-action";
	
	public static final String PARAM_SAVENODERULES_VALUE = "CashsaveNoderule";
	
	
    private CustomActionExecuter worker;
    
	public SendToSC_Cash(ClassManager classManager)
    {
		super();
    	CodeTransformer codeTrans = new CodeTransformer(classManager);
    	worker = (CustomActionExecuter) codeTrans.getObject("com.skytizens.alfresco.actions.SendToSC_CashEncoded", getClass());
    	if(worker == null){
    		worker = new CustomActionExecuter() {

				@Override
				public String getQueueName() {return null;}

				@Override
				public boolean getIgnoreLock() {return false;}

				@Override
				public boolean getTrackStatus() {return false;}

				@Override
				public ActionDefinition getActionDefinition() {return null;}

				@Override
				public void execute(Action action, NodeRef actionedUponNodeRef) {}

				@Override
				public void init() {}

				@Override
				public void setServiceRegistry(ServiceRegistry serviceRegistry) {}

				@Override
				public void setClassManager(ClassManager classManager) {}

				@Override
				public void setParameters(Map<String, String> parameters) {}

				@Override
				public void setRuntimeActionService(RuntimeActionService runtimeActionService) {}

				@Override
				public void setLockService(LockService lockService) {}

				@Override
				public void setBaseNodeService(NodeService nodeService) {}

				@Override
				public void setDictionaryService(DictionaryService dictionaryService) {}

				@Override
				public void setTrackStatus(boolean trackStatus) {}

				@Override
				public void setObjects(Map<String, Object> objects) {}
    		};
    	}else{
    		worker.setClassManager(classManager);
    	}
    }
	
	/**
	 * @param serviceRegistry the serviceRegistry to set
	 */
	@Override
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		worker.setServiceRegistry(serviceRegistry);
	}
	
	@Override
	public void setClassManager(ClassManager classManager) {
		// worker.setClassManager(classManager);
	}
	
	@Override
	public void setParameters(Map<String, String> parameters) {
		worker.setParameters(parameters);
	}

	@Override
	public void execute(Action action, NodeRef actionedUponNodeRef) {
		worker.execute(action, actionedUponNodeRef);
	}
	
	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) 
	{
		paramList.add(new ParameterDefinitionImpl(PARAM_SAVENODERULES_VALUE, DataTypeDefinition.NODE_REF, true, getParamDisplayLabel(PARAM_SAVENODERULES_VALUE)));
		
	}

	@Override
	public void setObjects(Map<String, Object> objects) {
		worker.setObjects(objects);
	}


	public static Object generateImageToAlfresco(final Object object, final RetryingTransactionHelper retryingTransactionHelper, final ServiceRegistry serviceRegistry, final NodeService nodeService, final ContentService contentService, final NodeRef destination,
			final File sourceFile, final File path, final String fileName, final String currentUser){
		final RetryingTransactionCallback<Object> processCallBack = new RetryingTransactionCallback<Object>()
		{
			public Object execute() throws Exception
			{   
				try {
					String tempName = fileName;
					int ind = tempName.lastIndexOf('.');
					if(ind > 0){
						tempName = tempName.substring(0, ind);
					}
					int cnt = 1;
					String fName = tempName + ".pdf";
					while(serviceRegistry.getNodeService().getChildByName(destination, ContentModel.ASSOC_CONTAINS, fName) != null){
						fName = tempName + "-" + cnt + ".pdf";
						cnt++;
					}

					Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
					props.put(ContentModel.PROP_NAME, fName);  

					// use the node service to create a new node
					NodeRef node = nodeService.createNode(
							destination, 
							ContentModel.ASSOC_CONTAINS, 
							QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, fName),
							ContentModel.TYPE_CONTENT, props).getChildRef();
					
					if(sourceFile != null){
						ContentWriter writer = contentService.getWriter(node, ContentModel.PROP_CONTENT, true);
						writer.setMimetype(MimetypeMap.MIMETYPE_PDF);
						writer.setEncoding("UTF-8");
						writer.putContent(sourceFile);
					}
					
					// Return a node reference to the newly created node
					//return null;
				}catch(Exception e){
					e.printStackTrace();
				} finally {
					try{
						if(sourceFile != null && sourceFile.exists()){
							sourceFile.delete();
							path.delete();
						}
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				return null;
			}
		};
		 return AuthenticationUtil.runAs(
				new AuthenticationUtil.RunAsWork<Object>() {
					public Object doWork() throws Exception {
						retryingTransactionHelper.doInTransaction(processCallBack, false, false);
						return null;
					}
				}, currentUser);
	}
}