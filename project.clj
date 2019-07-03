(defproject wiseloong/auth "0.1.0-SNAPSHOT"
            :description "wiseloong-认证登陆组件"
            :url "www.wiseloong.com"
            :license {:name "wiseloong"}

            :dependencies [[wiseloong/router "0.1.0-SNAPSHOT"]
                           [reagent "0.8.1"]
                           [reagent-utils "0.2.1"]
                           [clj-http "3.9.1"]
                           [cheshire "5.8.0"]
                           [buddy "2.0.0"]
                           [ring-cors "0.1.12"]]

            :jar-exclusions [#"(?:^|\/)demo\/"]

            :profiles {:dev {:dependencies [[metosin/compojure-api "2.0.0-alpha25"]
                                            [org.clojure/clojure "1.9.0"]
                                            [org.clojure/clojurescript "1.10.439"]]}})
