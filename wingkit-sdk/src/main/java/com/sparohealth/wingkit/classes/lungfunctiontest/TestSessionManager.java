package com.sparohealth.wingkit.classes.lungfunctiontest;

import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.sparohealth.wingkit.classes.Client;
import com.sparohealth.wingkit.classes.Test;
import com.sparohealth.wingkit.classes.TestSession;
import com.sparohealth.wingkit.classes.UploadTarget;
import com.sparohealth.wingkit.classes.Client.WingApiCallback;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;

/**
 * {@link TestSessionManager} handles the running of lung function tests
 */
public class TestSessionManager {
    private String TAG = "TestSessionManager";

    /// The Wing client used to interface with the Wing REST API.
    private Client mClient = null;
    /// The state of the test session.
    public TestSession.TestSessionState state = TestSession.TestSessionState.noTest;
    /// The active test session.
    public TestSession testSession;
    /// The upload target ids that have been used for the current test session.
    public List<String> usedUploadTargetIds = new ArrayList<>();
    /// The number of tests that are allowed to fail processing before the test session is considered invalid.
    public final int failedTestsThreshold = 2;
    /// The number of tests that are allowed to fail due to local failure reasons before the test session is considered invalid.
    public final int localTestFailureThreshold = 2;
    /// The interval at which the server will be pinged to check if processing is complete.
    public final long processingPollingInterval = 800;
    /// The threshold that represents the number of times the app should attempt to refresh the test session.
    public final int processingTimeoutThreshold = 10;
    /// The number of attempts the test session has been refreshed in effort to determine the processing state.
    private int number0fProcessingAttempts = 0;
    /// The Test Session Manager delegate;
    private TestSessionManagerDelegate mDelegate = null;
    private Timer mProcessingAttemptsTimer = new Timer();

    /// Read the number of attempts the test session has been refreshed in effort to determine the processing state.
    public int getNumber0fProcessingAttempts() {
        return this.number0fProcessingAttempts;
    }

    /// Set the Test Session Manager delegate object
    public void setDelegate(TestSessionManagerDelegate delegate) {
        mDelegate = delegate;
    }

    /// Initialize the Test Session Manager
    public TestSessionManager(Client client) {
        this.mClient = client;
    }

    public void resetProcessingAttemptsCount(){
        number0fProcessingAttempts = 0;
    }

    /// Handle completion event messages
    private void completed(TestSessionManagerError error) {
        if (this.mDelegate != null) {
            this.mDelegate.completed(error);
        }
    }

    private void processing() {
        if (this.mDelegate != null) {
            this.mDelegate.processing();
        }
    }

    /**
     * Run the test session processing checks
     */
    public void processTestSession() {
        Log.d(TAG, "Beginning to process the test session");

        processing();

        /// if the number of processing
        if (number0fProcessingAttempts >= processingTimeoutThreshold){
            Log.d(TAG, "Processing attempts exceeded");
            resetProcessingAttemptsCount();
            completed(TestSessionManagerError.processingTimeout);
        }
        else{
            mClient.retrieveTestSession(testSession.id, testSession.patientId, new WingApiCallback() {
                @Override
                public void onSuccessResponse(JSONObject result) {
                    try {
                        Log.d(TAG, "Got the test session data - " + result.toString());

                        resetProcessingAttemptsCount();
                        Gson gson = new Gson();

                        List<UploadTarget> uploads = testSession.uploads;

                        testSession = gson.fromJson(result.toString(), TestSession.class);
                        testSession.uploads = uploads;

                        int processedTestCount = 0;

                        for (Test test: testSession.tests) {
                            Test.TestStatus status = Test.TestStatus.valueOf(test.status.toLowerCase());
                            if (status == Test.TestStatus.complete || status == Test.TestStatus.error) {
                                processedTestCount++;
                            }
                        }

                        if (processedTestCount == usedUploadTargetIds.size() && processedTestCount == testSession.tests.size()) {
                            Log.d(TAG, "The processed test count = test count");

                            updateState();
                            resetProcessingAttemptsCount();
                            completed(null);
                        }
                        else {
                            Log.d(TAG, "Starting retry attempt");

                            number0fProcessingAttempts++;

                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    processTestSession();
                                }
                            }, processingPollingInterval);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                @Override
                public void onErrorResponse(Exception error) {
                    Log.e(TAG, "Error in Client.retrieveTestSession - " + error.getMessage() + "\n" + Arrays.toString(error.getStackTrace()));

                    resetProcessingAttemptsCount();

                    completed(TestSessionManagerError.retrieveTestSessionFailed);
                }
            });
        }

    }

