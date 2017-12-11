package com.sparohealth.wingkit.classes;
import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Client handles all Wing REST API service calls
 */
public class Client {
    private String BASE_API_URL = "https://3rd-party-api.mywing.io/v2";

    private static RequestQueue requestQueue;
    private Context appContext;
    private String token = "";
    private static String testsessionId = "";
    private String patientId = "";
    private OAuthCredentials oAuthCredentials;
    private String clientId = "";
    private String clientSecret = "";

    //AWS vars
    private static int[] defaultAcceptableStatusCodes = new int[]{200,300};
    private String identityPoolId = "us-east-1:af3df912-5e61-40dc-9c5e-651f7e0b3789";
    private Regions cognitoRegion = Regions.US_EAST_1;

    private CognitoCredentialsProvider credentialsProvider;
    private AmazonS3 s3;
    private TransferUtility transferUtility;

    /**
     * Initialize and set up the Client object
     * @param context The application context
     * @param clientId The OAuth clientId value
     * @param clientSecret The OAuth clientSecret value
     */
    public Client(Context context, String clientId, String clientSecret) {
        this.appContext = context;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        requestQueue =  Volley.newRequestQueue(appContext);
        oAuthCredentials = new OAuthCredentials(clientId,clientSecret);
        setupAWS();
    }

    /**
     * Authenticates the application with the Wing API using the assigned Client ID/Secret.
     * @param callback Callback object for sending success/error messages to the calling object
     * @throws JSONException
     */
    public void authenticate(final WingApiCallback callback) throws JSONException {
        String endpoint = BASE_API_URL + "/accounts/login";
        JSONObject params = new JSONObject();

        params.put("clientId",oAuthCredentials.id);
        params.put("clientSecret",oAuthCredentials.secret);

        JsonObjectRequest newRequest = new JsonObjectRequest
                (Request.Method.POST, endpoint, params, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Client",response.toString());
                        try {
                            token = response.getString("token");
                            callback.onSuccessResponse(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            callback.onErrorResponse(e);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Client",error.getMessage());
                        callback.onErrorResponse(error);
                    }
                }){

            //add headers
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  headers = new HashMap<String, String>();
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");

                return headers;
            }};

        getRequestQueue().add(newRequest);
    }

    /**
     * Creates a new WING test session
     * @param patientData
     * @param timezone
     * @param latitude
     * @param longitude
     * @param altitude
     * @param floor
     * @param guessedResult
     * @param callback
     * @return boolean
     */
    public void createTestSession(PatientData patientData, String timezone, final double latitude, final double longitude, final Integer altitude, final Integer floor, final Integer guessedResult, final WingApiCallback callback) throws JSONException {
        String endpoint = BASE_API_URL + "/test-sessions";
        JSONObject params = new JSONObject();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        String formattedDate = sdf.format(new Date());

        String patientString = new Gson().toJson(patientData);
        JSONObject patientObj = new JSONObject(patientString);
        patientObj.remove("age");

        params.put("localTimezone", formattedDate);
        params.put("patient",patientObj);

        JsonObjectRequest newRequest = new JsonObjectRequest
                (Request.Method.POST, endpoint, params, new Response.Listener<JSONObject>() {
                    //response callback
                    @Override
                    public void onResponse(JSONObject response) {
                        callback.onSuccessResponse(response);
                    }
                }, new Response.ErrorListener() {
                    //Error callback
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: 11/6/2017
                        Log.d("err",error.toString());
                        callback.onErrorResponse(error);
                    }
                }){

            //add headers
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  headers = new HashMap<String, String>();
                headers.put("Authorization", token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        getRequestQueue().add(newRequest);
    }


    /**
     * Retrieves a users WING test session

     * @param sessionId
     * @param patientId
     * @return boolean
     */
    public void retrieveTestSession(final String sessionId,final String patientId, final WingApiCallback callback){
        String endpoint = BASE_API_URL + "/patients/"+patientId+"/test-sessions/"+sessionId;

        //setup a request
        JsonObjectRequest newRequest = new JsonObjectRequest
                (Request.Method.GET, endpoint, null, new Response.Listener<JSONObject>() {
                    //response callback
                    @Override
                    public void onResponse(JSONObject response) {
                        callback.onSuccessResponse(response);
                    }
                }, new Response.ErrorListener() {
                    //Error callback
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("err",error.toString());
                        callback.onErrorResponse(error);
                    }
                }){
            //add headers
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  headers = new HashMap<String, String>();
                headers.put("Authorization", token);
                headers.put("Content-Type", "application/json");

                return headers;
            }
        };
        getRequestQueue().add(newRequest);
    }

    /**
     * method to retrieve the current network request queue
     * @return RequestQueue
     */
    private RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(appContext);
        }
        return requestQueue;
    }

    /**
     * Method to retrieve the application context
     * @return Context
     */
    public Context getAppContext(){
        return appContext;
    }

    public void createUploadTarget(final String testSessionId, final String patientId, final WingApiCallback callback) {
        String endpoint = BASE_API_URL + "/patients/"+patientId+"/test-sessions/"+testSessionId+"/upload";

        //setup a request
        JsonObjectRequest newRequest = new JsonObjectRequest
                (Request.Method.GET, endpoint, null, new Response.Listener<JSONObject>() {

                    //response callback
                    @Override
                    public void onResponse(JSONObject response) {
                        callback.onSuccessResponse(response);
                    }

                }, new Response.ErrorListener() {

                    //Error callback
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("err",error.toString());
                        callback.onErrorResponse(error);
                    }
                }){


            //add headers
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  headers = new HashMap<String, String>();
                headers.put("Authorization", token);
                headers.put("Content-Type", "application/json");

                return headers;
            }

        };

        getRequestQueue().add(newRequest);
    }

    /**
     * Upload a test recording to be processed
     * @param filePath The absolute path to the file being uploaded
     * @param target The upload target object
     * @param callback The status callback object
     * @throws JSONException
     */
    public void uploadFile(String filePath, UploadTarget target, final WingApiCallback callback) throws JSONException {

        File newFile = new File(filePath);

        TransferObserver observer = transferUtility.upload(
                target.bucket,     /* The bucket to upload to */
                target.key,    /* The key for the Uploaded object */
                newFile        /* The file where the data to upload exists */
        );

        callback.onSuccessResponse(new JSONObject().put("Upload Callback","test response"));
    }

    private void setupAWS(){
        credentialsProvider = new CognitoCachingCredentialsProvider(getAppContext(),identityPoolId,cognitoRegion);
        s3 = new AmazonS3Client(credentialsProvider);
        transferUtility = new TransferUtility(s3, getAppContext());
    }

    public void createPatient(PatientData newPatient){
        String endpoint = BASE_API_URL + "/patients/";
    }

    public class OAuthCredentials {
        /// The client id.
        public String id;

        /// The client secret.
        public String secret;

        /**
         Initializes a `OAuthCredentials` structure.
         - parameter id: The client id of your application.
         - parameter secret: The client secret of your application.
         */

        public OAuthCredentials(String id, String secret) {
            this.id = id;
            this.secret = secret;
        }
    }

    public interface WingApiCallback {
        void onSuccessResponse(JSONObject result);
        void onErrorResponse(Exception error);
    }
}
