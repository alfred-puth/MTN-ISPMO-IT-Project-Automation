package za.co.mtn.ppm.bpm.ismpo.project;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Class to process all IS PMO IT Project related updates.
 */

public class IspmoItProjectProcessor {
    private static final int TEXT_AREA_HTML_MAX = 4000;

    /**
     * Method to check if a String is Blank or Null
     *
     * @param string String for verification
     * @return Boolean (True or False)
     */
    private static boolean isNotBlankString(String string) {
        return string != null && !string.isEmpty() && !string.trim().isEmpty() && !string.equalsIgnoreCase("null");
    }

    /**
     * Method to write out to the console or log file
     *
     * @param str String to print to console
     */
    private static void log(final String str) {
        System.out.println(str);
    }

    /**
     * Method to get the SQLRunner Results values in a json Array for further processing
     *
     * @param jsonResultsArray The json "results" array
     * @return json Array with only the values of the "values" Object key
     */
    private static JSONArray getJsonValuesArray(JSONArray jsonResultsArray) {
        JSONObject jsonValuesObj = new JSONObject();
        // Iterate through the jsonResultsArray and get the "values" key from the jsonObject
        for (Object obj : jsonResultsArray) {
            JSONObject jsonObject = (JSONObject) obj;
            // Extract the desired key and its related JSONArray
            JSONArray valuesArray = jsonObject.getJSONArray("values");
            // Assign the key-value pair to the jsonValuesObj
            jsonValuesObj.put("values", valuesArray);
        }
        // Assign the array from the values of the jsonValuesObj
        return jsonValuesObj.getJSONArray("values");
    }

