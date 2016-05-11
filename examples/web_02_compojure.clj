(ns web-02-compojure)



;;; compojure

(require '[compojure.core :refer (ANY PUT GET POST defroutes)])

(defn echo-request
  [req]
  (str req))

(defn request-with-thing
  [thing]
  (fn [req]
    (println "I'm about thing")
    (pprint req)))

(defroutes example-routes
  (GET "/" [] echo-request)
  (POST "/:whatever/" [] echo-request)
  (ANY "/users/:id" [id] (request-with-thing id)))


(require '[com.stuartsierra.component :as component])

(defn handler-expecting-app
  [app id req]
  (let [db (:db app)
        email (:email app)]
    "presumably I use the db here to get stuff"))

(defn candidate-handler
  [app id]
  "business logic here"
  )

(defn candidate-from-list
  [app list-id list-index]
  "get the list"
  "get the id from the index"
  (candidate-handler app id)
  )

(defroutes jobseekers-routes
  (GET "/:id" [app id] (candidate-handler app id))
  (GET "/:list-id/:list-index" [app list-id list-index] (candidate-from-list app list-id list-index))
  (PUT "/:id/profile/:field")
  )


(defroutes
  ;;(cj/context "/jobseekers/" [] joseekers-routes)
  (ANY "/jobseekers/*" [] jobseekers-resource)


  )




(defn wrap-app
  [h app]
  (fn [req]
    (h (assoc req :app app))))

(defrecord web-app
    [config db mq email]
  )

(defn make-handler
  [web-app]
  (wrap-app api-service web-app))
