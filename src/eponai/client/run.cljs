(ns eponai.client.run
  (:require
    [eponai.client.utils :as utils]
    [eponai.client.auth :as auth]
    [eponai.common.parser :as parser]
    [eponai.client.parser.read]
    [eponai.client.parser.mutate]
    [eponai.client.parser.merge :as merge]
    [eponai.client.backend :as backend]
    [eponai.client.remotes :as remotes]
    [eponai.client.reconciler :as reconciler]
    [medley.core :as medley]
    [goog.dom :as gdom]
    [om.next :as om :refer [defui]]
    [taoensso.timbre :refer [error debug warn]]
    ;; Routing
    [cemerick.url :as url]
    [bidi.bidi :as bidi]
    [pushy.core :as pushy]
    [eponai.client.local-storage :as local-storage]
    [eponai.client.routes :as routes]
    [eponai.common.routes :as common.routes]
    [eponai.common.ui.router :as router]))

(defn update-route-fn [reconciler-atom]
  (fn [{:keys [handler route-params] :as match}]
    (let [r @reconciler-atom]
      (try (routes/transact-route! r handler {:route-params route-params
                                              :queue?       (some? (om/app-root r))})
           (catch :default e
             (debug "Tried to set route: " match ", got error: " e))))))

(defn apply-once [f]
  (let [appliced? (atom false)]
    (fn [x]
      (if @appliced?
        x
        (do (reset! appliced? true)
            (f x))))))


(defn wrap-route-logging [route-matcher]
  (fn [url]
    (let [match (route-matcher url)]
      (if (some? (:handler match))
        (debug "Matched url: " url " to route handler: " (:handler match) " whole match:" match)
        (warn "Could not match url: " url " to any route."))
      match)))

(defn set-current-route! [history update-route-fn]
  (let [match-all-routes (partial bidi/match-route common.routes/routes)
        match (match-all-routes (pushy/get-token history))]
    (update-route-fn match)))

(defonce history-atom (atom nil))

(defn- run [{:keys [auth-lock]
             :or   {auth-lock (auth/auth0-lock)}}]
  (let [init? (atom false)
        reconciler-atom (atom nil)
        _ (when-let [h @history-atom]
            (pushy/stop! h))
        match-route (partial bidi/match-route (common.routes/without-coming-soon-route common.routes/routes))
        update-route! (update-route-fn reconciler-atom)
        history (pushy/pushy update-route! (wrap-route-logging match-route))
        _ (reset! history-atom history)
        conn (utils/create-conn)
        parser (parser/client-parser)
        remote-config (reconciler/remote-config conn)
        send-fn (backend/send! reconciler-atom
                               ;; TODO: Make each remote's basis-t isolated from another
                               ;;       Maybe protocol it?
                               remote-config
                               {:did-merge-fn (apply-once
                                                (fn [reconciler]
                                                  (when-not @init?
                                                    (reset! init? true)
                                                    (debug "First merge happened. Adding reconciler to root.")
                                                    (binding [parser/*parser-allow-remote* false]
                                                      (om/add-root! reconciler router/Router (gdom/getElement router/dom-app-id))))))
                                :query-fn     (apply-once (fn [q]
                                                            {:pre [(sequential? q)]}
                                                            (into [:datascript/schema] q)))})
        reconciler (reconciler/create {:conn conn
                                       :parser parser
                                       :ui->props (utils/cached-ui->props-fn parser)
                                       :history history
                                       :auth-lock auth-lock
                                       :send-fn send-fn
                                       :remotes (:order remote-config)})]

    (reset! reconciler-atom reconciler)
    (binding [parser/*parser-allow-remote* false]
      (pushy/start! history)
      ;; Pushy is configured to not work with all routes.
      ;; We ensure that routes has been inited
      (when-not (:route (routes/current-route reconciler))
        (set-current-route! history update-route!)))
    (utils/init-state! reconciler send-fn parser router/Router)))

(defn run-prod []
  (run {}))

(defn run-dev []
  (run {:auth-lock (auth/fake-lock)}))

(defn on-reload! []
  (run-dev))