    /**
     * Method to get the IT Project Data
     *
     * @param ppmBaseUrl           PPM Base URL for identifying the PPM environment
     * @param username             PPM User for access to the PPM entities.
     * @param password             PPM User password
     * @param restUrl              REST API URL for the method
     * @param requestId            IT Project ID
     * @param itProjectRequestType IT Project Request Type Name
     * @return HashMap with It Project Tokens and Values
     */
    protected HashMap<String, String> getItProjectData(String ppmBaseUrl, String username, String password,
                                                       String restUrl, String requestId, String itProjectRequestType) {
        // REST API URL
        String sqlUrl = ppmBaseUrl + restUrl;
        log("IT Project Data Method POST Request Run SQL Query URL: " + sqlUrl);
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the POST Request and all the parameters
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload;
        switch (itProjectRequestType) {
            case "IS PMO IT-EPMO Project":
                jsonPayload = setItEpmoProjectDataSql(requestId).toString();
                break;
            case "IS PMO IT-KTLO Project":
                jsonPayload = setItKtloProjectDataSql(requestId).toString();
                break;
            case "IS PMO IT-Reporting and Analytics Project":
                jsonPayload = setItReportingAnalyticsProjectDataSql(requestId).toString();
                break;
            case "IS PMO IT-Infrastructure Project":
                jsonPayload = setItInfrastructureProjectDataSql(requestId).toString();
                break;
            default:
                throw new IllegalArgumentException("Invalid request type name: " + itProjectRequestType);
        }
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(sqlUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .post(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = null;
        // Declare HashMap<String, String> result for the return result
        HashMap<String, String> result = new HashMap<>();
        try {
            response = call.execute();
            // Get the Response from server for the GET REST Request done.
            if (response.isSuccessful()) {
                // Set the JSONObject from the Response Body
                JSONObject jsonSqlObj;
                // Check Response Body is not Null
                if (response.body() != null) {
                    jsonSqlObj = (JSONObject) JSONSerializer.toJSON(response.body().string());
                    // close connection when done with assigning the response to the JSON Object
                    response.close();
                    log("Getting IT Project Data Method JSON SQL Return output: " + jsonSqlObj);
                    // Set the JSONArray with the "columnHeaders" token Array List
                    JSONArray jsonColumnHeadersArray = jsonSqlObj.getJSONArray("columnHeaders");
                    // Set the JSONArray with the "results" token Array List
                    JSONArray jsonResultsArray = jsonSqlObj.getJSONArray("results");
                    // Check that jsonResultsArray is not empty
                    if (!jsonResultsArray.isEmpty()) {
                        // Set the Object with "values" key and Array set from the "results" key Array
                        JSONArray jsonColumnValuesArray = getJsonValuesArray(jsonResultsArray);

                        // Add the jsonColumnHeaders as Keys and jsonValue as Values to the HashMap
                        // Array
                        for (int i = 0; i < jsonColumnHeadersArray.size(); i++) {
                            if (isNotBlankString(jsonColumnValuesArray.get(i).toString())) {
                                result.put(jsonColumnHeadersArray.getString(i), jsonColumnValuesArray.get(i).toString());
                            }
                        }
                    } else {
                        log("Getting IT Project Data Method Results key is Empty");
                    }
                } else {
                    log("Getting IT Project Data Method POST Return Body is Empty");
                }
            } else {
                // Exit the processing for non-200 status codes
                log("Getting IT Project Data Method Failed : HTTP error code : " + response.code());
            }
        } catch (IOException e) {
            log("Getting IT Project Data Method IO Exception Failure");
            throw new RuntimeException("Getting IT Project Data Method IO Exception Failure: " + e);
        } finally {
            if (response != null) {
                response.close();
                if (!response.isSuccessful()) {
                    System.exit(1);
                }
            }
        }
        // Return HashMap<String, String>
        return result;
    }

    /**
     * Method to get the IT Project Milestone information
     *
     * @param ppmBaseUrl PPM Base URL for identifying the PPM environment
     * @param username   PPM User for access to the PPM entities.
     * @param password   PPM User password
     * @param restUrl    REST API URL for the method
     * @param requestId  IT Project Request ID
     * @return ArrayList Object with IT Project Milestone data
     */
    public ArrayList<ProjectMilestoneValues> getItProjectMilestoneData(String ppmBaseUrl, String username, String password,
                                                                       String restUrl, String requestId) {
        // REST API URL
        String sqlUrl = ppmBaseUrl + restUrl;
        log("IT Project Milestone Data Method POST Request Run SQL Query URL: " + sqlUrl);
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the POST Request and all the parameters
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload = setItProjectMilestonesSql(requestId).toString();
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(sqlUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .post(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = null;
        // Declare HashMap<String, String> result for the return result
        ArrayList<ProjectMilestoneValues> result = new ArrayList<>();
        try {
            response = call.execute();
            // Get the Response from server for the GET REST Request done.
            if (response.isSuccessful()) {
                // Set the JSONObject from the Response Body
                JSONObject jsonSqlObj;
                // Check Response Body is not Null
                if (response.body() != null) {
                    jsonSqlObj = (JSONObject) JSONSerializer.toJSON(response.body().string());
                    // close connection when done with assigning the response to the JSON Object
                    response.close();
                    log("Getting IT Project Milestone Data Method JSON SQL Return output: " + jsonSqlObj.toString());
                    // Set the JSONArray with the "results" token Array List
                    JSONArray jsonResultsArray = jsonSqlObj.getJSONArray("results");
                    // Check that jsonResultsArray is not empty
                    if (!jsonResultsArray.isEmpty()) {
                        // Iterate through the jsonResultsArray and get the "values" key from the jsonObject
                        for (Object jsonResultObject : jsonResultsArray) {
                            JSONObject jsonValueObject = (JSONObject) jsonResultObject;
                            // Extract the desired key and its related JSONArray
                            JSONArray jsonValueArray = jsonValueObject.getJSONArray("values");
                            // Assign the values to the ProjectMilestoneValues class and add to return results Array
                            result.add(new ProjectMilestoneValues(jsonValueArray.get(0).toString(), jsonValueArray.get(1).toString(), jsonValueArray.get(2).toString(), jsonValueArray.get(3).toString()));
                        }
                    } else {
                        log("Getting IT Project Milestone Data Method Results key is Empty");
                    }
                } else {
                    log("Getting IT Project Milestone Data Method POST Return Body is Empty");
                }
            } else {
                // Exit the processing for non-200 status codes
                log("Getting IT Project Milestone Data Method Failed : HTTP error code : " + response.code());
            }
        } catch (IOException e) {
            log("Getting IT Project Milestone Data Method IO Exception Failure");
            throw new RuntimeException("Getting IT Project Milestone Data Method IO Exception Failure: " + e);
        } finally {
            if (response != null) {
                response.close();
                if (!response.isSuccessful()) {
                    System.exit(1);
                }
            }
        }
        // Return HashMap<String, String>
        return result;
    }

    /**
     * Method to get the All types of Feature Request Data linked to the IT Project
     *
     * @param ppmBaseUrl PPM Base URL for identifying the PPM environment
     * @param username   PPM User for access to the PPM entities.
     * @param password   PPM User password
     * @param restUrl    REST API URL for the method
     * @return ArrayList Object with IS PMO Features and IS PMO Testing Features
     */
    protected HashMap<String, HashMap<String, String>> getPpmFeatureRequestData(String ppmBaseUrl, String username, String password,
                                                                                String restUrl, String featureRequestType, String itProjectRequestId) {
        // REST API URL
        String sqlUrl = ppmBaseUrl + restUrl;
        log("PPM Feature Data Method POST Request Run SQL Query URL: " + sqlUrl);
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the POST Request and all the parameters
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload;
        switch (featureRequestType) {
            case "IS PMO Feature":
                jsonPayload = setIspmoFeatureRequestSql(itProjectRequestId).toString();
                break;
            case "IS PMO Testing Feature":
                jsonPayload = setIspmoTestingFeatureRequestSql(itProjectRequestId).toString();
                break;
            case "Octane Initiated Feature":
                jsonPayload = setOctaneInitiatedFeatureRequestSql(itProjectRequestId).toString();
                break;
            default:
                throw new IllegalArgumentException("Invalid request type name: " + featureRequestType);
        }
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(sqlUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .post(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = null;
        // Declare HashMap<String, String> result for the return result
        HashMap<String, HashMap<String, String>> result = new HashMap<>();
        try {
            response = call.execute();
            // Get the Response from server for the GET REST Request done.
            if (response.isSuccessful()) {
                // Set the JSONObject from the Response Body
                JSONObject jsonSqlObj;
                // Check Response Body is not Null
                if (response.body() != null) {
                    jsonSqlObj = (JSONObject) JSONSerializer.toJSON(response.body().string());
                    // close connection when done with assigning the response to the JSON Object
                    response.close();
                    log("Getting PPM Feature Data Method JSON SQL Return output: " + jsonSqlObj);
                    // Set the JSONArray with the "columnHeaders" token Array List
                    JSONArray jsonColumnHeadersArray = jsonSqlObj.getJSONArray("columnHeaders");
                    // Set the JSONArray with the "results" token Array List
                    JSONArray jsonResultsArray = jsonSqlObj.getJSONArray("results");
                    // Check that jsonResultsArray is not empty
                    HashMap<String, String> innerHashMap = new HashMap<>();
                    if (!jsonResultsArray.isEmpty()) {
                        // Iterate through the jsonResultsArray and get the "values" key from the jsonObject
                        for (Object jsonResultObject : jsonResultsArray) {
                            JSONObject jsonValueObject = (JSONObject) jsonResultObject;
                            // Extract the desired key and its related JSONArray
                            JSONArray jsonColumnValuesArray = jsonValueObject.getJSONArray("values");
                            // Assign Variable to store the "FEATURE_REQ_ID" for the outer HsahMap
                            String featureRequestId = null;
                            // Assign the values to the ProjectMilestoneValues class and add to return results Array
                            for (int i = 0; i < jsonColumnHeadersArray.size(); i++) {
                                innerHashMap.put(jsonColumnHeadersArray.getString(i), jsonColumnValuesArray.get(i).toString());
                                // Check Header Columns for "FEATURE_REQ_ID" and assign the Column value to featureRequestId variable
                                if (jsonColumnHeadersArray.getString(i).equalsIgnoreCase("FEATURE_REQ_ID")) {
                                    featureRequestId = jsonColumnValuesArray.get(i).toString();
                                }
                            }
                            result.put(featureRequestId, innerHashMap);
                        }
                    } else {
                        log("Getting PPM Feature Data Method Results key is Empty");
                    }
                } else {
                    log("Getting PPM Feature Method POST Return Body is Empty");
                }
            } else {
                // Exit the processing for non-200 status codes
                log("Getting PPM Feature Data Method Failed : HTTP error code : " + response.code());
            }
        } catch (IOException e) {
            log("Getting IPPM Feature Data Method IO Exception Failure");
            throw new RuntimeException("Getting PPM Feature Data Method IO Exception Failure: " + e);
        } finally {
            if (response != null) {
                response.close();
                if (!response.isSuccessful()) {
                    System.exit(1);
                }
            }
        }
        // Return HashMap<String, String>
        return result;
    }

    /**
     * Method to get all the PPM Feature that is linked to the IT Project
     *
     * @param ppmBaseUrl PPM Base URL for identifying the PPM environment
     * @param username   PPM User for access to the PPM entities.
     * @param password   PPM User password
     * @param restUrl    REST API URL for the method
     * @param requestId  IT Project Request ID
     * @return ArrayList Object with IT Project Milestone data
     */
    protected ArrayList<String> getFeatureIdsLinkedToItProject(String ppmBaseUrl, String username, String password, String restUrl, String requestId) {
        // REST API URL
        String sqlUrl = ppmBaseUrl + restUrl;
        log("Feature IDs linked to IT Project Method POST Request Run SQL Query URL: " + sqlUrl);
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the POST Request and all the parameters
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload = setAllFeatureIdsSql(requestId).toString();
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(sqlUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .post(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = null;
        // Declare HashMap<String, String> result for the return result
        ArrayList<String> result = new ArrayList<>();
        try {
            response = call.execute();
            // Get the Response from server for the GET REST Request done.
            if (response.isSuccessful()) {
                // Set the JSONObject from the Response Body
                JSONObject jsonSqlObj;
                // Check Response Body is not Null
                if (response.body() != null) {
                    jsonSqlObj = (JSONObject) JSONSerializer.toJSON(response.body().string());
                    // close connection when done with assigning the response to the JSON Object
                    response.close();
                    log("Getting Feature IDs linked to IT Project Method JSON SQL Return output: " + jsonSqlObj.toString());
                    // Set the JSONArray with the "results" token Array List
                    JSONArray jsonResultsArray = jsonSqlObj.getJSONArray("results");
                    // Check that jsonResultsArray is not empty
                    if (!jsonResultsArray.isEmpty()) {
                        // Iterate through the jsonResultsArray and get the "values" key from the jsonObject
                        for (Object jsonResultObject : jsonResultsArray) {
                            JSONObject jsonValueObject = (JSONObject) jsonResultObject;
                            // Extract the desired key and its related JSONArray
                            JSONArray jsonValueArray = jsonValueObject.getJSONArray("values");
                            // Assign the values to ArrayList<String>
                            result.add(jsonValueArray.get(0).toString());
                        }
                    } else {
                        log("Getting Feature IDs linked to IT Project Method Results key is Empty");
                    }
                } else {
                    log("Getting Feature IDs linked to IT Project Method POST Return Body is Empty");
                }
            } else {
                // Exit the processing for non-200 status codes
                log("Getting Feature IDs linked to IT Project Method Failed : HTTP error code : " + response.code());
            }
        } catch (IOException e) {
            log("Getting Feature IDs linked to IT Project Method IO Exception Failure");
            throw new RuntimeException("Getting Feature IDs linked to IT Project Method IO Exception Failure: " + e);
        } finally {
            if (response != null) {
                response.close();
                if (!response.isSuccessful()) {
                    System.exit(1);
                }
            }
        }
        // Return HashMap<String, String>
        return result;
    }

    /**
     * Method to set the SQL Statement JSON Object for IS PMO IT-EPMO Project data
     *
     * @param projectRequestId IT Project Request ID
     * @return JSON Object with the SQL String
     */
    private JSONObject setItEpmoProjectDataSql(String projectRequestId) {
        // Create the sql string
        String sql = "SELECT kfpp.request_id AS ispmo_prj_num, kfpp.project_name AS description, krd1.visible_parameter3 AS epmo_project_num, replace(kfpp.prj_project_manager_username, '#@#', '; ') AS ispmo_pm, initcap(ppr.overall_health_indicator) AS ispmo_prj_rag, kfpp.prj_business_unit_meaning AS ispm_epmo_business_unit, krhd.visible_parameter1 AS ispmo_epmo_sub_area, krhd.visible_parameter2 AS ispmo_epmo_bu_priority, krhd.visible_parameter3 AS ispmo_epmo_org_priority, kr.description AS ispmo_prj_short_desc, krd1.visible_parameter11 AS ispmo_incl_retail_build, krd1.visible_parameter12 AS ispmo_incl_charg_sys, krd1.visible_parameter13 AS ispmo_incl_wholsal_rel, krd1.visible_parameter14 AS ispmo_incl_siya_rel, krd1.visible_parameter15 AS ispmo_incl_ilula_rel, krd1.visible_parameter20 AS ispmo_incl_siebel_rel, krd3.visible_parameter16 AS ispmo_epmo_pm, krhd.visible_parameter25 AS ispmo_func_test_auto, krhd.visible_parameter26 AS ispmo_perf_test, krhd.visible_parameter27 AS ispmo_serv_virtual";
        sql = sql.concat(getItProjectFromWhereClauses(projectRequestId));
        // Create a JSON Object for the SQL Runner REST Request
        JSONObject jsonObj = new JSONObject();
        // Adding key-value pairs
        jsonObj.put("querySql", sql);
        return jsonObj;
    }

    /**
     * Method to set the SQL Statement JSON Object for IS PMO IT-KTLO Project data
     *
     * @param projectRequestId IT Project Request ID
     * @return JSON Object with the SQL String
     */
    private JSONObject setItKtloProjectDataSql(String projectRequestId) {
        // Create the sql string
        String sql = "SELECT kfpp.request_id AS ispmo_prj_num, kfpp.project_name AS description, replace(kfpp.prj_project_manager_username, '#@#', '; ') AS ispmo_pm, initcap(ppr.overall_health_indicator) AS ispmo_prj_rag, kfpp.prj_business_unit_meaning AS ispm_epmo_business_unit, krhd.visible_parameter1 AS ispmo_epmo_sub_area, kr.description AS ispmo_prj_short_desc, krd1.visible_parameter11 AS ispmo_incl_retail_build, krd1.visible_parameter12 AS ispmo_incl_charg_sys, krd1.visible_parameter13 AS ispmo_incl_wholsal_rel, krd1.visible_parameter14 AS ispmo_incl_siya_rel, krd1.visible_parameter15 AS ispmo_incl_ilula_rel, krd1.visible_parameter20 AS ispmo_incl_siebel_rel, krhd.visible_parameter25 AS ispmo_func_test_auto, krhd.visible_parameter26 AS ispmo_perf_test, krhd.visible_parameter27 AS ispmo_serv_virtual";
        sql = sql.concat(getItProjectFromWhereClauses(projectRequestId));
        // Create a JSON Object for the SQL Runner REST Request
        JSONObject jsonObj = new JSONObject();
        // Adding key-value pairs
        jsonObj.put("querySql", sql);
        return jsonObj;
    }

    /**
     * Method to set the SQL Statement JSON Object for IS PMO IT-Infrastructure Project data
     *
     * @param projectRequestId IT Project Request ID
     * @return JSON Object with the SQL String
     */
    private JSONObject setItInfrastructureProjectDataSql(String projectRequestId) {
        // Create the sql string
        String sql = "SELECT kfpp.request_id AS ispmo_prj_num, kfpp.project_name AS description, krd1.visible_parameter3 AS epmo_project_num, replace(kfpp.prj_project_manager_username, '#@#', '; ') AS ispmo_pm, initcap(ppr.overall_health_indicator) AS ispmo_prj_rag, kfpp.prj_business_unit_meaning AS ispm_epmo_busi krhd.visible_parameter1 AS ispmo_epmo_sub_area, kr.description AS ispmo_prj_short_desc, krhd.visible_parameter25 AS ispmo_func_test_auto, krhd.visible_parameter26 AS ispmo_perf_test, krhd.visible_parameter27 AS ispmo_serv_virtual";
        sql = sql.concat(getItProjectFromWhereClauses(projectRequestId));
        // Create a JSON Object for the SQL Runner REST Request
        JSONObject jsonObj = new JSONObject();
        // Adding key-value pairs
        jsonObj.put("querySql", sql);
        return jsonObj;
    }

    /**
     * Method to set the SQL Statement JSON Object for IS PMO IT-Reporting and Analytics Project data
     *
     * @param projectRequestId IT Project Request ID
     * @return JSON Object with the SQL String
     */
    private JSONObject setItReportingAnalyticsProjectDataSql(String projectRequestId) {
        // Create the sql string
        String sql = "SELECT kfpp.request_id AS ispmo_prj_num, kfpp.project_name AS description, replace(kfpp.prj_project_manager_username, '#@#', '; ') AS ispmo_pm, initcap(ppr.overall_health_indicator) AS ispmo_prj_rag, kfpp.prj_business_unit_meaning AS ispm_epmo_busi krhd.visible_parameter1 AS ispmo_epmo_sub_area, kr.description AS ispmo_prj_short_desc, krhd.visible_parameter25 AS ispmo_func_test_auto, krhd.visible_parameter26 AS ispmo_perf_test, krhd.visible_parameter27 AS ispmo_serv_virtual";
        sql = sql.concat(getItProjectFromWhereClauses(projectRequestId));
        // Create a JSON Object for the SQL Runner REST Request
        JSONObject jsonObj = new JSONObject();
        // Adding key-value pairs
        jsonObj.put("querySql", sql);
        return jsonObj;
    }

    /**
     * Method that set the IT Project SQL FROM and WHERE clauses
     *
     * @param reqId IT Prohect Request ID
     * @return String with FROM and WHERE clauses
     */
    private String getItProjectFromWhereClauses(String reqId) {
        return " FROM kcrt_fg_pfm_project kfpp"
                .concat(" INNER JOIN kcrt_request_types krt ON kfpp.request_type_id = krt.request_type_id")
                .concat(" INNER JOIN kcrt_requests kr ON kfpp.request_id = kr.request_id")
                .concat(" INNER JOIN kcrt_statuses ks ON kr.status_id = ks.status_id")
                .concat(" INNER JOIN kcrt_req_header_details krhd ON kr.request_id = krhd.request_id")
                .concat(" INNER JOIN kcrt_request_details krd1 ON kr.request_id = krd1.request_id AND krd1.batch_number = 1")
                .concat(" INNER JOIN kcrt_request_details krd3 ON kr.request_id = krd3.request_id AND krd3.batch_number = 3")
                .concat(" INNER JOIN pm_projects pp ON kr.request_id = pp.pfm_request_id")
                .concat(" INNER JOIN pm_project_rollup ppr ON pp.rollup_id = ppr.rollup_id")
                .concat(" WHERE kfpp.request_id = ").concat(reqId);
    }

    /**
     * Method to set the SQL Statement JSON Object for the IT Project Milestones data
     *
     * @param reqId IT Project Request ID (Project Number)
     * @return JSON Object with the SQL String
     */
    private JSONObject setItProjectMilestonesSql(String reqId) {
        // Create the sql String
        String sql = "SELECT wti.name, wts.sched_finish_date, wta.act_finish_date, ks.state_name";
        sql = sql.concat(" FROM pm_projects pp")
                .concat(" INNER JOIN pm_work_plans pwp ON pp.project_id = pwp.project_id")
                .concat(" INNER JOIN wp_tasks wt ON pwp.work_plan_id = wt.work_plan_id")
                .concat(" INNER JOIN wp_task_info wti ON wt.task_info_id = wti.task_info_id AND wti.task_type_code = 'M'")
                .concat(" INNER JOIN wp_task_schedule wts ON wt.task_schedule_id = wts.task_schedule_id")
                .concat(" INNER JOIN wp_task_actuals wta ON wt.task_actuals_id = wta.actuals_id")
                .concat(" INNER JOIN wp_milestones wm ON wt.milestone_id = wm.milestone_id AND wm.major = 'Y'")
                .concat(" INNER JOIN kdrv_states ks ON wti.status = ks.state_id");
        sql = sql.concat(" WHERE pwp.entity_type = 'WORK_PLAN'").concat(" AND pp.pfm_request_id = ").concat(reqId);
        sql = sql.concat(" ORDER BY wt.sequence_number ASC");
        // Create a JSON Object for the SQL Runner REST Request
        JSONObject jsonObj = new JSONObject();
        // Adding key-value pairs
        jsonObj.put("querySql", sql);
        return jsonObj;
    }

    /**
     * Method to set the SQL Statement JSON Object for IS PMO Features Request data
     *
     * @param reqId IT Project Request ID (Project Number)
     * @return JSON Object with the SQL String
     */
    protected JSONObject setIspmoFeatureRequestSql(String reqId) {
        // Create the sql string
        String sql = "SELECT kr.request_id AS feature_req_id, kr.description AS description, krd.visible_parameter15 AS ispmo_prj_rag, krd.visible_parameter5 AS ispmo_pm, krd.visible_parameter34 AS ispmo_prj_short_desc, krd.visible_parameter4 AS ispmo_epmo_pm, krd.visible_parameter8 AS ispmo_epmo_bu_priority, krd.visible_parameter9 AS ispmo_epmo_org_priority, krd.visible_parameter3 AS ispm_epmo_business_unit, krd.visible_parameter2 AS ispmo_epmo_sub_area, krd.visible_parameter16 AS ispmo_incl_retail_build, krd.visible_parameter17 AS ispmo_incl_charg_sys, krd.visible_parameter18 AS ispmo_incl_wholsal_rel, krd.visible_parameter19 AS ispmo_incl_siya_rel, krd.visible_parameter20 AS ispmo_incl_ilula_rel, krd.visible_parameter25 AS ispmo_incl_siebel_rel";
        sql = sql.concat(" FROM pm_projects pp")
                .concat(" INNER JOIN kcrt_fg_master_proj_ref kfpr ON pp.project_id = kfpr.ref_master_project_id")
                .concat(" INNER JOIN kcrt_request_types krt ON kfpr.request_type_id = krt.request_type_id AND krt.reference_code = 'IS_PMO_FEATURE'")
                .concat(" INNER JOIN kcrt_requests kr ON kfpr.request_id = kr.request_id")
                .concat(" INNER JOIN kcrt_request_details krd ON kr.request_id = krd.request_id AND krd.batch_number = 1")
                .concat(" INNER JOIN kcrt_fg_agile_info kfai ON kfpr.request_id = kfai.request_id");
        sql = sql.concat(" WHERE kr.status_code IN ( 'NEW', 'IN_PROGRESS' )")
                .concat(" AND pp.pfm_request_id = ").concat(reqId);
        sql = sql.concat(" ORDER BY kr.request_id ASC");
        // Create a JSON Object for the SQL Runner REST Request
        JSONObject jsonObj = new JSONObject();
        // Adding key-value pairs
        jsonObj.put("querySql", sql);
        return jsonObj;
    }

    /**
     * Method to set the SQL Statement JSON Object for IS PMO Testing Features Request data
     *
     * @param reqId IT Project Request ID (Project Number)
     * @return JSON Object with the SQL String
     */
    protected JSONObject setIspmoTestingFeatureRequestSql(String reqId) {
        // Create the sql string
        String sql = "SELECT kr.request_id AS feature_req_id, kr.description AS description, krd.visible_parameter7 AS ispmo_prj_rag, krd.visible_parameter6 AS ispmo_pm, krd.visible_parameter16 AS ispmo_prj_short_desc, krd.visible_parameter22 AS ispmo_epmo_pm, krd.visible_parameter12 AS ispmo_epmo_bu_priority, krd.visible_parameter13 AS ispmo_epmo_org_priority, krd.visible_parameter8 AS ispm_epmo_business_unit, krd.visible_parameter11 AS ispmo_epmo_sub_area, krd.visible_parameter26 AS ispmo_incl_retail_build, krd.visible_parameter27 AS ispmo_incl_charg_sys, krd.visible_parameter28 AS ispmo_incl_wholsal_rel, krd.visible_parameter29 AS ispmo_incl_siya_rel, krd.visible_parameter30 AS ispmo_incl_ilula_rel, krd.visible_parameter31 AS ispmo_incl_siebel_rel, krd.visible_parameter37 AS ispmo_func_test_auto, krd.visible_parameter38 AS ispmo_perf_test, krd.visible_parameter39 AS ispmo_serv_virtual";
        sql = sql.concat(" FROM pm_projects pp")
                .concat(" INNER JOIN kcrt_fg_master_proj_ref kfpr ON pp.project_id = kfpr.ref_master_project_id")
                .concat(" INNER JOIN kcrt_request_types krt ON kfpr.request_type_id = krt.request_type_id AND krt.reference_code = 'IS_PMO_TESTING_FEATURE'")
                .concat(" INNER JOIN kcrt_requests kr ON kfpr.request_id = kr.request_id")
                .concat(" INNER JOIN kcrt_request_details krd ON kr.request_id = krd.request_id AND krd.batch_number = 1")
                .concat(" INNER JOIN kcrt_fg_agile_info kfai ON kfpr.request_id = kfai.request_id");
        sql = sql.concat(" WHERE kr.status_code IN ( 'NEW', 'IN_PROGRESS' )")
                .concat(" AND pp.pfm_request_id = ").concat(reqId);
        sql = sql.concat(" ORDER BY kr.request_id ASC");
        // Create a JSON Object for the SQL Runner REST Request
        JSONObject jsonObj = new JSONObject();
        // Adding key-value pairs
        jsonObj.put("querySql", sql);
        return jsonObj;
    }

    /**
     * Method to set the SQL Statement JSON Object for Octane Initiated Features Request data
     *
     * @param reqId IT Project Request ID (Project Number)
     * @return JSON Object with the SQL String
     */
    protected JSONObject setOctaneInitiatedFeatureRequestSql(String reqId) {
        // Create the sql string
        String sql = "SELECT kr.request_id AS feature_req_id, kr.description AS description, krd.visible_parameter15 AS ispmo_prj_rag, krd.visible_parameter5 AS ispmo_pm, krd.visible_parameter34 AS ispmo_prj_short_desc, krd.visible_parameter4 AS ispmo_epmo_pm, krd.visible_parameter8 AS ispmo_epmo_bu_priority, krd.visible_parameter9 AS ispmo_epmo_org_priority, krd.visible_parameter3 AS ispm_epmo_business_unit, krd.visible_parameter2 AS ispmo_epmo_sub_area, krd.visible_parameter16 AS ispmo_incl_retail_build, krd.visible_parameter17 AS ispmo_incl_charg_sys, krd.visible_parameter18 AS ispmo_incl_wholsal_rel, krd.visible_parameter19 AS ispmo_incl_siya_rel, krd.visible_parameter20 AS ispmo_incl_ilula_rel, krd.visible_parameter25 AS ispmo_incl_siebel_rel";
        sql = sql.concat(" FROM kcrt_fg_agile_info kfai")
                .concat(" INNER JOIN kcrt_request_types krt ON kfai.request_type_id = krt.request_type_id AND krt.reference_code = 'OCTANE_INITIATED_FEATURE'")
                .concat(" INNER JOIN kcrt_requests kr ON kfai.request_id = kr.request_id")
                .concat(" INNER JOIN kcrt_req_header_details krhd ON kr.request_id = krhd.request_id")
                .concat(" INNER JOIN kcrt_request_details krd ON krhd.request_id = krd.request_id AND krd.batch_number = 1")
                .concat(" INNER JOIN pm_projects pp ON krd.visible_parameter11 = pp.pfm_request_id");
        sql = sql.concat(" WHERE kr.status_code IN ( 'NEW', 'IN_PROGRESS' )")
                .concat(" AND upper(krhd.visible_parameter4) IN ( upper('Functional'), upper('Project Initiated (PPM)'), upper('Testing Feature') )")
                .concat(" AND pp.pfm_request_id = ").concat(reqId);
        sql = sql.concat(" ORDER BY kr.request_id ASC");
        // Create a JSON Object for the SQL Runner REST Request
        JSONObject jsonObj = new JSONObject();
        // Adding key-value pairs
        jsonObj.put("querySql", sql);
        return jsonObj;
    }

    /**
     * Method to set the SQL string to be used for extracting all the PPM Features linked to the IT Project
     *
     * @param reqId IT Project Request ID
     * @return JSON Object with the created SQL statement
     */
    private JSONObject setAllFeatureIdsSql(String reqId) {
        // Create the sql string starting with IS PMO Feature and IS PMO Testing Feature Request IDs
        String sql = "SELECT kfai.request_id";
        sql = sql.concat(" FROM pm_projects pp")
                .concat(" INNER JOIN kcrt_fg_master_proj_ref kfpr ON pp.project_id = kfpr.ref_master_project_id")
                .concat(" INNER JOIN kcrt_request_types krt ON kfpr.request_type_id = krt.request_type_id AND krt.reference_code IN ( 'IS_PMO_FEATURE', 'IS_PMO_TESTING_FEATURE' )")
                .concat(" INNER JOIN kcrt_fg_agile_info kfai ON kfpr.request_id = kfai.request_id")
                .concat(" INNER JOIN kcrt_requests kr ON kfai.request_id = kr.request_id")
                .concat(" WHERE kr.status_code IN ( 'NEW', 'IN_PROGRESS' )")
                .concat(" AND pp.pfm_request_id = ")
                .concat(reqId);
        // Add the UNION to SQL Query
        sql = sql.concat(" UNION ALL");
        // Add the Octane Initiated SQL Query
        sql = sql.concat(" SELECT kr.request_id")
                .concat(" FROM kcrt_fg_agile_info kfai")
                .concat(" INNER JOIN kcrt_request_types krt ON kfai.request_type_id = krt.request_type_id AND krt.reference_code = 'OCTANE_INITIATED_FEATURE'")
                .concat(" INNER JOIN kcrt_requests kr ON kfai.request_id = kr.request_id")
                .concat(" INNER JOIN kcrt_req_header_details krhd ON kr.request_id = krhd.request_id")
                .concat(" INNER JOIN kcrt_request_details krd ON krhd.request_id = krd.request_id AND krd.batch_number = 1")
                .concat(" INNER JOIN pm_projects pp ON krd.visible_parameter11 = pp.pfm_request_id")
                .concat(" WHERE kr.status_code IN ( 'NEW', 'IN_PROGRESS' )")
                .concat(" AND upper(krhd.visible_parameter4) IN ( upper('Functional'), upper('Project Initiated (PPM)'), upper('Testing Feature') )")
                .concat(" AND pp.pfm_request_id = ")
                .concat(reqId);
        // Ass ORDER BY clause to SQL Query
        sql = sql.concat(" ORDER BY 1 ASC");
        // Create a JSON Object for the SQL Runner REST Request
        JSONObject jsonObj = new JSONObject();
        // Adding key-value pairs
        jsonObj.put("querySql", sql);
        return jsonObj;
    }

    /**
     * Method to update the Octane Initiated Feature Request Fields from the IT Project
     * Excluding the IT Project Milestones for both request types
     *
     * @param ppmBaseUrl                     PPM Base URL for identifying the PPM environment
     * @param username                       PPM User for access to the PPM entities.
     * @param password                       PPM User password
     * @param restUrl                        REST API URL for the method
     * @param featureReqId                   IS PMO Feature or IS PMO Testing Feature Request Id
     * @param projectMilestoneValuesObjArray IT Project Milestones Array
     * @param projectFieldsObj               IT Project Fields Object data
     * @param ppmFeatureFieldsObj            PPM Feature Field Object data
     * @param projectRequestType             IT Project Request Type Name
     */
    protected void updateFeatureRequestFields(String ppmBaseUrl, String username, String password, String restUrl, String featureReqId, ArrayList<ProjectMilestoneValues> projectMilestoneValuesObjArray, HashMap<String, String> projectFieldsObj, HashMap<String, String> ppmFeatureFieldsObj, String projectRequestType) {
        // REST API URL
        String requestUrl = ppmBaseUrl + restUrl + "/" + featureReqId;
        log("<p stryle=\"margin-left:1px\">");
        log("PUT Feature Request Fields Update RT URL: " + requestUrl);
        log("</p><br>");
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the PUT Request with all the required parameters and timeouts
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload = setJsonObjectUpdateFeatureRequestTypeFields(projectMilestoneValuesObjArray, projectFieldsObj, ppmFeatureFieldsObj, projectRequestType).toString();
        log("<p stryle=\"margin-left:1px\">");
        log("Created PPM Feature Pay Load: " + jsonPayload);
        log("<hr></p><br>");
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(requestUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .put(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = null;
        try {
            response = call.execute();
            // Get the Response from server for the GET REST Request done.
            if (response.isSuccessful()) {
                // Set the JSONObject from the Response Body
                JSONObject jsonPutRequestObj;
                // Check Response Body is not Null
                if (response.body() != null) {
                    jsonPutRequestObj = (JSONObject) JSONSerializer.toJSON(response.body().string());
                    // close connection when done with assigning the response to the JSON Object
                    response.close();
                    log("<p stryle=\"margin-left:1px\">");
                    log("Successful PUT response output Updating RT: " + jsonPutRequestObj);
                    log("<hr></p><br>");
                }
            }
        } catch (IOException e) {
            log("Getting PPM Feature Request Update Method IO Exception Failure");
            throw new RuntimeException(e);
        } finally {
            if (response != null) {
                response.close();
                if (!response.isSuccessful()) {
                    System.exit(1);
                }
            }
        }

    }

    /**
     * Method to update the Octane Initiated Feature Request Fields from the IT Project
     * Excluding the IT Project Milestones for both request types
     *
     * @param ppmBaseUrl          PPM Base URL for identifying the PPM environment
     * @param username            PPM User for access to the PPM entities.
     * @param password            PPM User password
     * @param restUrl             REST API URL for the method
     * @param featureReqId        IS PMO Feature or IS PMO Testing Feature Request Id
     * @param projectFieldsObj    IT Project Fields Object data
     * @param ppmFeatureFieldsObj PPM Feature Field Object data
     * @param projectRequestType  IT Project Request Type Name
     */
    protected void updateFeatureRequestFields(String ppmBaseUrl, String username, String password, String restUrl, String featureReqId, HashMap<String, String> projectFieldsObj, HashMap<String, String> ppmFeatureFieldsObj, String projectRequestType) {
        // REST API URL
        String requestUrl = ppmBaseUrl + restUrl + "/" + featureReqId;
        log("<p stryle=\"margin-left:1px\">");
        log("PUT Feature Request Fields Update RT URL: " + requestUrl);
        log("</p><br>");
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the PUT Request with all the required parameters and timeouts
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload = setJsonObjectUpdateFeatureRequestTypeFields(projectFieldsObj, ppmFeatureFieldsObj, projectRequestType).toString();
        log("<p stryle=\"margin-left:1px\">");
        log("Created PPM Feature Pay Load: " + jsonPayload);
        log("<hr></p><br>");
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(requestUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .put(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = null;
        try {
            response = call.execute();
            // Get the Response from server for the GET REST Request done.
            if (response.isSuccessful()) {
                // Set the JSONObject from the Response Body
                JSONObject jsonPutRequestObj;
                // Check Response Body is not Null
                if (response.body() != null) {
                    jsonPutRequestObj = (JSONObject) JSONSerializer.toJSON(response.body().string());
                    // close connection when done with assigning the response to the JSON Object
                    response.close();
                    log("<p stryle=\"margin-left:1px\">");
                    log("Successful PUT response output Updating RT: " + jsonPutRequestObj);
                    log("<hr></p><br>");
                }
            }
        } catch (IOException e) {
            log("Getting PPM Feature Request Update Method IO Exception Failure");
            throw new RuntimeException(e);
        } finally {
            if (response != null) {
                response.close();
                if (!response.isSuccessful()) {
                    System.exit(1);
                }
            }
        }

    }

    /**
     * Method to update the IS PMO Feature, IS PMO Testing Feature and Octane Initiated Feature Request Type Fields from the IT Project:
     * - IT Project Status
     * - IT Project Phase
     *
     * @param ppmBaseUrl      PPM Base URL for identifying the PPM environment
     * @param username        PPM User for access to the PPM entities.
     * @param password        PPM User password
     * @param restUrl         REST API URL for the method
     * @param featureReqId    IS PMO Feature or IS PMO Testing Feature Request Id
     * @param itProjectStatus IT Project Status
     * @param itProjectPhase  IT Project Phase
     */
    protected void updateFeatureRequestStatusPhaseFields(String ppmBaseUrl, String username, String password, String restUrl, String featureReqId, String itProjectStatus, String itProjectPhase) {
        // REST API URL
        String requestUrl = ppmBaseUrl + restUrl + "/" + featureReqId;
        log("<p stryle=\"margin-left:1px\">");
        log("PUT Feature Request Status and Phase Fields Update RT URL: " + requestUrl);
        log("</p><br>");
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the PUT Request with all the required parameters and timeouts
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload = setJsonObjectUpdateFeatureProjectStatusPhaseFields(itProjectStatus, itProjectPhase).toString();
        log("<p stryle=\"margin-left:1px\">");
        log("Created Feature Request Status and Phase Fields Pay Load: " + jsonPayload);
        log("<hr></p><br>");
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(requestUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .put(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = null;
        try {
            response = call.execute();
            // Get the Response from server for the GET REST Request done.
            if (response.isSuccessful()) {
                // Set the JSONObject from the Response Body
                JSONObject jsonPutRequestObj;
                // Check Response Body is not Null
                if (response.body() != null) {
                    jsonPutRequestObj = (JSONObject) JSONSerializer.toJSON(response.body().string());
                    // close connection when done with assigning the response to the JSON Object
                    response.close();
                    log("<p stryle=\"margin-left:1px\">");
                    log("Successful PUT Feature Request Status and Phase Fields response output Updating RT: " + jsonPutRequestObj);
                    log("<hr></p><br>");
                }
            }
        } catch (IOException e) {
            log("Getting Feature Request Status and Phase Fields Method IO Exception Failure");
            throw new RuntimeException(e);
        } finally {
            if (response != null) {
                response.close();
                if (!response.isSuccessful()) {
                    System.exit(1);
                }
            }
        }

    }

    /**
     * Method to populate the JSON Object with the Payload for updating IS PMO PPM Features and IS PMO Testing PPM Features
     * Including IT Project Milestone Information
     *
     * @param itProjectMilestoneObjArray IT Project Milestone Object Array
     * @param itProjectFieldsObj         IT Project Field Data Object
     * @param featureFieldsObj           PPM Feature Field Data Object
     * @param itProjectRequestType       IT Project Request Type
     * @return Json Object with the payload
     */
    protected JSONObject setJsonObjectUpdateFeatureRequestTypeFields(ArrayList<ProjectMilestoneValues> itProjectMilestoneObjArray, HashMap<String, String> itProjectFieldsObj, HashMap<String, String> featureFieldsObj, String itProjectRequestType) {
        // Set the Token Prefix variables (RT Header or RT Details)
        final String headerFieldPrefix = "REQ.";
        final String detailsFieldPrefix = "REQD.";
        // Get the current date and time in "yyyy-MM-dd'T'HH:mm:ss" format" No need to
        // include the micro seconds and timezone
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        // Date of processing
        Date date = new Date();
        // Start with the required fields to create the IS POMO Feature
        // RT Token: LAST_UPDATE_DATE
        JSONObject tokensLastUpdateDateObj = new JSONObject();
        tokensLastUpdateDateObj.put("token", "REQ.LAST_UPDATE_DATE");
        tokensLastUpdateDateObj.put("dateValue", formatter.format(date));
        // RT Token: ENTITY_LAST_UPDATE_DATE
        JSONObject tokenEntityLastUpdateDateObj = new JSONObject();
        tokenEntityLastUpdateDateObj.put("token", "REQ.ENTITY_LAST_UPDATE_DATE");
        tokenEntityLastUpdateDateObj.put("dateValue", formatter.format(date));
        // Set the Field Array for the IS PMO Feature Request dynamically depending on the data
        JSONArray fieldArray = new JSONArray();
        fieldArray.add(tokensLastUpdateDateObj);
        fieldArray.add(tokenEntityLastUpdateDateObj);
        // Set the HTML Field Array for the IT Project Milestones
        fieldArray.add(setProjectMilestoneHtmlJson(itProjectMilestoneObjArray, detailsFieldPrefix, "ISPMO_MILESTONES"));
        // Iterate through the PPM Feature Request Field Tokens
        for (Map.Entry<String, String> featureFieldSet : featureFieldsObj.entrySet()) {
            // Check if IT Project Key(token) exist for the relevant PPM Feature Key (token)
            if (itProjectFieldsObj.containsKey(featureFieldSet.getKey())) {
                // Set the Feature Key and Value Variables
                final String featureKey = featureFieldSet.getKey();
                final String featureFieldValue = featureFieldSet.getValue();
                // PPM Feature Description Field update
                if (featureKey.equalsIgnoreCase("DESCRIPTION")) {
                    // JSON Object Variable for the Feature description
                    JSONObject descriptionFieldObject;
                    // Check if the IT Project Request type is equal to "IS PMO IT-EPMO Project"
                    if (itProjectRequestType.equalsIgnoreCase("IS PMO IT-EPMO Project")) {
                        // JSON Object Variable for the Feature description
                        descriptionFieldObject = setFeatureDescription(headerFieldPrefix, "DESCRIPTION", itProjectFieldsObj.get("ISPMO_PRJ_NUM"), itProjectFieldsObj.get("DESCRIPTION"), itProjectFieldsObj.get("EPMO_PROJECT_NUM"));
                    } else {
                        descriptionFieldObject = setFeatureDescription(headerFieldPrefix, "DESCRIPTION", itProjectFieldsObj.get("ISPMO_PRJ_NUM"), itProjectFieldsObj.get("DESCRIPTION"));
                    }
                    // check if Feature description a derived Feature description is different
                    if (!featureFieldValue.equalsIgnoreCase(descriptionFieldObject.getString(headerFieldPrefix + featureKey))) {
                        // Set the fiedArray for the Feature Description
                        fieldArray.add(descriptionFieldObject);
                    }
                } else {
                    /* All Other Feature Field updates
                     * Set the IT Project Field Value Variable */
                    final String projectFieldValue = itProjectFieldsObj.get(featureKey);
                    // Check if the Feature Value is blank/null
                    if (isNotBlankString(featureFieldValue)) {
                        // Not Blank/Null then Compare the Feature Value with the IT Project Value
                        if (!featureFieldValue.equalsIgnoreCase(projectFieldValue)) {
                            // Process the Feature Value with the IT Value when Feature value is Not Blank/Null
                            fieldArray.add(setRequestFieldJsonObj(detailsFieldPrefix + featureKey, projectFieldValue));
                        }
                    } else {
                        // Process the Feature Value with the IT Value when Feature value is Blank/Null
                        fieldArray.add(setRequestFieldJsonObj(detailsFieldPrefix + featureKey, projectFieldValue));
                    }
                }
            }
        }
        // Set the Field Object
        JSONObject fieldObj = new JSONObject();
        fieldObj.put("field", fieldArray);
        // Full JSON Object - Top Level
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("fields", fieldObj);
        return jsonObj;
    }

    /**
     * Method to populate the JSON Object with the Payload for updating Octane Initiated PPM Features
     * Excluding IT Project Milestone Information
     *
     * @param itProjectFieldsObj   IT Project Field Data Object
     * @param featureFieldsObj     PPM Feature Field Data Object
     * @param itProjectRequestType IT Project Request Type
     * @return Json Object with the payload
     */
    protected JSONObject setJsonObjectUpdateFeatureRequestTypeFields(HashMap<String, String> itProjectFieldsObj, HashMap<String, String> featureFieldsObj, String itProjectRequestType) {
        // Set the Token Prefix variables (RT Header or RT Details)
        final String headerFieldPrefix = "REQ.";
        final String detailsFieldPrefix = "REQD.";
        // Get the current date and time in "yyyy-MM-dd'T'HH:mm:ss" format" No need to
        // include the micro seconds and timezone
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        // Date of processing
        Date date = new Date();
        // Start with the required fields to create the IS POMO Feature
        // RT Token: LAST_UPDATE_DATE
        JSONObject tokensLastUpdateDateObj = new JSONObject();
        tokensLastUpdateDateObj.put("token", "REQ.LAST_UPDATE_DATE");
        tokensLastUpdateDateObj.put("dateValue", formatter.format(date));
        // RT Token: ENTITY_LAST_UPDATE_DATE
        JSONObject tokenEntityLastUpdateDateObj = new JSONObject();
        tokenEntityLastUpdateDateObj.put("token", "REQ.ENTITY_LAST_UPDATE_DATE");
        tokenEntityLastUpdateDateObj.put("dateValue", formatter.format(date));
        // Set the Field Array for the IS PMO Feature Request dynamically depending on the data
        JSONArray fieldArray = new JSONArray();
        fieldArray.add(tokensLastUpdateDateObj);
        fieldArray.add(tokenEntityLastUpdateDateObj);
        // Iterate through the PPM Feature Request Field Tokens
        for (Map.Entry<String, String> featureFieldSet : featureFieldsObj.entrySet()) {
            // Check if IT Project Key(token) exist for the relevant PPM Feature Key (token)
            if (itProjectFieldsObj.containsKey(featureFieldSet.getKey())) {
                // Set the Feature Key and Value Variables
                final String featureKey = featureFieldSet.getKey();
                final String featureFieldValue = featureFieldSet.getValue();
                // PPM Feature Description Field update
                if (featureKey.equalsIgnoreCase("DESCRIPTION")) {
                    // JSON Object Variable for the Feature description
                    JSONObject descriptionFieldObject;
                    // Check if the IT Project Request type is equal to "IS PMO IT-EPMO Project"
                    if (itProjectRequestType.equalsIgnoreCase("IS PMO IT-EPMO Project")) {
                        // JSON Object Variable for the Feature description
                        descriptionFieldObject = setFeatureDescription(headerFieldPrefix, "DESCRIPTION", itProjectFieldsObj.get("ISPMO_PRJ_NUM"), itProjectFieldsObj.get("DESCRIPTION"), itProjectFieldsObj.get("EPMO_PROJECT_NUM"));
                    } else {
                        descriptionFieldObject = setFeatureDescription(headerFieldPrefix, "DESCRIPTION", itProjectFieldsObj.get("ISPMO_PRJ_NUM"), itProjectFieldsObj.get("DESCRIPTION"));
                    }
                    // check if Feature description a derived Feature description is different
                    if (!featureFieldValue.equalsIgnoreCase(descriptionFieldObject.getString(headerFieldPrefix + featureKey))) {
                        // Set the fiedArray for the Feature Description
                        fieldArray.add(descriptionFieldObject);
                    }
                } else {
                    /* All Other Feature Field updates
                     * Set the IT Project Field Value Variable */
                    final String projectFieldValue = itProjectFieldsObj.get(featureKey);
                    // Check if the Feature Value is blank/null
                    if (isNotBlankString(featureFieldValue)) {
                        // Not Blank/Null then Compare the Feature Value with the IT Project Value
                        if (!featureFieldValue.equalsIgnoreCase(projectFieldValue)) {
                            // Process the Feature Value with the IT Value when Feature value is Not Blank/Null
                            fieldArray.add(setRequestFieldJsonObj(detailsFieldPrefix + featureKey, projectFieldValue));
                        }
                    } else {
                        // Process the Feature Value with the IT Value when Feature value is Blank/Null
                        fieldArray.add(setRequestFieldJsonObj(detailsFieldPrefix + featureKey, projectFieldValue));
                    }
                }
            }
        }
        // Set the Field Object
        JSONObject fieldObj = new JSONObject();
        fieldObj.put("field", fieldArray);
        // Full JSON Object - Top Level
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("fields", fieldObj);
        return jsonObj;
    }

    /**
     * Method to create the JSON Object to update the IS PMO Feature, IS PMO Testing Feature and Octane Initiated Feature Request
     * Type Fields with the IT Project Status and IT Project Phase
     *
     * @param itProjectStatus IT Project Status
     * @param itProjectPhase  IT Project Phase
     * @return JSONObject with the JSON Payload
     */
    protected JSONObject setJsonObjectUpdateFeatureProjectStatusPhaseFields(String itProjectStatus, String itProjectPhase) {
        // Set the Token Prefix variables (RT Header or RT Details)
        final String detailsFieldPrefix = "REQD.";
        // Get the current date and time in "yyyy-MM-dd'T'HH:mm:ss" format" No need to
        // include the micro seconds and timezone
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        // Date of processing
        Date date = new Date();
        // Start with the required fields to create the IS POMO Feature
        // RT Token: LAST_UPDATE_DATE
        JSONObject tokensLastUpdateDateObj = new JSONObject();
        tokensLastUpdateDateObj.put("token", "REQ.LAST_UPDATE_DATE");
        tokensLastUpdateDateObj.put("dateValue", formatter.format(date));
        // RT Token: ENTITY_LAST_UPDATE_DATE
        JSONObject tokenEntityLastUpdateDateObj = new JSONObject();
        tokenEntityLastUpdateDateObj.put("token", "REQ.ENTITY_LAST_UPDATE_DATE");
        tokenEntityLastUpdateDateObj.put("dateValue", formatter.format(date));
        // Set the Field Array for the IS PMO Feature Request dynamically depending on the data
        JSONArray fieldArray = new JSONArray();
        fieldArray.add(tokensLastUpdateDateObj);
        fieldArray.add(tokenEntityLastUpdateDateObj);
        // Add ISPMO_PRJ_STATUS tohen and string value
        setRequestFieldJsonObj(detailsFieldPrefix + "ISPMO_PRJ_STATUS", itProjectStatus);
        // Add ISPMO_PRJ_PHASE tohen and string value
        setRequestFieldJsonObj(detailsFieldPrefix + "ISPMO_PRJ_PHASE", itProjectPhase);
        // Set the Field Object
        JSONObject fieldObj = new JSONObject();
        fieldObj.put("field", fieldArray);
        // Full JSON Object - Top Level
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("fields", fieldObj);
        return jsonObj;
    }

    /**
     * Method to set the JSON Request Field Object for String Values
     *
     * @param strToken Token of the Request Field
     * @param strValue String Value Array
     * @return the JSONObject for the Request Field Object
     */
    public JSONObject setRequestFieldJsonObj(String strToken, String strValue) {
        // Declare the Request Field Object
        JSONObject requestFieldObj = new JSONObject();
        requestFieldObj.put("token", strToken);
        // Declare and Instantiate the strValue Array
        JSONArray stringValueArray = new JSONArray();
        stringValueArray.add(strValue);
        requestFieldObj.put("stringValue", stringValueArray);
        return requestFieldObj;
    }

    /**
     * Method to set the HTML for the IT Project Milestones
     *
     * @param projectMilestoneObj IT Project Major Milestone value array list
     * @param prefix              PPM Request Field Token Prefix
     * @param fieldToken          PPM Request Field Token
     * @return JSONObject with the HTML for the IT Project Milestone table
     */
    public JSONObject setProjectMilestoneHtmlJson(ArrayList<ProjectMilestoneValues> projectMilestoneObj, String prefix, String fieldToken) {
        // Set the HTML String
        String result;
        // Result String Length indicator
        int stringLength = 0;
        // Final Table string
        final String endHtmlTable = "</table>";
        // Set Json Object Vairable
        JSONObject milestoneJsonObject = new JSONObject();
        // Build the string with HTML tags for a table
        if (!projectMilestoneObj.isEmpty()) {
            // Header Table string
            String headerHtmlTable;
            // Add header of the html table
            headerHtmlTable = "<table style=\"border: 1px solid black; border-collapse: collapse; width: 98%;\">";
            headerHtmlTable = headerHtmlTable.concat("<tr>");
            headerHtmlTable = headerHtmlTable.concat("<td style=\"font-weight: bold; width: 40%;\">Milestone</td>");
            headerHtmlTable = headerHtmlTable.concat("<td style=\"font-weight: bold; width: 20%;\">Scheduled Finish</td>");
            headerHtmlTable = headerHtmlTable.concat("<td style=\"font-weight: bold; width: 20%;\">Actual Finish</td>");
            headerHtmlTable = headerHtmlTable.concat("<td style=\"font-weight: bold;\">Status</td>");
            headerHtmlTable = headerHtmlTable.concat("</tr>");
            // Assign the Header Table string to the Return Result string
            result = headerHtmlTable;
            // Set the Result String Length indicator after HTML Header was created
            stringLength = stringLength + headerHtmlTable.length();
            // Iterate through the Domain Involvement Object
            for (ProjectMilestoneValues projectMilestoneValues : projectMilestoneObj) {
                // Inner Table string
                String innerHtmlTable;
                // Create the Row in the table body
                innerHtmlTable = "<tr>";
                // Milestone
                innerHtmlTable = innerHtmlTable.concat("<td>").concat(projectMilestoneValues.getMilestoneTaskName()).concat("</td>");
                // Scheduled Finish
                String milestoneScheduledFinishDate = projectMilestoneValues.getMilestoneScheduledFinishDate();
                if (isNotBlankString(milestoneScheduledFinishDate)) {
                    // Format the Date String to "dd MMM yyyy" format
                    LocalDate scheduleFinishDate = LocalDate.parse(milestoneScheduledFinishDate.substring(0, 10), DateTimeFormatter.ISO_DATE);
                    String outputScheduleFinishDate = scheduleFinishDate.getDayOfMonth() + " " + scheduleFinishDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + scheduleFinishDate.getYear();
                    innerHtmlTable = innerHtmlTable.concat("<td>").concat(outputScheduleFinishDate).concat("</td>");
                } else {
                    innerHtmlTable = innerHtmlTable.concat("<td>").concat("-").concat("</td>");
                }
                // Actual Finish
                String milestoneActualFinishDate = projectMilestoneValues.getMilestoneActualFinishDate();
                if (isNotBlankString(milestoneActualFinishDate)) {
                    // Format the Date String to "dd MMM yyyy" format
                    LocalDate actualFinishDate = LocalDate.parse(milestoneActualFinishDate.substring(0, 10), DateTimeFormatter.ISO_DATE);
                    String outputActualFinishDate = actualFinishDate.getDayOfMonth() + " " + actualFinishDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + actualFinishDate.getYear();
                    innerHtmlTable = innerHtmlTable.concat("<td>").concat(outputActualFinishDate).concat("</td>");
                } else {
                    innerHtmlTable = innerHtmlTable.concat("<td>").concat("-").concat("</td>");
                }
                // Status
                innerHtmlTable = innerHtmlTable.concat("<td>").concat(projectMilestoneValues.getMilestoneTaskStatus()).concat("</td>");
                innerHtmlTable = innerHtmlTable.concat("</tr>");
                // Check if the Cumulative Result String Length indicator plus Inner Table string length is less than the HTML Text Area Maximum Character length
                if ((stringLength + innerHtmlTable.length() + endHtmlTable.length()) <= TEXT_AREA_HTML_MAX) {
                    // Assign the Inner Table string to the Return Result string
                    result = result.concat(innerHtmlTable);
                    // Set the Result String Length indicator after each iteration was created and less than HTML Text Area Maximum Character length
                    stringLength = stringLength + innerHtmlTable.length();
                } else {
                    // Break out of the loop if greater or equal to HTML Text Area Maximum Character length
                    break;
                }
            }
            // Assign the Final Table string to the Return Result string
            result = result.concat(endHtmlTable);
            // Set the Result String Length indicator after Final Table string was added
            stringLength = stringLength + endHtmlTable.length();
            log("IT Project Milestone HTML Table String length: " + stringLength);
        } else {
            result = "<p>No Milestones for the IT Project available</p>";
        }
        // Add token to Json Object
        milestoneJsonObject.put("token", prefix + fieldToken);
        // Declare and Instantiate the strValue Array
        JSONArray stringValueArray = new JSONArray();
        stringValueArray.add(result);
        // Add string value to the json object
        milestoneJsonObject.put("stringValue", stringValueArray);
        // Return the JSONObject
        return milestoneJsonObject;
    }

    /**
     * Method to set the HTML for the IT Project Milestones
     * Interim usage by ImpactAssessmentProcessor class
     *
     * @param projectMilestoneObj IT Project Major Milestone value array list
     * @return HTML string
     */
    @SuppressWarnings("unused")
    public String setProjectMilestoneHtml(ArrayList<ProjectMilestoneValues> projectMilestoneObj) {
        // Return String result
        String result = null;
        // Result String Length indicator
        int stringLength = 0;
        // Final Table string
        final String endHtmlTable = "</table>";
        // Format the date for display in HTML table
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        // Build the string with HTML tags for a table
        if (!projectMilestoneObj.isEmpty()) {
            // Header Table string
            String headerHtmlTable;
            // Add header of the html table
            headerHtmlTable = "<table style=\"border: 1px solid black; border-collapse: collapse; width: 98%;\">";
            headerHtmlTable = headerHtmlTable.concat("<tr>");
            headerHtmlTable = headerHtmlTable.concat("<td style=\"font-weight: bold; width: 40%;\">Milestone</td>");
            headerHtmlTable = headerHtmlTable.concat("<td style=\"font-weight: bold; width: 20%;\">Scheduled Finish</td>");
            headerHtmlTable = headerHtmlTable.concat("<td style=\"font-weight: bold; width: 20%;\">Actual Finish</td>");
            headerHtmlTable = headerHtmlTable.concat("<td style=\"font-weight: bold;\">Status</td>");
            headerHtmlTable = headerHtmlTable.concat("</tr>");
            // Assign the Header Table string to the Return Result string
            result = headerHtmlTable;
            // Set the Result String Length indicator after HTML Header was created
            stringLength = stringLength + headerHtmlTable.length();
            // Iterate through the Domain Involvement Object
            for (ProjectMilestoneValues projectMilestoneValues : projectMilestoneObj) {
                // Inner Table string
                String innerHtmlTable;
                // Create the Row in the table body
                innerHtmlTable = "<tr>";
                // Milestone
                innerHtmlTable = innerHtmlTable.concat("<td>").concat(projectMilestoneValues.getMilestoneTaskName()).concat("</td>");
                // Scheduled Finish
                String milestoneScheduledFinishDate = projectMilestoneValues.getMilestoneScheduledFinishDate();
                if (isNotBlankString(milestoneScheduledFinishDate)) {
                    // Format the Date String to "dd MMM yyyy" format
                    LocalDate scheduleFinishDate = LocalDate.parse(milestoneScheduledFinishDate.substring(0, 10), DateTimeFormatter.ISO_DATE);
                    String outputScheduleFinishDate = scheduleFinishDate.getDayOfMonth() + " " + scheduleFinishDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + scheduleFinishDate.getYear();
                    innerHtmlTable = innerHtmlTable.concat("<td>").concat(outputScheduleFinishDate).concat("</td>");
                } else {
                    innerHtmlTable = innerHtmlTable.concat("<td>").concat("-").concat("</td>");
                }
                // Actual Finish
                String milestoneActualFinishDate = projectMilestoneValues.getMilestoneActualFinishDate();
                if (isNotBlankString(milestoneActualFinishDate)) {
                    // Format the Date String to "dd MMM yyyy" format
                    LocalDate actualFinishDate = LocalDate.parse(milestoneActualFinishDate.substring(0, 10), DateTimeFormatter.ISO_DATE);
                    String outputActualFinishDate = actualFinishDate.getDayOfMonth() + " " + actualFinishDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + actualFinishDate.getYear();
                    innerHtmlTable = innerHtmlTable.concat("<td>").concat(outputActualFinishDate).concat("</td>");
                } else {
                    innerHtmlTable = innerHtmlTable.concat("<td>").concat("-").concat("</td>");
                }
                // Status
                innerHtmlTable = innerHtmlTable.concat("<td>").concat(projectMilestoneValues.getMilestoneTaskStatus()).concat("</td>");
                innerHtmlTable = innerHtmlTable.concat("</tr>");
                // Check if the Cumulative Result String Length indicator plus Inner Table string length is less than the HTML Text Area Maximum Character length
                if ((stringLength + innerHtmlTable.length() + endHtmlTable.length()) <= TEXT_AREA_HTML_MAX) {
                    // Assign the Inner Table string to the Return Result string
                    result = result.concat(innerHtmlTable);
                    // Set the Result String Length indicator after each iteration was created and less than HTML Text Area Maximum Character length
                    stringLength = stringLength + innerHtmlTable.length();
                } else {
                    // Break out of the loop if greater or equal to HTML Text Area Maximum Character length
                    break;
                }
            }
            // Assign the Final Table string to the Return Result string
            result = result.concat(endHtmlTable);
            // Set the Result String Length indicator after Final Table string was added
            stringLength = stringLength + endHtmlTable.length();
            log("IT Project Milestone HTML Table String length: " + stringLength);
        }
        return result;
    }

    /**
     * Method to set the PPM Feature Description with the IT Project Information
     * Used for IT-EPMO Projects only
     *
     * @param prefix            PPM Request Field Token Prefix
     * @param fieldToken        PPM Request Field Token
     * @param itProjectNumber   IS PMO Project number (Request ID)
     * @param itProjectName     IS PMO Project Name
     * @param epmoProjectNumber EPMO Project Number (Request ID)
     * @return JSONObject with the full PPM Feature Description
     */
    public JSONObject setFeatureDescription(String prefix, String fieldToken, String itProjectNumber, String itProjectName, String epmoProjectNumber) {
        // Example: (IS 12345) IT Project Name (EPMO 12345)
        String result;
        result = "(IS "
                .concat(itProjectNumber)
                .concat(") ")
                .concat(itProjectName)
                .concat(" (EPMO ")
                .concat(epmoProjectNumber).concat(")");
        // Set Json Object Vairable
        JSONObject descriptionFieldJsonObject = new JSONObject();
        // Add String to Json Object
        descriptionFieldJsonObject.put(prefix + fieldToken, result);
        // Return the JSONObject
        return descriptionFieldJsonObject;
    }

    /**
     * Method to set the PPM Feature Description with the IT Project Information
     * Used for None IT-EPMO Projects
     *
     * @param prefix          PPM Request Field Token Prefix
     * @param fieldToken      PPM Request Field Token
     * @param itProjectNumber IS PMO Project number (Request ID)
     * @param itProjectName   IS PMO Project Name
     * @return JSONObject with the full PPM Feature Description
     */
    public JSONObject setFeatureDescription(String prefix, String fieldToken, String itProjectNumber, String itProjectName) {
        // Example: (IS 12345) IT Project Name (EPMO 12345)
        String result;
        result = "(IS "
                .concat(itProjectNumber)
                .concat(") ")
                .concat(itProjectName);
        // Set Json Object Vairable
        JSONObject descriptionFieldJsonObject = new JSONObject();
        // Add String to Json Object
        descriptionFieldJsonObject.put(prefix + fieldToken, result);
        // Return the JSONObject
        return descriptionFieldJsonObject;
    }

    /**
     * Method to set the PPM Feature Description with the IT Project Information
     * Used for IT-EPMO Projects only in the ImpactAssessmentProcessor class
     *
     * @param itProjectNumber   IS PMO Project number (Request ID)
     * @param itProjectName     IS PMO Project Name
     * @param epmoProjectNumber EPMO Project Number (Request ID)
     * @return String with the full PPM Feature Description
     */
    @SuppressWarnings("unused")
    public String setFeatureDescription(String itProjectNumber, String itProjectName, String epmoProjectNumber) {
        // Example: (IS 12345) IT Project Name (EPMO 12345)
        String result;
        result = "(IS "
                .concat(itProjectNumber)
                .concat(") ")
                .concat(itProjectName)
                .concat(" (EPMO ")
                .concat(epmoProjectNumber).concat(")");
        return result;
    }

    /**
     * Method to set the PPM Feature Description with the IT Project Information
     * Used for None IT-EPMO Projects in the ImpactAssessmentProcessor class
     *
     * @param itProjectNumber IS PMO Project number (Request ID)
     * @param itProjectName   IS PMO Project Name
     * @return String with the full PPM Feature Description
     */
    @SuppressWarnings("unused")
    public String setFeatureDescription(String itProjectNumber, String itProjectName) {
        // Example: (IS 12345) IT Project Name
        String result;
        result = "(IS "
                .concat(itProjectNumber)
                .concat(") ")
                .concat(itProjectName);
        return result;
    }
}
