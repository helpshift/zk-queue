(defproject com.helpshift/zk-queue "0.2.0"
  :description "Clojure wrapper over Queue Recipe for Zookeeper"
  :url "https://github.com/helpshift/zk-queue"
  :license {:name "Apache Licence"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.slf4j/slf4j-api "1.7.7"]
                 [org.apache.zookeeper/zookeeper "3.4.6"]]
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"])
