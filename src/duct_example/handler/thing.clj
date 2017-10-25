(ns duct-example.handler.thing
  (:require [ataraxy.response :as response]
            [clojure.java.jdbc :as jdbc]
            [duct.database.sql]
            [integrant.core :as ig]))

(defprotocol Things
  (create-thing [db thing])
  (list-things [db])
  (fetch-thing [db id]))

(extend-protocol Things
  duct.database.sql.Boundary
  (create-thing [{db :spec} thing]
    (val (ffirst (jdbc/insert! db :things thing))))
  (list-things [{db :spec}]
    (jdbc/query db ["SELECT * FROM things"]))
  (fetch-thing [{db :spec} id]
    (first (jdbc/query db ["SELECT * FROM things WHERE id = ?" id]))))

(defmethod ig/init-key ::create [_ {:keys [db]}]
  (fn [{[_ thing] :ataraxy/result}]
    (let [id (create-thing db thing)]
      [::response/created (str "/things/" id)])))

(defmethod ig/init-key ::list [_ {:keys [db]}]
  (fn [_]
    [::response/ok (list-things db)]))

(defmethod ig/init-key ::fetch [_ {:keys [db]}]
  (fn [{[_ id] :ataraxy/result}]
    (if-let [thing (fetch-thing db id)]
      [::response/ok thing]
      [::response/not-found {:error :not-found}])))
