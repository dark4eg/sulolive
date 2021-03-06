(ns eponai.common.ui.elements.menu
  (:require
    [eponai.common.ui.elements.css :as css]
    [eponai.common.ui.dom :as dom]))

;; Menu elements
(defn- menu*
  "Custom menu element with provided content. For the provided content it's
  recommended to use any if the item- functions to generate compatible elements.
  Opts
  :classes - class keys to apply to this menu element.

  See css.cljc for available class keys."
  [opts & content]
  (apply dom/ul (css/add-class ::css/menu opts) content))

(defn tabs [opts & content]
  (apply menu* (css/add-class ::css/tabs opts) content))

(defn breadcrumbs [opts & content]
  (apply dom/ul (css/add-class :css/breadcrumbs opts) content))

(defn horizontal
  "Menu in horizontal layout.

  See menu* for general opts and recommended content."
  [opts & content]
  (apply menu* opts content))

(defn vertical
  "Menu in vertical layout.
  See menu* for general opts and recommended content."
  [opts & content]
  (apply menu* (css/add-class ::css/vertical opts) content))

;; Menu list item elements
(defn- item* [opts & content]
  (apply dom/li opts content))

(defn item
  "Custom menu item containing the provided content.

  Opts
  :classes - what class keys should be added to this item.

  See css.cljc for available class keys."
  [opts & content]
  (apply item* opts content))

(defn item-tab
  "Menu item representing a tab in some sort of stateful situation.

  Opts
  :is-active? - Whether this tab is in an active state.

  See item for general opts."
  [{:keys [is-active?] :as opts} & content]
  (item*
    (cond->> (css/add-class ::css/tabs-title (dissoc opts :is-active?))
             is-active?
             (css/add-class ::css/is-active))
    content))

(defn item-link
  "Menu item containing an anchor link.

  Opts
  :href - href for the containng anchor

  See item for general opts."
  [opts & content]
  (item*
    (select-keys opts [:key])
    (dom/a (dissoc opts :key) content)))

(defn item-dropdown
  "Menu item containg a link that opens a dropdown.
  Accepts a :dropdown key in opts containing the actual dropdown content element."
  [{:keys [dropdown href onClick classes]} & content]
  (item*
    {:classes (conj classes ::css/menu-dropdown)}
    (dom/a {:href href :onClick onClick} content)
    dropdown))

(defn item-text
  "Menu item element containing text only.

  See item for general opts."
  [opts & content]
  (apply item* (css/add-class ::css/menu-text opts) content))