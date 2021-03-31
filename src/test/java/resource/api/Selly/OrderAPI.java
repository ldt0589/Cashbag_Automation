package resource.api.Selly;

import com.aventstack.extentreports.ExtentTest;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import groovy.json.JsonParser;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.json.Json;
import resource.common.GlobalVariables;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;


import static io.restassured.RestAssured.given;

public class OrderAPI extends CartAPI{

    private JSONArray cartArr = null;
    private JSONArray deliverySessionArr = null;
    private JSONArray deliverySessionArray = null;
    private JSONObject deliverySessionObject = null;
    private JSONObject itemsObject = null;
    private JSONArray sessionArray = null;
    private JSONObject jsonSession = null;
    private JSONParser jsonParser = new JSONParser();;
    public static ArrayList<String> sessionIDList = new ArrayList<String>();
    public static ArrayList<String> sessionOrderIDList = new ArrayList<String>();
    public static ArrayList<String> deliverySessionIDList = new ArrayList<String>();
    public static String sessionKey = null;
    private String checksum = "SmaAalemakwAskd";
    private String payloadString;

    private RequestSpecification createMultiSessionOrderSpecification(String userToken) {
        return given().
                baseUri("https://" + GlobalVariables.SellyEnvironment).
                header("Authorization", "Bearer " + userToken).
                header("Content-Type","application/json").
                contentType("application/json").
//                log().ifValidationFails().
                relaxedHTTPSValidation();
    }

    private RequestSpecification createMultiDeliverySessionSpecification(String userToken) {
        return given().
                baseUri("https://" + GlobalVariables.SellyEnvironment).
                header("Authorization", "Bearer " + userToken).
                header("Content-Type","application/json").
                contentType("application/json").
//                log().ifValidationFails().
                relaxedHTTPSValidation();
    }

    private RequestSpecification createMultiOrderSpecification(String userToken) {
        return given().
                baseUri("https://" + GlobalVariables.SellyEnvironment).
                header("Authorization", "Bearer " + userToken).
                header("Content-Type","application/json").
                contentType("application/json").
//                log().ifValidationFails().
                relaxedHTTPSValidation();
    }

    public static String readPayloadDataFromJsonFile(String filePath) throws IOException {
        String data = null;
        try {
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(new java.io.FileReader(filePath));
            data = obj.toString();

        } catch (Exception e) {
            log4j.error("readPayloadDataFromJsonFile method - ERROR - " + e);
        }
        return data;
    }

    public void createMultiOrder(ExtentTest logTest, String sellerToken) throws IOException {
        try {

            createMultiSessionOrder(logTest, sellerToken);
            deliverySessionArray = createMultiDeliverySession(logTest, sellerToken);

            JSONObject payloadObject = new JSONObject();
            payloadObject.put("checkSum", "SmaAalemakwAskd");
            JSONObject customerObject = new JSONObject();
            customerObject.put("id", "60581310fcfe4748d00691b0");
            customerObject.put("location", "60581310fcfe4748d00691af");
            payloadObject.put("customer", customerObject);
            payloadObject.put("paymentMethod", "COD");
            payloadObject.put("sessionKey", sessionKey);
            JSONArray sessionArray = new JSONArray();

            for(int i=0; i < deliverySessionArray.size(); i++){
                itemsObject = (JSONObject) deliverySessionArray.get(i);
                sessionOrderIDList.add((String) itemsObject.get("sessionOrderID"));

                JSONArray sessionDeliveries = (JSONArray) itemsObject.get("deliveries");
                JSONObject deliveryObject = (JSONObject) sessionDeliveries.get(0);
                deliverySessionIDList.add((String) deliveryObject.get("session"));

                JSONObject sessionItem = new JSONObject();
                sessionItem.put("delivery", deliverySessionIDList.get(i));
                sessionItem.put("order", sessionOrderIDList.get(i));
                sessionArray.add(sessionItem);
            }
            payloadObject.put("session", sessionArray);

            RequestSpecification createMultiOrderSpecification = this.createMultiOrderSpecification(sellerToken);
            createMultiOrderSpecification.body(payloadObject).log().all();

            Response response = createMultiOrderSpecification.post("/order/multiple");

            logInfo(logTest, "-----> createMultiOrder Request Body: " + payloadObject.toString());
            logInfo(logTest, "-----> createMultiOrder Response Body: " + response.getBody().asString());


        } catch (Exception e) {
            log4j.error("createMultiOrder method - ERROR: " + e);
            logException(logTest, "createMultiOrder method - ERROR: ", e);
        }
    }