    /**
     * Upload the test recording file for processing
     * @param filePath The file path for the audio file being uploaded
     * @throws JSONException
     */
    public void uploadRecording(String filePath) throws JSONException {
        UploadTarget target = null;

        for (UploadTarget current : testSession.uploads) {
            if (!usedUploadTargetIds.contains(current.id)) {
                target = current;
                break;
            }
        }

        if (target != null) {
            usedUploadTargetIds.add(target.id);
            try {
                mClient.uploadFile(filePath, target, new WingApiCallback() {
                    @Override
                    public void onSuccessResponse(JSONObject result) {
                        Log.i("test", result.toString());
                        processTestSession();
                    }
                    @Override
                    public void onErrorResponse(Exception error) {
                        Log.i("test", error.toString());
                        completed(TestSessionManagerError.testUploadFailed);
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            getUploadTarget(filePath);
        }
    }

    /**
     * Create a new {@link UploadTarget}
     * @param filePath
     */
    private void getUploadTarget(final String filePath){
        mClient.createUploadTarget(testSession.id, testSession.patientId, new WingApiCallback() {
            @Override
            public void onSuccessResponse(JSONObject result) {
                Gson gson = new Gson();
                UploadTarget target = gson.fromJson(result.toString(), UploadTarget.class);
                testSession.uploads.add(target);

                try {
                    uploadRecording(filePath);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onErrorResponse(Exception error) {
                Log.d("TestSesionManager", error.getMessage());
                completed(TestSessionManagerError.uploadTargetCreationFailed);
            }
        });
    }

    /**
     * Update the current state of the session
     */
    private void updateState() {
        TestSession.TestSessionState newState = state;
        String bestTestChoice = testSession.bestTestChoice == null ? "" : testSession.bestTestChoice;

        if (bestTestChoice.toLowerCase().equals(BestTestChoice.reproducible.toLowerCase())) {
                newState = TestSession.TestSessionState.reproducibleTestFinal;
        }
        else if (bestTestChoice.toLowerCase().equals(BestTestChoice.highestReference.toLowerCase())) {
            newState = TestSession.TestSessionState.notReproducibleTestFinal;
        }
        else {
            if (testSession.tests.size() > 0) {
                Test mostRecent = testSession.tests.get(testSession.tests.size() - 1);
                Test.TestStatus status = Test.TestStatus.valueOf(mostRecent.status.toLowerCase());
                switch (status) {
                    case error: {
                        int failed = 0;

                        for (Test test : testSession.tests) {
                            Test.TestStatus testStatus = Test.TestStatus.valueOf(test.status.toLowerCase());
                            if (testStatus == Test.TestStatus.error) {
                                failed++;
                            }
                        }

                        newState = failed >= failedTestsThreshold ? TestSession.TestSessionState.notProcessedTestFinal : TestSession.TestSessionState.notProcessedTestFirst;
                        break;
                    }
                    case complete: {
                        int completeCount = 0;

                        for (Test test : testSession.tests) {
                            Test.TestStatus testStatus = Test.TestStatus.valueOf(test.status.toLowerCase());
                            if (testStatus == Test.TestStatus.complete) {
                                completeCount++;
                            }
                        }

                        if (completeCount == 1)
                            newState = TestSession.TestSessionState.goodTestFirst;
                        else if (completeCount == 2)
                            newState = TestSession.TestSessionState.notReproducibleTestFirst;

                        break;
                    }
                }
            }
        }

        state = newState;
    }

    /**
     * {@link TestSessionManager} Error enumeration
     */
    public enum TestSessionManagerError{
        /// Indicates the specified test session could not be loaded.
        retrieveTestSessionFailed,

        /// Indicates a upload target could not be created.
        createUploadTargetFailed,

        /// Indicates the processing request has timed out.
        processingTimeout,

        /// Indicates that an upload target could not be created to upload a test recording to.
        uploadTargetCreationFailed,

        /// Indicates the test recording failed to upload to S3.
        testUploadFailed
    }

    /**
     * Callback interface for sending session event messages
     */
    public interface TestSessionManagerDelegate {
        void completed(TestSessionManagerError status);
        void processing();
    }

    /**
     * Best Test Choice values
     */
    public class BestTestChoice{
        public static final String reproducible = "reproducible";
        public static final String highestReference = "highest reference";
    }
}

