(ns web-01-ring)

;; to be evaled in repl in order

(require 'dev)

(in-ns 'dev)

(go)

(require '[clojure-web-stack.account :as account]
         '[clojure-web-stack.task :as task]
         '[clojure-web-stack.user :as user]
         '[ring.adapter.jetty :as jetty]
         '[ring.mock.request :as mr]
         '[ring.util.response :as res]
         '[ring.util.codec :as codec]
         '[clojure.pprint :refer (pprint)]
         '[compojure.core :as cj]
         '[datomic.api :as d])

;;; abstractions. this is level 1!
;; bear in mind this is LOW LEVEL

;;; request maps

;; can generate them with ring.mock.request/request

(defn example-request
  []
  (mr/request :post "/resources/"
              {"name" "Admiral Snufflington"
               "type" "cat"
               "wearing" "socks"}))

(keys (example-request))

(pprint (example-request))

;; notice that the body is an inputstream - that's annoying! We'll see
;; what we can do about that later.
;; (this is also why `example-request` is a function - the inputstream
;; is mutable/read once)

(slurp (:body (example-request)))

;;; response maps

{:status 200 ; some HTTP status code
 :headers {} ; map of string => string|[string]
 :body "" ; string, [string], file, inputstream
 }

;; Spec for what's in request/response is here:
;; https://github.com/ring-clojure/ring/blob/master/SPEC

;;; handlers
;; take a request map, return a response map

(defn hello-world-handler
  [req]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body "Hello world"})

(pprint (hello-world-handler (example-request)))

(defn uri-echoer
  [req]
  (let [uri (:uri req)]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (str "This is " uri)}))

(pprint (uri-echoer (example-request)))

(defn post-params-echoer
  [req]
  (let [params (codec/form-decode (slurp (:body req)))]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (str "post params: " params)}))

(pprint (post-params-echoer (example-request)))

;; it's annoying to generate these maps by hand. here's a helper.

(defn simple-response
  [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

;; example of an API-type handler
;; we'll use the data-layer code that already exists in our example
;; project here for searching for tasks by title

(defn search-tasks
  [req]
  (let [search (get (codec/form-decode (slurp (:body req))) "search" "")
        results (task/search-title (d/db conn) "janed" search)]
    (simple-response (str "<pre>" (with-out-str (pprint results)) "</pre>"))))

(pprint (search-tasks (mr/request :post "/search/whatever" {:search "pack"})))

;; we can even run a server

(defonce server-handle (atom nil))

(defn server
  [handler]
  (when-let [s @server-handle]
    (.stop s)
    (reset! server-handle nil))
  (reset! server-handle (jetty/run-jetty handler {:port 8080 :join? false})))

;;; middlewares

;; middleware takes a handler, returns a handler
;; used to transform requests/responses

;; simple example - add headers and status to the response if not present

(defn wrap-basic-response
  [h]
  (fn [req]
    (let [response (h req)
          response-map (if (map? response)
                         response
                         {:body response})]
      (cond-> response-map
        (not (:status response))
        (assoc :status 200)
        (not (:headers response))
        (assoc :headers {"Content-Type" "text/html; charset=utf8"})))))

(defn uri-echoer
  [req]
  (:uri req))

(pprint ((wrap-basic-response uri-echoer) (example-request)))

(defn uri-echoer-with-some-stuff
  [req]
  (let [uri (:uri req)]
    {:status 201
     :body uri}))

(pprint ((wrap-basic-response uri-echoer-with-some-stuff) (example-request)))

;; ring comes with one to solve the annoying params parsing from body thing we were doing before

;; let's see how it works

(require '[ring.middleware.params :as params])

(defn echo-request
  [req]
  req)

;; merges several different types of params into :params key

(def echo-with-params
  (params/wrap-params ;; this is the middleware
   echo-request ;; takes the handler
   ))

;; return value is a new handler

(pprint (echo-with-params (example-request)))

(pprint (echo-with-params (mr/request :get "/hello/world" {:query-string "param"})))


;; more complex middleware - session handling
;; session middleware included with ring

(require '[ring.middleware.session :as session])

(defn print-request
  [req]
  (pprint req)
  (simple-response "Hello"))

(def print-with-session
  (session/wrap-session
   print-request))

(pprint (print-with-session (example-request)))

;; notice cookie setting and :session/key

;; now let's try using it a bit more

(defn set-session-stuff
  [req]
  (println "Request:")
  (pprint req)
  (assoc (simple-response "Hello")
         :session {:hello "world"}))

(let [handler (session/wrap-session set-session-stuff)
      first-response (handler (example-request))
      _ (println "Response:")
      _ (pprint first-response)
      cookie (first (get-in first-response [:headers "Set-Cookie"]))]
  (handler (assoc-in (example-request) [:headers "cookie"] cookie)))


;; conventionally, middlewares are named `wrap-<thing>`
