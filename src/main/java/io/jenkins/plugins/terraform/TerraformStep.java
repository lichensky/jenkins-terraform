package io.jenkins.plugins.terraform;

import java.io.File;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;

import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.TaskListener;

public class TerraformStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(TerraformStep.class.getName());
    private String projectPath;
    private String command;
    private String workspace;
    private String variablesFile;
    private String planFile;
    private String binPath = "terraform";
    private Boolean forceInit = true;

    @DataBoundConstructor
    public TerraformStep(String projectPath, String command) {
        this.projectPath = projectPath;
        this.command = command;
    }

    @DataBoundSetter
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    @DataBoundSetter
    public void setVariablesFile(String variablesFile) {
        this.variablesFile = variablesFile;
    }

    @DataBoundSetter
    public void setForceInit(Boolean forceInit) {
        this.forceInit = forceInit;
    }

    @DataBoundSetter
    public void setPlanFile(String planFile) {
        this.planFile = planFile;
    }

    @DataBoundSetter
    public void setBinPath(String binPath) {
        this.binPath = binPath;
    }

    public String getCommand() {
        return this.command;
    }

    public String getProjectPath() {
        return this.projectPath;
    }

    public String getWorkspace() {
        return this.workspace;
    }

    public String getVariablesFile() {
        return this.variablesFile;
    }

    public Boolean getForceInit() {
        return this.forceInit;
    }

    public String getPlanFile() {
        return this.planFile;
    }

    public String getBinPath() {
        return this.binPath;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(this, context);
    }

    private static class Execution extends SynchronousNonBlockingStepExecution {
        private static final long serialVersionUID = 1L;
        private TerraformStep step;

        private Execution(TerraformStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Object run() throws Exception {
            FilePath workspacePath = this.getContext().get(FilePath.class);
            Launcher launcher = this.getContext().get(Launcher.class);
            TaskListener listener = this.getContext().get(TaskListener.class);
            FilePath projectPath = getAbsoluteProjectPath(this.step.getProjectPath(), workspacePath);
            ProcStarter starter = launcher.launch().pwd(projectPath);

            String binPath = this.step.getBinPath();
            TerraformCommand command = TerraformCommand.fromString(this.step.getCommand());
            Optional<String> variablesFile = Optional.ofNullable(this.step.getVariablesFile());
            Optional<String> workspace = Optional.ofNullable(this.step.getWorkspace());
            Optional<String> planFilePath = Optional.ofNullable(this.step.getPlanFile());

            TerraformCommand[] supportedCommands = {
                TerraformCommand.PLAN,
                TerraformCommand.APPLY,
                TerraformCommand.DESTROY
            };

            Boolean isValidCommand = Stream.of(supportedCommands).anyMatch(
                supportedCommand -> supportedCommand.equals(command)
            );

            if (!isValidCommand) {
                throw new Exception(String.format("Unsupported command: %s",
                    command.toString()));
            }

            TerraformRunner terraformRunner = new TerraformRunner(starter, listener, binPath);
            terraformRunner.setVariablesFile(variablesFile);

            try {
                if (this.step.getForceInit()) {
                    LOGGER.fine("Initializing Terraform.");
                    terraformRunner.init();
                }

                if (workspace.isPresent()) {
                    LOGGER.log(Level.FINE, "Selecting {0} workspace.", workspace.get());
                    terraformRunner.selectWorkspace(workspace.get());
                }

                if (command == TerraformCommand.DESTROY) {
                    LOGGER.fine("Destroying existing infrastructure.");
                    terraformRunner.destroy();
                    return true;
                }

                LOGGER.fine("Planning changes.");
                String planFile = planFilePath.orElse(terraformRunner.plan());
                LOGGER.log(Level.FINE, "Plan summary saved to {0}.", planFile);

                if (command == TerraformCommand.APPLY) {
                    LOGGER.fine("Applying changes to infrastructure.");
                    terraformRunner.apply(planFile);
                }

                return planFile;
            } catch (Exception e) {
                throw new AbortException(e.getMessage());
            }
        }

        private FilePath getAbsoluteProjectPath(String projectPath, FilePath workspacePath) {
            File path = new File(projectPath);
            if (path.isAbsolute()) {
                return new FilePath(path);
            } else {
                return workspacePath.child(projectPath);
            }
        }

    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override public String getDisplayName() {
            return "Run Terraform from pipeline.";
        }

        @Override
        public String getFunctionName() {
            return "terraform";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return false;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(FilePath.class, Launcher.class, TaskListener.class);
        }
    }
}