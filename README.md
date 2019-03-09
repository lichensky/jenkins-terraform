# jenkins-terraform

Plugin which allows to run Terraform plans from Jenkins Pipeline.

## Example

Apply Terraform configuration:

```groovy
node() {
    stage('Apply infrastructure changes') {
        terraform projectPath: 'terraform',
                  command: 'apply',
                  variablesFile: 'variables.tfvars'
    }
}
```

Plan with aprooval:

```groovy
node() {
    stage('Terraform plan') {
        def planFile = terraform projectPath: 'terraform',
                                 command: 'plan',
                                 workspace: 'staging',
                                 variablesFile: 'variables/staging.tfvars'
    }

    stage('Wait for aprooval') {
        input('Ready to go?')
    }

    stage('Apply generated plan') {
        terraform projectPath: 'terraform',
                  command: 'plan',
                  workspace: 'staging',
                  variablesFile: 'variables/staging.tfvars',
                  planFile: planFile
    }
}
```
