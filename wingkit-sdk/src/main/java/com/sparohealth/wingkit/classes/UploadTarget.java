package com.sparohealth.wingkit.classes;

/**
 * Upload Target data
 */
public class UploadTarget {
    private enum Keys{
        id,
        key,
        bucket
    }

    public String id;
    public String key;
    public String bucket;

    /**
     * Initialize the {@link UploadTarget} object
     * @param id Upload target id
     * @param key Path for the upload
     * @param bucket Bucket used for the upload
     */
    public UploadTarget(String id, String key, String bucket){
        this.id = id;
        this.key = key;
        this.bucket = bucket;
    }
}
