;m Copyright © 2021 Atomist, Inc.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns atomist.main
  (:require [atomist.api :as api]
            [atomist.async :refer-macros [<? go-safe]]
            [atomist.cljs-log :as log]
            [atomist.git :as git]
            [atomist.github :as github]
            [atomist.gitflows :as gitflows]
            atomist.local-runner
            [cljs-node-io.core :as io]
            [cljs.core.async :as async :refer-macros [go] :refer [<!]]
            [clojure.string :as str]
            [goog.string :as gstring]
            ["micromatch" :as mm]))

(defn params->json-rules [handler]
  (fn [request]
    (go-safe (<? (assoc request
                        :atomist/branch-protection-rules
                        (merge
                         {:required_status_checks (if (:required_status_checks request)
                                                    {:strict true
                                                     :contexts (:required_status_checks request)})
                          :enforce_admins (or (:enforce_admins request) false)
                          :required_pull_request_reviews (when (or (:required_approving_review_count request)
                                                                   (:require_code_owner_reviews request)
                                                                   (:dismiss_stale_reviews request))
                                                           (merge
                                                            {}
                                                            (when (:required_approving_review_count request)
                                                              (select-keys request [:required_approving_review_count]))
                                                            (when (:require_code_owner_reviews request)
                                                              (select-keys request [:require_code_owner_reviews]))
                                                            (when (:dismiss_stale_reviews request)
                                                              (select-keys request [:dismiss_stale_reviews]))))

                          :restrictions nil}
                         (when (:required_linear_history request) (select-keys request [:required_linear_history]))
                         (when (:allow_force_pushes request) (select-keys request [:allow_force_pushes]))
                         (when (:allow_deletions request) (select-keys request [:allow_deletions]))))))))

(defn converge-branch-rules
  "converge branch rules on one branch ref (in one repo)"
  [request]
  (go-safe
   (when (and (:branchPatterns request)
              (.isMatch mm (-> request :ref :branch) (->> (:branchPatterns request)
                                                          (map str/trim)
                                                          (apply array))))
     (<? (github/branch-protection-rule
          request
          (-> request :ref :owner)
          (-> request :ref :repo)
          (-> request :ref :branch)
          (:atomist/branch-protection-rules request)))
     (<? (github/branch-requires-signed-commits
          request
          (-> request :ref :owner)
          (-> request :ref :repo)
          (-> request :ref :branch)
          (:required_signed_commits request))))))

(defn ->ref [m]
  (let [{branch :git.ref/name
         {repo :git.repo/name
          {token :github.org/installation-token
           owner :git.org/name} :git.repo/org} :git.ref/repo} m]
    {:token token
     :ref {:owner owner
           :repo repo
           :branch branch}}))

(defn on-push [handler]
  (fn [request]
    (go-safe
     (let [[branch-ref] (-> request :subscription :result first)
           {{:keys [owner repo branch]} :ref :as m} (->ref branch-ref)]
       (<? (converge-branch-rules
            (merge request m)))
       (<? (handler (assoc request
                           :atomist/status
                           {:code 0
                            :reason (gstring/format "converged rules on push of %s for %s/%s" branch owner repo)})))))))

(defn on-config-change [handler]
  (fn [request]
    (go-safe
     (<? (->> (for [[branch-ref n v] (-> request :subscription :result)
                    :let [{:keys [owner repo branch] :as m} (->ref branch-ref)]]
                (go-safe
                 (log/infof "Updating %s to %s on branch %s of %s/%s" n v branch owner repo)
                 (<? (converge-branch-rules (merge request m)))))
              (async/merge)
              (async/reduce conj [])))
     (<? (handler (assoc request
                         :atomist/status
                         {:code 0
                          :reason (gstring/format "converged branch rules on config change")}))))))

(defn ^:export handler
  [data sendreponse]
  (api/make-request
   data
   sendreponse
   (-> (api/finished)
       ;; please try to keep the order alphabetical to match default file-listings in datalog/subscription directory
       (api/mw-dispatch {:config-change.edn (-> #(go %)
                                                (on-config-change))
                         :on-push.edn (-> #(go %)
                                          (on-push))
                         :rescan-schedule (-> #(go %))
                         :sync (-> #(go %))})
       (params->json-rules)
       (api/add-skill-config)
       (api/log-event)
       (api/status))))

