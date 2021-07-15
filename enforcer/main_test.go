package main

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"reflect"
	"testing"

	"github.com/aws/aws-lambda-go/events"
	"github.com/google/go-github/github"
	"github.com/pkg/errors"
)

func TestMain(m *testing.M) {
	minPRReviewers = "2"
	owner = "test"
	repository = "test"
	githubToken = "testToken"

	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/repos/test/test/pulls/0/reviews" {
			fmt.Fprint(w, `[
			{"id": 0, "state": "APPROVED", "user": {"id": 0}},
			{"id": 1, "state": "APPROVED", "user": {"id": 1}}
      ]`)
		} else if r.URL.Path == "/repos/test/test/pulls/1/reviews" {
			fmt.Fprint(w, `[
			{"id": 0, "state": "APPROVED", "user": {"id": 0}},
			{"id": 1, "state": "APPROVED", "user": {"id": 0}},
			{"id": 2, "state": "CHANGES_REQUESTED", "user": {"id": 1}}
      ]`)
		}
		fmt.Fprintln(w, "")
	}))
	defer ts.Close()
	statusBaseURL = ts.URL
	testURL, err := url.Parse(ts.URL + "/")
	if err != nil {
		panic(err)
	}
	githubClient.BaseURL = testURL
	os.Exit(m.Run())
}

func TestCheckForError(t *testing.T) {
	t.Run("No Error", func(t *testing.T) {
		expected := events.APIGatewayProxyResponse{StatusCode: http.StatusOK}
		resp := checkForError(nil)
		if resp.StatusCode != expected.StatusCode {
			t.Errorf("Expected %+v got %+v\n", expected, resp)
		}
	})

	t.Run("Basic errors", func(t *testing.T) {
		expected := events.APIGatewayProxyResponse{
			StatusCode: http.StatusInternalServerError,
		}
		resp := checkForError(errors.New("Test error"))
		if resp.StatusCode != expected.StatusCode {
			t.Errorf("Expected %+v got %+v\n", expected, resp)
		}
	})
}

func TestPostStatus(t *testing.T) {
	status := &pullRequestStatus{}
	err := status.post("testSHA")
	if err != nil {
		t.Error(err)
	}
}

func TestCheckApprovals(t *testing.T) {
	t.Run("Default", func(t *testing.T) {
		pullRequestID := 0
		pullRequest := &github.PullRequest{Number: &pullRequestID}
		status, err := checkApprovals(pullRequest)
		if err != nil {
			t.Error(nil)
		}
		if status.State != "success" {
			t.Error("Approvals check should be successful")
		}
	})

	t.Run("Handle multiple reviews from one user", func(t *testing.T) {
		pullRequestID := 1
		pullRequest := &github.PullRequest{Number: &pullRequestID}
		status, err := checkApprovals(pullRequest)
		if err != nil {
			t.Error(err)
		}
		if status.State != "failure" {
			t.Error("Approvals check should fail")
		}
	})
}

func TestCheckTitle(t *testing.T) {
	status, err := checkTitle(&github.PullRequest{})
	if err != nil {
		t.Error(err)
	}
	if status.State != "success" {
		t.Error("Should be succcessful check")
	}
}

func TestCheckPullRequest(t *testing.T) {
	event := &github.PullRequestEvent{}
	if err := checkPullRequest(event.PullRequest); err != nil {
		t.Error(err)
	}
}

func TestProcessPullRequestEvent(t *testing.T) {
	handledEventActions := []string{"opened", "edited", "synchronize"}
	prEventTests := []struct {
		title string
		event *github.PullRequestEvent
		err   error
	}{
		{
			title: handledEventActions[0],
			event: &github.PullRequestEvent{Action: &handledEventActions[0]},
			err:   nil,
		},
		{
			title: handledEventActions[1],
			event: &github.PullRequestEvent{Action: &handledEventActions[1]},
			err:   nil,
		},
		{
			title: handledEventActions[2],
			event: &github.PullRequestEvent{Action: &handledEventActions[2]},
			err:   nil,
		},
	}
	for _, test := range prEventTests {
		t.Run(test.title, func(t *testing.T) {
			err := processPullRequestEvent(test.event)
			if err != test.err {
				t.Errorf("Expected %s got %s\n", test.err, err)
			}
		})
	}
}

func TestProcessPullRequestReviewEvent(t *testing.T) {
	number := 0
	event := &github.PullRequestReviewEvent{
		PullRequest: &github.PullRequest{
			Number: &number,
		},
	}
	if err := processPullRequestReviewEvent(event); err != nil {
		t.Error(err)
	}
}

func TestProcessStatusEvent(t *testing.T) {
	name := "test_name"
	event := &github.StatusEvent{
		Name: &name,
	}

	if err := processStatusEvent(event); err != nil {
		t.Error(err)
	}
}

func TestWebhookHandler(t *testing.T) {
	testCases := []struct {
		title    string
		request  events.APIGatewayProxyRequest
		response events.APIGatewayProxyResponse
		err      error
	}{
		{
			title:    "Empty request",
			request:  events.APIGatewayProxyRequest{},
			response: events.APIGatewayProxyResponse{StatusCode: http.StatusBadRequest},
			err:      errEmptyEventTypeHeader,
		},
		{
			title: "Unkown event",
			request: events.APIGatewayProxyRequest{
				Headers: map[string]string{eventTypeHeader: "unknown"},
				Body:    "{}",
			},
			response: events.APIGatewayProxyResponse{StatusCode: http.StatusBadRequest},
			err:      errParseWebook,
		},

		{
			title: "PullRequest event",
			request: events.APIGatewayProxyRequest{
				Headers: map[string]string{eventTypeHeader: "pull_request"},
				Body:    "{}",
			},
			response: events.APIGatewayProxyResponse{StatusCode: http.StatusOK},
			err:      nil,
		},
		{
			title: "PullRequestReview event",
			request: events.APIGatewayProxyRequest{
				Headers: map[string]string{eventTypeHeader: "pull_request_review"},
				Body:    "{}",
			},
			response: events.APIGatewayProxyResponse{StatusCode: http.StatusOK},
			err:      nil,
		},
		{
			title: "Status event",
			request: events.APIGatewayProxyRequest{
				Headers: map[string]string{eventTypeHeader: "status"},
				Body:    "{}",
			},
			response: events.APIGatewayProxyResponse{StatusCode: http.StatusOK},
			err:      nil,
		},
	}

	for _, test := range testCases {
		t.Run(test.title, func(t *testing.T) {
			response, err := webhookHandler(test.request)
			if err != test.err {
				t.Errorf("Expected error %s got %s", test.err, err)
			}
			if !reflect.DeepEqual(response, test.response) {
				t.Errorf("Expected response %+v got %+v", test.response, response)
			}
		})
	}
}
