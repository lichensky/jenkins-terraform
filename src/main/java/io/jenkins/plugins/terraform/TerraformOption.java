package io.jenkins.plugins.terraform;

public enum TerraformOption {
    INPUT("input"),
    PLAN_OUT("out"),
    VAR_FILE("var-file");

    private String option;

    TerraformOption(String option) {
        this.option = option;
    }

    @Override
    public String toString() {
        return this.option;
    }
}
