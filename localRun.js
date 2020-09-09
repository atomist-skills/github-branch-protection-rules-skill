var lr = require('@atomist/api-cljs/atomist.local_runner');
var main = require('./index.js');
var fs = require("fs");

lr.setEnv("prod-github-auth");

var fakePushEvent = lr.addConfiguration(
  lr.fakePushEvent("AEIB5886C", "slimslender", {name: "clj1", id: "AEIB5886C_AEIB5886C_slimslender_132627478"}, "master"),
  {name: "default", parameters: [{name: "topic", 
                                  value: "clojure"},
                                 {name: "branchPattern",
                                  value: "master"},
                                 {name: "allow_force_pushes",
                                  value: true},
                                 {name: "required_approving_review_count",
                                  value: 1},
                                 {name: "required_status_checks",
                                  value: ["github-secret-scanner-skill"]},
                                 {name: "required_linear_history",
                                  value: false}]}
);

lr.callEventHandler( 
  fakePushEvent,
  main.handler);
