# Contributing to the project

:+1::tada: First off, thanks for taking the time to contribute! :tada::+1:


If you plan to add some changes to the project, please keep in mind that code is not all. In fact, we would like you to follow 
some more steps to make sure that everything is done to prevent inconsistencies and other issues. Please, ascertain that:

- [ ] A [Pull Request](#pull-request) is created with 
  - valid [commit](#commit) messages
  - valid [branch](#branch-naming-conventions) names
  - rebase and [squash](#squash) to have 1 single commit message



## <a name="pull-request"></a> Pull Requests

Organize your commits to make your reviewer’s job easier. Reviewers normally prefer multiple small pull requests, instead of a 
single large pull request. 

Here is the workflow we are using for pull requests.

1. Submit your pull request. (PR name must match [commit name](#commit))
   => the CI should kick tests so you will have feedback about the different QA
2. If any issues are marked on the PR by CI please fix them. No broken PR will be reviewed.
3. Once the quality checks are OK, and you need your PR to be reviewed you must use the label **Need review**. If your PR is still a work in progress, do not use any label but this means no review will be performed.
4. The components owners will then identify who is going to do the review and then assign it. The reviewer will receive an automatic mail notification.
5. Once the reviewer starts the review, he/she should set the label **Reviewing**.
6. We use the new review system of GitHub so you will know if the reviewer request changes, approve it or just add some comments.
7. If any changes are requested please fix them and then once you are ready request a new review by ping the reviewer through GitHub.
8. Once the pull request is merged, please delete the branch.

Here are the label definitions for this workflow (label name : color code) :
* Need review                : #fef2c0
* Reviewing                  : #fbca04

## <a name="commit"></a> Commit Message Guidelines

We have a few rules over how our git commit messages should be formatted, in order to improve readability. 

### Commit Message Format
Each commit message consists of a **header**, a **body** and a **footer**.  The
header has a special
format that includes a **type**, a **scope** and a **subject**:

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

The **header** is mandatory and the **scope** of the header is optional.

Any line of the commit message cannot be longer 72 characters! This allows the message to be easier to read on GitHub as well as in various git tools.

#### Revert
If the commit reverts a previous commit, it should begin with `revert: `, followed by the header of the reverted commit.
In the body it should say: `This reverts commit <hash>.`, where the hash is the SHA of the commit being reverted.

#### Type
Must be one of the following:

* **feat**: A new feature
* **fix**: A bug fix
* **doc**: Documentation only changes
* **chore**: code refactoring, changes to the build process, auxiliary tools 
  and libraries such as documentation generation

#### Scope
The scope could be anything specifying place of the commit change. Most of the time it should be JIRA ticket reference followed by a useful context.

For example `TFD-66/webapp`, `TFD-67/cli`, `TPSVC-123`, `svc-datastore`, etc...

#### Subject
The subject contains succinct description of the change:

- Limit the subject line to 50 characters
- Capitalize the subject line
- Do not end the subject line with a period
- Use the imperative mood in the subject line

#### Body
Just as in the **subject**, use the imperative, present tense: "change" not "changed" nor "changes".
The body should include the motivation for the change and contrast this with previous behaviour.

- Wrap the body at 72 characters
- Use the body to explain what and why vs. how

#### Footer
The footer should contain

* any information about **Breaking Changes**
* [Smart commits](https://confluence.atlassian.com/fisheye/using-smart-commits-298976812.html) (aka JIRA commands)

**Breaking Changes** should start with the word `BREAKING CHANGE:` with a space or two newlines. The rest of the commit message is then used for this.

#### Examples

```
feat(TPSVC-38/webapp): analyse more flowchart libraries

* Add JointJS, JIT, JSNetworkX
* Add references on non OSS libs
* Remove canvg

#time 1h
#comments JointJS seems great
```

---

```
feat(TPSVC-66): add Swagger to Bookkeeper
```

---


## Branch naming conventions

Name your branch according to the following convention:

```username/JIRA-ID-short_description```

so that the branch can easily be traced back to you.

The first part of the branch name after ```username/``` should be the jira-id, followed by a hyphen and a short description. Use underscores to separate words in the short description.

The `@` character is forbidden, since this is not compatible with our CI tools.

## <a name="squash"></a> Squash

Within a pull request, a relatively small number of commits that break the problem into logical steps 
is preferred. For most pull requests, you’ll squash your changes down to 1 commit. You can use the following command to re-order, 
squash, edit, or change description of individual commits.

```git rebase -i <branch-name>```

You’ll then push to your branch on GitHub. 

Note: when updating your commit after pull request feedback, you should not force the push, otherwise you'll lose the comments from the Pull Request. In this case, you can simply pull, merge, and then push.

In addition to that, Git provides a tool to squash and merge at the bottom of the Pull Request page.

