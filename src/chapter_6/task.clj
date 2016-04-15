(ns chapter-6.task
  (:require [clojure.string :as str]
            [datomic.api :as d]
            [chapter-6.user :as user]))

(defn issue-id-from-title
  "Takes a db and task title, returns an issue-id using the first
   word in the title with a numeric postfix that does not already
   exist in the db."
  [db title]
  (->> (range)
       (map (partial str (.toUpperCase (first (str/split title #"\s"))) "-"))
       (remove (fn [issue-id] (d/entity db [:task/issue-id issue-id])))
       first))

(defn entity
  "Returns task entity from :db/id, string issue-id, or arg if already
   an entity."
  [db task]
  (cond (instance? datomic.query.EntityMap task) task
        (string? task) (d/entity db [:task/issue-id task])
        :else (d/entity db task)))

(defn create
  "Takes a Datomic connection, a user entity, and a map with task
   info, and attempts to create a task in the database. Returns
   transaction data. Task info map has keys:

   * :title (required)
   * :description
   * :status - one of :todo, :in-progress, :done [:todo]
   * :issue-id - defaults to the the word in title with a numeric
      postfix
   * :tags - a set of strings
   * :parent - task entity, issue id, or :db/id"
  [conn user
   {:keys [title description status issue-id tags parent]
    :or {status :todo}}]
  (assert title ":title is required")
  (let [tempid (d/tempid :db.part/task)
        db (d/db conn)
        status (keyword "task.status" (name status))
        issue-id (or issue-id (issue-id-from-title db title))
        tags (some->> tags
                      (map (partial hash-map :tag/name))
                      (set))
        parent (entity db parent)]
    @(d/transact conn
                 [(cond-> {:db/id tempid
                           :task/user (:db/id user)
                           :task/title title
                           :task/issue-id issue-id
                           :task/status status}
                    description (assoc :task/description description)
                    tags (assoc :task/tag tags)
                    parent (assoc :task/parent (:db/id parent)))
                  [:add-identity tempid :task/issue-id issue-id]])))

(defn set-parent
  "Sets parent of task to parent."
  [conn task parent]
  (let [db (d/db conn)]
    @(d/transact conn [[:db/add (:db/id (entity db task))
                        :task/parent (:db/id (entity db task))]])))

(defn set-status
  "Sets status of task to status, one of :todo :done :in-progress."
  [conn task status]
  (let [db (d/db conn)]
    @(d/transact conn [[:db/add (:db/id (entity db task))
                        :task/status (keyword "task.status" (name status))]])))

(defn add-tag
  "Adds a tag to task. Tag is the string name of the tag."
  [conn task tag]
  (let [db (d/db conn)]
    @(d/transact conn [{:db/id (:db/id (entity db task))
                        :task/tag #{{:tag/name tag}}}])))

(defn remove-tag
  "Removes tag from a task. Tag is the string name of the tag."
  [conn task tag]
  (let [db (d/db conn)]
    @(d/transact conn [[:db/retract (:db/id (entity db task))
                        :task/tag [:tag/name tag]]])))

(def task-pull-spec
  "Basic pull spec for task info."
  [:task/title :task/description {:task/status [:db/ident]}
   :task/issue-id {:task/tag [:tag/name]}])

(defn for-user
  "Returns all the top-level tasks for a user. If sub-tasks? is true,
   recursively returns the sub-tasks as well."
  ([db user]
   (for-user db user false))
  ([db user subtasks?]
   (let [pull-spec (cond-> task-pull-spec
                     subtasks? (conj {:task/_parent '...}))
         query '[:find (pull ?task spec)
                 :in $ ?user spec
                 :where [?task :task/user ?user]
                        (not [?task :task/parent _])]]
     (d/q query db (user/id db user) pull-spec))))

(defn search-title
  "Searches for tasks using a fulltext search on the title. Returns
   the matching tasks as well as the match score against the search
   string."
  [db user search]
  (d/q '[:find (pull ?task spec) ?score
         :in $ ?user ?search spec
         :where [(fulltext $ :task/title ?search) [[?task _ _ ?score]]]
                [?task :task/user ?user]]
       db (user/id db user) search task-pull-spec))
