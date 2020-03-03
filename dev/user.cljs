(ns user
  (:require [atomist.main]
            [atomist.cljs-log :as log]))

(enable-console-print!)

(def token (.. js/process -env -API_KEY_SLIMSLENDERSLACKS_STAGING))
(def github-token (.. js/process -env -GITHUB_TOKEN))

(defn fake-handler [& args]
  (log/info "args " args))

(comment
 ;; EVENT
 ;; - needs both API_KEY and github token in scmProvider credential
 (.catch
  (.then
   (atomist.main/handler #js {:data {:OnSchedule {}}
                              :secrets [{:uri "atomist://api-key" :value token}]
                              :configuration {:name "defaults"
                                              :parameters [{:name "Labels" :value ["bug:#FF0000:Bug"]}]}
                              :extensions {:team_id "AK748NQC5"}}
                         fake-handler)
   (fn [v] (log/info "value " v)))
  (fn [error] (log/info "error " error))))
