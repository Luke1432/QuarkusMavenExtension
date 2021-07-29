package io.quarkus.bot.reporting.maven.extension;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "beer")
public class ReportingMavenExtension extends AbstractMavenLifecycleParticipant {
    static ObjectMapper objectMapper=new ObjectMapper();
    public void afterSessionStart(MavenSession session)
            throws MavenExecutionException {

    }

    public void afterProjectsRead(MavenSession session)
            throws MavenExecutionException {

    }

    public void afterSessionEnd(MavenSession session)
            throws MavenExecutionException {

        Path path;
        ProjectReport projectReport = new ProjectReport();

        System.out.println("This is the afterSessionEnd method. ");

        MavenExecutionResult result = session.getResult();
        List<MavenProject> projects = result.getTopologicallySortedProjects();
        ArrayList<String> buildResults = new ArrayList<String>();

        for (MavenProject project : projects) {
            ArrayList<Object> buildResult = new ArrayList<Object>();
            BuildSummary buildSummary = result.getBuildSummary(project);

            if (buildSummary == null) {
                continue;
            }

            buildResult.add(projectReport.getName());
            buildResult.add(projects.indexOf(project));
            buildResult.add(buildSummary.getProject().getArtifactId());

            buildResults.add(String.valueOf(buildResult));
        }

        System.out.println(String.valueOf(buildResults));

        try {
            FileOutputStream file = new FileOutputStream("target/beer.json");
            objectMapper.writeValue(file, String.valueOf(buildResults));
            file.flush();
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class BuildReport {
        private List<ProjectReport> projectReports;

        public List<ProjectReport> getProjectReports() {
            return this.projectReports;

        }
    }

    class ProjectReport {
        private String name;
        private String status;
        private String baseDir;
        private String errorMessage;

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
