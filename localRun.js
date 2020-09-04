var lr = require('@atomist/api-cljs/atomist.local_runner');
var main = require('./index.js');
var fs = require("fs");

lr.setEnv("prod-github-auth");

var fakePushEvent = lr.addConfiguration(
  lr.fakePushEvent("AEIB5886C", "slimslender", {name: "clj1", id: "AEIB5886C_AEIB5886C_slimslender_132627478"}, "slimtest1"),
  {name: "default", parameters: [{name: "repoConfig", 
                                  value: "{\"has_wiki\": false}"},
                                 {name: "topic", 
                                  value: "clojure"},
                                 {name: "branchPattern",
                                  value: "slimtest1"},
                                 {name: "branchProtectionRule",
                                  value: fs.readFileSync("rule.json").toString()}]}
);

lr.callEventHandler( 
  fakePushEvent,
  main.handler);
