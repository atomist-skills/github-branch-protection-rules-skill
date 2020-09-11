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

var convergeBranchRules = async (request, repo, branch) => {
  if (
    request.topic &&
    repo.topics &&
    repo.topics.includes(request.topic) &&
    request.branchPattern &&
    branch.match(new RegExp(request.branchPattern))
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
