package com.sparohealth.wingkit.classes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by darien.sandifer on 11/10/2017.
 */

public class PatientData {
    /// The unique ID for the patient.
    public String externalId;

    /// The patient's biological sex.
    public BiologicalSex biologicalSex;

    /// The patient's ethnicity.
    public String ethnicity;

    /// The patient's height (in inches).
    public int height;

    /// The patient's age.
    public int age;

    ///DOB
    public String dob;

    /**
     * Create the PatientData object
     * @param patientId Patient identifier
     * @param biologicalSex Biological sex of the patient
     * @param ethnicity Ethnicity of the patient
     * @param height Height (in inches) of the patient
     * @param age Age of the patient
     * @param dob Date of Birth of the patient
     */
    public PatientData(String patientId, BiologicalSex biologicalSex, String ethnicity, int height, int age,Date dob){
        this.externalId = patientId;
        this.biologicalSex = biologicalSex;
        this.ethnicity = ethnicity;
        this.height = height;
        this.age = age;
        this.dob = getISO8601StringForDate(dob);
    }

    /**
     * Create the PatientData object
     * @param patientId Patient identifier
     * @param biologicalSex Biological sex of the patient
     * @param ethnicity Ethnicity of the patient
     * @param height Height (in inches) of the patient
     * @param age Age of the patient
     */
    public PatientData(String patientId, BiologicalSex biologicalSex, String ethnicity, int height, int age){
        this.externalId = patientId;
        this.biologicalSex = biologicalSex;
        this.ethnicity = ethnicity;
        this.height = height;
        this.age = age;
    }

    private String getISO8601StringForDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }


    /**
     * Biological Sex enumeration
     */
    public enum BiologicalSex {
        male,
        female
    }

    /**
     * Ethnicity enumeration
     */
    public enum Ethnicity {
        other("other"),
        nativeAmerican("american indian or alaskan native"),
        Asian("asian"),
        black("black or african american"),
        pacificIslander("native hawaiian or pacific islander"),
        whiteNonHispanic ("white (non-hispanic)"),
        whiteHispanic("white (hispanic)"),
        twoOrMore("two or more");

        private final String text;

        private static final Map<String, Ethnicity> lookup = new HashMap<>();

        static {
            for (Ethnicity e : Ethnicity.values()) {
                lookup.put(e.getText(), e);
            }
        }

        Ethnicity(final String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public static Ethnicity get(String text) {
            return lookup.get(text);
        }

        @Override
        public String toString() {
            return this.text;
        }
    }
}
