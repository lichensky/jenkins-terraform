package io.jenkins.plugins.terraform;

public enum TerraformFlag {
    AUTO_APPROVE("-auto-approve"),
    NO_COLOR("-no-color");

    private String flag;

    TerraformFlag(String flag) {
        this.flag = flag;
    }

    @Override
    public String toString() {
        return this.flag;
    }
}
