package za.co.mtn.ppm.bpm.ismpo.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to update the following PPM Feature Request Types:
 * <ul>
 *     <li>IS PMO Feature</li>
 *     <li>IS PMO Testing Feature</li>
 *     <li>Octane Initiated Feature</li>
 * </ul>
 */
public class UpdateAllFeatureFields {
    // Variable to set the REST API URL
    private static final String REQ_REST_URL = "rest2/dm/requests";
    private static final String SQL_REST_URL = "rest2/sqlRunner/runSqlQuery";

    /**
     * Main method to this class passing specific arguments
     *
     * @param args List of Arguments:<ul>
     *             <li>ENV_BASE_URL: args[0] (PPM Base URL)</li>
     *             <li>REST_USERNAME: args[1] (PPM System User - ppmsysuser)</li>
     *             <li>REST_USER_PASSWORD: args[2] (PPM System User Password)</li>
     *             <li>PRJ_REQUEST_ID: args[3] (IT Project ID/No)</li>
     *             </ul>
     */
    public static void main(String[] args) {
        // Verify that all Command Line Arguments has been submitted
        if (args.length < 5) {
            log("The Class Command Line Arguments is incorrect!");
            printCommandLineArguments();
            System.exit(1);
        }
        // Assign parameters to variables for usage in methods
        log("**** Class Command Line Arguments****");
        // Base URL for PPM
        log("ENV_BASE_URL: " + args[0]);
        // REST API Username
        log("REST_USERNAME: " + args[1]);
        // IT Project Request ID
        log("PRJ_REQUEST_ID: " + args[3]);
        // IT Project Request Type Name
        log("IT_PROJECT_REQUEST_TYPE: " + args[4]);
        log("**** End of Class Command Line Arguments****");

        final String ppmBaseUrl = args[0];
        final String username = args[1];
        final String password = args[2];
        final String requestId = args[3];
        final String projectRequestType = args[4];

        // Create new instances of IspmoItProjectProcessorOld class
        IspmoItProjectProcessor processor = new IspmoItProjectProcessor();
        log("<<-- Start Update ALL Feature Fields -->>");
        log("<<- Get IT Project Data with SQL Query ->>");
        HashMap<String, String> itProjectInformation = processor.getItProjectData(ppmBaseUrl, username, password, SQL_REST_URL, requestId, projectRequestType);
        log("<<- Get IT Project Milestones with SQL Query ->>");
        ArrayList<ProjectMilestoneValues> projectMilestoneArraylist = processor.getItProjectMilestoneData(ppmBaseUrl, username, password, SQL_REST_URL, requestId);

        if (projectMilestoneArraylist.isEmpty()) {
            log("No IT Project Work Plan and Milestones added to the IT Project #" + requestId);
        }
        log("<<- Get IS PMO Feature Data linked to the IT Project with SQL Query ->>");
        HashMap<String, HashMap<String, String>> ispmoFeatureInformtation = processor.getPpmFeatureRequestData(ppmBaseUrl, username, password, SQL_REST_URL, "IS PMO Feature", requestId);
        if (ispmoFeatureInformtation.isEmpty()) {
            log("- No IS PMO Feature Data linked to this IT Project");
        } else {
            log("<<-- Update IS PMO Feature Request Fields -->>");
            int ispmoFeatureCreatedCounter = 1;
            for (Map.Entry<String, HashMap<String, String>> set : ispmoFeatureInformtation.entrySet()) {
                // Printing Keys(Request ID) for outer Map
                log("<strong><<- IS PMO Feature ID: " + set.getKey() + "->></strong>");
                // Process the update of the Request Type Fields//
                processor.updateFeatureRequestFields(ppmBaseUrl, username, password, REQ_REST_URL, set.getKey(), projectMilestoneArraylist, itProjectInformation, set.getValue(), projectRequestType);
                log("<strong><<- End for IS PMO Feature Update process #" + ispmoFeatureCreatedCounter + " ->></strong>");
                ispmoFeatureCreatedCounter++;
            }
        }
        log("<<- Get IS PMO Testing Feature Data linked to the IT Project with SQL Query ->>");
        HashMap<String, HashMap<String, String>> ispmoTestingFeatureInformtation = processor.getPpmFeatureRequestData(ppmBaseUrl, username, password, SQL_REST_URL, "IS PMO Testing Feature", requestId);
        if (ispmoTestingFeatureInformtation.isEmpty()) {
            log("- No IS PMO Testing Feature Data linked to this IT Project");
        } else {
            log("<<-- Update IS PMO Testing Feature Request Fields -->>");
            int ispmoTestingFeatureCreateCounter = 1;
            for (Map.Entry<String, HashMap<String, String>> set : ispmoTestingFeatureInformtation.entrySet()) {
                // Printing Keys(Request ID) for outer Map
                log("<strong><<- IS PMO Testing Feature ID: " + set.getKey() + "->>");
                // Process the update of the Request Type Fields//
                processor.updateFeatureRequestFields(ppmBaseUrl, username, password, REQ_REST_URL, set.getKey(), projectMilestoneArraylist, itProjectInformation, set.getValue(), projectRequestType);
                log("<strong><<- End for IS PMO Testing Feature Update process #" + ispmoTestingFeatureCreateCounter + " ->></strong>");
                ispmoTestingFeatureCreateCounter++;
            }
        }
        log("<<- Get Octane Initiated Feature Data linked to the IT Project with SQL Query ->>");
        HashMap<String, HashMap<String, String>> octaneInitiatedFeatureInformtation = processor.getPpmFeatureRequestData(ppmBaseUrl, username, password, SQL_REST_URL, "Octane Initiated Feature", requestId);
        if (octaneInitiatedFeatureInformtation.isEmpty()) {
            log("- No Octane Initiated Feature Data linked to this IT Project");
        } else {
            log("<<-- Update Octane Initiated Feature Request Fields -->>");
            int octaneInitiatedFeatureCreateCounter = 1;
            for (Map.Entry<String, HashMap<String, String>> set : ispmoTestingFeatureInformtation.entrySet()) {
                log("<strong><<- Octane Initiated Feature ID: " + set.getKey() + "->></strong>");
                // Process the update of the Request Type Fields//
                processor.updateFeatureRequestFields(ppmBaseUrl, username, password, REQ_REST_URL, set.getKey(), itProjectInformation, set.getValue(), projectRequestType);
                log("<strong><<- End for Octane Initiated Feature Update process #" + octaneInitiatedFeatureCreateCounter + " ->></strong>");
                octaneInitiatedFeatureCreateCounter++;
            }
        }
    }

    /**
     * Method to write out the Command Line Arguments for this class
     */
    private static void printCommandLineArguments() {
        log("Command Line Arguments Layout: sc_it_project_update_feature <ENV_BASE_URL> <REST_USERNAME> <REST_USER_PASSWORD> <PRJ_REQUEST_ID> <IT_PROJECT_REQUEST_TYPE>");
        log("ENV_BASE_URL: args[0] (PPM Base URL)");
        log("REST_USERNAME: args[1] (PPM System User - ppmsysuser)");
        log("REST_USER_PASSWORD: args[2] (PPM System User Password)");
        log("PRJ_REQUEST_ID: args[3] (IT Project ID/No)");
        log("IT_PROJECT_REQUEST_TYPE: args[3] (IT Project Request Type Name)");
    }

    private static void measureMemoryUsage(String prefixMessage) {
        long memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        double memoryUsageMB = memoryUsage / (1024.0 * 1024.0); // Convert bytes to megabytes
        System.out.printf("%s Memory Usage: %.2f MB\n", prefixMessage, memoryUsageMB);
    }

    /**
     * Method to write out to the console or log file
     *
     * @param str String to print to console
     */
    private static void log(final String str) {
        System.out.println(str);
    }
}
