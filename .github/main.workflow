workflow "New workflow" {
  on = "push"
  resolves = ["Build Docker Image"]
}

action "Build Docker Image" {
  uses = "docker://docker:stable"
  args = ["build", "-t", "navikt/miaindekserer:$GITHUB_SHA", "."]
}
