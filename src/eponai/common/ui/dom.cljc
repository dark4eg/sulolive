(ns eponai.common.ui.dom
  (:require
    [eponai.common.ui.elements.css :as css]
    [om.dom :as dom]
    [taoensso.timbre :refer [debug]]
    #?(:cljs [goog.object :as gobj])))

(defn add-keys-to-content [content]
  (letfn [(get-key [x] #?(:cljs (gobj/get x "key")
                          :clj (:react-key x)))
          (set-key [x v] #?(:cljs (.cloneElement js/React x #js {:key v})
                            :clj (assoc x :react-key v)))]
    (->> content
         (map-indexed (fn [i x]
                        (when x
                          (let [v (get-key x)]
                            (cond-> x (nil? v) (set-key (str i))))))))))

(defn element [el-fn {:keys [classes] :as opts} content]
  (let [react-opts (-> opts
                       (dissoc :classes)
                       (assoc :className (css/keys->class-str classes))
                       #?(:cljs clj->js))
        content (flatten content)]
    (condp = (count content)
      0 (el-fn react-opts)
      1 (el-fn react-opts (first content))
      (apply el-fn react-opts (add-keys-to-content content)))))

(defn div [opts & content]
  (element dom/div opts content))

(defn a [opts & content]
  (element dom/a opts content))

(defn li [opts & content]
  (element dom/li opts content))

(defn ul [opts & content]
  (element dom/ul opts content))

(defn strong [opts & content]
  (element dom/strong opts content))

(defn i [opts & content]
  (element dom/i opts content))

(defn span [opts & content]
  (element dom/span opts content))

(defn label [opts & content]
  (element dom/label opts content))

(defn h3 [opts & content]
  (element dom/h3 opts content))

(defn input [opts & content]
  (element dom/input opts content))