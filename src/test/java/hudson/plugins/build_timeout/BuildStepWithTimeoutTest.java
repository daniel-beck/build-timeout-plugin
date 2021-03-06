package hudson.plugins.build_timeout;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.build_timeout.operations.AbortOperation;
import hudson.plugins.build_timeout.operations.FailOperation;
import hudson.tasks.Builder;
import hudson.util.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class BuildStepWithTimeoutTest {
    @Rule
    public BuildTimeOutJenkinsRule j = new BuildTimeOutJenkinsRule();
    private final static long TINY_DELAY = 100L;
    private final static long HUGE_DELAY = 5000L;

    @Before
    public void before() {
        // this allows timeout shorter than 3 minutes.
        BuildTimeoutWrapper.MINIMUM_TIMEOUT_MILLISECONDS = 100;
    }

    @Test
    public void testTimeoutWasNotTriggered() throws Exception {
        final FreeStyleProject project = createProjectWithBuildStepWithTimeout(TINY_DELAY, null);

        final FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();

        j.assertBuildStatusSuccess(build);
        j.assertLogContains("Test", build);
    }

    @Test
    public void testTimeoutWasTriggeredWithoutAction() throws Exception {
        final FreeStyleProject project = createProjectWithBuildStepWithTimeout(HUGE_DELAY, null);

        final FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();

        j.assertBuildStatus(Result.ABORTED, build);
        assertLogDoesNotContain("Test", build);
    }


    @Test
    public void testTimeoutWasTriggeredWithAbortOperation() throws Exception {
        final FreeStyleProject project = createProjectWithBuildStepWithTimeout(HUGE_DELAY, new AbortOperation());

        final FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();

        j.assertBuildStatus(Result.ABORTED, build);
        assertLogDoesNotContain("Test", build);
    }

    @Test
    public void testTimeoutWasTriggeredWithFailOperation() throws Exception {
        final FreeStyleProject project = createProjectWithBuildStepWithTimeout(HUGE_DELAY, new FailOperation());

        final FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();

        j.assertBuildStatus(Result.FAILURE, build);
        assertLogDoesNotContain("Test", build);
    }

    private FreeStyleProject createProjectWithBuildStepWithTimeout(long delay, BuildTimeOutOperation operation) throws IOException {
        final FreeStyleProject project = j.createFreeStyleProject();
        final List<BuildTimeOutOperation> operations;

        if (operation!=null) {
            operations = new ArrayList<BuildTimeOutOperation>();
            operations.add(operation);
        }
        else {
            operations = null;
        }

        final Builder step = new BuildStepWithTimeout(new FakeBuildStep(delay),
                new QuickBuildTimeOutStrategy(500), operations);

        project.getBuildersList().add(step);

        return project;
    }

    private static void assertLogDoesNotContain(String text, FreeStyleBuild build) throws IOException {
        assertFalse(IOUtils.toString(build.getLogReader()).contains(text));
    }
}
