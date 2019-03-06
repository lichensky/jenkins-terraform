package io.jenkins.plugins.terraform;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import hudson.Proc;
import hudson.Launcher.ProcStarter;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

import io.jenkins.plugins.terraform.TerraformCommand;

public class TerraformRunner {
    private final ArgumentListBuilder args;
    private ProcStarter starter;
    private TaskListener listener;
    private Optional<String> varFile;

    public TerraformRunner(ProcStarter starter, TaskListener listener, String binPath) {
        this.starter = starter;
        this.listener = listener;
        this.args = new ArgumentListBuilder(binPath);
    }

    public void init() throws Exception {
        ArgumentListBuilder args = this.args.clone()
            .add(TerraformCommand.INIT)
            .add(TerraformFlag.NO_COLOR);
        run(args);
    }

    public void selectWorkspace(String workspace) throws Exception {
        ArgumentListBuilder args = this.args.clone()
            .add(TerraformCommand.WORKSPACE)
            .add(TerraformCommand.SELECT)
            .add(workspace)
            .add(TerraformFlag.NO_COLOR);
        run(args);
    }

    public String plan() throws Exception {
        String planFilePath = createPlanFile();
        ArgumentListBuilder args = this.args.clone()
            .add(TerraformCommand.PLAN)
            .add(TerraformFlag.NO_COLOR)
            .add(getInputOption())
            .add(getVariablesFileArg())
            .addKeyValuePair("-", TerraformOption.PLAN_OUT.toString(),
                planFilePath, false);
        run(args);
        return planFilePath;

    }

    public void apply(String planFile) throws Exception {
        ArgumentListBuilder args = this.args.clone()
            .add(TerraformCommand.APPLY)
            .add(TerraformFlag.NO_COLOR)
            .add(getInputOption())
            .add(getAutoApprove())
            .add(getVariablesFileArg())
            .add(planFile);
        run(args);
    }

    public void destroy() throws Exception {
        ArgumentListBuilder args = this.args.clone()
            .add(TerraformCommand.DESTROY)
            .add(TerraformFlag.NO_COLOR)
            .add(getInputOption())
            .add(getAutoApprove())
            .add(getVariablesFileArg());
        run(args);
    }

    public void setVariablesFile(Optional<String> varFile) {
        this.varFile = varFile;
    }

    private String[] getVariablesFileArg() {
        ArgumentListBuilder args = new ArgumentListBuilder();
        if (varFile.isPresent()) {
            args.addKeyValuePair("-", TerraformOption.VAR_FILE.toString(),
                varFile.get(), false);
        }
        return args.toCommandArray();
    }

    private String[] getAutoApprove() {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(TerraformFlag.AUTO_APPROVE);

        return args.toCommandArray();
    }

    private String[] getInputOption() {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.addKeyValuePair("-", TerraformOption.INPUT.toString(),
                "false", false);

        return args.toCommandArray();
    }


    private void run(ArgumentListBuilder args) throws Exception {
        this.starter.cmds(args);
        this.starter.stdout(listener);

        Proc proc = this.starter.start();
        int exitCode = proc.join();

        if (exitCode != 0) {
            throw new Exception("Terraform command failed!");
        }
    }

    private String createPlanFile() throws IOException {
        return File.createTempFile("plan", "").getAbsolutePath();
    }
}