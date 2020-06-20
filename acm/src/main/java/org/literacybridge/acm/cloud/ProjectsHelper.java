package org.literacybridge.acm.cloud;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectsHelper {
    static final String DEPLOYMENTS_BUCKET_NAME = "acm-content-updates";

    IdentityPersistence identityPersistence;

    public ProjectsHelper(IdentityPersistence identityPersistence) {
        this.identityPersistence = identityPersistence;
    }

    @SuppressWarnings("unused")
    public static class DeploymentInfo {
        String project;         // Like UNICEF-CHPS
        String deploymentName;  // Like UNICEF-CHPS-19-3
        String versionMarker;   // Like UNICEF-CHPS-19-3-a
        String revId;           // Like a
        String fileName;        // name of the .zip file
        Date fileDate;          // Date the file was last modified (ie, uploaded to s3)
        String eTag;            // eTag of the .zip file.
        long size;              // size of the .zip file.
        boolean isCurrent;      // true if this is the ".current" Deployment

        DeploymentInfo(String project) {
            this.project = project;
        }

        public String getProject() {
            return project;
        }

        public String getDeploymentName() {
            return deploymentName;
        }

        public String getVersionMarker() {
            return versionMarker;
        }

        public String getRevId() {
            return revId;
        }

        public String getFileName() {
            return fileName;
        }

        public Date getFileDate() {
            return fileDate;
        }

        public String geteTag() {
            return eTag;
        }

        public long getSize() {
            return size;
        }

        public boolean isCurrent() {
            return isCurrent;
        }
    }

    private final Authenticator authInstance = Authenticator.getInstance();

    // Matches deploymentName-suffix.current or .rev. Like TEST-19-2-ab.rev
    private final Pattern markerPattern = Pattern.compile("((\\w+(?:-\\w+)*)-(\\w+))\\.(current|rev)");

    /**
     * Get the latest deployment info for the project.
     *
     * @param project Project for which to get Deployment info.
     * @return A map from Deployment Name to Deployment Info.  Currently, only a single
     * Deployment.
     */
    public Map<String, DeploymentInfo> getDeploymentsInfo(String project) {
        Map<String, DeploymentInfo> deplInfo = new HashMap<>();

        AmazonS3 s3Client = authInstance.getAwsInterface().getS3Client();
        ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request().withBucketName(
            ProjectsHelper.DEPLOYMENTS_BUCKET_NAME).withPrefix("projects/" + project + "/");

        ListObjectsV2Result result = s3Client.listObjectsV2(listObjectsV2Request);
        List<S3ObjectSummary> s3ObjectSummaries = result.getObjectSummaries();

        ProjectsHelper.DeploymentInfo deploymentInfo = new ProjectsHelper.DeploymentInfo(project);

        // First, look for the ".current" or ".rev" file. Then we'll look for the matching .zip.
        for (S3ObjectSummary summary : s3ObjectSummaries) {
            String[] parts = summary.getKey().split("/");
            if (parts.length == 3) {
                Matcher matcher = markerPattern.matcher(parts[2]);
                if (matcher.matches()) {
                    deploymentInfo.isCurrent = true;
                    deploymentInfo.versionMarker = matcher.group(1);    // like TEST-19-1-ap
                    deploymentInfo.deploymentName = matcher.group(2);   // like TEST-19-1
                    deploymentInfo.revId = matcher.group(3);            // like ap
                }
            }
        }

        // Knowing the current Deployment (in s3), gather its size.
        Pattern targetZipPattern = Pattern.compile(String.format("(?:content-)?%s.zip",
            deploymentInfo.versionMarker));
        for (S3ObjectSummary summary : s3ObjectSummaries) {
            // The S3 key is like projects/UNICEF-CHPS/content-UNICEF-CHPS-19-3-a.zip
            String[] parts = summary.getKey().split("/");
            if (parts.length == 3) {
                Matcher matcher = targetZipPattern.matcher(parts[2]);
                if (matcher.matches()) {
                    deploymentInfo.size = summary.getSize();    // 20mB or whatever
                    deploymentInfo.eTag = summary.getETag();    // hash of content
                    deploymentInfo.fileDate = summary.getLastModified(); // Date object stored
                    deploymentInfo.fileName = matcher.group();  // like content-UNICEF-CHPS-19-3-a.zip
                    deplInfo.put(deploymentInfo.deploymentName, deploymentInfo);
                }
            }
        }
        return deplInfo;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean downloadDeployment(DeploymentInfo deploymentInfo,
        File outputFile,
        BiConsumer<Long, Long> progressHandler)
    {
        if (authInstance.isAuthenticated() && authInstance.isOnline()) {
            return authInstance.getAwsInterface()
                .downloadS3Object(DEPLOYMENTS_BUCKET_NAME,
                    "projects/" + deploymentInfo.project + "/" + deploymentInfo.getFileName(),
                    outputFile,
                    progressHandler);
        }
        return false;
    }

    /**
     * Uploads a zip file of a deployment to S3. Creates a revision marker.
     * @param inputZipFile The file to be uploaded.
     * @param program to which the deployment belongs.
     * @param deployment name, like 'DEMO-20-3' (program-year-depl).
     * @param revision of the deployment, 'a', 'b', ...
     * @param progressHandler to receive progress notifications.
     * @return true if uploaded successfully.
     */
    public boolean uploadDeployment(File inputZipFile,
        String program,
        String deployment,
        String revision,
        BiConsumer<Long, Long> progressHandler)
    {
        if (authInstance.isAuthenticated() && authInstance.isOnline()) {
            Authenticator.AwsInterface aws = authInstance.getAwsInterface();
            String prefix = String.format("projects/%s/%s-%s", program, deployment, revision);
            boolean zipUploaded = aws.uploadS3Object(DEPLOYMENTS_BUCKET_NAME,
                    prefix+".zip",
                    inputZipFile,
                    progressHandler);
            ByteArrayInputStream revStream = new ByteArrayInputStream(new byte[0]);
            boolean revUploaded = aws.uploadS3Object(DEPLOYMENTS_BUCKET_NAME,
                prefix+".rev",
                revStream, 0,
                null);
            return zipUploaded && revUploaded;
        }
        return false;
    }

}
