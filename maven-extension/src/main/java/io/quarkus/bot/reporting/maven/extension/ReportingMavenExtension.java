package io.quarkus.bot.reporting.maven.extension;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
            Path projectPath = topLevelProjectPath.relativize(project.getBasedir().toPath());

            if (buildSummary == null) {
                buildReport.addProjectReport(
                        ProjectReport.skipped(project.getName(), projectPath, project.getGroupId(),
                                project.getArtifactId()));
            } else if (buildSummary instanceof BuildFailure) {
                buildReport.addProjectReport(
                        ProjectReport.failure(project.getName(), projectPath,
                                result.getExceptions().stream().map(t -> t.getMessage().replaceAll("\u001B\\[[;\\d]*m", ""))
                                        .collect(Collectors.toList()),
                                project.getGroupId(), project.getArtifactId()));
            } else if (buildSummary instanceof BuildSuccess) {
                buildReport.addProjectReport(
                        ProjectReport.success(project.getName(), projectPath, project.getGroupId(),
                                project.getArtifactId()));
            }

        }

        try {
            Files.createDirectories(Path.of("target"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (FileOutputStream file = new FileOutputStream("target/build-report.json")) {
            OBJECT_MAPPER.writeValue(file, buildReport);
        } catch (Exception e) {
            e.printStackTrace();
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
        private ReportingMavenExtension.StatusValues status;//make this an enum
        private String baseDir;
        private List<String> errors;
        private String groupId;
        private String artifactId;

        public static ProjectReport success(String name, Path baseDir, String groupId, String artifactId) {
            return new ProjectReport(name, StatusValues.SUCCESS, baseDir, Collections.emptyList(), groupId, artifactId);
        }

        public static ProjectReport failure(String name, Path baseDir, List<String> errors, String groupId,
                String artifactId) {
            return new ProjectReport(name, StatusValues.FAILURE, baseDir, errors, groupId, artifactId);
        }

        public static ProjectReport skipped(String name, Path baseDir, String groupId, String artifactId) {
            return new ProjectReport(name, StatusValues.SKIPPED, baseDir, Collections.emptyList(), groupId, artifactId);
        }

        private ProjectReport(String name, ReportingMavenExtension.StatusValues status, Path baseDir, List<String> errors,
                String groupId,
                String artifactId) {
            this.name = name;
            this.status = status;
            this.baseDir = baseDir.toString();
            this.errors = errors;
            this.artifactId = artifactId;
            this.groupId = groupId;
        }

        // getters
        public String getName() {
            return this.name;
        }

        public ReportingMavenExtension.StatusValues getStatus() {
            return this.status;
        }

        public String getBaseDir() {
            return this.baseDir;
        }

        public List<String> getErrors() {
            return this.errors;
        }

        public String getArtifactId() {
            return this.artifactId;
        }

        public String getGroupId() {
            return this.groupId;
        }

    }

    enum StatusValues {
        SUCCESS,
        FAILURE,
        SKIPPED
    }
}
