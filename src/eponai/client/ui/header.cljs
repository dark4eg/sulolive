(ns eponai.client.ui.header
  (:require [om.next :as om :refer-macros [defui]]
            [eponai.client.ui :refer-macros [opts]]
            [eponai.common.parser.mutate :as mutate]
            [eponai.client.ui.modal :refer [->Modal]]
            [eponai.client.ui.add_transaction :as add.t :refer [->AddTransaction]]
            [sablono.core :as html :refer-macros [html]]
            [garden.core :refer [css]]))

(defui Header
  static om/IQueryParams
  (params [this] {:add-transaction (om/get-query add.t/AddTransaction)})
  static om/IQuery
  (query [this]
    '[{:query/header [:db/id ::show-transaction-modal]}
      {:proxy/add-transaction ?add-transaction}])
  Object
  (render
    [this]
    (let [{:keys [query/header proxy/add-transaction]} (om/props this)
          {:keys [db/id ::show-transaction-modal]} (first header)
          modal-trigger #(mutate/cas! this id ::show-transaction-modal
                                      show-transaction-modal
                                      (not show-transaction-modal))]
      (html [:nav
             (opts {:style {:display         "flex"
                            :flex-wrap       "no-wrap"
                            :justify-content "space-between"}
                    :class "navbar navbar-default navbar-fixed-top topnav"
                    :role  "navigation"})

             [:div
              (opts {:class "navbar-brand"
                     :style {:display "flex"
                             :flex-direction "row"
                             :margin-right "5.0em"}})
              "JourMoney"]

             [:button
              (opts {:style {}
                     :on-click modal-trigger
                     :class "btn btn-primary btn-md"})
              "New Transaction"]
             [:div
              (opts {:style {:display        "flex"
                             :flex-direction "row-reverse"}})
              [:form
               {:action "/api/logout"
                :id "logout-form"
                :method "get"}]

              [:button
               {:class "btn btn-default btn-md"
                :type "submit"
                :form "logout-form"} "logout"]

              [:button
               {:class "btn btn-default btn-md"}
               "settings"]]

             (when show-transaction-modal
               (->Modal {:dialog-content #(->AddTransaction add-transaction)
                         :on-close       modal-trigger}))]))))
;(defui Header
;  static om/IQueryParams
;  (params [this] {:add-transaction (om/get-query add.t/AddTransaction)})
;  static om/IQuery
;  (query [this]
;         '[{:query/header [:db/id ::show-transaction-modal]}
;           {:proxy/add-transaction ?add-transaction}])                      ;; what to transact?
;  Object
;  (render
;    [this]
;    (let [{:keys [query/header proxy/add-transaction]} (om/props this)
;          {:keys [db/id ::show-transaction-modal]} (first header)
;          modal-trigger #(parser/cas! this id ::show-transaction-modal
;                              show-transaction-modal
;                              (not show-transaction-modal))]
;      (html [:div (style {:display         "flex"
;                          :flex-wrap       "no-wrap"
;                          :justify-content "space-between"})
;             [:div (style {:display "flex"
;                           :width   "33%"})
;              [:panel {:class "panel-heading"}
;               [:span {:class "network-name"} "JourMoney"]]]
;             [:button (merge (style {:width "33%"})
;                             {:on-click modal-trigger
;                              :class "btn btn-primary btn-md"})
;              "New Transaction"]
;             [:div (style {:display        "flex"
;                           :flex-direction "row-reverse"
;                           :width          "33%"})
;              [:button {:class "btn btn-default btn-md"} "logout"]
;              [:button  {:class "btn btn-default btn-md"} "settings"]]
;             (when show-transaction-modal
;               (->Modal {:dialog-content #(->AddTransaction add-transaction)
;                         :on-close       modal-trigger}))]))))

(def ->Header (om/factory Header))
