(ns ^{:doc "A thin clojure wrapper over Zookeeper Queue Recipe"
      :author "Kiran Kulkarni <kk.questworld@gmail.com>"}
  com.helpshift.zk-queue
  (:require [clojure.string :as cs]
            [com.helpshift.zk-queue.utils :refer [to-byte-array]])
  (:import (org.apache.zookeeper ZooKeeper WatchedEvent Watcher)
           (java.util.concurrent CountDownLatch TimeUnit)
           org.apache.zookeeper.recipes.queue.DistributedQueue))

(defn- zk-node->zk-node-str
  [{:keys [host port]}]
  (when port
    host
    (str host ":" port)))


(defn- make-watcher
  ([handler]
     (reify Watcher
       (process [this event]
         (handler (when event
                    {:event-type (keyword (.. ^WatchedEvent event
                                              getType
                                              name))
                     :keeper-state (keyword (.. ^WatchedEvent event
                                                getState
                                                name))
                     :path (.getPath ^WatchedEvent event)}))))))


(defn zk-connect!
  [zk-nodes & {:keys [chroot zk-timeout-msec
                      conn-timeout]
               :or {chroot ""
                    zk-timeout-msec 5000
                    conn-timeout 30}}]
  {:pre [every? :host zk-nodes]}
  (let [zk-node-string (cs/join ","
                                (mapv zk-node->zk-node-str zk-nodes))
        connection-string (str zk-node-string chroot)
        latch (CountDownLatch. 1)
        unlatch-watcher (make-watcher (fn [event]
                                        (when (= (:keeper-state event)
                                                 :SyncConnected)
                                          (.countDown latch))))
        client (ZooKeeper. connection-string
                           zk-timeout-msec
                           unlatch-watcher)]
    (when (.await latch conn-timeout TimeUnit/SECONDS)
      client)))


(defn init-queue!
  "Give queue-path and zookeeper-nodes.
   It will establish connection to zookeeper-cluster and initialize Queue.
   Returns queue object"
  [zk-queue-path & zk-nodes]
  {:pre [(every? :host zk-nodes)
         (seq zk-queue-path)]}
  (if-let [zk-client (zk-connect! zk-nodes)]
    (DistributedQueue. zk-client
                       zk-queue-path
                       nil)
    (throw (ex-info "Failed to Connect to zookeeper"
                    {:nodes zk-nodes}))))


(defn close!
  "Closes connection with zookeeper associate with this queue."
  [zk-queue]
  (.close zk-queue))


(defn enqueue
  "Accepts data and enqueues it"
  [^DistributedQueue zk-queue data]
  (.offer zk-queue (to-byte-array data)))


(defn dequeue
  "Removes the head of the queue. Returns nil if queue is empty"
  [^DistributedQueue zk-queue]
  (.poll zk-queue))


(defn peek
  "Returns the data at first element, doesn't remove it.
   If queue is empty returns nil"
  [^DistributedQueue zk-queue]
  (.peek zk-queue))


(defn blocking-dequeue
  "Removes the head of the queue, blocks if queue is empty"
  [zk-queue]
  (.take zk-queue))
