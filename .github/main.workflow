workflow "New workflow" {
  on = "push"
  resolves = ["Tag Docker Image"]
}

action "Build Docker Image" {
  uses = "docker://docker:stable"
  args = ["build", "-t", "miaindekserer", "."]
}

action "Tag Docker Image" {
  needs = ["Build Docker Image"]
  uses = "actions/docker/cli@master"
  args = "tag miaindekserer navikt/miaindekserer:$GITHUB_SHA"
}
