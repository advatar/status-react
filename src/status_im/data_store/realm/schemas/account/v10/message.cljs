(ns status-im.data-store.realm.schemas.account.v10.message
  (:require [taoensso.timbre :as log]))

(def schema {:name       :message
             :primaryKey :message-id
             :properties {:message-id     :string
                          :from           :string
                          :to             {:type     :string
                                           :optional true}
                          :group-id       {:type     :string
                                           :optional true}
                          :content        :string          ;; TODO make it ArrayBuffer
                          :content-type   :string
                          :username       {:type     :string
                                           :optional true}
                          :timestamp      :int
                          :chat-id        {:type    :string
                                           :indexed true}
                          :outgoing       :bool
                          :retry-count    {:type    :int
                                           :default 0}
                          :same-author    :bool
                          :same-direction :bool
                          :preview        {:type     :string
                                           :optional true}
                          :message-type   {:type     :string
                                           :optional true}
                          :message-status {:type     :string
                                           :optional true}
                          :user-statuses  {:type       :list
                                           :objectType "user-status"}
                          :clock-value    {:type    :int
                                           :default 0}
                          :show?          {:type    :bool
                                           :default true}}})

(defn migration [_ new-realm]
  (log/debug "migrating message schema v10")
  (let [messages (.objects new-realm "message")]
    (dotimes [i (.-length messages)]
      (let [message (aget messages i)
            type    (aget message "message-type")]
        (when (or (= type "wallet-command")
                  (= type "wallet-request")
                  (= type "command")
                  (= type "command-request"))
          (.delete new-realm message))))))
