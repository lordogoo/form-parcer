import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.sun.deploy.util.StringUtils;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by Mark on 2015-10-30.
 *
 * this script reads an html kmri form from the openmrs forms module
 * and extracts the relevant information so that it can be displayed
 * on the wiki.
 *
 * This script is meant to be run in the console and accepts two parameters
 *
 * the first parameter is the name of a text file that contains the form code
 *
 * the next parameter is the name of the file to output the resulting html table to
 */
public class Converter {

    public static void main( final String[] args ){
        /********************************************************
         * Important
         *
         * this program needs four parameters to run.
         * make sure to set these four parameters before running.
         *******************************************************/
        //make sure that four arguments are entered into the program
        if(args.length != 4) {
            //display an error
            System.out.println("Error");
            System.out.println("This script must take in four arguments.");
            System.out.println("First it needs a string name of a file to be parsed.");
            System.out.println("Seccond it needs needs a string name of an output file if it doesnt exsist one will be created");
            System.out.println("Third it needs your kmri openmrs username");
            System.out.println("Fourth it needs your kmri openmrs password");
        }else {
            //begin formatting the information
            System.out.println("Converting file");
            System.out.println("Input file: "+args[0]);
            System.out.println("Output file: "+args[1]);

            try{

                String username = args[2];
                String password = args[3];

                System.out.println(System.getProperty("user.dir"));
                System.out.println();

                //extract only the relevent open mrs html tags
                String filetext = readFile(args[0]);
                List<String> inputList = new ArrayList<String>();
                Document doc = Jsoup.parse(filetext);
                Elements obs = doc.getElementsByTag("obs");
                for (Element comp : obs) {
                    String conceptId = comp.attr("conceptId");
                    conceptId = conceptId.replaceAll("\\s","");
                    if(!conceptId.equals("")) {
                        System.out.println("add ConceptId "+conceptId);
                        inputList.add(conceptId);
                    }
                    String answerConceptId = comp.attr("answerConceptId");
                    answerConceptId = answerConceptId.replaceAll("\\s","");
                    if(!answerConceptId.equals("")) {
                        System.out.println("add answerConceptId "+answerConceptId);
                        inputList.add(answerConceptId);
                    }
                    String answerConceptIds = comp.attr("answerConceptIds");
                    answerConceptIds = answerConceptIds.replaceAll("\\s","");
                    if(!answerConceptIds.equals("")) {
                        String[] s = separateCommas(answerConceptIds);
                        for(int i = 0; i < s.length; i++) {
                            System.out.println("add answerConceptIds "+s[i]);
                            inputList.add(s[i]);
                        }
                    }
                    String defaultValue = comp.attr("defaultValue");
                    defaultValue = defaultValue.replaceAll("\\s","");
                    if(!defaultValue.equals("")) {
                        System.out.println("add defaultValue "+defaultValue);
                        inputList.add(defaultValue);
                    }
                }
                Elements obsgroup = doc.getElementsByTag("obsgroup");
                for (Element comp : obsgroup) {
                    String conceptId = comp.attr("conceptId");
                    conceptId = conceptId.replaceAll("\\s","");
                    if(!conceptId.equals("")) {
                        System.out.println("add conceptId "+conceptId);
                        inputList.add(conceptId);
                    }
                }

                //navigate page
                HtmlPage page = null;
                String url = "http://208.77.196.178:10080/openmrs/index.htm";
                WebClient webClient = new WebClient(BrowserVersion.CHROME);
                //webClient.setThrowExceptionOnScriptError(false);
                page = webClient.getPage( url );
                HtmlTextInput userInput = (HtmlTextInput) page.getElementById("username");
                userInput.setValueAttribute(username);
                HtmlPasswordInput passwordInput = (HtmlPasswordInput) page.getElementById("password");
                passwordInput.setValueAttribute(password);
                HtmlElement theElement2 = (HtmlElement) page.getForms().get(0).getInputByValue("Log In");
                theElement2.click();


                //create mapping from infromation from webpage
                List<String> outputList = new ArrayList<String>();
                for(int i = 0; i < inputList.size();i++) {
                    if((inputList.get(i).length()<36)) {
                        //check dictionary
                        HtmlPage infopage = webClient.getPage("http://208.77.196.178:10080/openmrs/dictionary/concept.htm?conceptId="+inputList.get(i));
                        String src = infopage.getWebResponse().getContentAsString();
                        Pattern p2 = Pattern.compile("UUID</th>\n\t\t\t<td>(.*?)</td>");
                        Matcher m2 = p2.matcher(src);
                        int count = 0;
                        while (m2.find()) {
                            outputList.add(m2.group(1));
                            count++;
                        }
                        if(count < 1){
                            throw new Exception("Couldn't find uuid in page for the #"+i+" consept with conceptid="+inputList.get(i));
                        }
                    }else {
                        outputList.add(inputList.get(i));
                    }
                }
                webClient.close();

                System.out.println(inputList.size()+" "+outputList.size());

                //create hashmap
                HashMap<String,String> hm = new HashMap<String,String>();
                for(int i = 0; i < inputList.size();i++) {
                    System.out.println(inputList.get(i)+" "+outputList.get(i));
                    hm.put(inputList.get(i),outputList.get(i));
                }
                System.out.println();

                //change elements from input to output values
                for (Element comp : obs) {
                    String conceptId = comp.attr("conceptId");
                    conceptId = conceptId.replaceAll("\\s","");
                    String outconceptId = hm.get(conceptId);
                    comp.attr("conceptId",outconceptId);

                    if(comp.hasAttr("answerConceptId")) {
                        String answerConceptId = comp.attr("answerConceptId");
                        answerConceptId = answerConceptId.replaceAll("\\s","");
                        String outanswerConceptId = hm.get(answerConceptId);
                        comp.attr("answerConceptId",outanswerConceptId);
                    }

                    if(comp.hasAttr("answerConceptIds")) {
                        String answerConceptIds = comp.attr("answerConceptIds");
                        answerConceptIds = answerConceptIds.replaceAll("\\s","");
                        String[] s = separateCommas(answerConceptIds);
                        String outanswerConceptIds = "";
                        for(int i = 0; i < s.length; i++) {
                            outanswerConceptIds += hm.get(s[i]);
                            if (i < s.length-1) {
                                outanswerConceptIds += ",";
                            }
                        }
                        comp.attr("answerConceptIds",outanswerConceptIds);
                    }
                    if(comp.hasAttr("defaultValue")) {
                        String defaultValue = comp.attr("defaultValue");
                        defaultValue = defaultValue.replaceAll("\\s","");
                        String outdefaultValue = hm.get(defaultValue);
                        comp.attr("defaultValue",outdefaultValue);
                    }
                }
                for (Element comp : obsgroup) {
                    if(comp.hasAttr("conceptId")) {
                        String conceptId = comp.attr("conceptId");
                        conceptId = conceptId.replaceAll("\\s", "");
                        String outconceptId = hm.get(conceptId);
                        try {
                            comp.attr("conceptId", outconceptId);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("error: " + conceptId + " " + outconceptId);
                        }
                    }
                }

                //output results
                PrintWriter writer = new PrintWriter(args[1], "UTF-8");
                writer.write(doc.html());
                writer.flush();
                writer.close();

            }catch(IOException e){
                e.printStackTrace();
                System.exit(0);
            }catch(Exception e){
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    public static String readFile(String path)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    public static String[] separateCommas(String list) {
        //separate commas
        List<String> inputList = new ArrayList<String>();
        if(list.contains(",")) {
            String[] temp = list.split(",");
            return temp;
        }else{
            String[] temp = new String[1];
            temp[0] = list;
            return temp;
        }

    }

}
