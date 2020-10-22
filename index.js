/*
 * Copyright © 2020 Atomist, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var api = require("@atomist/api-cljs/atomist.middleware");

var configureReviews = (request) => {
  if (request.required_approving_review_count || request.require_code_owner_reviews || request.dismiss_stale_reviews) { 
    return {
      ... (request.required_approving_review_count && {required_approving_review_count: request.required_approving_review_count}),
      ... (request.require_code_owner_reviews && {require_code_owner_reviews: request.require_code_owner_reviews}),
      ... (request.dismiss_stale_reviews && {dismiss_stale_reviews: request.dismiss_stale_reviews})
    };
  } else {
    return null;
  }
};

var checkBranchPattern = (pattern_string, branch) => {
  return pattern_string.split(",")
           .map(s => {return new RegExp(s.trim());})
           .map(re => {return branch.match(re);})
           .some(match => {return !!match;});
}

var convergeBranchRules = async (request, repo, branch) => {
  if (
    request.topic &&
    repo.topics &&
    repo.topics.includes(request.topic) &&
    request.branchPattern &&
    checkBranchPattern(request.branchPattern, branch)
  ) {
    await repo.branchProtectionRule(
      branch,
      {
         ... {required_status_checks: ((request.required_status_checks && {strict:true, contexts: request.required_status_checks}) || null)},
         ... {enforce_admins: (request.enforce_admins||false)},
         ... {required_pull_request_reviews: configureReviews(request)},
         ... {restrictions: null},
         ... (request.required_linear_history && {required_linear_history: request.required_linear_history}),
         ... (request.allow_force_pushes && {allow_force_pushes: request.allow_force_pushes}),
         ... (request.allow_deletions && {allow_deletions: request.allow_deletions})
      }
    );
    if (request.required_signed_commits) {
      await repo.branchRequiresSignedCommits(branch,true);
    } else {
      await repo.branchRequiresSignedCommits(branch, false);
    }
  }
  return true;
};

exports.handler = api.handler({
  OnAnyPush: async (request) => {
    await request.withRepo(async (repo) => {
      return await convergeBranchRules(
        request,
        repo,
        request.data.Push[0].branch
      );
    });
  },
  OnPullRequest: async (request) => {
    await request.withRepo(async (repo) => {
      return await convergeBranchRules(
        request,
        repo,
        request.data.PullRequest[0].baseBranchName
      );
    });
  },
  sync: api.scanBranches(async (request, repo, branch) => {
    return await api.eachConfig(request, async (req) => {
      return await convergeBranchRules(req, repo, branch);
    });
  }),
  OnSchedule: api.scanBranches(convergeBranchRules)
});
