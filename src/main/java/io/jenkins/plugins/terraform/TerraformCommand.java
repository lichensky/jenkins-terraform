package io.jenkins.plugins.terraform;

public enum TerraformCommand {
    INIT("init"),
    PLAN("plan"),
    APPLY("apply"),
    DESTROY("destroy"),
    WORKSPACE("workspace"),
    SELECT("select");

    private String command;

    TerraformCommand(String command) {
        this.command = command;
    }

    public static TerraformCommand fromString(String command) {
        for (TerraformCommand terraformCommand : TerraformCommand.values()) {
            if (terraformCommand.command.equalsIgnoreCase(command)) {
                return terraformCommand;
            }
        }
        throw new IllegalArgumentException(String.format("%s is not valid command", command));
    }

    @Override
    public String toString() {
        return command;
    }
}