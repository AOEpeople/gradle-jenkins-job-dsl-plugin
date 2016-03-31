
job("simple-job") {
    description "Job for testing"

    steps {
        shell 'echo hello world'
    }
}
