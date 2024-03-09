package za.co.mtn.ppm.bpm.ismpo.project;

import java.util.ArrayList;

/**
 * This class is used for updating the IT Project Phase and Status from the IT Project workflow execution step commands to the
 * IS PMO Feature, IS PMO Testing Feature and Octane Initiated Feature Request Types.
 */
public class UpdatePhaseStatusFeatureFields {
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
     *             <li>PRJ_STATUS: args[4] (IT Project Status)</li>
     *             <li>PRJ_PHASE: args[5] (IT Project Phase)</li>
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
        // IT Project Phase
        log("PRJ_STATUS: " + args[4]);
        // IT Project Status
        log("PRJ_PHASE: " + args[5]);
        log("**** End of Class Command Line Arguments****");

        final String ppmBaseUrl = args[0];
        final String username = args[1];
        final String password = args[2];
        final String requestId = args[3];
        final String itProjectStatus = args[4];
        final String itProjectPhase = args[5];
        // Create new instances of IspmoItProjectProcessor class
        IspmoItProjectProcessor stateProcessor = new IspmoItProjectProcessor();

        log("<<-- Start Update ALL Feature Fields -->>");
        log("<<- Get All Feature IDs linked to the IT Project with SQL Query ->>");
        ArrayList<String> allFeatureIds = stateProcessor.getFeatureIdsLinkedToItProject(ppmBaseUrl, username, password, SQL_REST_URL, requestId);
        if (allFeatureIds.isEmpty()) {
            log("- No IS PMO Feature Data linked to this IT Project");
        } else {
            log("<<- Update IT Project Status and IT Project Phase fields ->>");
            int featureUpdateCounter = 1;
            for (String allFeatureId : allFeatureIds) {
                // PPM Output
                // Printing Keys(Request ID) for outer Map
                log("<strong><<- PPM Feature ID: " + allFeatureId + "->></strong>");
                // Process the update of the Request Type Fields//
                stateProcessor.updateFeatureRequestStatusPhaseFields(ppmBaseUrl, username, password, REQ_REST_URL, allFeatureId, itProjectStatus, itProjectPhase);
                log("<strong><<- End for IS PMO Feature Update process #" + featureUpdateCounter + " ->></strong>");
                featureUpdateCounter++;
            }
        }
    }

    /**
     * Method to write out the Command Line Arguments for this class
     */
    private static void printCommandLineArguments() {
        log("Command Line Arguments Layout: sc_it_project_state_update_feature <ENV_BASE_URL> <REST_USERNAME> <REST_USER_PASSWORD> <PRJ_REQUEST_ID> <PRJ_STATUS> <PRJ_PHASE>");
        log("ENV_BASE_URL: args[0] (PPM Base URL)");
        log("REST_USERNAME: args[1] (PPM System User - ppmsysuser)");
        log("REST_USER_PASSWORD: args[2] (PPM System User Password)");
        log("PRJ_REQUEST_ID: args[3] (IT Project ID/No)");
        log("PRJ_STATUS: args[4] (IT Project Status)");
        log("PRJ_PHASE: args[5] (IT Project Phase)");
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
