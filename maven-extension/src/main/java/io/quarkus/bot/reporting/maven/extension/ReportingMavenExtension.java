package io.quarkus.bot.reporting.maven.extension;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "quarkus-build-report")
public class ReportingMavenExtension extends AbstractMavenLifecycleParticipant {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public void afterSessionEnd(MavenSession session)
            throws MavenExecutionException {

        MavenExecutionResult result = session.getResult();
        List<MavenProject> projects = result.getTopologicallySortedProjects();

        BuildReport buildReport = new BuildReport();

        Path topLevelProjectPath = result.getProject().getBasedir().toPath();

        for (MavenProject project : projects) {
            BuildSummary buildSummary = result.getBuildSummary(project);

            if (buildSummary == null) {
                buildReport.addProjectReport(
                        ProjectReport.skipped(project.getName(), project.getBasedir(), project.getGroupId(),
                                project.getArtifactId()));
            } else if (buildSummary instanceof BuildFailure) {
                buildReport.addProjectReport(
                        ProjectReport.failure(project.getName(), project.getBasedir(), result.getExceptions(),
                                project.getGroupId(), project.getArtifactId()));
            } else if (buildSummary instanceof BuildSuccess) {
                buildReport.addProjectReport(
                        ProjectReport.success(project.getName(), project.getBasedir(), project.getGroupId(),
                                project.getArtifactId()));
            }

        }

        try (FileOutputStream file = new FileOutputStream("beer.json")) {
            System.out.println(OBJECT_MAPPER.writeValueAsString(buildReport));
            OBJECT_MAPPER.writeValue(file, buildReport);
        } catch (Exception e) {
            System.out.println("Exception in afterEndSession");
        }
    }

    static class BuildReport {

        private List<ProjectReport> projectReports = new ArrayList<>();

        public void addProjectReport(ProjectReport projectReport) {
            this.projectReports.add(projectReport);
        }

        public List<ProjectReport> getProjectReports() {
            return this.projectReports;
        }
    }

    static class ProjectReport {
        private String name;
        private String status;
        private File baseDir;
        private List<Throwable> errors;
        private String groupId;
        private String artifactId;

        public static ProjectReport success(String name, File baseDir, String groupId, String artifactId) {
            return new ProjectReport(name, "success", baseDir, null, groupId, artifactId);
        }

        public static ProjectReport failure(String name, File baseDir, List<Throwable> errors, String groupId,
                String artifactId) {
            return new ProjectReport(name, "error", baseDir, errors, groupId, artifactId);
        }

        public static ProjectReport skipped(String name, File baseDir, String groupId, String artifactId) {
            return new ProjectReport(name, "skipped", baseDir, null, groupId, artifactId);
        }

        private ProjectReport(String name, String status, File baseDir, List<Throwable> errors, String groupId,
                String artifactId) {
            this.name = name;
            this.status = status;
            this.baseDir = baseDir;
            this.errors = errors;
            this.artifactId = artifactId;
            this.groupId = groupId;
        }

        // getters
        public String getName() {
            return this.name;
        }

        public String getStatus() {
            return this.status;
        }

        public File getBaseDir() {
            return this.baseDir;
        }

        public List<Throwable> getErrorMessage() {
            return this.errors;
        }

        public String getArtifactId() {
            return this.artifactId;
        }

        public String getGroupId() {
            return this.groupId;
        }

    }
}
