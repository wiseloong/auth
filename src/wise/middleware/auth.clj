(ns wise.middleware.auth
    (:require [wise.auth.auth :refer [auth-backend]]
      [clojure.string :as cstr]
      [compojure.api.meta :refer [restructure-param]]
      [ring.util.http-response :refer [unauthorized]]
      [buddy.auth :refer [authenticated?]]
      [buddy.auth.middleware :refer [wrap-authentication]]))

;; 使用前需要实现 get-auth 方法, xx-fn获取用户的所有拥有权限的uri地址集合，入参m为{:id 用户id}
;; [wise.auth.middleware :refer [get-auth]]
;; (defmethod get-auth :default [m] (xx-fn m))
(defmulti get-auth identity)

(defn- pattern-urls [urls]
       (map #(re-pattern (str "^" % ".*")) urls))

(defn- permission? [request]
       (let [request-url (:uri request)
             user-id (-> request :identity :user :id)
             auth-urls (get-auth {:id user-id})
             auth-urls (remove cstr/blank? auth-urls)
             auth-urls (pattern-urls auth-urls)]
            (if (empty? auth-urls)
              false
              (boolean (some #(re-matches % request-url) auth-urls)))))

(defn authenticated-mw
      "判断是否有权限"
      [handler]
      (fn [request]
          (if (authenticated? request)
            (if (permission? request)
              (handler request)
              (unauthorized "没有权限！"))
            (unauthorized "没有登陆！"))))

(defn login-mw
      "判断是否登陆"
      [handler]
      (fn [request]
          (if (authenticated? request)
            (handler request)
            (unauthorized "没有登陆！"))))

(defn token-auth-mw
      "Middleware used on routes requiring token authentication"
      [handler]
      (-> handler
          (wrap-authentication auth-backend)))

(defmethod restructure-param :current-user
           [_ binding acc]
           (update-in acc [:letks] into [binding `(:user (:identity ~'+compojure-api-request+))]))