    public JSONArray createMultiDeliverySession(ExtentTest logTest, String sellerToken) throws IOException {
        try {

            RequestSpecification createMultiDeliverySessionSpec = this.createMultiDeliverySessionSpecification(sellerToken);
//            String payload = String.format(readPayloadDataFromJsonFile(GlobalVariables.create_multi_delivery_payload), "SmaAalemakwAskd", sessionIDList,  "60581310fcfe4748d00691b0", "60581310fcfe4748d00691af");
//            createMultiDeliverySessionSpec.body(payload);

            JSONObject payloadObject = new JSONObject();
            payloadObject.put("checkSum", "SmaAalemakwAskd");
            JSONObject customerObject = new JSONObject();
            customerObject.put("id", "60581310fcfe4748d00691b0");
            customerObject.put("location", "60581310fcfe4748d00691af");
            payloadObject.put("customer", customerObject);
            payloadObject.put("sessionOrders", sessionIDList);

            createMultiDeliverySessionSpec.body(payloadObject).log().all();

            Response response = createMultiDeliverySessionSpec.post("/order/delivery/sessions-multiple");

            logInfo(logTest, "-----> createMultiDeliverySessionSpec Request Body: " + payloadObject.toString());
            logInfo(logTest, "-----> createMultiDeliverySessionSpec Response Body: " + response.getBody().asString());

            deliverySessionObject = (JSONObject) jsonParser.parse(response.body().asString());

            deliverySessionArr = (JSONArray) ((JSONObject) deliverySessionObject.get("data")).get("session");

            return deliverySessionArr;

        } catch (Exception e) {
            log4j.error("createMultiDeliverySession method - ERROR: " + e);
            logException(logTest, "createMultiDeliverySession method - ERROR: ", e);
        }
        return deliverySessionArr;
    }

    public void createMultiSessionOrder(ExtentTest logTest, String sellerToken) throws IOException {
        try {

            cartArr = getItemsInCart(logTest, sellerToken);

            ArrayList<String> itemIDList = new ArrayList<String>();
//            ArrayList<String> itemMarketPriceList = new ArrayList<String>();
//            ArrayList<String> itemCodeList = new ArrayList<String>();

            for(int i=0; i < cartArr.size(); i++){
                JSONObject itemsObject = (JSONObject) cartArr.get(i);
                JSONArray itemArr = (JSONArray) itemsObject.get("items");
                for(int y=0; y < itemArr.size(); y++){
                    JSONObject itemObject = (JSONObject) itemArr.get(y);
                    String itemID = (String) itemObject.get("_id");
                    itemIDList.add(itemID);
                }

            }
            logInfo(logTest, "-----> Item ID list " + itemIDList);

            RequestSpecification createMultiSessionOrderSpec = this.createMultiSessionOrderSpecification(sellerToken);
            createMultiSessionOrderSpec.body(new HashMap<String, Object>() {{
                put("items", itemIDList);
            }}).log().all();
            Response response = createMultiSessionOrderSpec.post("/order/sessions-multiple");

            logInfo(logTest, "-----> createMultiSessionOrder Response Body: " + response.getBody().asString());

            jsonSession = (JSONObject) jsonParser.parse(response.body().asString());
            sessionKey = (String) ((JSONObject) jsonSession.get("data")).get("key");
            sessionArray = (JSONArray) ((JSONObject) jsonSession.get("data")).get("sessions");

            for(int z=0; z < sessionArray.size(); z++) {
                JSONObject sessionsObject = (JSONObject) sessionArray.get(z);
                String sessionID = (String) sessionsObject.get("_id");
                sessionIDList.add(sessionID);
            }
            logInfo(logTest, "-----> sessionID List: " + sessionIDList);
            logInfo(logTest, "-----> session Key: " + sessionKey);

        } catch (Exception e) {
            log4j.error("createMultiSessionOrder method - ERROR: " + e);
            logException(logTest, "createMultiSessionOrder method - ERROR: ", e);
        }
    }

}