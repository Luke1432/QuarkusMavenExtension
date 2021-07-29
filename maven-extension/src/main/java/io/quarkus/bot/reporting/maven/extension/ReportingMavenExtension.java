package io.quarkus.bot.reporting.maven.extension;

import java.io.FileOutputStream;
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

        for (MavenProject project : projects) {
            BuildSummary buildSummary = result.getBuildSummary(project);

            if (buildSummary == null) {
                buildReport.addProjectReport(ProjectReport.skipped(project.getName(), null));
            } else if (buildSummary instanceof BuildFailure) {
                buildReport.addProjectReport(ProjectReport.failure(project.getName(), null, null));
            } else if (buildSummary instanceof BuildSuccess) {
                buildReport.addProjectReport(ProjectReport.success(project.getName(), null));
            }

            // TODO  buildResult.add(buildSummary.getProject().getArtifactId());
        }

        try(FileOutputStream file = new FileOutputStream("beer.json")) {
            System.out.println(OBJECT_MAPPER.writeValueAsString(buildReport));
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
        private String status;
        private String baseDir;
        private String errorMessage;

        public static ProjectReport success(String name, String baseDir) {
            return new ProjectReport(name, "success", baseDir, null);
        }

        public static ProjectReport failure(String name, String baseDir, String errorMessage) {
            return new ProjectReport(name, "error", baseDir, errorMessage);
        }

        public static ProjectReport skipped(String name, String baseDir) {
            return new ProjectReport(name, "skipped", baseDir, null);
        }

        private ProjectReport(String name, String status, String baseDir, String errorMessage) {
            this.name = name;
            this.status = status;
            this.baseDir = baseDir;
            this.errorMessage = errorMessage;
        }

        // getters
        public String getName() {
            return this.name;
        }

        public String getStatus() {
            return this.status;
        }

        public String getBaseDir() {
            return this.baseDir;
        }

        public String getErrorMessage() {
            return this.errorMessage;
        }

    }
}
