package com.sparohealth.wingkit.classes;

import java.util.Date;
import java.util.List;

/**
 * Created by darien.sandifer on 11/14/2017.
 */

public class TestSession {

    private enum Keys {
        id,
        patientId,
        startedAt,
        endedAt,
        lungFunctionZone,
        respiratoryState,
        referenceMetric,
        bestTestChoice,
        bestTest,
        tests,
        testSessionState,
        pefPredicted,
        fev1Predicted,
        metadata,
        latitude,
        longitude,
        altitude,
        floor,
        uploads
    }

    public String id;
    public String patientId;
    public Date startedAt;
    public Date endedAt;
    public String lungFunctionZone;
    public String respiratoryState;
    public String referenceMetric;
    public Double pefPredicted;
    public Double fev1Predicted;
    public Double latitude;
    public Double longitude;
    public Double altitude;
    public Double floor;
    public String bestTestChoice;
    public Test bestTest;
    public List<Test> tests;
    public double breathDuration;

    public List<UploadTarget> uploads;

    public void TestSession(){

    }

    /// The `TestSessionState` enum describes the various states a test session can be in.
    public enum TestSessionState {
        /// Indicates that no tests have been performed during the session.
        noTest,

        /// Indicates that the test session includes one successful test.
        goodTestFirst,

        /// Indicates that the test session includes one test that wasn't able to be processed.
        notProcessedTestFirst,

        /// Indicates that the test session includes two complete tests that aren't reproducible.
        notReproducibleTestFirst,

        /// Indicates that the test session has concluded with non-reproducible results.
        notReproducibleTestFinal,

        /// Indicates that the test session has concluded with reproducible results.
        reproducibleTestFinal,

        /// Indiciates that the test session has concluded with at least two non-processable tests.
        notProcessedTestFinal
    }
}
