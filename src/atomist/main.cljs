(ns atomist.main
  (:require [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [<! >! timeout chan]]
            [clojure.string :as s]
            [goog.string :as gstring]
            [http.client :as client]
            [goog.string.format]
            [atomist.cljs-log :as log]
            [atomist.api :as api]
            [atomist.github :as github]
            [clojure.core.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn converge-rules [handler]
  (fn [{:keys [labels] :as request}]
    (go
     (log/info "converge " (:ref request) labels)
     (<! (handler request)))))

(defn check-rules [handler]
  (fn [request]
    (handler request)))

(defn ^:export handler
  "handler
    must return a Promise - we don't do anything with the value
    params
      data - Incoming Request #js object
      sendreponse - callback ([obj]) puts an outgoing message on the response topic"
  [data sendreponse]
  (api/make-request
   data
   sendreponse
   (fn [request]
     (cond

       (contains? (:data request) :Repo)
       ((-> (api/finished :message "adding standard branch protection rules to new Repo"
                          :success "adding standard branch protection rules to new Repo")
            (converge-rules)
            (api/extract-github-token)
            (api/create-ref-from-repo-event)
            (check-rules)
            (api/add-skill-config :Rules :RepoFilter)) request)

       (contains? (:data request) :OnSchedule)
       ((-> (api/finished :message "handling scheduled sync" :success "successfully handled OnSchedule event")
            (api/repo-iterator (converge-rules
                                (fn [r]
                                  (go
                                   (try
                                     (log/info "first send the labels and refs " (:ref r))
                                     (select-keys r [:labels :ref])
                                     (catch :default ex
                                       (log/error ex "failed to lookup")
                                       {}))))))
            (check-rules)
            (api/add-skill-config :Labels :RepoFilter)) request)

       :else
       (go
        (log/errorf "Unrecognized event %s" request)
        (api/finish request))))))
