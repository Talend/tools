# Guidelines Enforcer

## Goals

Enforce our pull requests guidelines.

## Usage

The enforcer is deployed as an [AWS Lambda function](https://aws.amazon.com/lambda/) triggered using [Github Webhooks](https://developer.github.com/webhooks/).
Once checked it updates the [pull request status](https://developer.github.com/v3/repos/statuses/)
enabling the repository owner to block pull request merge on failures.

Example: [ui_pull_request_checks](https://console.aws.amazon.com/lambda/home?region=us-east-1#/functions/ui_pull_request_checks)

## Install

If you plan to hack on the enforcer you’ll need a working [Go](https://golang.org) environment with [dep](https://github.com/golang/dep) installed.  
Then run `go get github.com/Talend/tools/enforcer` to download the source code. 
Once downloaded run `dep ensure` to fetch all the dependencies.

Then you’re all set up. You can then run `go test .` to test the enforcer or `./build.sh` to build it as an [AWS Lambda function](https://docs.aws.amazon.com/lambda/latest/dg/lambda-go-how-to-create-deployment-package.html).