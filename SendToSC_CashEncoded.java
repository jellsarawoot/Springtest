package com.skytizens.alfresco.actions;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ClassManager;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.interfaces.CustomActionExecuter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;

import antlr.debug.NewLineEvent;

public class SendToSC_CashEncoded extends ActionExecuterAbstractBase implements CustomActionExecuter {
	
	private static String MODULE_NAME = "Media Converter";
	private static String MODULE_CODE = "0";
	private static String MODULE_ENCODED_CODE = "0";

	private static final Log logger = LogFactory.getLog(SendToSC_CashEncoded.class);

	private boolean useModule = false;
	private ServiceRegistry serviceRegistry;
	private NodeService nodeService;
	private RetryingTransactionHelper retryingTransactionHelper;
	private ContentService contentService;

	private String scCash;
	private String setFont;

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
		this.scCash = parameters.containsKey("scCash") ? parameters.get("scCash") : null;
		this.setFont = parameters.containsKey("setFont") ? parameters.get("setFont") : null;
	}

	@Override
	protected void executeImpl(final Action action, final NodeRef nodeRef) {

		if (useModule) {
			try {
				NodeRef pngNodeRef = getNodeRef(this.scCash); 
				File pathpng = getTempFilepng(pngNodeRef);
				String currentUser = AuthenticationUtil.getRunAsUser();
				String fName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
				File path = getTempFile(nodeRef);  // input text file
				Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
				props.put(ContentModel.PROP_NAME, fName);
				NodeRef saveNodeRules = (NodeRef) action.getParameterValue(SendToSC_Cash.PARAM_SAVENODERULES_VALUE);
				NodeRef destination = nodeService.getPrimaryParent(nodeRef).getParentRef(); // target folder
				if(saveNodeRules != null){
					destination = saveNodeRules;
				}
				File tempDir = TempFileProvider.getTempDir();
				String target = tempDir.getAbsolutePath() + "/" + getClass().getSimpleName() + "_pngFile_" //Result file  
						+ nodeRef.getId() + ".pdf";
				String source = tempDir.getAbsolutePath() + "/" + getClass().getSimpleName() + "_tmp empty pdf_" // tmp empty pdf 
						+ nodeRef.getId() + ".pdf";
				String source2 = tempDir.getAbsolutePath() + "/" + getClass().getSimpleName() + "_sign_target_" // tmp empty multipage pdf 
						+ nodeRef.getId() + ".pdf";
				String input = tempDir.getAbsolutePath() + "/" + getClass().getSimpleName() + "_sign_Final_" // text fixed
						+ nodeRef.getId() + ".txt";
				
				String getpdfFile = convertTopdf(source,pathpng.getAbsolutePath());  //source pdf 1 page 
				String textFix = fixLine(input, path.getAbsolutePath());	//fixing line 
				
				BufferedReader in = new BufferedReader(
						  new InputStreamReader (new FileInputStream(textFix), "utf-8"));
				
				
				List<List<String>> alltext = new ArrayList<List<String>>();
				ArrayList<String> myArrList = new ArrayList<String>();
		          int n = 1;
		          String strs = "";
		          String str;
		          int numPage = 1;
		          while ((str = in.readLine()) != null) {
		              if (str.length() > 0) {
		            	  if (str.charAt(0)!='1'){
		            	  }
		                  strs = strs + str;
		                  myArrList.add(str);
		                  str += "\n";
		                  if (str.charAt(0)=='1') {
		                	  if(numPage > 1){
		            			  alltext.add(myArrList);
		            		  }
		          	      strs = "";
		          	      myArrList = new ArrayList<String>();
		                  
		          	      numPage++;
		                  }
		              }
		              n++;
		         }
		          alltext.add(myArrList);
		          myArrList = new ArrayList<String>();
		          
		          PdfReader cover = new PdfReader(getpdfFile);
		          Document document = new Document();
		          PdfCopy copy = new PdfCopy(document, new FileOutputStream(source2));
		          document.open();
		          for(int i = 1;i<=alltext.size();i++){
		        	  copy.addPage(copy.getImportedPage(cover, 1));
		          }
		          document.close();
		          
		          String finalfile = manipulatePdf(source2,target,alltext,this.setFont);
		          
		          File targetFile = new File(finalfile);
					File t1 = new File(source);
		          SendToSC_Cash.generateImageToAlfresco(this, retryingTransactionHelper, serviceRegistry,
							nodeService, contentService, destination, targetFile, path, fName, currentUser);
				  
				  
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String  manipulatePdf(String src, String dest,List<List<String>> alltext,String setFont) throws IOException, DocumentException {
        PdfReader reader = new PdfReader(src);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(dest));
        BaseFont base = BaseFont.createFont(setFont, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font f = new Font(base, 8.5f);
        char ch1 = (char) 3585; //ก   
		char ch2 = (char) 3675; //๛
        Pattern p1 = Pattern.compile("[0-9]{1,2}\\s[^0-9\n\\s]+?\\s[0-9]{4}-?");
        Pattern p2 = Pattern.compile("^-");
        Pattern p3 = Pattern.compile("^0{1}[^0-9][^A-Z]");
        Pattern p4 = Pattern.compile("[" + ch1 + "-" + ch2 + "]");
        Pattern p5 = Pattern.compile("[=]{17}");
        int Height = 838;
        int Width = 12;
        int numpage = 1;
        int spaceLine = 10;
        for (List<String> temp : alltext){
        	PdfContentByte over = stamper.getOverContent(numpage);
        	for(String line :temp){
        		Matcher m = p1.matcher(line);
        		Matcher m2 = p2.matcher(line);
        		Matcher m3 = p3.matcher(line);
        		Matcher m4 = p4.matcher(line+1);
        		Matcher m5 = p5.matcher(line);
        		if(m4.find()){
        			Height = Height-5;
        		}
        		if (m2.find()) {
					Height = Height - 20;
				}
        		
        		if (m3.find()) {
        			Height = Height-spaceLine-10;
				}
        		 String str = " "+line.substring(1);
                 Phrase w = new Phrase(str,f);
                 Height = Height - spaceLine;
             ColumnText.showTextAligned(over, Element.ALIGN_LEFT, w, Width, Height, 0);
             if(m5.find()){
       			Height = Height+30;
       		 }
             over.setCharacterSpacing(-0.2f);
             
        	}
        	 
        	 over.saveState();
             PdfGState gs1 = new PdfGState();
             gs1.setFillOpacity(1f);
             over.setGState(gs1);
             over.restoreState();
             numpage++;
             Height = 838;
             Width = 15;
             
       }
       
        stamper.close();
        reader.close();
        return dest;
    }
 

	public synchronized File getTempFile(NodeRef nodeRef) throws Exception {
		String name = getClass().getSimpleName() + "_Spoolfile_source_" + nodeRef.getId() + ".txt";
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
	public synchronized File getTempFilepng(NodeRef nodeRef) throws Exception {
		String name = getClass().getSimpleName() + "_Spoolfile_source_" + nodeRef.getId() + ".png";
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
	
	private static Boolean checkString(String str) {
		return (str != null) && (!str.isEmpty());
	}
	private static  int[] findPosition(char value,String line) {
		String characterToString = Character.toString(value);
			int a[] = new int[StringUtils.countOccurrencesOf(line, characterToString)];
			int i = 0;
			for(int n = 0 ;n<a.length;n++){
				if(line.indexOf(value) == -1 || line.length()<1){
					break;
				}
				if(i<1){
					a[i] = line.indexOf(value);
					line = line.substring(a[i]+1);
				}
				else{
					a[i] = line.indexOf(value);
					line = line.substring(a[i]+1);
					a[i] = a[i] + a[i-1]+1;
				}
				i++;
			}
				
		return a;
	}
	private NodeRef getNodeRef(String path)
	{
		NodeRef pathNodeRef = null;

		if(path != null && !path.isEmpty())
		{
			StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
			pathNodeRef = nodeService.getRootNode(storeRef);
			QName qname = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "company_home");
			List<ChildAssociationRef> assocRefs = nodeService.getChildAssocs(pathNodeRef, ContentModel.ASSOC_CHILDREN, qname);
			pathNodeRef = assocRefs.get(0).getChildRef();
			String[] paths = path.split("/");
			for(String name : paths){
				if(!name.isEmpty())
				{
					pathNodeRef = nodeService.getChildByName(pathNodeRef, ContentModel.ASSOC_CONTAINS, name);
					if(pathNodeRef == null){
						return null;
					}
				}
			}
		}

		return pathNodeRef;
	}
private static String fixLine(String targetfile,String path){
    
    try {
      String charset = "ISO-8859-11"; 
      char ch1 = (char) 3585; //ก   
      char ch2 = (char) 3675; //๛
      char ch3 = (char) 3630; //ฮ
      
      char Va1 = (char) 3656; //่
      char Va2 = (char) 3657; //้
      char Va3 = (char) 3658; //๊
      char Va4 = (char) 3659; //๋
      char Va5 = (char) 3660; //์
      
      char Vb1 = (char) 3636; //ิ
      char Vb2 = (char) 3637; //ี
      char Vb3 = (char) 3638; //ึ
      char Vb4 = (char) 3639; //ื
      char Vb5 = (char) 3655; //็
      char Vb6 = (char) 3661; //ํ
      char Vb7 = (char) 3633; //ั
      
      char Vc1 = (char) 3640; //ุ
      char Vc2 = (char) 3641; //ู
      
      ArrayList<String> myArrList = new ArrayList<String>();
        BufferedReader in = new BufferedReader( 
            new InputStreamReader (new FileInputStream(path), charset));
      OutputStream out = new BufferedOutputStream(new FileOutputStream(targetfile),1024);      
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
      String regexA = "[" + ch1 + "-" + ch2 + "]";
      String regexB = "[" + ch1 + "-" + ch3 + "]";
      String regexC = "[" + Vc1 + "" + Vc2 + "]";
      String regexD = "["+Va1+""+Va2+""+Va3+""+Va4+""+Va5+""+Vb1+""+Vb2+""+Vb3+""+Vb4+""+Vb5+""+Vb6+""+Vb7+"]";
      Pattern p = Pattern.compile(regexA);
      Pattern p1 = Pattern.compile(regexB);
      Pattern p2 = Pattern.compile(regexC);
      Pattern p3 = Pattern.compile(regexD);
      
      int[] a1 = new int[0],a2=new int[0],a3=new int[0],a4=new int[0],a5=new int[0]; 
      int[] b1 = new int[0],b2=new int[0],b3=new int[0],b4=new int[0],b5=new int[0],b6=new int[0],b7=new int[0]; 
      int[] c1=new int[0],c2=new int[0]; 
      String line;
      Character[] Ca1 = new Character[0],Ca2=new Character[0],Ca3=new Character[0],Ca4=new Character[0],Ca5=new Character[0],
    		      Cd1 = new Character[0],Cd2=new Character[0],Cd3=new Character[0],Cd4=new Character[0],Cd5=new Character[0];
      Character[] Cb1 = new Character[0],Cb2=new Character[0],Cb3=new Character[0],Cb4=new Character[0],Cb5=new Character[0],Cb6=new Character[0],Cb7=new Character[0];
      Character[] Cc1=new Character[0],Cc2=new Character[0];
      int linee = 1;
      while( (line = in.readLine()) != null) {
      Matcher m = p.matcher(line);
      Matcher m1 = p1.matcher(line);
      Matcher m2 = p2.matcher(line);
      Matcher m3 = p3.matcher(line);
        if(m.find()){
          if(m1.find()){   //find main tense
            myArrList.add(line); 
          }
          else {
            String mains = null;
            if(myArrList.size() >0){
               mains = myArrList.get(0);
						}
						if ((m3.find() || line.length() == 1 || line.charAt(0) == '1') && checkString(mains)) {
							StringBuilder str = new StringBuilder();
							for (int i = 0; i < mains.length(); i++) {
								str.append(mains.charAt(i));
								if (i < Cc1.length && Cc1[i] != null) {
									str.append((char) Cc1[i]);
								}
								if (i < Cc2.length && Cc2[i] != null) {
									str.append((char) Cc2[i]);
								}
								if (i < Cb1.length && Cb1[i] != null) {
									str.append((char) Cb1[i]);
								}
								if (i < Cb2.length && Cb2[i] != null) {
									str.append((char) Cb2[i]);
								}
								if (i < Cb3.length && Cb3[i] != null) {
									str.append((char) Cb3[i]);
								}
								if (i < Cb4.length && Cb4[i] != null) {
									str.append((char) Cb4[i]);
								}
								if (i < Cb5.length && Cb5[i] != null) {
									str.append((char) Cb5[i]);
								}
								if (i < Cb6.length && Cb6[i] != null) {
									str.append((char) Cb6[i]);
								}
								if (i < Cb7.length && Cb7[i] != null) {
									str.append((char) Cb7[i]);
								}
								if (i < Cd1.length && Cd1[i] != null) {
									str.append((char) Cd1[i]);
								}
								if (i < Cd2.length && Cd2[i] != null) {
									str.append((char) Cd2[i]);
								}
								if (i < Cd3.length && Cd3[i] != null) {
									str.append((char) Cd3[i]);
								}
								if (i < Cd4.length && Cd4[i] != null) {
									str.append((char) Cd4[i]);
								}
								if (i < Cd5.length && Cd5[i] != null) {
									str.append((char) Cd5[i]);
								}

								if (i < Ca1.length && Ca1[i] != null) {
									str.append((char) Ca1[i]);
								}
								if (i < Ca2.length && Ca2[i] != null) {
									str.append((char) Ca2[i]);
								}
								if (i < Ca3.length && Ca3[i] != null) {
									str.append((char) Ca3[i]);
								}
								if (i < Ca4.length && Ca4[i] != null) {
									str.append((char) Ca4[i]);
								}
								if (i < Ca5.length && Ca5[i] != null) {
									str.append((char) Ca5[i]);
								}
                
              }
                 writer.write(str.toString());
                 writer.println();
                if(!isEmpty(Cc1) || !isEmpty(Cc2)){
                  writer.println();
                   }
                 a1 = new int[0]; a2=new int[0]; a3=new int[0]; a4=new int[0]; a5=new int[0]; 
               b1 = new int[0]; b2=new int[0]; b3=new int[0]; b4=new int[0]; b5=new int[0]; b6=new int[0]; b7=new int[0]; 
               c1=new int[0]; c2=new int[0]; 
               Ca1 = new Character[0]; Ca2=new Character[0]; Ca3=new Character[0]; Ca4=new Character[0]; Ca5=new Character[0];
               Cd1 = new Character[0]; Cd2=new Character[0]; Cd3=new Character[0]; Cd4=new Character[0]; Cd5=new Character[0];
               Cb1 = new Character[0]; Cb2=new Character[0]; Cb3=new Character[0]; Cb4=new Character[0]; Cb5=new Character[0]; Cb6=new Character[0]; Cb7=new Character[0];
               Cc1=new Character[0]; Cc2=new Character[0];
              myArrList.clear();
              mains = null;
              
            }
            if(!checkString(mains) || m3.find()){
               writer.write(line.substring(0, 1));
               writer.println();
               
              if(line.indexOf(Va1) > -1){
                a1 = findPosition(Va1,line);
                if(Ca1.length != 0){
                	Cd1 = new Character[line.length()];
                    for(int i = 0 ; i< a1.length ;i++){
                      Cd1[a1[i]] = line.charAt(a1[i]);
                    }
                }
                else{
                	Ca1 = new Character[line.length()];
                    for(int i = 0 ; i< a1.length ;i++){
                      Ca1[a1[i]] = line.charAt(a1[i]);
                    }
                }
                
              }
            if(line.indexOf(Va2) > -1){
                a2 = findPosition(Va2,line);
                if(Ca2.length != 0){
                	Cd2 = new Character[line.length()];
	                for(int i = 0 ; i< a2.length ;i++){
	                  Cd2[a2[i]] = line.charAt(a2[i]);
	                }
                }
                else{
	                Ca2 = new Character[line.length()];
	                for(int i = 0 ; i< a2.length ;i++){
	                  Ca2[a2[i]] = line.charAt(a2[i]);
	                }
                }
              }
              if(line.indexOf(Va3) > -1){
                a3 = findPosition(Va3,line);
                if(Ca3.length != 0){
                	Cd3 = new Character[line.length()];
                    for(int i = 0 ; i< a3.length ;i++){
                      Cd3[a3[i]] = line.charAt(a3[i]);
                    }
                }
                else{
                	Ca3 = new Character[line.length()];
                    for(int i = 0 ; i< a3.length ;i++){
                      Ca3[a3[i]] = line.charAt(a3[i]);
                    }
                }
                
              }
              if(line.indexOf(Va4) > -1){
                a4 = findPosition(Va4,line);
                if(Ca4.length != 0){
                	Cd4 = new Character[line.length()];
                    for(int i = 0 ; i< a4.length ;i++){
                      Cd4[a4[i]] = line.charAt(a4[i]);
                    }
                }
                else{
                	Ca4 = new Character[line.length()];
                    for(int i = 0 ; i< a4.length ;i++){
                      Ca4[a4[i]] = line.charAt(a4[i]);
                    }
                }
                
              }
              if(line.indexOf(Va5) > -1){
                a5 = findPosition(Va5,line);
                if(Ca5.length != 0){
                	Cd5 = new Character[line.length()];
                    for(int i = 0 ; i< a5.length ;i++){
                      Cd5[a5[i]] = line.charAt(a5[i]);
                    }
                }
                else{
                	Ca5 = new Character[line.length()];
                    for(int i = 0 ; i< a5.length ;i++){
                      Ca5[a5[i]] = line.charAt(a5[i]);
                    }
                }
              }
              if(line.indexOf(Vb1) > -1){
                b1 = findPosition(Vb1,line);
                Cb1 = new Character[line.length()];
                for(int i = 0 ; i< b1.length ;i++){
                  Cb1[b1[i]] = line.charAt(b1[i]);
                }
              }
              if(line.indexOf(Vb2) > -1){
                b2 = findPosition(Vb2,line);
                Cb2 = new Character[line.length()];
                for(int i = 0 ; i< b2.length ;i++){
                  Cb2[b2[i]] = line.charAt(b2[i]);
                }
                
              }
              if(line.indexOf(Vb3) > -1){
                b3 = findPosition(Vb3,line);
                Cb3 = new Character[line.length()];
                for(int i = 0 ; i< b3.length ;i++){
                  Cb3[b3[i]] = line.charAt(b3[i]);
                }
                
              }
              if(line.indexOf(Vb4) > -1){
                b4 = findPosition(Vb4,line);
                Cb4 = new Character[line.length()];
                for(int i = 0 ; i< b4.length ;i++){
                  Cb4[b4[i]] = line.charAt(b4[i]);
                }
              }
              if(line.indexOf(Vb5) > -1){
                b5 = findPosition(Vb5,line);
                Cb5 = new Character[line.length()];
                for(int i = 0 ; i< b5.length ;i++){
                  Cb5[b5[i]] = line.charAt(b5[i]);
                }
              }
              if(line.indexOf(Vb6) > -1){
                b6 = findPosition(Vb6,line);
                Cb6 = new Character[line.length()];
                for(int i = 0 ; i< b6.length ;i++){
                  Cb6[b6[i]] = line.charAt(b6[i]);
                }
              }
              if(line.indexOf(Vb7) > -1){
                b7 = findPosition(Vb7,line);
                Cb7 = new Character[line.length()];
                for(int i = 0 ; i< b7.length ;i++){
                  Cb7[b7[i]] = line.charAt(b7[i]);
                }
                
              }
              
            }
            if(m2.find()){
              if(line.indexOf(Vc1) > -1){
                c1 = findPosition(Vc1,line);
                Cc1 = new Character[line.length()];
                for(int i = 0 ; i< c1.length ;i++){
                  Cc1[c1[i]] = line.charAt(c1[i]);
                }
              }
              if(line.indexOf(Vc2) > -1){
                c2 = findPosition(Vc2,line);
                Cc2 = new Character[line.length()];
                for(int i = 0 ; i< c2.length ;i++){
                  Cc2[c2[i]] = line.charAt(c2[i]);
                }
              }
              
            }
          }
        }
        else if(!m.find() || line.length() <= 1){
          String mains = null;
          if(myArrList.size() >0){
             mains = myArrList.get(0);
            StringBuilder  str = new StringBuilder();
            for(int i =0;i<mains.length();i++){
              str.append(mains.charAt(i));
              if(i < Cc1.length && Cc1[i] != null){
                str.append((char) Cc1[i]);
              }
              if(i < Cc2.length && Cc2[i] != null){
                str.append((char) Cc2[i]);
              }
              if(i < Cb1.length && Cb1[i] != null){
                str.append((char) Cb1[i]);
              }
              if(i < Cb2.length && Cb2[i] != null){
                str.append((char) Cb2[i]);
              }
              if(i < Cb3.length && Cb3[i] != null){
                str.append((char) Cb3[i]);
              }
              if(i < Cb4.length && Cb4[i] != null){
                str.append((char) Cb4[i]);
              }
              if(i < Cb5.length && Cb5[i] != null){
                str.append((char) Cb5[i]);
              }
              if(i < Cb6.length && Cb6[i] != null){
                str.append((char) Cb6[i]);
              }
              if(i < Cb7.length && Cb7[i] != null){
                str.append((char) Cb7[i]);
              }
              if(i < Cd1.length && Cd1[i] != null){
                  str.append((char) Cd1[i]);
            }
              if(i < Cd2.length && Cd2[i] != null){
                  str.append((char) Cd2[i]);
            }
              if(i < Cd3.length && Cd3[i] != null){
                  str.append((char) Cd3[i]);
            }
              if(i < Cd4.length && Cd4[i] != null){
                  str.append((char) Cd4[i]);
            }
              if(i < Cd5.length && Cd5[i] != null){
                  str.append((char) Cd5[i]);
            }
              if(i < Ca1.length && Ca1[i] != null){
                  str.append((char) Ca1[i]);
            }
            if(i < Ca2.length && Ca2[i] != null){
              str.append((char) Ca2[i]);
            }
            if(i < Ca3.length && Ca3[i] != null){
              str.append((char) Ca3[i]);
            }
            if(i < Ca4.length && Ca4[i] != null){
              str.append((char) Ca4[i]);
            }
            if(i < Ca5.length && Ca5[i] != null){
              str.append((char) Ca5[i]);
            }
             
            }
               writer.write(str.toString());
               writer.println();
              if(!isEmpty(Cc1) || !isEmpty(Cc2)){
                writer.println();
                 }
               a1 = new int[0]; a2=new int[0]; a3=new int[0]; a4=new int[0]; a5=new int[0]; 
             b1 = new int[0]; b2=new int[0]; b3=new int[0]; b4=new int[0]; b5=new int[0]; b6=new int[0]; b7=new int[0]; 
             c1=new int[0]; c2=new int[0]; 
             Ca1 = new Character[0]; Ca2=new Character[0]; Ca3=new Character[0]; Ca4=new Character[0]; Ca5=new Character[0];
             Cd1 = new Character[0]; Cd2=new Character[0]; Cd3=new Character[0]; Cd4=new Character[0]; Cd5=new Character[0];
             Cb1 = new Character[0]; Cb2=new Character[0]; Cb3=new Character[0]; Cb4=new Character[0]; Cb5=new Character[0]; Cb6=new Character[0]; Cb7=new Character[0];
             Cc1=new Character[0]; Cc2=new Character[0];
            myArrList.clear();
            mains = null;
          }
          
          writer.write(line);
          writer.println();
        }
        
        
     
    }
      writer.flush();
      writer.close();
    }catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new AlfrescoRuntimeException("Unable to work with file .", e);
    }
    return targetfile;
  }

	@Override
	public void setObjects(Map<String, Object> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		// TODO Auto-generated method stub
		
	}
	private static String convertTopdf(String outputFiles,String inputFile) throws DocumentException, IOException{
		 
	     List<String> files = new ArrayList<String>();
	     files.add(inputFile);
	     
	     Document document = new Document();
	     PdfWriter.getInstance(document, new FileOutputStream(new File(outputFiles)));
	     document.open();
	     Rectangle one = new Rectangle(595,842);
	     for (String f : files) {
	         document.newPage();
	         Image image = Image.getInstance(new File(f).getAbsolutePath());
	         image.setAbsolutePosition(0, 0);
	         image.setBorderWidth(0);
	         document.setPageSize(one);
	         image.scaleAbsolute(one); 
	         document.add(image);
	     }
	     document.close();
	     return outputFiles;
	 }
	
	private static boolean isEmpty(Character[] tab){
		for(Character ch : tab){
			if(ch != null) return false;
		}
		return true;
	}
}
