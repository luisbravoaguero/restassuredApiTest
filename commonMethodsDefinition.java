package definitions;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.*;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.response.ExtractableResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map;

import static net.serenitybdd.rest.SerenityRest.given;


public class commonsStep {
    Properties baseURLproperties = new Properties();
    Properties endPointproperties = new Properties();
    public static RequestSpecification request;


    public void loadProperties() {
        try ( FileInputStream file = new FileInputStream("src/test/resources/properties/config.properties")){
            baseURLproperties.load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadEndPoints(){
        try ( FileInputStream file = new FileInputStream("src/test/resources/properties/endpoint.properties")){
            endPointproperties.load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public RequestSpecification Request(String url){
        loadProperties();
        String baseURL = baseURLproperties.getProperty(url);
        if (baseURL == null) {
            throw new IllegalArgumentException("La URI base no puede ser nula");
        }
       request = given().log().all().baseUri(baseURL);
        return request;
    }

    public String getPath(String endpoint) {
        loadEndPoints();
        String basePath = endPointproperties.getProperty(endpoint);
        if (basePath == null) {
            throw new IllegalArgumentException("La ruta del endpoint no puede ser nula");
        }
        return basePath;
    }


    public String extractValueFromResponse(Response response, String jsonPath){
        ExtractableResponse<Response> extractableResponse = response.then().extract();
        return extractableResponse.path(jsonPath);
    }

    public String extractValueFromSpecificKeyResponse(Response response, String key){
	String jsonPath = findJsonPth(key);
	if(jsonPath == null){
	return jsonPath;
	}
        ExtractableResponse<Response> extractableResponse = response.then().extract();
        return extractableResponse.path(jsonPath);
    }


    public void updateJsonFile(String filePath, String objectKey, String keyToUpdate, String newValue) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File jsonFile = new File(filePath);
        JsonNode rootNode = objectMapper.readTree(jsonFile);

        JsonNode targetNode  = rootNode.path(objectKey);
        if(!targetNode.isMissingNode()){
            updateNode(targetNode, keyToUpdate, newValue);
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, rootNode);
    }

    public void updateNode(JsonNode node, String keyToUpdate, String newValue){
        if(node.isObject()){
            ObjectNode objectNode = (ObjectNode) node;
            if(objectNode.has(keyToUpdate)){
                objectNode.put(keyToUpdate,newValue);
            }
            Iterator<Map.Entry<String,JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()){
                Map.Entry<String,JsonNode> field = fields.next();
                updateNode(field.getValue(),keyToUpdate,newValue);
            }
        }else if(node.isArray()){
            for(JsonNode arrayNode: node){
                updateNode(arrayNode, keyToUpdate,newValue);
            }
        }
    }
    public String readJsonFile(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File jsonFile = new File(filePath);
        JsonNode jsonNode = objectMapper.readTree(jsonFile);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
    }

    public File extractObjectFromJsonFile(String sourceFilePath, String objectKey, String destinationFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File sourceFile = new File(sourceFilePath);
        JsonNode rootNode = objectMapper.readTree(sourceFile);
        JsonNode targetNode = rootNode.path(objectKey);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(destinationFilePath),targetNode);
        return  new File(destinationFilePath);
    }





    public String findJsonPth(JsonNode node, String key, String currentPath){
        if (node.isObject()){
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()){
                Map.Entry<String,JsonNode> field = fields.next();
                String newPath = currentPath.isEmpty() ? field.getKey() : currentPath + "." + field.getKey();
                if(field.getKey().equals(key)){
                    return newPath;
                }
                String result = findJsonPth(field.getValue(), key, newPath);
                if(result != null){
                    return result;
                }
            }
        }else if(node.isArray()){
            ArrayNode arrayNode = (ArrayNode) node;
            for(int i = 0; i < arrayNode.size(); i++){
                String newPath = currentPath + "["+i+"]";
                String result = findJsonPth(arrayNode.get(i), key, newPath);
                if(result != null){
                    return result;
                }
            }
        }
        return null;
    }
}