package com.sparohealth.wingkit.classes;

import java.util.Date;

/**
 * Created by darien.sandifer on 11/28/2017.
 */

public class Test {
    private enum Keys {
        id,
        takenAt,
        status,
        breathDuration,
        exhaleCurve,
        totalVolume,
        pef,
        fev1,
        uploadTargetId
    }

    /**
     * Test Status enumeration
     */
    public enum TestStatus {
        /// test started
        started,
        /// test complete
        complete,
        /// test uploaded
        uploaded,
        /// test processing
        processing,
        /// test error
        error
    }

    public String id;
    public String status;
    public Date takenAt;
    public Double breathDuration;
    public Double[][] exhaleCurve;
    public Double totalVolume;
    public Double pef;
    public Double fev1;
    public String uploadTargetId;
}
