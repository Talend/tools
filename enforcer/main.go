package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"sort"
	"strconv"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
	"github.com/google/go-github/github"
	"github.com/pkg/errors"
	"golang.org/x/oauth2"
)

const (
	webhookSecretKey = ""
	eventTypeHeader  = "X-GitHub-Event"

	// Errors
	errCreatingStatus = "Error creating status"
)

var (
	minPRReviewers = os.Getenv("MIN_PR_REVIEWERS")
	owner          = os.Getenv("OWNER")
	repository     = os.Getenv("REPOSITORY")
	githubToken    = os.Getenv("GITHUB_TOKEN")
	tc             = oauth2.NewClient(
		oauth2.NoContext,
		oauth2.StaticTokenSource(&oauth2.Token{AccessToken: githubToken}),
	)
	githubClient = github.NewClient(tc)

	statusBaseURL = "https://api.github.com"

	errParseWebook          = errors.New("Error parsing Github webhook")
	errEmptyEventTypeHeader = errors.New("Empty event type header")
	errEventNotHandled      = errors.New("Event not handled")
)

// TODO: Handle error better by providing the proper status code depending on
// error type
func checkForError(err error) events.APIGatewayProxyResponse {
	if err != nil {
		return events.APIGatewayProxyResponse{
			StatusCode: http.StatusInternalServerError,
		}
	}
	return events.APIGatewayProxyResponse{
		StatusCode: http.StatusOK,
	}
}

type pullRequestStatus struct {
	State       string `json:"state"`
	TargetURL   string `json:"target_url"`
	Description string `json:"description"`
	Context     string `json:"context"`
}

func (status *pullRequestStatus) post(commitSHA string) error {
	s, err := json.Marshal(status)
	if err != nil {
		return errors.Wrap(err, "Error marshalling pull request status")
	}
	url := fmt.Sprintf(
		"%s/repos/%s/%s/statuses/%s",
		statusBaseURL,
		owner,
		repository,
		commitSHA,
	)
	req, err := http.NewRequest("POST", url, bytes.NewReader(s))
	if err != nil {
		return errors.Wrap(err, "Error creating POST request")
	}
	req.Header.Set("Authorization", fmt.Sprintf("token %s", githubToken))
	req.Header.Set("Content-Type", "application/json")
	_, err = http.DefaultClient.Do(req)
	if err != nil {
		return errors.Wrap(err, errCreatingStatus)
	}
	log.Printf("Udating status: %s\n%s\n", url, s)
	return nil
}

type pullRequestCheck func(*github.PullRequest) (*pullRequestStatus, error)

// TODO: Invert status type
// start with a wrong status then change it to an OK one if condition checks
func checkApprovals(pullRequest *github.PullRequest) (*pullRequestStatus, error) {
	status := &pullRequestStatus{
		State:       "success",
		Description: "All checks are OK",
		Context:     "guidelines/reviews",
		TargetURL:   "https://github.com/Talend/tools/blob/master/tools-root-github/CONTRIBUTING.md",
	}

	ctx := context.Background()
	reviews, _, err := githubClient.PullRequests.ListReviews(ctx, owner, repository, pullRequest.GetNumber(), &github.ListOptions{})
	if err != nil {
		return status, errors.Wrap(err, "Can't get pull request reviews")
	}

	sort.Slice(reviews, func(i, j int) bool {
		return reviews[i].GetSubmittedAt().Before(reviews[j].GetSubmittedAt())
	})
	var approvedReviewsCount = 0
	var approvedUserReviews = make(map[int64]string)
	for _, review := range reviews {
		if review.GetState() == "APPROVED" &&
			approvedUserReviews[review.GetUser().GetID()] != "APPROVED" {
			approvedReviewsCount++
		}
		approvedUserReviews[review.GetUser().GetID()] = review.GetState()
	}

	minPullRequestApprovals, err := strconv.Atoi(minPRReviewers)
	if err != nil {
		return status, errors.Wrap(err, "Error converting MIN_PR_REVIEWERS")
	}
	if approvedReviewsCount < minPullRequestApprovals {
		status.Description = fmt.Sprintf("You need at least %d review approvals.", minPullRequestApprovals)
		status.State = "failure"
	}
	return status, nil
}

func checkTitle(pullRequest *github.PullRequest) (*pullRequestStatus, error) {
	status := &pullRequestStatus{
		State:       "success",
		Description: "Title is OK",
		Context:     "guidelines/title",
		TargetURL:   "https://github.com/Talend/tools/blob/master/tools-root-github/CONTRIBUTING.md",
	}
	return status, nil
}

func checkPullRequest(pullRequest *github.PullRequest) error {
	checks := []pullRequestCheck{
		checkTitle,
		checkApprovals,
	}
	for _, check := range checks {
		status, err := check(pullRequest)
		if err != nil {
			return errors.Wrap(err, "Error checking pull request")
		}
		err = status.post(pullRequest.GetHead().GetSHA())
		if err != nil {
			return errors.Wrap(err, "Error submitting pull request status")
		}
	}
	return nil
}

func processPullRequestEvent(event *github.PullRequestEvent) error {
	log.Printf("Processing pull request: %d - Action: %s\n", event.GetNumber(), event.GetAction())
	switch event.GetAction() {
	case "opened":
		return checkPullRequest(event.PullRequest)
	case "edited":
		return checkPullRequest(event.PullRequest)
	case "synchronize":
		return checkPullRequest(event.PullRequest)
	}
	return nil
}

func processPullRequestReviewEvent(event *github.PullRequestReviewEvent) error {
	log.Printf("Processing pull request review: %d\n", event.PullRequest.GetNumber())

	if event.GetAction() == "submitted" || event.GetAction() == "edited" {
		return checkPullRequest(event.PullRequest)
	}
	return nil
}

func processStatusEvent(event *github.StatusEvent) error {
	log.Printf("Processing status event: %s\n", event.GetName())
	return nil
}

func webhookHandler(request events.APIGatewayProxyRequest) (events.APIGatewayProxyResponse, error) {

	log.Printf("Processing Lambda request %s\n", request.RequestContext.RequestID)

	// TODO: Implement secret verification
	// payload, err := github.ValidatePayload(request, webhookSecretKey)
	// if err != nil {
	// 	return nil, err
	// }
	eventType := request.Headers[eventTypeHeader]
	if eventType == "" {
		return events.APIGatewayProxyResponse{
			StatusCode: http.StatusBadRequest,
		}, errEmptyEventTypeHeader
	}
	event, err := github.ParseWebHook(eventType, []byte(request.Body))
	if err != nil {
		return events.APIGatewayProxyResponse{
			StatusCode: http.StatusBadRequest,
		}, errParseWebook
	}
	switch event := event.(type) {
	case *github.PullRequestEvent:
		checkForError(processPullRequestEvent(event))
	case *github.PullRequestReviewEvent:
		checkForError(processPullRequestReviewEvent(event))
	case *github.StatusEvent:
		checkForError(processStatusEvent(event))
	default:
		break
	}
	return events.APIGatewayProxyResponse{
		StatusCode: http.StatusOK,
	}, nil
}

func main() {
	if minPRReviewers == "" {
		log.Fatal("MIN_PR_REVIEWERS must be set")
	}
	lambda.Start(webhookHandler)
}
