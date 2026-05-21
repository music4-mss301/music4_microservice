package vn.edu.fpt.sba.music4.util;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Utility class for handling Amazon S3 operations
 * Refactored for Spring Boot Dependency Injection
 */
@Component
public class AWSUtil {

    private final AmazonS3 s3Client;
    private final String bucketName;

    // Constructor Injection là best practice trong Spring Boot
    public AWSUtil(AmazonS3 s3Client, @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Extracts the object key from a given S3 URL.
     *
     * @param url full S3 object URL
     * @return extracted object key (relative path in bucket)
     */
    public String extractKeyFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath(); // e.g., "/folder/file.mp3"
            if (path.startsWith("/")) {
                return path.substring(1); // Remove leading "/"
            }
            return path;
        } catch (URISyntaxException e) {
            // Return original URL for debugging if parsing fails
            return url;
        }
    }

    /**
     * Deletes all objects within a "folder" (prefix) in the S3 bucket.
     * Handles pagination of results.
     *
     * @param folderName the folder path (prefix) to delete
     */
    public void deleteFolder(String folderName) {
        // Ensure the folder name ends with "/"
        if (!folderName.endsWith("/")) {
            folderName += "/";
        }

        ObjectListing objectListing = s3Client.listObjects(bucketName, folderName);
        List<S3ObjectSummary> summaries = objectListing.getObjectSummaries();

        // Delete each object in the current batch
        for (S3ObjectSummary obj : summaries) {
            s3Client.deleteObject(bucketName, obj.getKey());
        }

        // Continue deleting if there are more objects (pagination)
        while (objectListing.isTruncated()) {
            objectListing = s3Client.listNextBatchOfObjects(objectListing);
            summaries = objectListing.getObjectSummaries();
            for (S3ObjectSummary obj : summaries) {
                s3Client.deleteObject(bucketName, obj.getKey());
            }
        }
    }
}